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

package net.daporkchop.fp2.core.mode.heightmap.server.scale;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.core.mode.heightmap.util.HeightmapPosArrayList;
import net.daporkchop.fp2.core.server.world.IFarLevelServer;

import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Scales heightmap tiles by copying the sample with the greatest height deviation in each 2x2 square of high-detail samples.
 * <p>
 * This probably isn't the absolute best solution, but it's pretty fast and does a good job of preserving detail in bumpy regions.
 *
 * @author DaPorkchop_
 */
public class HeightmapScalerMinMax extends AbstractFarGenerator<HeightmapPos, HeightmapTile> implements IFarScaler<HeightmapPos, HeightmapTile> {
    public HeightmapScalerMinMax(@NonNull IFarLevelServer world, @NonNull IFarTileProvider<HeightmapPos, HeightmapTile> provider) {
        super(world, provider);
    }

    @Override
    public List<HeightmapPos> outputs(@NonNull HeightmapPos srcPos) {
        return ImmutableList.of(srcPos.up());
    }

    @Override
    public List<HeightmapPos> inputs(@NonNull HeightmapPos dstPos) {
        checkArg(dstPos.level() > 0, "cannot generate inputs for level 0!");

        int x = dstPos.x() << 1;
        int z = dstPos.z() << 1;
        int level = dstPos.level() - 1;

        List<HeightmapPos> out = new HeightmapPosArrayList(4);
        out.add(new HeightmapPos(level - 1, x, z));
        out.add(new HeightmapPos(level - 1, x, z + 1));
        out.add(new HeightmapPos(level - 1, x + 1, z));
        out.add(new HeightmapPos(level - 1, x + 1, z + 1));
        return out;
    }

    @Override
    public long scale(@NonNull HeightmapTile[] srcs, @NonNull HeightmapTile dst) {
        HeightmapData data = new HeightmapData();

        for (int subX = 0; subX < 2; subX++) {
            for (int subZ = 0; subZ < 2; subZ++) {
                HeightmapTile src = srcs[subX * 2 + subZ];
                if (src == null) { //null source tiles are treated as effectively empty
                    continue;
                }

                int baseX = subX * (HT_VOXELS >> 1);
                int baseZ = subZ * (HT_VOXELS >> 1);

                for (int x = 0; x < HT_VOXELS; x += 2) {
                    for (int z = 0; z < HT_VOXELS; z += 2) {
                        int dstX = baseX + (x >> 1);
                        int dstZ = baseZ + (z >> 1);

                        for (int layer = 0; layer < MAX_LAYERS; layer++) {
                            if (this.scaleSample(src, x, z, layer, data)) {
                                dst.setLayer(dstX, dstZ, layer, data);
                            }
                        }
                    }
                }
            }
        }

        return 0L;
    }

    protected boolean scaleSample(HeightmapTile src, int srcX, int srcZ, int layer, HeightmapData data) {
        double height0 = src.getLayerOnlyHeight(srcX, srcZ, layer);
        double height1 = src.getLayerOnlyHeight(srcX, srcZ + 1, layer);
        double height2 = src.getLayerOnlyHeight(srcX + 1, srcZ, layer);
        double height3 = src.getLayerOnlyHeight(srcX + 1, srcZ + 1, layer);

        double sum = 0.0d;
        double cnt = 0.0d;
        if (!Double.isNaN(height0)) {
            sum += height0;
            cnt++;
        }
        if (!Double.isNaN(height1)) {
            sum += height1;
            cnt++;
        }
        if (!Double.isNaN(height2)) {
            sum += height2;
            cnt++;
        }
        if (!Double.isNaN(height3)) {
            sum += height3;
            cnt++;
        }

        if (cnt == 0.0d) { //no samples were valid
            return false;
        }

        double avg = sum / cnt;

        double d0 = Double.isNaN(height0) ? Double.NEGATIVE_INFINITY : abs(height0 - avg);
        double d1 = Double.isNaN(height1) ? Double.NEGATIVE_INFINITY : abs(height1 - avg);
        double d2 = Double.isNaN(height2) ? Double.NEGATIVE_INFINITY : abs(height2 - avg);
        double d3 = Double.isNaN(height3) ? Double.NEGATIVE_INFINITY : abs(height3 - avg);

        if (!Double.isNaN(height0) && d0 >= d1 && d0 >= d2 && d0 >= d3) {
            src.getLayer(srcX, srcZ, layer, data);
        } else if (!Double.isNaN(height1) && d1 >= d0 && d1 >= d2 && d1 >= d3) {
            src.getLayer(srcX, srcZ + 1, layer, data);
        } else if (!Double.isNaN(height2) && d2 >= d0 && d2 >= d1 && d2 >= d3) {
            src.getLayer(srcX + 1, srcZ, layer, data);
        } else {
            src.getLayer(srcX + 1, srcZ + 1, layer, data);
        }
        return true;
    }
}
