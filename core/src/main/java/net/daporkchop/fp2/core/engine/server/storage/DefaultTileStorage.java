/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.fp2.core.engine.server.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.TilePosCodec;
import net.daporkchop.fp2.core.engine.api.server.storage.FTileStorage;
import net.daporkchop.fp2.core.engine.server.TileProvider;
import net.daporkchop.fp2.core.engine.tile.ITileHandle;
import net.daporkchop.fp2.core.engine.tile.ITileMetadata;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.engine.tile.TileSnapshot;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.core.util.datastructure.java.list.ArraySliceAsList;
import net.daporkchop.fp2.core.util.datastructure.java.list.ListUtils;
import net.daporkchop.lib.common.annotation.BorrowOwnership;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.engine.tile.ITileMetadata.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Default implementation of {@link FTileStorage}.
 *
 * @author DaPorkchop_
 */
public class DefaultTileStorage implements FTileStorage {
    protected static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    protected static final String COLUMN_NAME_DIRTY_TIMESTAMP = "dirty_timestamp";
    protected static final String COLUMN_NAME_DATA = "data";

    /**
     * If {@code true}, storage operations will use on-heap {@code byte[]}s instead of off-heap memory.
     * <p>
     * This will cause a lot of additional GC churn, but is far less error-prone.
     */
    protected static final boolean HEAP_BUFFERS = false;

    /**
     * If {@code true}, bulk storage operations inherited from {@link FTileStorage} will use optimized implementations instead of
     * simply processing each tile individually.
     */
    protected static final boolean OPTIMIZED_BULK_OPS = true;

    protected static final Cached<ArrayAllocator<ByteBuffer[]>> ALLOC_BYTEBUFFER_ARRAY = GlobalAllocators.getArrayAllocatorForComponentType(ByteBuffer.class);

    static {
        //we're reading longs directly out of byte arrays
        PUnsafe.requireTightlyPackedByteArrays();
    }

    protected static long fromByteArrayLongLE(@NonNull byte[] src) {
        checkArg(src.length == Long.BYTES, "incorrectly sized array: expected " + Long.BYTES + ", contains %d", src.length);
        return PUnsafe.getUnalignedLongLE(src, PUnsafe.arrayByteElementOffset(0));
    }

    protected static byte[] toByteArrayLongLE(long val) {
        byte[] dst = new byte[Long.BYTES];
        PUnsafe.putUnalignedLongLE(dst, PUnsafe.arrayByteElementOffset(0), val);
        return dst;
    }

    protected static long readTimestampFromNullableByteArray(byte[] src) {
        return src != null
                ? fromByteArrayLongLE(src)
                : TIMESTAMP_BLANK; //key isn't present in db -> the tile doesn't exist
    }

    protected static long readTimestampFromDirectMemory(@BorrowOwnership long addr, int size) {
        if (size < 0) {
            return TIMESTAMP_BLANK; //key isn't present in db -> the tile doesn't exist
        } else {
            checkArg(size == Long.BYTES, "incorrectly sized array: expected " + Long.BYTES + ", contains %d", size);
            return PUnsafe.getUnalignedLongLE(addr);
        }
    }

