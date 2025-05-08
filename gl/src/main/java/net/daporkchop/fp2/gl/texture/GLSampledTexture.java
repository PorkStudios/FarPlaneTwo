/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base class for textures with sampler parameters.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class GLSampledTexture extends GLStorageTexture implements ISamplingParameters {
    protected final @Positive int levels;

    protected GLSampledTexture(@NonNull OpenGL gl, @NonNull TextureTarget target, @NonNull TextureInternalFormat internalFormat, @Positive int levels) {
        super(gl, target, internalFormat);

        try {
            this.levels = positive(levels, "levels");

            //configure the base and maximum mipmap levels
            // (i don't think this is actually necessary)
            this.setMipmapLevels(0, levels - 1);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    /**
     * Sets this texture's base and maximum mipmap levels.
     *
     * @param baseLevel the base mipmap level (inclusive)
     * @param maxLevel  the maximum mipmap level (inclusive)
     */
    public final void setMipmapLevels(@NotNegative int baseLevel, @NotNegative int maxLevel) {
        this.checkOpen();
        checkArg(baseLevel <= maxLevel, "baseLevel (%s) must be less than or equal to maxLevel (%s)", baseLevel, maxLevel);

        this.setParameters(parameterSetter -> {
            parameterSetter.set(GL_TEXTURE_BASE_LEVEL, baseLevel);
            parameterSetter.set(GL_TEXTURE_MAX_LEVEL, maxLevel);
        });
    }

    /**
     * Sets the given texture parameter to the given value.
     *
     * @param pname the texture parameter
     * @param param the parameter value
     */
    @Override
    public final void setParameter(int pname, int param) {
        this.checkOpen();

        if (this.dsa) {
            this.gl.glTextureParameter(this.id, pname, param);
        } else {
            this.bind(target -> {
                this.gl.glTexParameter(target.id(), pname, param);
            });
        }
    }

    /**
     * Sets the given texture parameter to the given value.
     *
     * @param pname the texture parameter
     * @param param the parameter value
     */
    @Override
    public final void setParameter(int pname, float param) {
        this.checkOpen();

        if (this.dsa) {
            this.gl.glTextureParameter(this.id, pname, param);
        } else {
            this.bind(target -> {
                this.gl.glTexParameter(target.id(), pname, param);
            });
        }
    }

    /**
     * Sets multiple texture parameters.
     *
     * @param action a function which will be called with a {@link ParameterSetter} which may be used to set texture parameters
     */
    @Override
    public final void setParameters(@NonNull Consumer<ParameterSetter> action) {
        this.checkOpen();

        if (this.dsa) {
            action.accept(new DSATextureParameterSetter(this.gl, this.id));
        } else {
            this.bind(target -> {
                action.accept(new BoundTextureParameterSetter(this.gl, target));
            });
        }
    }

    /**
     * Hints that the texture's storage should be invalidated.
     * <p>
     * After invalidation, the texture contents become undefined.
     * <p>
     * This may do nothing if the OpenGL implementation doesn't support texture invalidation.
     */
    public final void invalidateHint() {
        this.checkOpen();
        if (this.invalidateSubdata) {
            this.invalidateLevels(0, this.levels);
        }
    }

    /**
     * Invalidates this texture's storage.
     * <p>
     * After invalidation, the texture contents become undefined.
     *
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata}
     */
    @GLRequires(GLExtension.GL_ARB_invalidate_subdata)
    public final void invalidate() {
        this.invalidateLevels(0, this.levels);
    }

    /**
     * Hints that this texture's storage should be invalidated at the given mipmap levels.
     * <p>
     * After invalidation, the texture contents at the given mipmap levels become undefined.
     * <p>
     * This may do nothing if the OpenGL implementation doesn't support texture invalidation.
     *
     * @param first the index of the first mipmap level to invalidate
     * @param count the number of mipmap levels to invalidate
     */
    public final void invalidateLevelsHint(@NotNegative int first, @NotNegative int count) {
        this.checkOpen();
        checkRangeLen(this.levels, first, count);

        if (this.invalidateSubdata) {
            this.invalidateLevels(first, count);
        }
    }

    /**
     * Invalidates this texture's storage at the given mipmap levels.
     * <p>
     * After invalidation, the texture contents at the given mipmap levels become undefined.
     *
     * @param first the index of the first mipmap level to invalidate
     * @param count the number of mipmap levels to invalidate
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata}
     */
    @GLRequires(GLExtension.GL_ARB_invalidate_subdata)
    public final void invalidateLevels(@NotNegative int first, @NotNegative int count) {
        this.checkOpen();
        checkRangeLen(this.levels, first, count);

        for (int level = first; level < count; level++) {
            this.gl.glInvalidateTexImage(this.id, level);
        }
    }

    /**
     * Sets texture properties using the legacy {@code glTexParameter*} functions which operate on the active shader program.
     *
     * @author DaPorkchop_
     */
    static final class BoundTextureParameterSetter extends ParameterSetter {
        private final TextureTarget target;

        BoundTextureParameterSetter(OpenGL gl, TextureTarget target) {
            super(gl);
            this.target = target;
        }

        @Override
        public void set(int pname, int param) {
            this.gl.glTexParameter(this.target.id(), pname, param);
        }

        @Override
        public void set(int pname, float param) {
            this.gl.glTexParameter(this.target.id(), pname, param);
        }
    }

    /**
     * Sets texture parameters using the modern bindless {@code glTextureParameter*} functions.
     *
     * @author DaPorkchop_
     */
    static final class DSATextureParameterSetter extends ParameterSetter {
        private final int id;

        DSATextureParameterSetter(OpenGL gl, int id) {
            super(gl);
            this.id = id;
        }

        @Override
        public void set(int pname, int param) {
            this.gl.glTextureParameter(this.id, pname, param);
        }

        @Override
        public void set(int pname, float param) {
            this.gl.glTextureParameter(this.id, pname, param);
        }
    }
}
