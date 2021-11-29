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

package net.daporkchop.fp2.mode.heightmap.server.gen.rough;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;

import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Rough heightmap generator for the vanilla superflat world type.
 *
 * @author DaPorkchop_
 */
public class FlatHeightmapGenerator extends AbstractRoughHeightmapGenerator {
    protected final HeightmapData[] datas = new HeightmapData[MAX_LAYERS];

    public FlatHeightmapGenerator(@NonNull IFarWorldServer world) {
        super(world);

        FlatGeneratorInfo generatorInfo = this.loadGeneratorInfoFromWorld(world);

        List<FlatLayerInfo> layers = generatorInfo.getFlatLayers();
        int topOpaqueLayerIndex = -1;
        int topWaterLayerIndex = -1;
        IntList topTransparentLayers = new IntArrayList();

        for (int i = layers.size() - 1; i >= 0; i--) {
            FlatLayerInfo layer = layers.get(i);

            int blockType = blockType(layer.getLayerMaterial());
            if (topWaterLayerIndex < 0 && layer.getLayerMaterial().getBlock() == Blocks.WATER) {
                topWaterLayerIndex = i;
            } else if (blockType == BLOCK_TYPE_OPAQUE) {
                topOpaqueLayerIndex = i;
                break;
            } else if (blockType == BLOCK_TYPE_TRANSPARENT && topTransparentLayers.size() < EXTRA_LAYERS.length) { //any additional layers will be discarded
                topTransparentLayers.add(i);
            }
        }

        if (topOpaqueLayerIndex >= 0) {
            this.datas[DEFAULT_LAYER] = this.toHeightmapData(generatorInfo, topOpaqueLayerIndex, DEFAULT_LAYER);
        }
        if (topWaterLayerIndex >= 0) {
            this.datas[WATER_LAYER] = this.toHeightmapData(generatorInfo, topWaterLayerIndex, WATER_LAYER);
        }
        for (int i = 0; i < topTransparentLayers.size(); i++) {
            this.datas[EXTRA_LAYERS[i]] = this.toHeightmapData(generatorInfo, topTransparentLayers.getInt(i), DEFAULT_LAYER);
        }
    }

    protected FlatGeneratorInfo loadGeneratorInfoFromWorld(@NonNull WorldServer world) {
        FlatGeneratorInfo generatorInfo = FlatGeneratorInfo.createFlatGeneratorFromString(world.getWorldInfo().getGeneratorOptions());
        generatorInfo.updateLayers();
        return generatorInfo;
    }

    protected HeightmapData toHeightmapData(@NonNull FlatGeneratorInfo generatorInfo, int layerIndex, int secondaryConnection) {
        HeightmapData data = new HeightmapData();
        FlatLayerInfo layer = generatorInfo.getFlatLayers().get(layerIndex);

        data.state = layer.getLayerMaterial();
        data.biome = FastRegistry.getBiome(generatorInfo.getBiome());
        data.height_int = layer.getMinY() + layer.getLayerCount();
        data.light = this.getLightAtLayer(generatorInfo.getFlatLayers(), layerIndex);
        data.secondaryConnection = secondaryConnection;

        return data;
    }

    @SuppressWarnings("deprecation")
    protected int getLightAtLayer(@NonNull List<FlatLayerInfo> layers, int layerIndex) {
        int skyLight = 15; //sky light is initially 15 since we start at the surface
        int blockLight = 0;

        for (int i = layers.size() - 1; i > layerIndex; i--) {
            FlatLayerInfo layer = layers.get(i);

            IBlockState state = layer.getLayerMaterial();
            long opacity = max(1L, (long) state.getLightOpacity() * layer.getLayerCount());
            int brightness = state.getLightValue();

            //reduce sky light based on layer opacity
            skyLight = (int) max(0L, skyLight - opacity);
            //reduce block light based on layer opacity, or set it to the own block's brightness level if higher
            blockLight = (int) max(brightness, blockLight - opacity);
        }

        //if the layer itself is brighter, use that as the brightness level
        blockLight = max(layers.get(layerIndex).getLayerMaterial().getLightValue(), blockLight);

        return packLight(skyLight, blockLight);
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }

    @Override
    public void generate(@NonNull HeightmapPos pos, @NonNull HeightmapTile tile) {
        for (int layer = 0; layer < MAX_LAYERS; layer++) {
            HeightmapData data = this.datas[layer];
            if (data == null) { //the layer is unused, skip it
                continue;
            }

            //set the data at this layer across all block columns
            for (int x = 0; x < T_VOXELS; x++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    tile.setLayer(x, z, layer, data);
                }
            }
        }
    }
}
