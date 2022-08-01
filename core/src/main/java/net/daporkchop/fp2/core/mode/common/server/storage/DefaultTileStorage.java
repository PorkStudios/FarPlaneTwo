/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common.server.storage;

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
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarPosCodec;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.storage.FTileStorage;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.core.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.core.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.core.util.datastructure.java.list.ArraySliceAsList;
import net.daporkchop.fp2.core.util.datastructure.java.list.ListUtils;
import net.daporkchop.fp2.core.util.recycler.Recycler;
import net.daporkchop.fp2.core.util.recycler.SimpleRecycler;
import net.daporkchop.fp2.core.util.serialization.variable.IVariableSizeRecyclingCodec;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.common.util.PArrays;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.mode.api.tile.ITileMetadata.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Default implementation of {@link FTileStorage}.
 *
 * @author DaPorkchop_
 */
public class DefaultTileStorage<POS extends IFarPos, T extends IFarTile> implements FTileStorage<POS, T> {
    protected static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    protected static final String COLUMN_NAME_DIRTY_TIMESTAMP = "dirty_timestamp";
    protected static final String COLUMN_NAME_DATA = "data";

    protected static final Cached<ArrayAllocator<ByteBuffer[]>> ALLOC_BYTEBUFFER_ARRAY = GlobalAllocators.getArrayAllocatorForComponentType(ByteBuffer.class);
    protected static final Cached<Recycler<ByteBuffer>> FAKE_DIRECT_BYTEBUFFER_RECYCLER = Cached.threadLocal(() -> new SimpleRecycler<ByteBuffer>() {
        @Override
        protected ByteBuffer allocate0() {
            return DirectBufferHackery.emptyByte();
        }

        @Override
        protected void reset0(ByteBuffer value) {
            //no-op
        }
    }, ReferenceStrength.WEAK);

    protected static long readLongLE(@NonNull byte[] src) {
        checkRangeLen(src.length, 0, Long.BYTES);
        return readLongLE(src, PUnsafe.arrayByteElementOffset(0));
    }

    protected static long readLongLE(@NonNull byte[] src, int index) {
        checkRangeLen(src.length, index, Long.BYTES);
        return readLongLE(src, PUnsafe.arrayByteElementOffset(index));
    }

    protected static long readLongLE(long addr) {
        return readLongLE(null, addr);
    }

    protected static long readLongLE(Object base, long offset) {
        long val = PUnsafe.getLong(base, offset);
        return PlatformInfo.IS_BIG_ENDIAN ? Long.reverseBytes(val) : val;
    }

    protected static byte[] writeLongLE(long val) {
        byte[] dst = new byte[Long.BYTES];
        writeLongLE(dst, 0, val);
        return dst;
    }

    protected static void writeLongLE(@NonNull byte[] dst, int index, long val) {
        checkRangeLen(dst.length, index, Long.BYTES);

        PUnsafe.putLong(dst, PUnsafe.ARRAY_BYTE_BASE_OFFSET + index, PlatformInfo.IS_BIG_ENDIAN ? Long.reverseBytes(val) : val);
    }

