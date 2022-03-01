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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.IOWorker;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IMixinIOWorker1_16 {
    /**
     * Equivalent to {@link IOWorker#load(ChunkPos)}, but doesn't block the calling thread.
     */
    CompletableFuture<CompoundNBT> fp2_IOWorker_loadFuture(ChunkPos pos);

    /**
     * Gets a {@link Stream} over the positions of all regions that exist.
     * <p>
     * Note that the returned {@link Stream} <strong>must</strong> be closed!
     *
     * @return a {@link Stream} over the positions of all regions that exist
     */
    Stream<ChunkPos> fp2_IOWorker_listChunks() throws IOException;

    /**
     * Gets a {@link Stream} over the positions of all regions that exist, along with the associated NBT data.
     * <p>
     * Note that the returned {@link Stream} <strong>must</strong> be closed!
     *
     * @return a {@link Stream} over the positions of all regions that exist
     */
    default Stream<Map.Entry<ChunkPos, CompoundNBT>> fp2_IOWorker_listChunksWithData() throws IOException {
        return this.fp2_IOWorker_listChunks()
                .map(pos -> new AbstractMap.SimpleEntry<>(pos, this.fp2_IOWorker_loadFuture(pos).join()));
    }
}
