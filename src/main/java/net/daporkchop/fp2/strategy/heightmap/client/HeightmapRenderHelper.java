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

package net.daporkchop.fp2.strategy.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class HeightmapRenderHelper {
    public static final int HEIGHTMAP_RENDER_SIZE = T_VOXELS * T_VOXELS * IVEC4_SIZE;

    public static ByteBuf bakePiece(@NonNull HeightmapPiece piece) {
        ByteBuf buffer = Constants.allocateByteBuf(HEIGHTMAP_RENDER_SIZE);

        final int blockX = piece.pos().blockX();
        final int blockZ = piece.pos().blockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int height = piece.height(x, z);
                int block = piece.block(x, z);

                pos.setPos(blockX + (x << piece.level()), height, blockZ + (z << piece.level()));
                biomeAccess.biome(Biome.getBiome(piece.biome(x, z), Biomes.PLAINS));

                buffer.writeInt(height)
                        .writeInt((piece.light(x, z) << 24) | block)
                        .writeInt(Minecraft.getMinecraft().getBlockColors().colorMultiplier(Block.getStateById(block), biomeAccess, pos, 0));

                biomeAccess.biome(Biome.getBiome(piece.waterBiome(x, z), Biomes.PLAINS));
                int waterColor = Minecraft.getMinecraft().getBlockColors().colorMultiplier(Blocks.WATER.getDefaultState(), biomeAccess, pos, 0);
                buffer.writeInt((waterColor & 0x00FFFFFF) | (piece.waterLight(x, z) << 24));
            }
        }
        return buffer;
    }
}
