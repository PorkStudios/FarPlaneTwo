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

package net.daporkchop.fp2.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * The different render modes that may be activated.
 *
 * @author DaPorkchop_
 */
public enum DrawMode implements AutoCloseable {
    SHADER {
        @Override
        public DrawMode begin() {
            super.begin();

            return this;
        }

        @Override
        public void close() {
            super.close();
        }
    },
    LEGACY {
        @Override
        public DrawMode begin() {
            super.begin();

            GlStateManager.glEnableClientState(GL_VERTEX_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glEnableClientState(GL_COLOR_ARRAY);

            GlStateManager.resetColor();
            return this;
        }

        @Override
        public void close() {
            GlStateManager.glDisableClientState(GL_VERTEX_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.glDisableClientState(GL_COLOR_ARRAY);

            super.close();
        }
    };

    private static DrawMode ACTIVE_RENDER_MODE;

    /**
     * Activates this render mode.
     */
    public DrawMode begin() {
        checkState(ACTIVE_RENDER_MODE == null, "cannot switch to %s when %s is already active!", this, ACTIVE_RENDER_MODE);
        ACTIVE_RENDER_MODE = this;

        GlStateManager.depthFunc(GL_LESS);
        MC.entityRenderer.enableLightmap();

        return this;
    }

    @Override
    public void close() {
        MC.entityRenderer.disableLightmap();
        GlStateManager.depthFunc(GL_LEQUAL);

        ACTIVE_RENDER_MODE = null;
    }

    /**
     * @throws IllegalStateException if this render mode isn't active
     */
    public void require() {
        checkState(ACTIVE_RENDER_MODE == this, "active render mode is %s (required: %s)", ACTIVE_RENDER_MODE, this);
    }
}
