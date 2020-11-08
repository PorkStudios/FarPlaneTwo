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

package net.daporkchop.fp2.mode.heightmap.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.piece.IFarPieceData;
import net.daporkchop.fp2.mode.api.server.gen.IFarAssembler;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator implements IFarGeneratorExact<HeightmapPos, HeightmapPiece, IFarPieceData> {
    @Override
    public final void generatePieceData(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos pos, IFarPieceData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final long generate(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos pos, @NonNull HeightmapPiece piece, IFarPieceData data, IFarAssembler<IFarPieceData, HeightmapPiece> assembler) {
        this.generateHeightmap(world, pos, piece);
        return 0L;
    }

    protected abstract void generateHeightmap(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos pos, @NonNull HeightmapPiece piece);
}
