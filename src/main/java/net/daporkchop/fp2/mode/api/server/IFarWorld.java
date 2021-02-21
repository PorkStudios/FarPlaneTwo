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

package net.daporkchop.fp2.mode.api.server;

import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.minecraft.world.WorldServer;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author DaPorkchop_
 */
public interface IFarWorld<POS extends IFarPos, P extends IFarPiece> extends Closeable {
    WorldServer world();

    AsyncBlockAccess blockAccess();

    /**
     * Gets the {@link IFarPiece} at the given position.
     * <p>
     * If the piece is already loaded, it will be returned. Otherwise, it will be queued for loading and this method will return {@code null}.
     *
     * @param pos the position of the piece to get
     * @return the piece, or {@code null} if it isn't loaded yet
     */
    Compressed<POS, P> getPieceLazy(@NonNull POS pos);

    /**
     * Fired whenever a block state changes.
     *
     * @param x the X coordinate of the block that changed
     * @param y the Y coordinate of the block that changed
     * @param z the Z coordinate of the block that changed
     */
    void blockChanged(int x, int y, int z);

    /**
     * @return the (possibly {@code null}) {@link IFarGeneratorRough} used for rough generation of far terrain
     */
    IFarGeneratorRough<POS, P> generatorRough();

    /**
     * @return the {@link IFarGeneratorExact} used for block-accurate generation of far terrain
     */
    IFarGeneratorExact<POS, P> generatorExact();

    /**
     * @return the {@link IFarScaler} used for downscaling the far terrain pieces
     */
    IFarScaler<POS, P> scaler();

    /**
     * @return the {@link IFarStorage} used for persistence of far terrain pieces
     */
    IFarStorage<POS, P> storage();

    /**
     * @return the {@link IFarRenderMode} that this world is used by
     */
    IFarRenderMode<POS, P> mode();

    void save();

    @Override
    void close() throws IOException;
}
