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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLRequires;
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
public abstract class GLSampledTexture extends GLStorageTexture {
    protected final @Positive int levels;

    protected GLSampledTexture(@NonNull OpenGL gl, @NonNull TextureTarget target, @NonNull TextureInternalFormat internalFormat, @Positive int levels) {
        super(gl, target, internalFormat);

        try {
            this.levels = positive(levels, "levels");

            //configure the base and maximum mipmap levels
            // (i don't think this is actually necessary)
            this.mipmapLevels(0, levels - 1);
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
    public final void mipmapLevels(int baseLevel, int maxLevel) {
        this.checkOpen();
        checkArg(baseLevel <= maxLevel, "baseLevel (%s) must be less than or equal to maxLevel (%s)", baseLevel, maxLevel);

        this.setParameters(parameterSetter -> {
            parameterSetter.set(GL_TEXTURE_BASE_LEVEL, baseLevel);
            parameterSetter.set(GL_TEXTURE_MAX_LEVEL, maxLevel);
        });
    }

    /**
     * Sets this texture's filtering mode.
     *
     * @param minFilter the texture minification filter
     * @param magFilter the texture magnification filter
     */
    public final void filter(@NonNull TextureMinFilter minFilter, @NonNull TextureMagFilter magFilter) {
        this.checkOpen();

        this.setParameters(parameterSetter -> {
            parameterSetter.set(GL_TEXTURE_MIN_FILTER, minFilter.id());
            parameterSetter.set(GL_TEXTURE_MAG_FILTER, magFilter.id());
        });
    }

    /**
     * Sets the given texture parameter to the given value.
     *
     * @param pname the texture parameter
     * @param param the parameter value
     */
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
     * Sets multiple texture parameters.
     *
     * @param action a function which will be called with a {@link ParameterSetter} which may be used to set texture parameters
     */
    public final void setParameters(@NonNull Consumer<ParameterSetter> action) {
        this.checkOpen();

        if (this.dsa) {
            action.accept(new DSAParameterSetter(this.gl, this.id));
        } else {
            this.bind(target -> {
                action.accept(new BoundParameterSetter(this.gl, target));
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
            this.invalidate();
        }
    }

    /**
     * Invalidates this texture's contents.
     * <p>
     * After invalidation, the texture contents become undefined.
     *
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata}
     */
    @GLRequires(GLExtension.GL_ARB_invalidate_subdata)
    public final void invalidate() {
        this.checkOpen();
        for (int level = 0; level < this.levels; level++) {
            this.gl.glInvalidateTexImage(this.id, level);
        }
    }

    /**
     * A handle for setting texture parameter values for this texture.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static abstract class ParameterSetter {
        protected final OpenGL gl;

        //@formatter:off
        public abstract void set(int pname, int param);
        public abstract void set(int pname, float param);
        //@formatter:on
    }

    /**
     * Sets texture properties using the legacy {@code glTexParameter*} functions which operate on the active shader program.
     *
     * @author DaPorkchop_
     */
    static final class BoundParameterSetter extends ParameterSetter {
        private final TextureTarget target;

        BoundParameterSetter(OpenGL gl, TextureTarget target) {
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
    static final class DSAParameterSetter extends ParameterSetter {
        private final int id;

        DSAParameterSetter(OpenGL gl, int id) {
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
