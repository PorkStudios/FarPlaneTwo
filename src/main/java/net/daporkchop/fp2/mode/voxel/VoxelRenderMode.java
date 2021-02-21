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

package net.daporkchop.fp2.mode.voxel;

import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.common.AbstractFarRenderMode;
import net.daporkchop.fp2.mode.voxel.event.RegisterExactVoxelGeneratorsEvent;
import net.daporkchop.fp2.mode.voxel.event.RegisterRoughVoxelGeneratorsEvent;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.server.gen.exact.CCVoxelGenerator;
import net.daporkchop.fp2.mode.voxel.server.gen.exact.VanillaVoxelGenerator;
import net.daporkchop.fp2.mode.voxel.server.gen.rough.CWGVoxelGenerator;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.OrderedRegistry;
import net.daporkchop.fp2.util.event.AbstractOrderedRegistryEvent;

/**
 * Implementation of {@link IFarRenderMode} for the voxel rendering mode.
 *
 * @author DaPorkchop_
 */
public class VoxelRenderMode extends AbstractFarRenderMode<VoxelPos, VoxelPiece> {
    @Override
    protected AbstractOrderedRegistryEvent<IFarGeneratorExact.Factory<VoxelPos, VoxelPiece>> exactGeneratorFactoryEvent() {
        return new RegisterExactVoxelGeneratorsEvent(new OrderedRegistry<IFarGeneratorExact.Factory<VoxelPos, VoxelPiece>>()
                .addLast("cubic_chunks", world -> Constants.isCubicWorld(world) ? new CCVoxelGenerator() : null)
                .addLast("vanilla", world -> new VanillaVoxelGenerator()));
    }

    @Override
    protected AbstractOrderedRegistryEvent<IFarGeneratorRough.Factory<VoxelPos, VoxelPiece>> roughGeneratorFactoryEvent() {
        return new RegisterRoughVoxelGeneratorsEvent(new OrderedRegistry<IFarGeneratorRough.Factory<VoxelPos, VoxelPiece>>()
                //TODO: remove "false && " once i actually get this working
                .addLast("cubic_world_gen", world -> false && Constants.isCwgWorld(world) ? new CWGVoxelGenerator() : null));
    }
}
