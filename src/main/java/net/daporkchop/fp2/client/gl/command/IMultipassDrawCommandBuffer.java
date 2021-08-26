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

package net.daporkchop.fp2.client.gl.command;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.function.IntPredicate;

/**
 * A buffer of multiple {@link IDrawCommand}s, with multiple commands sharing the same index (multiple render passes).
 *
 * @author DaPorkchop_
 */
public interface IMultipassDrawCommandBuffer<C extends IDrawCommand> extends RefCounted {
    // general methods

    @Override
    int refCnt();

    @Override
    IMultipassDrawCommandBuffer<C> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    /**
     * @return the number of separate render passes
     */
    int passes();

    /**
     * @return a new {@link C} compatible with the draw commands stored in this command buffer
     */
    C newCommand();

    /**
     * @return the concrete class for type {@link C}
     */
    Class<C> commandClass();

    // contents

    /**
     * @return the number of commands in this buffer
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
     * Loads the commands at the given index into the given {@link C} instance.
     *
     * @param commands the instance for the command to be loaded into (one command per pass)
     * @param index    the index to load the command from
     */
    void load(@NonNull C[] commands, int index);

    /**
     * Stores the commands from the given {@link C} instance at the given index.
     *
     * @param commands the instance for the command to be stored (one command per pass)
     * @param index    the index to store the command at
     */
    void store(@NonNull C[] commands, int index);

    /**
     * Clears the given number of commands starting at the given index.
     *
     * @param index the index of the first command to clear
     * @param count the number of commands to be cleared
     */
    void clearRange(int index, int count);

    // drawing

    /**
     * Executes the draw commands in this command buffer.
     *
     * @param pass the render pass
     */
    void draw(int pass);

    // shaders and selection

    /**
     * Configures the given shader builder according to the internal implementation details of this draw command buffer.
     *
     * @param builder the builder to configure
     * @return the configured builder
     */
    <B extends ShaderManager.AbstractShaderBuilder<B, S>, S extends ShaderProgram<S>> B configureShader(@NonNull B builder);

    /**
     * Selects which commands should be rendered using a compute shader.
     * <p>
     * The shader must:
     * - have included {@code comp/command_buffer_selection.comp}
     * - have been passed to {@link #configureShader(ShaderManager.AbstractShaderBuilder)} before linkage
     * - be active
     * <p>
     * If any methods are called which would modify the state of this command buffer (such as {@link #store(IDrawCommand[], int)} or {@link #resize(int)}), the
     * effects of selection will be lost.
     *
     * @param computeShaderProgram the compute shader program to use for selection
     */
    void select(@NonNull ComputeShaderProgram computeShaderProgram);

    /**
     * Selects which commands should be rendered using a Java function.
     * <p>
     * The selector will be called with the indices of all non-empty draw commands, and returns whether or not they may be rendered in subsequent calls
     * to {@link #draw(int)}.
     * <p>
     * If any methods are called which would modify the state of this command buffer (such as {@link #store(IDrawCommand[], int)} or {@link #resize(int)}), the
     * effects of selection will be lost.
     *
     * @param selector the function to use for selection
     */
    void select(@NonNull IntPredicate selector);
}
