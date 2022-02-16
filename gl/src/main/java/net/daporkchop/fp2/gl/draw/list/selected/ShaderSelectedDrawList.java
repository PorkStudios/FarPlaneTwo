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

package net.daporkchop.fp2.gl.draw.list.selected;

import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;

/**
 * A {@link DrawList} whose draw commands are selected by a transform shader.
 * <p>
 * Commands are selected using a "selection shader", which is a special case of a transform shader. Selection shaders are constructed by the {@link ShaderSelectedDrawList} using the
 * corresponding {@link #configureTransformLayoutForSelection(TransformLayoutBuilder)}, {@link #configureTransformBindingForSelection(TransformBindingBuilder)},
 * {@link #configureTransformShaderForSelection(TransformShaderBuilder)} and {@link #configureTransformShaderProgramForSelection(TransformShaderProgramBuilder)} methods. Unlike
 * typical transform shaders, which transform an array of source data into an array of destination data, selection shaders are special in that they transform an array of source
 * data <i>where each element corresponds to a single draw command</i> into an array of {@code bool}s indicating whether or not the command should be drawn. Users must not supply
 * their own {@code void main()} in the selection shader source, but must instead define a function {@code bool select()} which returns {@code false} if the current draw command
 * should be disabled.
 *
 * @author DaPorkchop_
 */
public interface ShaderSelectedDrawList<C extends DrawCommand> extends SelectedDrawList<C> {
    /**
     * Configures a {@link TransformLayoutBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformLayoutBuilder} to configure
     * @return the configured {@link TransformLayoutBuilder}
     */
    TransformLayoutBuilder configureTransformLayoutForSelection(@NonNull TransformLayoutBuilder builder);

    /**
     * Configures a {@link TransformBindingBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformBindingBuilder} to configure
     * @return the configured {@link TransformBindingBuilder}
     */
    TransformBindingBuilder configureTransformBindingForSelection(@NonNull TransformBindingBuilder builder);

    /**
     * Configures a {@link TransformShaderBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformShaderBuilder} to configure
     * @return the configured {@link TransformShaderBuilder}
     */
    TransformShaderBuilder configureTransformShaderForSelection(@NonNull TransformShaderBuilder builder);

    /**
     * Configures a {@link TransformShaderProgramBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformShaderProgramBuilder} to configure
     * @return the configured {@link TransformShaderProgramBuilder}
     */
    TransformShaderProgramBuilder configureTransformShaderProgramForSelection(@NonNull TransformShaderProgramBuilder builder);
}