    public static <POS extends IFarPos, T extends IFarTile> FStorageItemFactory<FTileStorage<POS, T>> factory(@NonNull AbstractFarTileProvider<POS, T> tileProvider) {
        //build the token for the current tile storage
        byte[] currentToken;
        {
            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
            try {
                buf.writeCharSequence(DefaultTileStorage.class.getTypeName(), StandardCharsets.UTF_8); //class name
                buf.writeByte(0);

                buf.writeIntLE(0); //tile storage version
                buf.writeIntLE(tileProvider.mode().storageVersion()); //tile format version

                //copy buffer contents to a byte[]
                currentToken = new byte[buf.readableBytes()];
                buf.readBytes(currentToken);
            } finally {
                buf.release();
            }
        }

        return new FStorageItemFactory<FTileStorage<POS, T>>() {
            @Override
            public ConfigurationResult configure(@NonNull ConfigurationCallback callback) {
                int expectedPositionSize = toIntExact(tileProvider.mode().posCodec().size());

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
            public FTileStorage<POS, T> create(@NonNull FStorageInternal storageInternal) {
                return new DefaultTileStorage<>(tileProvider, storageInternal);
            }
        };
    }

    protected final AbstractFarTileProvider<POS, T> tileProvider;
    protected final IFarPosCodec<POS> posCodec;
    protected final IVariableSizeRecyclingCodec<T> tileCodec;
    protected final Cached<Recycler<byte[]>> posBufferRecyclerCache;

    protected final FStorageInternal storageInternal;

    protected final FStorageColumn columnTimestamp;
    protected final FStorageColumn columnDirtyTimestamp;
    protected final FStorageColumn columnData;

    protected final Set<Listener<POS, T>> listeners = new CopyOnWriteArraySet<>();

    protected final LoadingCache<POS, ITileHandle<POS, T>> handleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(fp2().globalConfig().performance().terrainThreads())
            .weakValues()
            .build(CacheLoader.from(pos -> new DefaultTileHandle<>(pos, this)));

    protected DefaultTileStorage(@NonNull AbstractFarTileProvider<POS, T> tileProvider, @NonNull FStorageInternal storageInternal) {
        this.tileProvider = tileProvider;
        this.posCodec = tileProvider.mode().posCodec();
        this.tileCodec = tileProvider.mode().tileCodec();

        { //create recycler for temporary serialized position buffers
            int posSize = toIntExact(this.posCodec.size());
            Supplier<byte[]> factory = () -> new byte[posSize];
            this.posBufferRecyclerCache = Cached.threadLocal(() -> SimpleRecycler.withFactory(factory), ReferenceStrength.WEAK);
        }

        this.storageInternal = storageInternal;

        Map<String, FStorageColumn> columns = storageInternal.getColumns();
        this.columnTimestamp = Objects.requireNonNull(columns.get(COLUMN_NAME_TIMESTAMP), COLUMN_NAME_TIMESTAMP);
        this.columnDirtyTimestamp = Objects.requireNonNull(columns.get(COLUMN_NAME_DIRTY_TIMESTAMP), COLUMN_NAME_DIRTY_TIMESTAMP);
        this.columnData = Objects.requireNonNull(columns.get(COLUMN_NAME_DATA), COLUMN_NAME_DATA);
    }

    @Override
    public void close() throws FStorageException {
        this.storageInternal.close();
    }

    @Override
    public ITileHandle<POS, T> handleFor(@NonNull POS pos) {
        return this.handleCache.getUnchecked(pos);
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public List<ITileSnapshot<POS, T>> multiSnapshot(@NonNull List<POS> positions) {
        int length = positions.size();

        if (length == 0) { //nothing to do!
            return ImmutableList.of();
        }

        //read from storage
        List<byte[]> valueBytes = this.storageInternal.readGet(access -> {
            int posSize = toIntExact(this.posCodec.size());
            int tileMaxSize = toIntExact(this.tileCodec.maxSize());

            ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();
            ArrayAllocator<ByteBuffer[]> byteBufferArrayAlloc = ALLOC_BYTEBUFFER_ARRAY.get();
            Recycler<ByteBuffer> byteBufferRecycler = FAKE_DIRECT_BYTEBUFFER_RECYCLER.get();

            //helper variables for array sizes

            //allocate temporary arrays for the value sizes
            int[] sizes = intArrayAlloc.atLeast(multiplyExact(length, 2));

            //allocate an array of direct ByteBuffers
            int tmpBuffersKeysOffset = 0;
            int tmpBuffersTimestampsTilesOffset = tmpBuffersKeysOffset + length;
            int tmpBufferCount = tmpBuffersTimestampsTilesOffset + multiplyExact(length, 2);
            ByteBuffer[] tmpBuffers = byteBufferRecycler.allocate(tmpBufferCount, byteBufferArrayAlloc);

            //allocate temporary off-heap buffer for serialized keys, tile timestamps and tile data
            long bufferKeysOffset = 0L;
            long bufferTimestampsTilesOffset = addExact(bufferKeysOffset, multiplyExact(length, posSize));
            long buffer = PUnsafe.allocateMemory(addExact(bufferTimestampsTilesOffset, addExact(multiplyExact(length, (long) tileMaxSize), multiplyExact(length, (long) LONG_SIZE))));
            try {
                long bufferKeys = buffer + bufferKeysOffset;
                long bufferTimestampsTiles = buffer + bufferTimestampsTilesOffset;

                { //serialize keys
                    int i = 0;
                    for (long addr = bufferKeys; i < length; i++, addr += posSize) {
                        this.posCodec.store(positions.get(i), addr);

                        //configure buffer
                        DirectBufferHackery.reset(tmpBuffers[tmpBuffersKeysOffset + i], addr, posSize);
                    }
                }

                { //configure buffers for timestamps and data
                    int i = 0;
                    for (long addr = bufferTimestampsTiles; i < length;) {
                        DirectBufferHackery.reset(tmpBuffers[tmpBuffersTimestampsTilesOffset + i++], addr, LONG_SIZE);
                        addr += LONG_SIZE;
                        DirectBufferHackery.reset(tmpBuffers[tmpBuffersTimestampsTilesOffset + i++], addr, LONG_SIZE);
                        addr += tileMaxSize;
                    }
                }

                //read all timestamps and values simultaneously
                checkState(access.multiGet(
                        ListUtils.repeatSequence(ImmutableList.of(this.columnTimestamp, this.columnData), length),
                        ListUtils.repeatElements(ArraySliceAsList.wrap(tmpBuffers, tmpBuffersKeysOffset, length), 2),
                        ArraySliceAsList.wrap(tmpBuffers, tmpBuffersTimestampsTilesOffset, length * 2),
                        sizes));

                //TODO: actually do something with the retrieved data
                throw new UnsupportedOperationException();
            } finally {
                PUnsafe.freeMemory(buffer);
                byteBufferRecycler.release(tmpBuffers, tmpBufferCount, byteBufferArrayAlloc);
                intArrayAlloc.release(sizes);
            }
        });

        //construct snapshots
        List<ITileSnapshot<POS, T>> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            byte[] timestampBytes = valueBytes.get((i << 1) + 0);
            byte[] tileBytes = valueBytes.get((i << 1) + 1);

            out.add(timestampBytes != null
                    ? new TileSnapshot<>(positions.get(i), readLongLE(timestampBytes), tileBytes)
                    : null);
        }
        return out;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public LongList multiTimestamp(@NonNull List<POS> positions) {
        int length = positions.size();
        LongList out = new LongArrayList(length);

        if (length == 0) { //nothing to do!
            return out;
        }

        //read all timestamps simultaneously
        List<byte[]> timestamps = this.storageInternal.readGet(access -> access.multiGet(
                Arrays.asList(PArrays.filled(length, FStorageColumn.class, this.columnTimestamp)),
                positions.stream().map(POS::toBytes).collect(Collectors.toList())));

        //deserialize timestamp values
        timestamps.forEach(timestamp -> out.add(timestamp != null ? readLongLE(timestamp) : TIMESTAMP_BLANK));
        return out;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public BitSet multiSet(@NonNull List<POS> positions, @NonNull List<ITileMetadata> metadatas, @NonNull List<T> tiles) {
        if (positions.isEmpty()) { //nothing to do!
            return new BitSet();
        }

        BitSet result = this.storageInternal.transactAtomicGet(access -> {
            int length = positions.size();

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            //read timestamps and dirty timestamps
            List<byte[]> timestampsAndDirtyTimestamps;
            {
                //prepare parameter arrays for MultiGet
                int doubleLength = multiplyExact(length, 2);
                FStorageColumn[] columns = new FStorageColumn[doubleLength];
                byte[][] keys = new byte[doubleLength][];

                for (int i = 0; i < doubleLength; ) {
                    byte[] keyBytes = positions.get(i >> 1).toBytes();

                    columns[i] = this.columnTimestamp;
                    keys[i++] = keyBytes;
                    columns[i] = this.columnDirtyTimestamp;
                    keys[i++] = keyBytes;
                }

                timestampsAndDirtyTimestamps = access.multiGet(Arrays.asList(columns), Arrays.asList(keys));
            }

            ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
            try {
                BitSet out = new BitSet(length);

                for (int i = 0; i < length; i++) {
                    byte[] timestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 0);
                    long timestamp = timestampBytes != null
                            ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                            : TIMESTAMP_BLANK;

                    byte[] dirtyTimestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 1);
                    long dirtyTimestamp = dirtyTimestampBytes != null
                            ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                            : TIMESTAMP_BLANK;

                    byte[] keyBytes = allKeyBytes[i];
                    ITileMetadata metadata = metadatas.get(i);
                    T tile = tiles.get(i);

                    if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                        //skip this position
                        continue;
                    }

                    //the tile is about to be modified
                    out.set(i);

                    //store new timestamp in db
                    access.put(this.columnTimestamp, keyBytes, writeLongLE(metadata.timestamp()));

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
    @SneakyThrows(FStorageException.class)
    public LongList multiDirtyTimestamp(@NonNull List<POS> positions) {
        int length = positions.size();
        LongList out = new LongArrayList(length);

        if (length == 0) { //nothing to do!
            return out;
        }

        //read all dirty timestamps simultaneously
        List<byte[]> dirtyTimestamps = this.storageInternal.readGet(access -> access.multiGet(
                Arrays.asList(PArrays.filled(length, FStorageColumn.class, this.columnDirtyTimestamp)),
                positions.stream().map(POS::toBytes).collect(Collectors.toList())));

        //deserialize dirty timestamp values
        dirtyTimestamps.forEach(dirtyTimestamp -> out.add(dirtyTimestamp != null ? readLongLE(dirtyTimestamp) : TIMESTAMP_BLANK));
        return out;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public BitSet multiMarkDirty(@NonNull List<POS> positions, long dirtyTimestamp) {
        //we'll lock all of the positions at once, compare each one and then modify as many as needed. the logic here is identical to
        //  DefaultTileHandle#markDirty(long), but in bulk, and since DefaultTileHandle doesn't cache anything internally, we don't need to get any instances
        //  of DefaultTileHandle or do any additional synchronization.

        if (positions.isEmpty()) { //nothing to do!
            return new BitSet();
        }

        BitSet result = this.storageInternal.transactAtomicGet(access -> {
            int length = positions.size();

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            //read timestamps and dirty timestamps
            List<byte[]> timestampsAndDirtyTimestamps;
            {
                //prepare parameter arrays for MultiGet
                int doubleLength = multiplyExact(length, 2);
                FStorageColumn[] columns = new FStorageColumn[doubleLength];
                byte[][] keys = new byte[doubleLength][];

                for (int i = 0; i < doubleLength; ) {
                    byte[] keyBytes = positions.get(i >> 1).toBytes();

                    columns[i] = this.columnTimestamp;
                    keys[i++] = keyBytes;
                    columns[i] = this.columnDirtyTimestamp;
                    keys[i++] = keyBytes;
                }

                timestampsAndDirtyTimestamps = access.multiGet(Arrays.asList(columns), Arrays.asList(keys));
            }

            //iterate through positions, updating the dirty timestamps as needed
            byte[] dirtyTimestampArray = new byte[Long.BYTES];

            BitSet out = new BitSet(length);
            for (int i = 0; i < length; i++) {
                byte[] timestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 0);
                long timestamp = timestampBytes != null
                        ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                byte[] dirtyTimestampBytes = timestampsAndDirtyTimestamps.get((i << 1) + 1);
                long existingDirtyTimestamp = dirtyTimestampBytes != null
                        ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                if (timestamp == TIMESTAMP_BLANK //the tile doesn't exist, so we can't mark it as dirty
                    || dirtyTimestamp <= timestamp
                    || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                    //skip this position
                    continue;
                }

                //store new dirty timestamp in db
                writeLongLE(dirtyTimestampArray, 0, dirtyTimestamp);
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
    public BitSet multiClearDirty(@NonNull List<POS> positions) {
        if (positions.isEmpty()) { //nothing to do!
            return new BitSet();
        }

        return this.storageInternal.transactAtomicGet(access -> {
            int length = positions.size();

            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            //read all dirty timestamps simultaneously
            List<byte[]> dirtyTimestamps = access.multiGet(
                    Arrays.asList(PArrays.filled(length, FStorageColumn.class, this.columnDirtyTimestamp)),
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
    public void addListener(@NonNull Listener<POS, T> listener) {
        checkState(this.listeners.add(listener), "listener %s already added?!?", listener);
    }

    @Override
    public void removeListener(@NonNull Listener<POS, T> listener) {
        checkState(this.listeners.remove(listener), "listener %s not present?!?", listener);
    }
}
