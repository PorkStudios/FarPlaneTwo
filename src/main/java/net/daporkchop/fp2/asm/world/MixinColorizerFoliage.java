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

package net.daporkchop.fp2.asm.world;

import net.daporkchop.fp2.client.render.object.VertexBufferObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapTerrainRenderer.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBufferData;

/**
 * @author DaPorkchop_
 */
@Mixin(ColorizerFoliage.class)
public abstract class MixinColorizerFoliage {
    @Inject(method = "Lnet/minecraft/world/ColorizerFoliage;setFoliageBiomeColorizer([I)V",
            at = @At("HEAD"))
    private static void setFoliageBiomeColorizer_head(int[] data, CallbackInfo ci) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            GRASS_BUFFER.position(256 * 256);
            GRASS_BUFFER.put(data).clear();

            try (VertexBufferObject vbo = new VertexBufferObject().bind()) {
                glBufferData(GL_ARRAY_BUFFER, GRASS_BUFFER, GL_STATIC_DRAW);

                GRASS_COLORS.useBuffer(vbo, GL_RGBA8);
            }
        });
    }
}