    public static FStorageItemFactory<FTileStorage> factory(@NonNull TileProvider tileProvider) {
        //build the token for the current tile storage
        byte[] currentToken;
        {
            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
            try {
                buf.writeCharSequence(DefaultTileStorage.class.getTypeName(), StandardCharsets.UTF_8); //class name
                buf.writeByte(0);

                buf.writeIntLE(0); //tile storage version
                buf.writeIntLE(EngineConstants.STORAGE_VERSION); //tile format version

                //copy buffer contents to a byte[]
                currentToken = new byte[buf.readableBytes()];
                buf.readBytes(currentToken);
            } finally {
                buf.release();
            }
        }

        return new FStorageItemFactory<FTileStorage>() {
            @Override
            public ConfigurationResult configure(@NonNull ConfigurationCallback callback) {
                int expectedPositionSize = TilePosCodec.size();

                //register the columns
                callback.registerColumn(COLUMN_NAME_TIMESTAMP, //tile timestamp
                        FStorageColumnHintsInternal.builder()
                                .expectedAverageKeySize(expectedPositionSize)
                                .expectedAverageValueSize(LONG_SIZE)
                                .build(),
                        ColumnRequirement.FAIL_IF_MISSING);
                callback.registerColumn(COLUMN_NAME_DIRTY_TIMESTAMP, //tile dirty timestamp
                        FStorageColumnHintsInternal.builder()
                                .expectedAverageKeySize(expectedPositionSize)
                                .expectedAverageValueSize(LONG_SIZE)
                                .build(),
                        ColumnRequirement.FAIL_IF_MISSING);
                callback.registerColumn(COLUMN_NAME_DATA, //tile data
                        FStorageColumnHintsInternal.builder()
                                .expectedAverageKeySize(expectedPositionSize)
                                .expectedAverageValueSize(4096)
                                .build(),
                        ColumnRequirement.FAIL_IF_MISSING);

                //write the token
                callback.setToken(currentToken);

                Optional<byte[]> existingToken = callback.getExistingToken();
                return !existingToken.isPresent() || Arrays.equals(currentToken, existingToken.get())
                        ? ConfigurationResult.CREATE_IF_MISSING //item doesn't exist or has a matching token, keep it as-is
                        : ConfigurationResult.DELETE_EXISTING_AND_CREATE; //item exists but has a mismatched token, re-create it
            }

            @Override
            public FTileStorage create(@NonNull FStorageInternal storageInternal) {
                return new DefaultTileStorage(tileProvider, storageInternal);
            }
        };
    }

    protected final TileProvider tileProvider;

    protected final FStorageInternal storageInternal;

    protected final FStorageColumn columnTimestamp;
    protected final FStorageColumn columnDirtyTimestamp;
    protected final FStorageColumn columnData;

    //immutable two-element list containing the following: [this.columnTimestamp, this.columnData]
    protected final List<FStorageColumn> listColumns_Timestamp_Data;

    //immutable two-element list containing the following: [this.columnTimestamp, this.columnDirtyTimestamp]
    protected final List<FStorageColumn> listColumns_Timestamp_DirtyTimestamp;

    protected final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    protected final LoadingCache<TilePos, ITileHandle> handleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(fp2().globalConfig().performance().terrainThreads())
            .weakValues()
            .build(CacheLoader.from(pos -> new DefaultTileHandle(pos, this)));

    protected DefaultTileStorage(@NonNull TileProvider tileProvider, @NonNull FStorageInternal storageInternal) {
        this.tileProvider = tileProvider;

        this.storageInternal = storageInternal;

        Map<String, FStorageColumn> columns = storageInternal.getColumns();
        this.columnTimestamp = Objects.requireNonNull(columns.get(COLUMN_NAME_TIMESTAMP), COLUMN_NAME_TIMESTAMP);
        this.columnDirtyTimestamp = Objects.requireNonNull(columns.get(COLUMN_NAME_DIRTY_TIMESTAMP), COLUMN_NAME_DIRTY_TIMESTAMP);
        this.columnData = Objects.requireNonNull(columns.get(COLUMN_NAME_DATA), COLUMN_NAME_DATA);

        this.listColumns_Timestamp_Data = ListUtils.immutableListOf(this.columnTimestamp, this.columnData);
        this.listColumns_Timestamp_DirtyTimestamp = ListUtils.immutableListOf(this.columnTimestamp, this.columnDirtyTimestamp);
    }

    @Override
    public void close() throws FStorageException {
        this.storageInternal.close();
    }

    @Override
    public ITileHandle handleFor(@NonNull TilePos pos) {
        return this.handleCache.getUnchecked(pos);
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public List<ITileSnapshot> multiSnapshot(@NonNull List<TilePos> positions) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiSnapshot(positions);
        }

        int length = positions.size();

        if (length == 0) { //nothing to do!
            return ImmutableList.of();
        }

