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

package net.daporkchop.fp2.mode.api.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.minecraft.world.WorldServer;

/**
 * Extracts height and color information from a world for use by a rendering mode.
 * <p>
 * Once initialized, instances of this class are expected to be safely usable by multiple concurrent threads.
 *
 * @author DaPorkchop_
 */
public interface IFarGeneratorRough<POS extends IFarPos, P extends IFarPiece, D extends IFarPieceData> extends IFarGenerator {
    @Override
    void init(@NonNull WorldServer world);

    /**
     * Generates the piece data for the piece at the given position.
     * <p>
     * This method may be safely implemented as a no-op for render modes that do not utilize the piece data system.
     *
     * @param pos  the position of the piece to generate
     * @param data the piece data to generate
     */
    void generatePieceData(@NonNull POS pos, @NonNull D data);

    /**
     * Generates a rough estimate of the terrain in the given piece.
     *
     * @param pos       the position of the piece to generate
     * @param piece     the piece to generate
     * @param data      the piece data to generate. May be ignored by render modes that do not utilize the piece data system
     * @param assembler an {@link IFarAssembler} which may be used to assemble the piece based on the piece data. May be ignored by render modes that
     *                  do not utilize the piece data system
     * @return the extra data to be saved with the piece
     */
    long generate(@NonNull POS pos, @NonNull P piece, @NonNull D data, @NonNull IFarAssembler<D, P> assembler);

    /**
     * @return whether or not this generator can generate pieces at low resolution
     */
    boolean supportsLowResolution();

    /**
     * @return whether or not low-resolution pieces are inaccurate
     */
    boolean isLowResolutionInaccurate();
}
