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

package net.daporkchop.fp2.client;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
//TODO: possibly delete this, or at least make it work correctly
@SideOnly(Side.CLIENT)
@SuppressWarnings("deprecation")
public class FP2ResourceReloadListener implements IResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        /*try (ShaderStorageBuffer globalInfo = GLOBAL_INFO.bind()) {
            IntBuffer buffer = Constants.createIntBuffer(COLORMAP_FOLIAGE_SIZE >> 2);
            buffer.put(TextureUtil.readImageData(resourceManager, new ResourceLocation("textures/colormap/foliage.png"))).clear();
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, COLORMAP_FOLIAGE_OFFSET, buffer);

            buffer = Constants.createIntBuffer(COLORMAP_GRASS_SIZE >> 2);
            buffer.put(TextureUtil.readImageData(resourceManager, new ResourceLocation("textures/colormap/grass.png"))).clear();
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, COLORMAP_GRASS_OFFSET, buffer);
        } catch (IOException e) {
        }*/
    }
}
