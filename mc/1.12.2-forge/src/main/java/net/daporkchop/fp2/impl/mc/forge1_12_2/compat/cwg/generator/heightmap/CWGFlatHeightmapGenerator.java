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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator.heightmap;

import io.github.opencubicchunks.cubicchunks.cubicgen.preset.FlatGeneratorSettings;
import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.generator.heightmap.FlatHeightmapGenerator;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Rough heightmap generator for CubicWorldGen's "FlatCubic" world type.
 *
 * @author DaPorkchop_
 */
public class CWGFlatHeightmapGenerator extends FlatHeightmapGenerator {
    public CWGFlatHeightmapGenerator(@NonNull IFarWorldServer world) {
        super(world);
    }

    @Override
    protected FlatGeneratorInfo loadGeneratorInfoFromWorld(@NonNull WorldServer world) {
        FlatGeneratorSettings settings = FlatGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions());
        checkState(settings.version == 1, "unsupported FlatCubic preset version: %d (expected: 1)", settings.version);

        FlatGeneratorInfo generatorInfo = new FlatGeneratorInfo();

        //translate FlatCubic generator options to vanilla format
        generatorInfo.setBiome(this.registry.biome2id(Biomes.PLAINS)); //FlatCubic always uses the same biome
        settings.layers.values().stream().map(flatLayer -> {
            IBlockState state = flatLayer.blockState.getBlockState();
            FlatLayerInfo layerInfo = new FlatLayerInfo(3, flatLayer.toY - flatLayer.fromY, state.getBlock(), state.getBlock().getMetaFromState(state));
            layerInfo.setMinY(flatLayer.fromY);
            return layerInfo;
        }).forEach(generatorInfo.getFlatLayers()::add);

        return generatorInfo;
    }
}
