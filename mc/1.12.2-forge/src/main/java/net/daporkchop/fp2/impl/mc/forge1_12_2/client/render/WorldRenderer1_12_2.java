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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.render;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.FarWorldClient1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.ResourceProvider1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.SingleBiomeBlockAccess;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 * @author DaPorkchop_
 */
@Getter
public class WorldRenderer1_12_2 implements WorldRenderer, AutoCloseable {
    protected final Minecraft mc;
    protected final GL gl;

    protected final FarWorldClient1_12_2 world;

    protected final TextureUVs1_12_2 textureUVs;

    protected final GameRegistry1_12_2 registry;
    protected final byte[] renderTypeLookup;

    public WorldRenderer1_12_2(@NonNull Minecraft mc, @NonNull FarWorldClient1_12_2 world) {
        this.mc = mc;
        this.world = world;

        this.registry = world.fp2_IFarWorld_registry();

        //look up and cache the render type for each block state
        this.renderTypeLookup = new byte[this.registry.statesCount()];
        this.registry.states().forEach(state -> {
            //TODO: i need to do something about this: grass is rendered as CUTOUT_MIPPED, which makes it always render both faces

            int typeIndex;
            switch (this.registry.id2state(state).getBlock().getRenderLayer()) {
                case SOLID:
                    typeIndex = 0;
                    break;
                case CUTOUT:
                case CUTOUT_MIPPED:
                    typeIndex = 1;
                    break;
                case TRANSLUCENT:
                    typeIndex = 2;
                    break;
                default:
                    throw new IllegalArgumentException(this.registry.id2state(state).getBlock().getRenderLayer().name());
            }
            this.renderTypeLookup[state] = (byte) typeIndex;
        });

        this.gl = GL.builder()
                .withResourceProvider(new ResourceProvider1_12_2(this.mc))
                .wrapCurrent();

        this.textureUVs = new TextureUVs1_12_2(world.fp2_IFarWorld_registry(), this.gl, mc);
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        return this.mc.getBlockColors().colorMultiplier(this.registry.id2state(state), new SingleBiomeBlockAccess().biome(this.registry.id2biome(biome)), new BlockPos(x, y, z), 0);
    }

    @Override
    public TerrainRenderingBlockedTracker blockedTracker() {
        return ((IMixinRenderGlobal) this.mc.renderGlobal).fp2_vanillaRenderabilityTracker();
    }

    @Override
    public Object terrainTextureId() {
        return this.mc.getTextureMapBlocks().getGlTextureId();
    }

    @Override
    public Object lightmapTextureId() {
        return this.mc.entityRenderer.lightmapTexture.getGlTextureId();
    }

    @Override
    public void close() {
        this.textureUVs.close();
        this.gl.close();
    }
}
