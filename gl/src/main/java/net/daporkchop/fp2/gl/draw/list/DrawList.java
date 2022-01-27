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

package net.daporkchop.fp2.gl.draw.list;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;

/**
 * A list of multiple {@link DrawCommand}s which can be executed in a batch.
 * <p>
 * Using a {@link DrawList} makes it possible to efficiently issue many draw commands at once. Additionally, as {@link DrawList}s are only indirectly referenced in a command buffer,
 * they can be modified and resized without the command buffer being rebuilt.
 * <p>
 * Drawing from a {@link DrawList}s is the only way in which global draw attributes can be accessed. The index of a draw command inside the list is used to retrieve the corresponding
 * global draw attribute values from the bound buffer.
 * <p>
 * Draw lists can be <strong>selected</strong>. This allows commands to be conditionally disabled at draw time without the command buffer being rebuilt. Selection can be done
 * in one of two ways:<br>
 * <ol>
 *     <li>{@link CommandBufferBuilder#drawList(DrawShaderProgram, DrawMode, DrawList, GLBitSet)} selects commands using a {@link GLBitSet} with the same capacity as this draw
 *     list. A draw command at a given index will be disabled if the corresponding bit at the same index is {@code false}.</li>
 *     <li>{@link CommandBufferBuilder#drawList(DrawShaderProgram, DrawMode, DrawList, TransformShaderProgram, TransformBinding)} selects commands using a "selection shader", which
 *     is a special case of a transform shader. Selection shaders are constructed by the {@link DrawList} using the corresponding
 *     {@link #configureTransformLayoutForSelection(TransformLayoutBuilder)}, {@link #configureTransformBindingForSelection(TransformBindingBuilder)},
 *     {@link #configureTransformShaderForSelection(TransformShaderBuilder)} and {@link #configureTransformShaderProgramForSelection(TransformShaderProgramBuilder)} methods. Unlike
 *     typical transform shaders, which transform an array of source data into an array of destination data, selection shaders are special in that they transform an array of source
 *     data <i>where each element corresponds to a single draw command</i> into an array of {@code bool}s indicating whether or not the command should be drawn. Users must not supply
 *     their own {@code void main()} in the selection shader source, but must instead define a function {@code bool select()} which returns {@code false} if the current draw command
 *     should be disabled.</li>
 * </ol>
 *
 * @author DaPorkchop_
 */
public interface DrawList<C extends DrawCommand> extends GLResource {
    //
    // GENERAL
    //

    /**
     * @return the number of commands that this buffer can store
     */
    int capacity();

    /**
     * Sets the capacity of this command buffer.
     * <p>
     * If the new capacity is less than the current capacity, the buffer's contents will be truncated. If greater than the current capacity, the
     * data will be extended with empty commands.
     *
     * @param capacity the new capacity
     */
    void resize(int capacity);

    /**
     * Sets the command at the given index to the given command.
     *
     * @param index   the command index
     * @param command the command
     */
    void set(int index, @NonNull C command);

    /**
     * Gets the command at the given index.
     *
     * @param index the command index
     * @return the command
     */
    C get(int index);

    /**
     * Sets the command at the given index to an empty command.
     *
     * @param index the command index
     */
    void clear(int index);

    //
    // SELECTION
    //

    /**
     * Configures a {@link TransformLayoutBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformLayoutBuilder} to configure
     * @return the configured {@link TransformLayoutBuilder}
     */
    default TransformLayoutBuilder configureTransformLayoutForSelection(@NonNull TransformLayoutBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures a {@link TransformBindingBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformBindingBuilder} to configure
     * @return the configured {@link TransformBindingBuilder}
     */
    default TransformBindingBuilder configureTransformBindingForSelection(@NonNull TransformBindingBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures a {@link TransformShaderBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformShaderBuilder} to configure
     * @return the configured {@link TransformShaderBuilder}
     */
    default TransformShaderBuilder configureTransformShaderForSelection(@NonNull TransformShaderBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures a {@link TransformShaderProgramBuilder} for being used for draw list command selection.
     *
     * @param builder the {@link TransformShaderProgramBuilder} to configure
     * @return the configured {@link TransformShaderProgramBuilder}
     */
    default TransformShaderProgramBuilder configureTransformShaderProgramForSelection(@NonNull TransformShaderProgramBuilder builder) {
        throw new UnsupportedOperationException();
    }
}
