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

package net.daporkchop.fp2.impl.mc.forge1_16.client.render;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATLightTexture1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.world.FarWorldClient1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.BiomeColorBlockDisplayReader1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.ResourceProvider1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.registry.GameRegistry1_16;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.math.BlockPos;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class WorldRenderer1_16 implements WorldRenderer, AutoCloseable {
    private static int toLayerIndex(RenderType type) {
        if (type == RenderType.solid()) {
            return IFarRenderer.LAYER_SOLID;
        } else if (type == RenderType.cutout() || type == RenderType.cutoutMipped()) {
            return IFarRenderer.LAYER_CUTOUT; //TODO: cutout needs alpha testing (i may need discard; ...)
        } else if (type == RenderType.translucent()) {
            return IFarRenderer.LAYER_TRANSPARENT;
        } else {
            return IFarRenderer.LAYER_SOLID;
        }
    }

    protected final Minecraft mc;
    protected final GL gl;

    protected final FarWorldClient1_16 world;

    protected final TextureUVs1_16 textureUVs;

    protected final GameRegistry1_16 registry;
    protected final byte[] renderTypeLookup;

    @SuppressWarnings("deprecation")
    public WorldRenderer1_16(@NonNull Minecraft mc, @NonNull FarWorldClient1_16 world) {
        this.mc = mc;
        this.world = world;

        this.registry = world.fp2_IFarWorld_registry();

        this.renderTypeLookup = new byte[this.registry.statesCount()];
        this.registry.states().forEach(state -> {
            BlockState blockState = this.registry.id2state(state);
            RenderType renderType;
            if (blockState.getFluidState().isEmpty()) {
                renderType = RenderTypeLookup.getChunkRenderType(blockState);
            } else {
                renderType = RenderTypeLookup.getRenderLayer(blockState.getFluidState());
            }
            this.renderTypeLookup[state] = (byte) toLayerIndex(renderType);
        });

        this.gl = GL.builder()
                .withResourceProvider(new ResourceProvider1_16(this.mc))
                .wrapCurrent();

        this.textureUVs = new TextureUVs1_16(world.fp2_IFarWorld_registry(), this.gl, mc);
    }

    @Override
    public void close() {
        this.textureUVs.close();
        this.gl.close();
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        return this.mc.getBlockColors().getColor(this.registry.id2state(state), new BiomeColorBlockDisplayReader1_16(this.registry.id2biome(biome)), new BlockPos(x, y, z), 0);
    }

    @Override
    public TerrainRenderingBlockedTracker blockedTracker() {
        //TODO
        return (chunkX, chunkY, chunkZ) -> false;
    }

    @Override
    public Object terrainTextureId() {
        return this.mc.getModelManager().getAtlas(PlayerContainer.BLOCK_ATLAS).getId();
    }

    @Override
    public Object lightmapTextureId() {
        return ((ATLightTexture1_16) this.mc.gameRenderer.lightTexture()).getLightTexture().getId();
    }
}
