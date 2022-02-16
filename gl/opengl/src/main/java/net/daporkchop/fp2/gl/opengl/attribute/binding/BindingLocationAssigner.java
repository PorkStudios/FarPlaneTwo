/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.binding;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLExtension;
import net.daporkchop.fp2.gl.opengl.OpenGL;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class BindingLocationAssigner {
    protected final int maxFragmentColors;
    protected final int maxShaderStorageBuffers;
    protected final int maxTextureUnits;
    protected final int maxVertexAttributes;
    protected final int maxUniformBuffers;

    protected int fragmentColors;
    protected int shaderStorageBuffers;
    protected int textureUnits;
    protected int vertexAttributes;
    protected int uniformBuffers;

    public BindingLocationAssigner(@NonNull OpenGL gl, @NonNull GLAPI api) {
        this.maxFragmentColors = api.glGetInteger(GL_MAX_DRAW_BUFFERS);
        this.maxShaderStorageBuffers = GLExtension.GL_ARB_shader_storage_buffer_object.supported(gl) ? api.glGetInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0;
        this.maxTextureUnits = api.glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        this.maxVertexAttributes = api.glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        this.maxUniformBuffers = api.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
    }

    public int fragmentColor() {
        checkState(this.fragmentColors < this.maxFragmentColors, "cannot use more than %d fragment colors!", this.maxFragmentColors);
        return this.fragmentColors++;
    }

    public int shaderStorageBuffer() {
        checkState(this.shaderStorageBuffers < this.maxShaderStorageBuffers, "cannot use more than %d shader storage buffers!", this.maxShaderStorageBuffers);
        return this.shaderStorageBuffers++;
    }

    public int textureUnit() {
        checkState(this.textureUnits < this.maxTextureUnits, "cannot use more than %d texture units!", this.maxTextureUnits);
        return this.textureUnits++;
    }

    public int vertexAttribute(int slots) {
        checkState(this.vertexAttributes + slots <= this.maxVertexAttributes, "cannot use more than %d vertex attributes!", this.maxVertexAttributes);
        int idx = this.vertexAttributes;
        this.vertexAttributes += slots;
        return idx;
    }

    public int uniformBuffer() {
        checkState(this.uniformBuffers < this.maxUniformBuffers, "cannot use more than %d uniform buffers!", this.maxUniformBuffers);
        return this.uniformBuffers++;
    }
}
