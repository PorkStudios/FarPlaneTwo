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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.GLBuffer;

/**
 * @author DaPorkchop_
 */
public final class GLBufferTexture extends GLTexture {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_texture_buffer_object)
            .add(GLExtension.GL_EXT_gpu_shader4); //TODO: this isn't a reliable way to check for shader support, ideally we want to check if GL_EXT_gpu_shader4 OR GLSL 1.40 (GL 3.2) is supported

    public static GLBufferTexture create(@NonNull OpenGL gl) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLBufferTexture(gl);
    }

    private GLBufferTexture(@NonNull OpenGL gl) {
        super(gl, TextureTarget.TEXTURE_BUFFER);
    }

    /**
     * Sets the buffer object which provides the storage for this buffer texture.
     *
     * @param internalFormat the internal format of the data in the given buffer
     * @param buffer         the buffer
     */
    public void setBuffer(@NonNull TextureInternalFormat internalFormat, @NonNull GLBuffer buffer) {
        this.checkOpen();

        //actually attach the buffer object to this buffer texture
        if (this.dsa) {
            this.gl.glTextureBuffer(this.id, internalFormat.id(), buffer.id());
        } else {
            this.bind(target -> {
                this.gl.glTexBuffer(target.id(), internalFormat.id(), buffer.id());
            });
        }
    }
}