        //read from storage
        return this.storageInternal.readGet(access -> {
            if (HEAP_BUFFERS) {
                ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();
                int[] sizes = intArrayAlloc.atLeast(multiplyExact(length, 2));

                try {
                    List<byte[]> keyBytes = positions.stream().map(TilePosCodec::toByteArray).collect(Collectors.toList());

                    List<byte[]> timestampsTilesBytes = access.multiGet(
                            ListUtils.repeatSequence(this.listColumns_Timestamp_Data, length),
                            ListUtils.repeatElements(keyBytes, 2));

                    //read the resulting tiles into heap objects
                    List<ITileSnapshot> out = new ArrayList<>(length);
                    for (int i = 0, j = 0; i < length; i++) {
                        byte[] timestampBytes = timestampsTilesBytes.get(j++);
                        byte[] tileBytes = timestampsTilesBytes.get(j++);

                        if (timestampBytes == null) { //timestamp wasn't present in the db, meaning the tile doesn't exist
                            out.add(null);
                        } else { //the tile exists
                            out.add(TileSnapshot.of(positions.get(i), readTimestampFromNullableByteArray(timestampBytes), tileBytes));
                        }
                    }
                    return out;
                } finally {
                    intArrayAlloc.release(sizes);
                }
            }

            final int tileMaxSize = toIntExact(Tile.CODEC.maxSize());

            ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();

            //helper variables for array sizes

            //allocate temporary arrays for the value sizes
            int[] sizes = intArrayAlloc.atLeast(multiplyExact(length, 2));

            //allocate an array of direct ByteBuffers
            ByteBuffer[] keyBuffers = new ByteBuffer[length];
            ByteBuffer[] timestampTileBuffers = new ByteBuffer[length * 2];

            //allocate temporary off-heap buffer for serialized keys, tile timestamps and tile data
            final long bufferKeysOffset = 0L;
            final int bufferKeysStride = TilePosCodec.size();
            final long bufferKeysSize = length * (long) bufferKeysStride;

            final long bufferTimestampsOffset = bufferKeysOffset + bufferKeysSize;
            final int bufferTimestampsStride = Long.BYTES;
            final long bufferTimestampsSize = length * (long) bufferTimestampsStride;

            final long bufferTilesOffset = bufferTimestampsOffset + bufferTimestampsSize;
            final int bufferTilesStride = tileMaxSize;
            final long bufferTilesSize = length * (long) bufferTilesStride;

            final long bufferSize = bufferTilesOffset + bufferTilesSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize keys and configure key buffers
                for (int i = 0; i < length; i++) {
                    long addr = buffer + bufferKeysOffset + i * (long) bufferKeysStride;
                    TilePosCodec.store(positions.get(i), addr);

                    keyBuffers[i] = DirectBufferHackery.wrapByte(addr, bufferKeysStride);
                }

                //configure buffers for timestamps and data
                for (int i = 0, j = 0; i < length; i++) {
                    long timestampAddr = buffer + bufferTimestampsOffset + i * (long) bufferTimestampsStride;
                    timestampTileBuffers[j++] = DirectBufferHackery.wrapByte(timestampAddr, bufferTimestampsStride);

                    long tileAddr = buffer + bufferTilesOffset + i * (long) bufferTilesStride;
                    timestampTileBuffers[j++] = DirectBufferHackery.wrapByte(tileAddr, bufferTilesStride);
                }

                //read all timestamps and values simultaneously
                checkState(access.multiGet(
                        ListUtils.repeatSequence(this.listColumns_Timestamp_Data, length),
                        ListUtils.repeatElements(ArraySliceAsList.wrap(keyBuffers, 0, length), 2),
                        ArraySliceAsList.wrap(timestampTileBuffers, 0, length * 2),
                        sizes));

                //read the resulting tiles into heap objects
                List<ITileSnapshot> out = new ArrayList<>(length);
                for (int i = 0, j = 0; i < length; i++) {
                    int timestampSize = sizes[j++];
                    int tileSize = sizes[j++];

                    long timestampAddr = buffer + bufferTimestampsOffset + i * (long) bufferTimestampsStride;
                    long tileAddr = buffer + bufferTilesOffset + i * (long) bufferTilesStride;

                    if (timestampSize < 0) { //timestamp wasn't present in the db, meaning the tile doesn't exist
                        out.add(null);
                    } else { //the tile exists
                        out.add(TileSnapshot.of(positions.get(i),
                                readTimestampFromDirectMemory(timestampAddr, timestampSize),
                                tileAddr, tileSize));
                    }
                }
                return out;
            } finally {
                PUnsafe.freeMemory(buffer);
                Arrays.fill(timestampTileBuffers, null);
                Arrays.fill(keyBuffers, null);
                intArrayAlloc.release(sizes);
            }
        });
    }

    @SneakyThrows(FStorageException.class)
    private LongList genericMultiGetTimestamps(@NonNull List<TilePos> positions, @NonNull FStorageColumn column) {
        int length = positions.size();
        LongList out = new LongArrayList(length);

        if (length == 0) { //nothing to do!
            return out;
        }

        //read all timestamps simultaneously
        List<byte[]> timestamps = this.storageInternal.readGet(access -> access.multiGet(
                ListUtils.repeat(column, length),
                positions.stream().map(TilePosCodec::toByteArray).collect(Collectors.toList())));

        //deserialize timestamp values
        timestamps.forEach(timestamp -> out.add(readTimestampFromNullableByteArray(timestamp)));
        return out;
    }

    @Override
    public LongList multiTimestamp(@NonNull List<TilePos> positions) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiTimestamp(positions);
        }

        return this.genericMultiGetTimestamps(positions, this.columnTimestamp);
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public BitSet multiSet(@NonNull List<TilePos> positions, @NonNull List<ITileMetadata> metadatas, @NonNull List<Tile> tiles) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiSet(positions, metadatas, tiles);
        }

        if (positions.isEmpty()) { //nothing to do!
            return new BitSet();
        }

        BitSet result = this.storageInternal.transactAtomicGet(access -> {
            int length = positions.size();

            //TODO: optimized off-heap implementation?

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(TilePosCodec::toByteArray).toArray(byte[][]::new);

            //read timestamps and dirty timestamps
            List<byte[]> timestampsAndDirtyTimestamps = access.multiGet(
                    ListUtils.repeatSequence(this.listColumns_Timestamp_DirtyTimestamp, length),
                    ListUtils.repeatElements(Arrays.asList(allKeyBytes), 2));

            ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
            try {
                BitSet out = new BitSet(length);

                for (int i = 0; i < length; i++) {
                    byte[] timestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 0);
                    long timestamp = readTimestampFromNullableByteArray(timestampBytes);

                    byte[] dirtyTimestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 1);
                    long dirtyTimestamp = readTimestampFromNullableByteArray(dirtyTimestampBytes);

                    byte[] keyBytes = allKeyBytes[i];
                    ITileMetadata metadata = metadatas.get(i);
                    Tile tile = tiles.get(i);

                    if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                        //skip this position
                        continue;
                    }

                    //the tile is about to be modified
                    out.set(i);

                    //store new timestamp in db
                    access.put(this.columnTimestamp, keyBytes, toByteArrayLongLE(metadata.timestamp()));

                    //clear dirty timestamp if needed
                    if (metadata.timestamp() >= dirtyTimestamp) {
                        access.delete(this.columnDirtyTimestamp, keyBytes);
                    }

                    //encode tile and store it in db
                    buf.clear();
                    if (tile.write(buf)) { //the tile was empty, remove it from the db!
                        access.delete(this.columnData, keyBytes);
                    } else { //the tile was non-empty, store it in the db
                        access.put(this.columnData, keyBytes, Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.writerIndex()));
                    }
                }

                return out;
            } finally {
                buf.release();
            }
        });

        if (!result.isEmpty()) { //non-empty bitset indicates at least some positions were modified, so we should commit the transaction and notify listeners
            this.listeners.forEach(listener -> listener.tilesChanged(result.stream().mapToObj(positions::get)));
        }

        return result;
    }

    @Override
    public LongList multiDirtyTimestamp(@NonNull List<TilePos> positions) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiDirtyTimestamp(positions);
        }

        return this.genericMultiGetTimestamps(positions, this.columnDirtyTimestamp);
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public BitSet multiMarkDirty(@NonNull List<TilePos> positions, long dirtyTimestamp) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiMarkDirty(positions, dirtyTimestamp);
        }

        //we'll lock all of the positions at once, compare each one and then modify as many as needed. the logic here is identical to
        //  DefaultTileHandle#markDirty(long), but in bulk, and since DefaultTileHandle doesn't cache anything internally, we don't need to get any instances
        //  of DefaultTileHandle or do any additional synchronization.

        int length = positions.size();
        if (length == 0) { //nothing to do!
            return new BitSet();
        }

        BitSet result = this.storageInternal.transactAtomicGet(access -> {
            //TODO: optimized off-heap implementation?

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(TilePosCodec::toByteArray).toArray(byte[][]::new);

            //read timestamps and dirty timestamps
            List<byte[]> timestampsAndDirtyTimestamps = access.multiGet(
                    ListUtils.repeatSequence(this.listColumns_Timestamp_DirtyTimestamp, length),
                    ListUtils.repeatElements(Arrays.asList(allKeyBytes), 2));

            //iterate through positions, updating the dirty timestamps as needed
            byte[] dirtyTimestampArray = toByteArrayLongLE(dirtyTimestamp);

            BitSet out = new BitSet(length);
            for (int i = 0, j = 0; i < length; i++) {
                byte[] timestampBytes = timestampsAndDirtyTimestamps.get(j++);
                long timestamp = readTimestampFromNullableByteArray(timestampBytes);

                byte[] dirtyTimestampBytes = timestampsAndDirtyTimestamps.get(j++);
                long existingDirtyTimestamp = readTimestampFromNullableByteArray(dirtyTimestampBytes);

                if (timestamp == TIMESTAMP_BLANK //the tile doesn't exist, so we can't mark it as dirty
                    || dirtyTimestamp <= timestamp
                    || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                    //skip this position
                    continue;
                }

                //store new dirty timestamp in db
                access.put(this.columnDirtyTimestamp, allKeyBytes[i], dirtyTimestampArray);

                //save the position to return it as part of the result stream
                out.set(i);
            }
            return out;
        });

        if (!result.isEmpty()) { //non-empty bitset indicates at least some positions were modified, so we should commit the transaction and notify listeners
            this.listeners.forEach(listener -> listener.tilesDirty(result.stream().mapToObj(positions::get)));
        }
        return result;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public BitSet multiClearDirty(@NonNull List<TilePos> positions) {
        if (!OPTIMIZED_BULK_OPS) {
            return FTileStorage.super.multiClearDirty(positions);
        }

        int length = positions.size();
        if (length == 0) { //nothing to do!
            return new BitSet();
        }

        return this.storageInternal.transactAtomicGet(access -> {
            //TODO: optimized off-heap implementation?

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(TilePosCodec::toByteArray).toArray(byte[][]::new);

            //read all dirty timestamps simultaneously
            List<byte[]> dirtyTimestamps = access.multiGet(
                    ListUtils.repeat(this.columnDirtyTimestamp, length),
                    Arrays.asList(allKeyBytes));

            BitSet out = new BitSet(length);
            for (int i = 0; i < length; i++) {
                if (dirtyTimestamps.get(i) != null) { //the tile is dirty
                    out.set(i);

                    //clear dirty timestamp in db
                    access.delete(this.columnDirtyTimestamp, allKeyBytes[i]);
                }
            }
            return out;
        });
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public void clear() {
        this.storageInternal.clearColumns(ImmutableList.of(this.columnData, this.columnTimestamp, this.columnDirtyTimestamp));
    }

    @Override
    public void addListener(@NonNull Listener listener) {
        checkState(this.listeners.add(listener), "listener %s already added?!?", listener);
    }

    @Override
    public void removeListener(@NonNull Listener listener) {
        checkState(this.listeners.remove(listener), "listener %s not present?!?", listener);
    }
}
