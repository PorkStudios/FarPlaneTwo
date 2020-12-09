/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.mode.common.client.FarRenderIndex;
import net.daporkchop.fp2.mode.common.client.IFarRenderBaker;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class HeightmapRenderer extends AbstractFarRenderer<HeightmapPos, HeightmapPiece> {
    public static final ShaderProgram TERRAIN_SHADER = ShaderManager.get("heightmap/terrain");
    public static final ShaderProgram WATER_STENCIL_SHADER = ShaderManager.get("heightmap/water_stencil");
    public static final ShaderProgram WATER_SHADER = ShaderManager.get("heightmap/water");

    public HeightmapRenderer(@NonNull WorldClient world) {
        super(world);
    }

    @Override
    protected HeightmapRenderCache createCache() {
        return new HeightmapRenderCache(this);
    }

    @Override
    public IFarRenderBaker<HeightmapPos, HeightmapPiece> baker() {
        return new HeightmapRenderBaker();
    }

    @Override
    protected void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum, @NonNull FarRenderIndex index) {
        int size = index.upload(0, this.drawCommandBuffer);
        if (size > 0) {
            //solid terrain
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();
                glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE * INT_SIZE, size, FarRenderIndex.ENTRY_SIZE * INT_SIZE);
                GlStateManager.enableAlpha();
            }
            //water
            if (true) {
                glEnable(GL_STENCIL_TEST);

                WATER_STENCIL_SHADER.use();
                glUniform1i(WATER_STENCIL_SHADER.uniformLocation("seaLevel"), 63);
                {
                    GlStateManager.colorMask(false, false, false, false);

                    GlStateManager.clear(GL_STENCIL_BUFFER_BIT);
                    glStencilMask(0xFF);
                    glStencilFunc(GL_ALWAYS, 1, 0xFF); //always allow all fragments
                    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

                    GlStateManager.depthMask(false);

                    glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE * INT_SIZE, size, FarRenderIndex.ENTRY_SIZE * INT_SIZE);

                    GlStateManager.depthMask(true);

                    GlStateManager.colorMask(true, true, true, true);
                }

                WATER_SHADER.use();
                glUniform1i(WATER_SHADER.uniformLocation("seaLevel"), 63);
                glUniform1i(WATER_SHADER.uniformLocation("in_state"), TexUVs.STATEID_TO_INDEXID.get(Block.getStateId(Blocks.WATER.getDefaultState())));
                {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
                    GlStateManager.alphaFunc(GL_GREATER, 0.1f);

                    glStencilMask(0);
                    glStencilFunc(GL_EQUAL, 1, 0xFF);
                    glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                    glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE * INT_SIZE, size, FarRenderIndex.ENTRY_SIZE * INT_SIZE);

                    GlStateManager.disableBlend();
                }
                WATER_SHADER.close();

                glDisable(GL_STENCIL_TEST);
            }
        }
    }

    @Override
    public RenderMode mode() {
        return RenderMode.HEIGHTMAP;
    }
}
