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

package net.daporkchop.fp2.gl.opengl.attribute.binding;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.shader.ShaderType;

/**
 * Describes a binding between an attribute format and the contents of an attribute buffer in a shader.
 *
 * @author DaPorkchop_
 */
public interface BindingLocation<B extends BaseAttributeBufferImpl<?>> {
    /**
     * @return the {@link AttributeUsage} which this binding location fulfils
     */
    AttributeUsage usage();

    /**
     * Configures the given shader program for this binding prior to linking it.
     *
     * @param api     the {@link GLAPI} instance
     * @param program the ID of the program to configure
     */
    void configureProgramPreLink(@NonNull GLAPI api, int program);

    /**
     * Configures the given shader program for this binding after linking it.
     *
     * @param api     the {@link GLAPI} instance
     * @param program the ID of the program to configure
     */
    void configureProgramPostLink(@NonNull GLAPI api, int program);

    /**
     * Generates the GLSL code required for the user to access this attribute.
     *
     * @param type    the type of shader to generate code for
     * @param builder a {@link StringBuilder} which the generated GLSL code should be appended to
     */
    void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder);

    /**
     * Configures the OpenGL state in order to bind the given {@link B} to this binding location.
     *
     * @param api    the {@link GLAPI} instance
     * @param buffer the {@link B} to bind
     */
    void configureBuffer(@NonNull GLAPI api, @NonNull B buffer);

    /**
     * Configures the OpenGL state in order to bind the given {@link B} to this binding location.
     *
     * @param state  a {@link MutableState} to write state changes to
     * @param buffer the {@link B} to bind
     */
    void configureState(@NonNull MutableState state, @NonNull B buffer);
}
