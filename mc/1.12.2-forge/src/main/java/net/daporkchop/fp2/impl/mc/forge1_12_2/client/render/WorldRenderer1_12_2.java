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
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.FakeFarWorldClient1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.ResourceProvider1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.SingleBiomeBlockAccess;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.minecraft.util.math.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class WorldRenderer1_12_2 implements WorldRenderer, AutoCloseable {
    protected final Minecraft mc;
    protected final GL gl;

    protected final FakeFarWorldClient1_12_2 world;

    protected final TextureUVs1_12_2 textureUVs;

    protected final GameRegistry1_12_2 registry;
    protected final byte[] renderTypeLookup;

    protected final FloatBuffer tempMatrix = BufferUtils.createFloatBuffer(MatrixHelper.MAT4_ELEMENTS);

    public WorldRenderer1_12_2(@NonNull Minecraft mc, @NonNull FakeFarWorldClient1_12_2 world) {
        this.mc = mc;
        this.world = world;

        this.registry = world.fp2_IFarWorld_registry();

        //look up and cache the render type for each block state
        this.renderTypeLookup = new byte[this.registry.states().max().getAsInt() + 1];
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
    public GlobalUniformAttributes globalUniformAttributes() {
        GlobalUniformAttributes attributes = new GlobalUniformAttributes();

        //optifine compatibility: disable fog if it's turned off, because optifine only does this itself if no vanilla terrain is being rendered
        //  (e.g. it's all being discarded in frustum culling)
        if (OF && (PUnsafe.getInt(this.mc.gameSettings, OF_FOGTYPE_OFFSET) == OF_OFF && PUnsafe.getBoolean(this.mc.entityRenderer, OF_ENTITYRENDERER_FOGSTANDARD_OFFSET))) {
            GlStateManager.disableFog();
        }

        { //camera
            this.initModelViewProjectionMatrix(attributes);

            float partialTicks = this.mc.getRenderPartialTicks();
            Entity entity = this.mc.getRenderViewEntity();
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

            attributes.positionFloorX = floorI(x);
            attributes.positionFloorY = floorI(y);
            attributes.positionFloorZ = floorI(z);

            attributes.positionFracX = (float) frac(x);
            attributes.positionFracY = (float) frac(y);
            attributes.positionFracZ = (float) frac(z);
        }

        { //fog
            this.initFogColor(attributes);

            attributes.fogMode = glGetBoolean(GL_FOG) ? glGetInteger(GL_FOG_MODE) : 0;

            attributes.fogDensity = glGetFloat(GL_FOG_DENSITY);
            attributes.fogStart = glGetFloat(GL_FOG_START);
            attributes.fogEnd = glGetFloat(GL_FOG_END);
            attributes.fogScale = 1.0f / (attributes.fogEnd - attributes.fogStart);
        }

        return attributes;
    }

    private void initModelViewProjectionMatrix(GlobalUniformAttributes attributes) {
        ArrayAllocator<float[]> alloc = GlobalAllocators.ALLOC_FLOAT.get();

        float[] modelView = alloc.atLeast(MatrixHelper.MAT4_ELEMENTS);
        float[] projection = alloc.atLeast(MatrixHelper.MAT4_ELEMENTS);
        try {
            //load both matrices into arrays
            glGetFloat(GL_MODELVIEW_MATRIX, (FloatBuffer) this.tempMatrix.clear());
            this.tempMatrix.get(modelView);
            glGetFloat(GL_PROJECTION_MATRIX, (FloatBuffer) this.tempMatrix.clear());
            this.tempMatrix.get(projection);

            //pre-multiply matrices on CPU to avoid having to do it per-vertex on GPU
            MatrixHelper.multiply4x4(projection, modelView, attributes.modelViewProjectionMatrix);

            //offset the projected points' depth values to avoid z-fighting with vanilla terrain
            MatrixHelper.offsetDepth(attributes.modelViewProjectionMatrix, fp2().client().isReverseZ() ? -0.00001f : 0.00001f);
        } finally {
            alloc.release(projection);
            alloc.release(modelView);
        }
    }

    private void initFogColor(GlobalUniformAttributes attributes) {
        //buffer needs to fit 16 elements, but only the first 4 will be used
        long addr = PUnsafe.allocateMemory(16 * FLOAT_SIZE);
        try {
            FloatBuffer buffer = DirectBufferHackery.wrapFloat(addr, 16);
            glGetFloat(GL_FOG_COLOR, buffer);

            attributes.fogColorR = buffer.get(0);
            attributes.fogColorG = buffer.get(1);
            attributes.fogColorB = buffer.get(2);
            attributes.fogColorA = buffer.get(3);
        } finally {
            PUnsafe.freeMemory(addr);
        }
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
