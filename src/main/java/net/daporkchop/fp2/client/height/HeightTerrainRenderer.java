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

package net.daporkchop.fp2.client.height;

import com.hackoeur.jglm.Mat4;
import net.daporkchop.fp2.client.common.TerrainRenderer;
import net.daporkchop.pepsimod.util.render.MatrixHelper;
import net.daporkchop.pepsimod.util.render.shader.ShaderManager;
import net.daporkchop.pepsimod.util.render.shader.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ShortBuffer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static net.minecraft.client.renderer.OpenGlHelper.glBindBuffer;
import static net.minecraft.client.renderer.OpenGlHelper.glGenBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public class HeightTerrainRenderer extends TerrainRenderer {
    public static int I = 0;
    public static ShaderProgram RED_SHADER = ShaderManager.get("red");
    public static final int MESH_IBO = glGenBuffers();
    public static final int MESH_VERTEX_COUNT;

    static {
        ShortBuffer buffer = BufferUtils.createShortBuffer(HEIGHT_TILE_SIZE * HEIGHT_TILE_SIZE * 2 + 1);
        genMesh(HEIGHT_TILE_SIZE, buffer);
        MESH_VERTEX_COUNT = buffer.flip().remaining();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
        GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
    }

    private static void genMesh(int size, ShortBuffer out) {
        for (int x = 0; x < size - 1; x++) {
            if ((x & 1) == 0) {
                for (int z = 0; z < size; z++) {
                    out.put((short) (x * size + z)).put((short) ((x + 1) * size + z));
                }
            } else {
                for (int z = size - 1; z > 0; z--) {
                    out.put((short) ((x + 1) * size + z)).put((short) (x * size + z - 1));
                }
            }
        }
        if ((size & 1) != 0 && size > 2) {
            out.put((short) ((size - 1) * size));
        }
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        //GlStateManager.enableBlend();
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        glPushMatrix();
        glTranslated(-this.x, -this.y, -this.z);

        this.modelView = MatrixHelper.getModelViewMatrix(this.modelView);
        this.proj = MatrixHelper.getProjectionMatrix(this.proj);

        try (ShaderProgram shader = RED_SHADER.use()) {
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_view"), false, this.modelView);

            //glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);

            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            builder.pos(0, 0, 0).endVertex();
            builder.pos(0, 70, 0).endVertex();
            builder.pos(1, 70, 0).endVertex();
            builder.pos(1, 0, 0).endVertex();
            tessellator.draw();
        }

        glPopMatrix();

        GlStateManager.enableCull();
        //GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
    }
}
