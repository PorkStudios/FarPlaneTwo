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

import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;

/**
 * A {@link DrawList} whose draw commands can be <strong>selected</strong>. This allows commands to be conditionally disabled at draw time without the command buffer being rebuilt.
 * Selection can be donein one of two ways:<br>
 * <ul>
 *     <li> selects commands using a {@link GLBitSet} with the same capacity as this draw
 *     list. A draw command at a given index will be disabled if the corresponding bit at the same index is {@code false}.</li>
 *     <li>{@link CommandBufferBuilder#drawList(DrawShaderProgram, DrawMode, ShaderSelectedDrawList, TransformShaderProgram, TransformBinding)} selects commands using a "selection shader", which
 *     is a special case of a transform shader. Selection shaders are constructed by the {@link ShaderSelectedDrawList} using the corresponding
 *     {@link ShaderSelectedDrawList#configureTransformLayoutForSelection(TransformLayoutBuilder)}, {@link ShaderSelectedDrawList#configureTransformBindingForSelection(TransformBindingBuilder)},
 *     {@link ShaderSelectedDrawList#configureTransformShaderForSelection(TransformShaderBuilder)} and {@link ShaderSelectedDrawList#configureTransformShaderProgramForSelection(TransformShaderProgramBuilder)} methods. Unlike
 *     typical transform shaders, which transform an array of source data into an array of destination data, selection shaders are special in that they transform an array of source
 *     data <i>where each element corresponds to a single draw command</i> into an array of {@code bool}s indicating whether or not the command should be drawn. Users must not supply
 *     their own {@code void main()} in the selection shader source, but must instead define a function {@code bool select()} which returns {@code false} if the current draw command
 *     should be disabled.</li>
 * </ul>
 *
 * @author DaPorkchop_
 * @see JavaSelectedDrawList
 * @see ShaderSelectedDrawList
 */
public interface SelectedDrawList<C extends DrawCommand> extends DrawList<C> {
}
