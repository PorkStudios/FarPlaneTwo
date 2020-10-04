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

package net.daporkchop.fp2.mode.api.server;

import lombok.NonNull;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.api.CompressedPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Handles reading and writing of far terrain data.
 *
 * @author DaPorkchop_
 */
public interface IFarStorage<POS extends IFarPos, P extends IFarPiece, B extends IFarPieceBuilder> extends Closeable {
    /**
     * @return the root directory for piece storage
     */
    File storageRoot();

    /**
     * Loads the piece at the given position.
     *
     * @param pos the position of the piece to load
     * @return the loaded piece, or {@code null} if it doesn't exist
     */
    CompressedPiece<POS, P, B> load(@NonNull POS pos);

    /**
     * Stores the given piece at the given position, atomically replacing any existing piece.
     *
     * @param pos   the position to save the data at
     * @param piece the piece to save
     */
    void store(@NonNull POS pos, @NonNull CompressedPiece<POS, P, B> piece);

    /**
     * @return the {@link RenderMode} that this storage is used for
     */
    RenderMode mode();

    /**
     * Closes this storage.
     * <p>
     * If write operations are queued, this method will block until they are completed.
     */
    @Override
    void close() throws IOException;
}
