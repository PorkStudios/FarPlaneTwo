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
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.world.chunk.storage;

import com.mojang.datafixers.util.Either;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.chunk.storage.ATIOWorker__Entry1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage.IMixinIOWorker1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage.IMixinRegionFileCache1_16;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.IOWorker;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Mixin(IOWorker.class)
public abstract class MixinIOWorker1_16 implements IMixinIOWorker1_16 {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private Map<ChunkPos, ATIOWorker__Entry1_16> pendingWrites;

    @Shadow
    @Final
    private RegionFileCache storage;

    @Shadow
    protected abstract <T> CompletableFuture<T> submitTask(Supplier<Either<T, Exception>> p_235975_1_);

    @Shadow
    public abstract CompletableFuture<Void> synchronize();

    @Override
    public CompletableFuture<CompoundNBT> fp2_IOWorker_loadFuture(ChunkPos pos) {
        //exactly the same as vanilla IOWorker#load, but doesn't have to join the task
        return this.submitTask(() -> {
            ATIOWorker__Entry1_16 entry = this.pendingWrites.get(pos);
            if (entry != null) {
                return Either.left(entry.getData());
            } else {
                try {
                    return Either.left(this.storage.read(pos));
                } catch (Exception e) {
                    LOGGER.warn("Failed to read chunk {}", pos, e);
                    return Either.right(e);
                }
            }
        });
    }

    @Override
    public Stream<ChunkPos> fp2_IOWorker_listChunks() throws IOException {
        //synchronize first to ensure all cached writes are visible on disk
        //  i could also just include the keys from pendingWrites in the stream and allow duplicates, but i'm too lazy to solve this same problem yet again, especially
        //  when i'll probably just end up replacing the whole thing with regionlib in the not too distant future
        return this.synchronize().thenCompose(unused -> this.submitTask(() -> {
            try (Stream<Vec2i> regionPositions = PorkUtil.<IMixinRegionFileCache1_16>uncheckedCast(this.storage).fp2_RegionFileCache_listRegions()) {
                return Either.left(regionPositions
                        .map(regionPos -> this.submitTask(() -> {
                            try {
                                return Either.left(PorkUtil.<IMixinRegionFileCache1_16>uncheckedCast(this.storage).fp2_RegionFileCache_listChunksInRegion(regionPos));
                            } catch (Exception e) {
                                LOGGER.warn("Failed to list chunks in region {}", regionPos, e);
                                return Either.right(e);
                            }
                        }))
                        .map(CompletableFuture::join)
                        .flatMap(List::stream));
            } catch (Exception e) {
                LOGGER.warn("Failed to list chunks", e);
                return Either.right(e);
            }
        })).join();
    }
}
