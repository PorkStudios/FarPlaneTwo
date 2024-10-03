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

package net.daporkchop.fp2.gl.texture;

import lombok.NonNull;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base class for textures with sampler parameters.
 *
 * @author DaPorkchop_
 */
public abstract class GLSampledTexture extends GLTexture {
    protected GLSampledTexture(OpenGL gl, @NonNull TextureTarget target) {
        super(gl, target);
    }

    /**
     * Sets this texture's base and maximum mipmap levels.
     *
     * @param baseLevel the base mipmap level (inclusive)
     * @param maxLevel  the maximum mipmap level (inclusive)
     */
    public final void mipmapLevels(@NotNegative int baseLevel, @NotNegative int maxLevel) {
        this.checkOpen();
        checkArg(baseLevel <= maxLevel, "baseLevel (%s) must be less than or equal to maxLevel (%s)", baseLevel, maxLevel);

        this.bind(target -> {
            this.gl.glTexParameter(target.id(), GL_TEXTURE_BASE_LEVEL, baseLevel);
            this.gl.glTexParameter(target.id(), GL_TEXTURE_MAX_LEVEL, maxLevel);
        });
    }
}
