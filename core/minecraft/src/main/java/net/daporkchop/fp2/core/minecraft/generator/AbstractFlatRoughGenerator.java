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

package net.daporkchop.fp2.core.minecraft.generator;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import lombok.NonNull;
import lombok.ToString;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileData;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.server.gen.rough.AbstractRoughVoxelGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;

/**
 * @author DaPorkchop_
 */
//TODO: this doesn't support generating at lower detail levels
public abstract class AbstractFlatRoughGenerator extends AbstractRoughVoxelGenerator {
    protected final Int2ObjectSortedMap<TileData> transitionData;

    public AbstractFlatRoughGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        super(world, provider);
        this.transitionData = this.computeTransitionData(world);
    }

    protected abstract List<Layer> getOriginalLayers(IFarLevelServer world);

    protected Int2ObjectSortedMap<TileData> computeTransitionData(IFarLevelServer world) {
        List<Layer> layers = new ArrayList<>(this.getOriginalLayers(world));

        if (layers.isEmpty()) {
            return new Int2ObjectAVLTreeMap<>();
        }

        //ensure all the input layers have no empty space between them
        for (int lastY = layers.get(0).maxY, i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            Preconditions.checkState(layer.minY == lastY, "flat layers have a gap between them from y=%s to y=%s", lastY, layer.minY);
            lastY = layer.maxY;
        }

        IntAxisAlignedBB coordLimits = world.coordLimits();

        //clamp layers to their vanilla bounds
        for (Iterator<Layer> itr = layers.iterator(); itr.hasNext(); ) {
            Layer layer = itr.next();

            if (layer.minY == layer.maxY //layer is empty
                || layer.maxY <= coordLimits.minY() || layer.minY >= coordLimits.maxY()) { //layer is entirely outside coordinate limits
                itr.remove();
                continue;
            }

            if (layer.minY < coordLimits.minY()) { //shrink the layer upwards
                layer.setBounds(coordLimits.minY(), layer.maxY);
            }

            if (layer.maxY >= coordLimits.maxY()) { //shrink the layer downwards
                layer.setBounds(layer.minY, coordLimits.maxY());
            }

            if (layer.minY == layer.maxY) {
                itr.remove();
                continue;
            }
        }

        //add extra air layers so that all Y values from Integer.MIN_VALUE to Integer.MAX_VALUE are covered
        if (layers.get(0).minY > Integer.MIN_VALUE) {
            layers.add(0, new Layer(Integer.MIN_VALUE, layers.get(0).minY, 0, 0));
        }
        if (layers.get(layers.size() - 1).minY < Integer.MAX_VALUE) {
            layers.add(new Layer(layers.get(layers.size() - 1).maxY, Integer.MAX_VALUE, 0, 0));
        }

        //ensure all the layers have no empty space between them (again)
        for (int lastY = layers.get(0).maxY, i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            Preconditions.checkState(layer.minY == lastY, "flat layers have a gap between them from y=%s to y=%s", lastY, layer.minY);
            lastY = layer.maxY;
        }

        //spread sky light downwards
        int skyLight = BlockLevelConstants.MAX_LIGHT;
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);

            layer.incomingSkyLightFromAbove = skyLight;

            int fromLightOpacity = this.extendedStateRegistryData().lightOpacity(layer.state);
            if (skyLight != BlockLevelConstants.MAX_LIGHT || fromLightOpacity != 0) {
                fromLightOpacity = Math.max(fromLightOpacity, 1);
                for (int y = layer.maxY; y > layer.minY && skyLight != 0; y--) {
                    skyLight = Math.max(skyLight - fromLightOpacity, 0);
                }
            }
        }

        //TODO: spread block light

        Int2ObjectSortedMap<TileData> result = new Int2ObjectAVLTreeMap<>();
        for (int i = 1; i < layers.size(); i++) {
            Layer bottomLayer = layers.get(i - 1);
            Layer topLayer = layers.get(i);

            int bottomType = this.extendedStateRegistryData().type(bottomLayer.state);
            int topType = this.extendedStateRegistryData().type(topLayer.state);

            if (bottomType == topType) {
                continue;
            }

            TileData data = new TileData();
            data.x = data.y = data.z = 0;
            int blockLight;
            if (bottomType < topType) { //the face is facing towards negative coordinates
                data.edges = EDGE_DIR_NEGATIVE << 2;
                data.states[1] = topLayer.state;
                data.biome = topLayer.biome;
                blockLight = topLayer.incomingBlockLightFromBelow;
            } else {
                data.edges = EDGE_DIR_POSITIVE << 2;
                data.states[1] = bottomLayer.state;
                data.biome = bottomLayer.biome;
                blockLight = bottomLayer.incomingBlockLightFromAbove;
            }
            data.light = BlockLevelConstants.packLight(bottomLayer.incomingSkyLightFromAbove, blockLight);

            result.put(bottomLayer.maxY, data);
        }
        return result;
    }

    @ToString
    protected static final class Layer {
        public int minY; //inclusive
        public int maxY; //exclusive
        public final int state;
        public final int biome;

        private int incomingSkyLightFromAbove;
        private int incomingBlockLightFromAbove;
        private int incomingBlockLightFromBelow;

        public Layer(int minY, int maxY, int state, int biome) {
            this.setBounds(minY, maxY);
            this.state = state;
            this.biome = biome;
        }

        public void setBounds(int minY, int maxY) {
            Preconditions.checkArgument(minY <= maxY, "minY (%s) may not be greater than maxY (%s)", minY, maxY);
            this.minY = minY;
            this.maxY = maxY;
        }

        public long height() {
            return (long) this.maxY - this.minY;
        }
    }

    @Override
    public boolean canGenerate(@NonNull TilePos pos) {
        return pos.level() == 0;
    }

    @Override
    public void generate(@NonNull TilePos pos, @NonNull Tile tile) {
        for (Int2ObjectMap.Entry<TileData> entry : this.transitionData.subMap(pos.minBlockY(), pos.maxBlockY()).int2ObjectEntrySet()) {
            int y = (entry.getIntKey() - pos.minBlockY()) >> pos.level();
            TileData data = entry.getValue();
            for (int x = 0; x < T_VOXELS; x++) {
                for (int z = 0; z < T_VOXELS; z++) {
                    tile.set(x, y, z, data);
                }
            }
        }
    }
}
