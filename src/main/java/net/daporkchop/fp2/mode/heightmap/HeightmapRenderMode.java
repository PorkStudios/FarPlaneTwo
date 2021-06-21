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

package net.daporkchop.fp2.mode.heightmap;

import io.github.opencubicchunks.cubicchunks.cubicgen.flat.FlatTerrainProcessor;
import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.common.AbstractFarRenderMode;
import net.daporkchop.fp2.mode.heightmap.ctx.HeightmapClientContext;
import net.daporkchop.fp2.mode.heightmap.ctx.HeightmapServerContext;
import net.daporkchop.fp2.mode.heightmap.event.RegisterExactHeightmapGeneratorsEvent;
import net.daporkchop.fp2.mode.heightmap.event.RegisterRoughHeightmapGeneratorsEvent;
import net.daporkchop.fp2.mode.heightmap.server.HeightmapWorld;
import net.daporkchop.fp2.mode.heightmap.server.gen.exact.CCHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.exact.VanillaHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.rough.CWGFlatHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.rough.CWGHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.rough.FlatHeightmapGenerator;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.event.AbstractOrderedRegistryEvent;
import net.daporkchop.fp2.util.registry.LinkedOrderedRegistry;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Implementation of {@link IFarRenderMode} for the heightmap rendering mode.
 *
 * @author DaPorkchop_
 */
public class HeightmapRenderMode extends AbstractFarRenderMode<HeightmapPos, HeightmapTile> {
    public HeightmapRenderMode() {
        super(HeightmapConstants.STORAGE_VERSION);
    }

    @Override
    protected AbstractOrderedRegistryEvent<IFarGeneratorExact.Factory<HeightmapPos, HeightmapTile>> exactGeneratorFactoryEvent() {
        return new RegisterExactHeightmapGeneratorsEvent(new LinkedOrderedRegistry<IFarGeneratorExact.Factory<HeightmapPos, HeightmapTile>>()
                .addLast("cubic_chunks", world -> Constants.isCubicWorld(world) ? new CCHeightmapGenerator(world) : null)
                .addLast("vanilla", VanillaHeightmapGenerator::new));
    }

    @Override
    protected AbstractOrderedRegistryEvent<IFarGeneratorRough.Factory<HeightmapPos, HeightmapTile>> roughGeneratorFactoryEvent() {
        return new RegisterRoughHeightmapGeneratorsEvent(new LinkedOrderedRegistry<IFarGeneratorRough.Factory<HeightmapPos, HeightmapTile>>()
                .addLast("superflat", world -> Constants.getTerrainGenerator(world) instanceof ChunkGeneratorFlat ? new FlatHeightmapGenerator(world) : null)
                .addLast("flatcubic", world -> Constants.getTerrainGenerator(world) instanceof FlatTerrainProcessor ? new CWGFlatHeightmapGenerator(world) : null)
                .addLast("cubic_world_gen", world -> Constants.isCwgWorld(world) ? new CWGHeightmapGenerator(world) : null));
    }

    @Override
    protected HeightmapTile newTile() {
        return new HeightmapTile();
    }

    @Override
    public IFarServerContext<HeightmapPos, HeightmapTile> serverContext(@NonNull WorldServer world) {
        return new HeightmapServerContext(world, this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IFarClientContext<HeightmapPos, HeightmapTile> clientContext(@NonNull WorldClient world) {
        return new HeightmapClientContext(this);
    }

    @Override
    public IFarDirectPosAccess<HeightmapPos> directPosAccess() {
        return HeightmapDirectPosAccess.INSTANCE;
    }

    @Override
    public HeightmapPos readPos(@NonNull ByteBuf buf) {
        return new HeightmapPos(buf);
    }

    @Override
    public HeightmapPos[] posArray(int length) {
        return new HeightmapPos[length];
    }

    @Override
    public HeightmapTile[] tileArray(int length) {
        return new HeightmapTile[length];
    }
}
