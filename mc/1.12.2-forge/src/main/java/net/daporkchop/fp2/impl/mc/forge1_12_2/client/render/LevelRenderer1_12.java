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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.render;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.renderer.ATEntityRenderer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.renderer.IMixinRenderGlobal1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.world.level.FLevelClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.SingleBiomeBlockAccess1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.Util1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12;
import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 * @author DaPorkchop_
 */
@Getter
public class LevelRenderer1_12 implements LevelRenderer, AutoCloseable {
    protected static final TL<SingleBiomeBlockAccess1_12> SINGLE_BIOME_BLOCK_ACCESS_CACHE = TL.initializedWith(SingleBiomeBlockAccess1_12::new);

    protected final FP2Core fp2;
    protected final Minecraft mc;

    protected final FLevelClient1_12 level;

    protected final TextureUVs1_12 textureUVs;

    protected final GameRegistry1_12 registry;
    protected final byte[] renderTypeLookup;

    public LevelRenderer1_12(@NonNull Minecraft mc, @NonNull FLevelClient1_12 level) {
        this.fp2 = level.fp2();
        this.mc = mc;
        this.level = level;

        this.registry = (GameRegistry1_12) level.registry();

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

        this.textureUVs = new TextureUVs1_12(this.fp2, level.registry(), mc);
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        Recycler<BlockPos.MutableBlockPos> recycler = Util1_12.MUTABLEBLOCKPOS_RECYCLER.get();
        BlockPos.MutableBlockPos pos = recycler.allocate().setPos(x, y, z);

        int colorMultiplier = this.mc.getBlockColors().colorMultiplier(
                this.registry.id2state(state),
                SINGLE_BIOME_BLOCK_ACCESS_CACHE.get().biome(this.registry.id2biome(biome)),
                pos, 0);

        recycler.release(pos);
        return colorMultiplier;
    }

    @Override
    public TerrainRenderingBlockedTracker blockedTracker() {
        return ((IMixinRenderGlobal1_12) this.mc.renderGlobal).fp2_vanillaRenderabilityTracker();
    }

    @Override
    public OpenGL gl() {
        return this.fp2.client().gl();
    }

    @Override
    public Object terrainTextureId() {
        return this.mc.getTextureMapBlocks().getGlTextureId();
    }

    @Override
    public Object lightmapTextureId() {
        return ((ATEntityRenderer1_12) this.mc.entityRenderer).getLightmapTexture().getGlTextureId();
    }

    @Override
    public void close() {
        this.textureUVs.close();
    }
}
