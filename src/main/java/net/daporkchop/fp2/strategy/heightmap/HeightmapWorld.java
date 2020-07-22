/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.strategy.heightmap;

import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomCubicWorldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.daporkchop.fp2.strategy.heightmap.gen.HeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.gen.cc.CCHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.gen.cwg.CWGHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.gen.vanilla.VanillaHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.scale.HeightmapScaler;
import net.daporkchop.fp2.strategy.heightmap.scale.HeightmapScalerMax;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.lib.binary.netty.PUnpooled;
import net.daporkchop.lib.common.function.io.IORunnable;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.compression.zstd.ZstdDeflater;
import net.daporkchop.lib.compression.zstd.ZstdInflater;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.minecraft.world.WorldServer;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.server.ServerConstants.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Representation of a world used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@Accessors(fluent = true)
public class HeightmapWorld implements IFarWorld {
    private static final Ref<ZstdDeflater> DEFLATER_CACHE = ThreadRef.soft(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()));
    private static final Ref<ZstdInflater> INFLATER_CACHE = ThreadRef.soft(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()));

    @Getter
    protected final WorldServer world;
    @Getter
    protected final HeightmapGenerator generator;
    @Getter
    protected final HeightmapScaler scaler;

    protected final Path storageRoot;

    protected final Map<HeightmapPos, CompletableFuture<HeightmapPiece>> cache = new ObjObjConcurrentHashMap<>();

    public HeightmapWorld(@NonNull WorldServer world) {
        this.world = world;
        if (Constants.isCubicWorld(world)) { //TODO: this
            if (Constants.CWG && world.getWorldType() instanceof CustomCubicWorldType) {
                this.generator = new CWGHeightmapGenerator();
            } else {
                this.generator = new CCHeightmapGenerator();
            }
        } else {
            this.generator = new VanillaHeightmapGenerator();
        }
        this.generator.init(world);

        this.scaler = new HeightmapScalerMax();

        this.storageRoot = world.getChunkSaveLocation().toPath().resolve("fp2/" + RenderStrategy.HEIGHTMAP.name().toLowerCase());
    }

    @Override
    public HeightmapPiece getPieceBlocking(@NonNull IFarPos posIn) {
        HeightmapPos pos = (HeightmapPos) posIn;
        return this.loadFullPiece(pos).join();
    }

    @Override
    public HeightmapPiece getPieceNowOrLoadAsync(@NonNull IFarPos posIn) {
        HeightmapPos pos = (HeightmapPos) posIn;
        return this.loadFullPiece(pos).getNow(null);
    }

    @Override
    public void blockChanged(int x, int y, int z) {
        //TODO: this whole thing is not really concurrency-safe
        /*this.loadFullPiece(key)
                .thenApplyAsync(piece -> {
                    if (this.dirtyChunks.replace(key, 1, 2)) {
                        CachedBlockAccess world = ((CachedBlockAccess.Holder) this.world).fp2_cachedBlockAccess();
                        try {
                            this.generator.generateExact(world, piece);
                            this.savePiece(piece);
                        } finally {
                            this.dirtyChunks.remove(key, 2);
                        }
                    }
                    return piece;
                }, GENERATION_WORKERS)
                .thenAcceptAsync(((IFarContext) this.world).fp2_tracker()::pieceChanged);*/
    }

    protected CompletableFuture<HeightmapPiece> loadFullPiece(HeightmapPos key) {
        return this.cache.computeIfAbsent(key, pos -> {
            CompletableFuture<HeightmapPiece> future = new CompletableFuture<>();
            IO_WORKERS.submit(() -> {
                //load piece if possible
                {
                    HeightmapPiece piece = this.tryLoadPiece(pos);
                    if (piece != null) {
                        future.complete(piece);
                        return;
                    }
                }

                if (pos.level() == 0) { //root level, generate piece
                    GENERATION_WORKERS.submit(() -> {
                        try {
                            future.complete(this.generatePiece(pos.x(), pos.z()));
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                } else { //higher level, generate all four pieces below it
                    CompletableFuture<HeightmapPiece>[] belowFutures = uncheckedCast(new CompletableFuture[]{
                            this.loadFullPiece(new HeightmapPos(pos.x() << 1, pos.z() << 1, pos.level() - 1)),
                            this.loadFullPiece(new HeightmapPos((pos.x() << 1), (pos.z() << 1) + 1, pos.level() - 1)),
                            this.loadFullPiece(new HeightmapPos((pos.x() << 1) + 1, (pos.z() << 1), pos.level() - 1)),
                            this.loadFullPiece(new HeightmapPos((pos.x() << 1) + 1, (pos.z() << 1) + 1, pos.level() - 1))
                    });

                    CompletableFuture.completedFuture(new HeightmapPiece[4])
                            .thenCombine(belowFutures[0], (a, p) -> {
                                a[0] = p;
                                return a;
                            })
                            .thenCombine(belowFutures[1], (a, p) -> {
                                a[1] = p;
                                return a;
                            })
                            .thenCombine(belowFutures[2], (a, p) -> {
                                a[2] = p;
                                return a;
                            })
                            .thenCombine(belowFutures[3], (a, p) -> {
                                a[3] = p;
                                return a;
                            })
                            .thenApplyAsync(a -> {
                                HeightmapPiece piece = new HeightmapPiece(pos.x(), pos.z(), pos.level());
                                this.scaler.scale(a, piece);
                                try {
                                    return piece;
                                } finally {
                                    this.savePiece(piece);
                                }
                            }, GENERATION_WORKERS)
                            .whenComplete((v, t) -> {
                                if (t != null) {
                                    future.completeExceptionally(t);
                                } else {
                                    future.complete(v);
                                }
                            });
                }
            });
            return future;
        });
    }

    protected HeightmapPiece tryLoadPiece(HeightmapPos pos) {
        try {
            Path path = this.storagePath(pos);
            if (!Config.debug.disablePersistence && Files.exists(path) && Files.isRegularFile(path)) {
                try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                    ByteBuf input = PUnpooled.wrap(channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size()), true);
                    if (input.readableBytes() >= 8 && input.readInt() == HEIGHTMAP_STORAGE_VERSION) {
                        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(input.readInt());
                        try {
                            checkState(INFLATER_CACHE.get().decompress(input, buf));
                            return new HeightmapPiece(buf);
                        } finally {
                            buf.release();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected HeightmapPiece generatePiece(int x, int z) {
        CachedBlockAccess world = ((CachedBlockAccess.Holder) this.world).fp2_cachedBlockAccess();
        HeightmapPiece piece = new HeightmapPiece(x, z, 0);
        this.generator.generateRough(world, piece);

        try {
            return piece;
        } finally {
            this.savePiece(piece);
        }
    }

    protected void savePiece(@NonNull HeightmapPiece piece) {
        if (Config.debug.disablePersistence || !piece.isDirty()) {
            return;
        }
        IO_WORKERS.submit((IORunnable) () -> {
            if (!piece.clearDirty()) {
                return;
            }

            Path cachePath = this.storagePath(piece.pos());
            Files.createDirectories(cachePath.getParent());
            ByteBuf raw = PooledByteBufAllocator.DEFAULT.ioBuffer();
            ByteBuf compressed = PooledByteBufAllocator.DEFAULT.ioBuffer();
            try (FileChannel channel = FileChannel.open(cachePath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                piece.write(raw);

                compressed.ensureWritable(8 + Zstd.PROVIDER.compressBound(raw.readableBytes()));
                compressed.writeInt(HEIGHTMAP_STORAGE_VERSION).writeInt(raw.readableBytes());
                checkState(DEFLATER_CACHE.get().compress(raw, compressed));
                compressed.readBytes(channel, compressed.readableBytes());
                checkState(!compressed.isReadable());
            } finally {
                raw.release();
                compressed.release();
            }
        });
    }

    protected Path storagePath(@NonNull HeightmapPos pos) {
        return this.storageRoot.resolve(PStrings.fastFormat("%d/%d.%d.fp2", pos.level(), pos.x(), pos.z()));
    }
}
