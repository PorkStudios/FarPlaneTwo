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

package net.daporkchop.fp2.strategy.common;

import lombok.NonNull;
import net.minecraft.world.WorldServer;

/**
 * @author DaPorkchop_
 */
public interface IFarWorld {
    WorldServer world();

    /**
     * Gets the {@link IFarPiece} at the given position.
     *
     * @param pos the position of the piece to get
     * @return the piece
     */
    IFarPiece getPieceBlocking(@NonNull IFarPiecePos pos);

    /**
     * Gets the {@link IFarPiece} at the given position.
     * <p>
     * If the piece is already loaded, it is returned. Otherwise this method returns {@code null} and will be queued for loading asynchronously.
     *
     * @param pos the position of the piece to get
     * @return the piece, or {@code null} if it isn't already loaded
     */
    IFarPiece getPieceNowOrLoadAsync(@NonNull IFarPiecePos pos);

    /**
     * Fired whenever a block state changes.
     *
     * @param x the X coordinate of the block that changed
     * @param y the Y coordinate of the block that changed
     * @param z the Z coordinate of the block that changed
     */
    void blockChanged(int x, int y, int z);
}
