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

package net.daporkchop.fp2.mode.heightmap.server.gen.rough;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import lombok.NonNull;
import net.daporkchop.fp2.mode.common.server.AbstractFarGenerator;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPieceBuilder;
import net.daporkchop.fp2.util.compat.cwg.CWGContext;
import net.daporkchop.fp2.util.compat.cwg.CWGHelper;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.compat.cwg.CWGContext.*;

/**
 * @author DaPorkchop_
 */
public class CWGHeightmapGenerator extends AbstractFarGenerator implements IFarGeneratorRough<HeightmapPos, HeightmapPieceBuilder> {
    protected Ref<CWGContext> ctx;

    @Override
    public void init(@NonNull WorldServer world) {
        super.init(world);
        this.ctx = ThreadRef.soft(() -> new CWGContext(world, 0, 2));
    }

    @Override
    public void generate(@NonNull HeightmapPos pos, @NonNull HeightmapPieceBuilder piece) {
        int level = pos.level();
        int baseX = pos.blockX();
        int baseZ = pos.blockZ();

        HeightmapData data = new HeightmapData();

        CWGContext ctx = this.ctx.get();
        ctx.init(baseX >> 4, 0, baseZ >> 4, level);

        int gbCacheStart = ctx.gbCacheStart();
        int gbCacheSize = ctx.gbCacheSize();
        Biome[] gbCache = ctx.gbCache();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int blockX = baseX + (x << level);
                int blockZ = baseZ + (z << level);

                int height = CWGHelper.getHeight(ctx, blockX, blockZ);
                double density = ctx.get(blockX, height, blockZ);

                double dx = ctx.get(blockX + 1, height, blockZ) - density;
                double dy = ctx.get(blockX, height + 1, blockZ) - density;
                double dz = ctx.get(blockX, height, blockZ + 1) - density;

                Biome biome = gbCache[((z >> 2) + gbCacheStart) * gbCacheSize + ((x >> 2) + gbCacheStart)];

                IBlockState state = Blocks.AIR.getDefaultState();
                for (IBiomeBlockReplacer replacer : ctx.replacersForBiome(biome)) {
                    state = replacer.getReplacedBlock(state, blockX, height, blockZ, dx, dy, dz, density);
                }

                data.height = height;
                data.state = Block.getStateId(state);
                data.light = packCombinedLight((height < this.seaLevel ? max(15 - (this.seaLevel - height) * 3, 0) : 15) << 20);
                data.biome = Biome.getIdForBiome(biome);
                data.waterLight = packCombinedLight(15 << 20);
                data.waterBiome = Biome.getIdForBiome(biome);

                piece.set(x, z, data);
            }
        }
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }

    @Override
    public boolean isLowResolutionInaccurate() {
        return true;
    }
}
