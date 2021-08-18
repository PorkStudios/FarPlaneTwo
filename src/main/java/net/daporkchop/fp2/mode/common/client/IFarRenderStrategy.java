/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.common.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommand;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBufferFactory;
import net.daporkchop.fp2.client.gl.indirect.elements.DrawElementsIndirectCommand;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.index.AbstractRenderIndex;
import net.daporkchop.lib.unsafe.capability.Releasable;
import net.minecraft.util.BlockRenderLayer;

import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderStrategy<POS extends IFarPos, T extends IFarTile, C extends IDrawIndirectCommand> extends Releasable {
    //
    // BAKE METHODS
    //

    /**
     * Gets the positions of all the tiles whose baked contents are affected by the content of the given tile.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param srcPos the position of the tile
     * @return the positions of all the tiles whose contents are affected by the content of the given tile
     */
    Stream<POS> bakeOutputs(@NonNull POS srcPos);

    /**
     * Gets the positions of all the tiles needed to bake the given tile.
     * <p>
     * The returned {@link Stream} must be sequential and in sorted order!
     *
     * @param dstPos the position of the tile to generate
     * @return the positions of all the tiles needed to create the given tile
     */
    Stream<POS> bakeInputs(@NonNull POS dstPos);

    //
    // RENDER DATA METHODS
    //

    default AbstractRenderIndex<POS, ?, ?, ?> createRenderIndex(@NonNull IFarRenderMode<POS, T> mode) {
        throw new AbstractMethodError();
    }

    IDrawIndirectCommandBufferFactory<C> createCommandBufferFactory();

    /**
     * @return the size of the render tree data stored by this rendering strategy
     */
    long renderDataSize();

    void deleteRenderData(long renderData);

    void toDrawCommands(long renderData, @NonNull DrawElementsIndirectCommand[] commands);

    //
    // BAKE METHODS
    //

    boolean bake(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output);

    //
    // RENDER METHODS
    //

    void render(@NonNull AbstractRenderIndex<POS, ?, ?, ?> index, @NonNull BlockRenderLayer layer, boolean pre);
}
