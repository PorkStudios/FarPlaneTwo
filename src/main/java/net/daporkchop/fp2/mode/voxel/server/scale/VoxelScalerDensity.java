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

package net.daporkchop.fp2.mode.voxel.server.scale;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.scale.IFarScaler;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPieceBuilder;

import java.util.Arrays;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class VoxelScalerDensity implements IFarScaler<VoxelPos, VoxelPiece, VoxelPieceBuilder> {
    @Override
    public Stream<VoxelPos> outputs(@NonNull VoxelPos srcPos) {
        return Stream.of(srcPos.up()); //TODO: fix this
    }

    @Override
    public Stream<VoxelPos> inputs(@NonNull VoxelPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");

        int x = dstPos.x() << 1;
        int y = dstPos.y() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level() - 1;

        final int size = 3;
        VoxelPos[] positions = new VoxelPos[size * size * size];
        int i = 0;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    positions[i++] = new VoxelPos(x + dx, y + dy, z + dz, level);
                }
            }
        }
        return Arrays.stream(positions);
    }

    @Override
    public void scale(@NonNull VoxelPiece[] srcs, @NonNull VoxelPieceBuilder dst) {
    }
}
