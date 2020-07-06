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

package net.daporkchop.fp2.client.common;

import com.hackoeur.jglm.Mat4;
import com.hackoeur.jglm.Matrices;
import com.hackoeur.jglm.Vec3;
import net.daporkchop.pepsimod.util.render.MatrixHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;

import java.nio.FloatBuffer;

/**
 * @author DaPorkchop_
 */
public abstract class TerrainRenderer extends IRenderHandler {
    public double x;
    public double y;
    public double z;

    public Mat4 projectionMatrix;
    public Mat4 viewMatrix;
    public FloatBuffer proj;
    public FloatBuffer modelView;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        world.provider.setSkyRenderer(null);
        mc.renderGlobal.renderSky(partialTicks, 2);
        world.provider.setSkyRenderer(this);

        Entity entity = mc.getRenderViewEntity();
        this.x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        this.y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks + entity.getEyeHeight();
        this.z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;

        this.projectionMatrix = Matrices.perspective(mc.entityRenderer.getFOVModifier(partialTicks, true), mc.displayWidth / mc.displayHeight, 0.05f, 10000000f);
        Vec3d lookVec = entity.getLook(partialTicks);

        Vec3 eye = new Vec3((float) this.x, (float) this.y, (float) this.z);
        Vec3 f = new Vec3((float) lookVec.x, (float)lookVec.y, (float) lookVec.z);
        if (false) {
            Vec3 u = new Vec3(0f, 1f, 0f);
            Vec3 s = f.cross(u).getUnitVector();
            u = s.cross(f);

            this.viewMatrix = new Mat4(
                    s.getX(), u.getX(), -f.getX(), 0f,
                    s.getY(), u.getY(), -f.getY(), 0f,
                    s.getZ(), u.getZ(), -f.getZ(), 0f,
                    -s.dot(eye), -u.dot(eye), f.dot(eye), 1f);
        } else {
            this.viewMatrix = Matrices.lookAt(
                    eye,
                    eye.add(f),
                    new Vec3(0f, 1f, 0f));
        }
        this.modelView = MatrixHelper.getModelViewMatrix(this.modelView);
        this.proj = MatrixHelper.getProjectionMatrix(this.proj);
    }
}
