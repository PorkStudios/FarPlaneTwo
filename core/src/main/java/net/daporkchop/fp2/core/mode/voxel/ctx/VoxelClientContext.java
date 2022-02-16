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

package net.daporkchop.fp2.core.mode.voxel.ctx;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.core.client.world.IFarWorldClient;
import net.daporkchop.fp2.core.mode.common.ctx.AbstractFarClientContext;
import net.daporkchop.fp2.core.mode.voxel.VoxelPos;
import net.daporkchop.fp2.core.mode.voxel.VoxelTile;
import net.daporkchop.fp2.core.mode.voxel.client.VoxelRenderer;

/**
 * @author DaPorkchop_
 */
public class VoxelClientContext extends AbstractFarClientContext<VoxelPos, VoxelTile> {
    public VoxelClientContext(@NonNull IFarWorldClient world, @NonNull FP2Config config, @NonNull IFarRenderMode<VoxelPos, VoxelTile> mode) {
        super(world, config, mode);
    }

    @Override
    protected IFarRenderer renderer0(IFarRenderer old, @NonNull FP2Config config) {
        /*if (OFHelper.of_Config_isShaders()) {
            return old; //TODO: transform feedback renderer
        } else {*/
        return old instanceof VoxelRenderer.ShaderMultidraw ? old : new VoxelRenderer.ShaderMultidraw(this);
        //}
    }
}
