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

package net.daporkchop.fp2.core.mode.heightmap.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;

/**
 * @author DaPorkchop_
 */
public class VanillaHeightmapGenerator extends AbstractExactHeightmapGenerator {
    protected static final int Y_LIMIT = 0; //TODO: don't hardcode this to 0 (because 1.17)

    public VanillaHeightmapGenerator(@NonNull IFarWorldServer world, @NonNull IFarTileProvider<HeightmapPos, HeightmapTile> provider) {
        super(world, provider);
    }

    @Override
    protected void computeElevations(@NonNull FBlockWorld world, @NonNull int[] elevations, int blockX, int blockZ) {
        throw new UnsupportedOperationException(); //TODO
        /*int y = world.getTopBlockY(blockX, blockZ);
        if (y < Y_LIMIT) { //there are no blocks in this column, therefore nothing to do
            return;
        }

        //vanilla worlds have the convenient trait that everything in the column is already generated and loaded, which allows us to simply iterate from top to bottom

        int usedExtraLayers = 0;
        for (int prevState = -1; y >= Y_LIMIT; y--) {
            int state = world.getState(blockX, y, blockZ);

            if (state == prevState) { //skip duplicate block states
                continue;
            }
            prevState = state;

            if (state.isOpaqueCube()) { //solid block: save elevation and immediately return, no other layers will be visible
                elevations[DEFAULT_LAYER] = y;
                return;
            } else if (state.getBlock() == Blocks.WATER) { //water: remember the first Y value we meet it at, discard all other occurrences
                if (elevations[WATER_LAYER] == Integer.MIN_VALUE) {
                    elevations[WATER_LAYER] = y;
                }
            } else if (usedExtraLayers < EXTRA_LAYERS.length && (state.getMaterial().isSolid() || state.getMaterial().isLiquid())) { //all other blocks: put the first few on extra layers, discard everything else
                elevations[EXTRA_LAYERS[usedExtraLayers++]] = y;
            }
        }*/
    }
}
