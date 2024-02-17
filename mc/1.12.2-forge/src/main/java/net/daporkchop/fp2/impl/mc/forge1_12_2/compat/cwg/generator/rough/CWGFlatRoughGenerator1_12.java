/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator.rough;

import io.github.opencubicchunks.cubicchunks.cubicgen.preset.FlatGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.preset.FlatLayer;
import lombok.NonNull;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.minecraft.generator.AbstractFlatRoughGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.minecraft.init.Biomes;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author DaPorkchop_
 */
public class CWGFlatRoughGenerator1_12 extends AbstractFlatRoughGenerator {
    public CWGFlatRoughGenerator1_12(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        super(world, provider);
    }

    @Override
    protected List<Layer> getOriginalLayers(IFarLevelServer world) {
        FlatGeneratorSettings cwgSettings = FlatGeneratorSettings.fromJson(((WorldServer) world.implLevel()).getWorldInfo().getGeneratorOptions());

        int biome = this.registry().biome2id(Biomes.PLAINS); //this is hardcoded in FlatCubicWorldType

        //convert CWG FlatLayers into vanilla FlatLayerInfos
        List<Layer> layers = new ArrayList<>(cwgSettings.layers.size());
        for (FlatLayer flatLayer : cwgSettings.layers.values()) {
            layers.add(new Layer(flatLayer.fromY, flatLayer.toY, this.registry().state2id(flatLayer.blockState.getBlockState()), biome));
        }

        if (!layers.isEmpty()) { //insert air in the gaps between layers
            for (int i = layers.size() - 1; i > 0; i--) {
                Layer topLayer = layers.get(i);
                Layer bottomLayer = layers.get(i - 1);
                if (topLayer.minY > bottomLayer.maxY) {
                    layers.add(i, new Layer(bottomLayer.maxY, topLayer.minY, 0, biome));
                }
            }
        }

        return layers;
    }
}
