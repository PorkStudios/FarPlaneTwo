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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.core.mode.voxel.server.gen.rough.AbstractRoughVoxelGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.world.level.block.rough.AbstractRoughFBlockLevelHolder;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.CWGContext;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.world.WorldServer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.core.mode.voxel.VoxelConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class CWGRoughFBlockLevelHolder1_12 extends AbstractRoughFBlockLevelHolder {
    private final Cached<CWGContext> ctx;
    private final CWGRoughFBlockLevel1_12 blockLevel;

    public CWGRoughFBlockLevelHolder1_12(@NonNull IFarLevelServer level) {
        super(level);

        this.ctx = Cached.threadLocal(() -> new CWGContext(this.registry(), (WorldServer) level.implLevel(), AbstractRoughVoxelGenerator.CACHE_SIZE, 2, max(HT_SHIFT, VT_SHIFT)), ReferenceStrength.WEAK);
        this.blockLevel = new CWGRoughFBlockLevel1_12(this);
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.dataLimits().intersects(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
