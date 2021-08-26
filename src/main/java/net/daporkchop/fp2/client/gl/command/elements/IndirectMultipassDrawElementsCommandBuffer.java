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

package net.daporkchop.fp2.client.gl.command.elements;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.DrawMode;
import net.daporkchop.fp2.client.gl.ElementType;
import net.daporkchop.fp2.client.gl.command.AbstractMultipassDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.command.IMultipassDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.util.alloc.Allocator;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link IMultipassDrawCommandBuffer} which buffers {@link DrawElementsCommand}s and executes them using indirect multidraw.
 *
 * @author DaPorkchop_
 */
public class IndirectMultipassDrawElementsCommandBuffer extends AbstractMultipassDrawCommandBuffer<DrawElementsCommand> implements IMultipassDrawElementsCommandBuffer {
    protected final ElementType type;
    protected final GLBuffer buffer = new GLBuffer(GL_STREAM_DRAW);

    protected final long stride;
    protected long addr;

    protected boolean needsBarrier;

    public IndirectMultipassDrawElementsCommandBuffer(@NonNull Allocator alloc, int passes, @NonNull DrawMode mode, @NonNull ElementType type) {
        super(alloc, passes, mode);

        this.type = type;
        this.stride = DrawElementsIndirectCommand._SIZE * passes;
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        this.addr = this.alloc.realloc(this.addr, newCapacity * this.stride);

        if (oldCapacity < newCapacity) { //extend with blank commands
            this.clearRange(oldCapacity, newCapacity - oldCapacity);
        }
        this.dirty = true;
    }

    @Override
    protected void doRelease() {
        this.buffer.delete();
    }

    protected void flush() {
        if (this.dirty) { //contents are dirty, re-upload
            this.dirty = false;

            try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
                buffer.upload(this.addr, this.capacity * this.stride);
            }
        }
    }

    @Override
    public void load(@NonNull DrawElementsCommand[] commands, int index) {
        checkArg(commands.length == this.passes, "invalid number of commands (%d, expected %d)", commands.length, this.passes);

        long addr = this.addr + checkIndex(this.capacity, index) * this.stride;
        DrawElementsIndirectCommand tmp = new DrawElementsIndirectCommand();
        for (int pass = 0; pass < this.passes; pass++, addr += DrawElementsIndirectCommand._SIZE) {
            tmp.load(addr);
            commands[pass].count(tmp.count()).firstIndex(tmp.firstIndex()).baseVertex(tmp.baseVertex());
        }
    }

    @Override
    public void store(@NonNull DrawElementsCommand[] commands, int index) {
        checkArg(commands.length == this.passes, "invalid number of commands (%d, expected %d)", commands.length, this.passes);

        long addr = this.addr + checkIndex(this.capacity, index) * this.stride;
        DrawElementsIndirectCommand tmp = new DrawElementsIndirectCommand().baseInstance(index).instanceCount(1);
        for (int pass = 0; pass < this.passes; pass++, addr += DrawElementsIndirectCommand._SIZE) {
            DrawElementsCommand command = commands[pass];
            tmp.count(command.count()).firstIndex(command.firstIndex()).baseVertex(command.baseVertex()).store(addr);
        }
        this.dirty = true;
    }

    @Override
    public void clearRange(int index, int count) {
        checkRangeLen(this.capacity, index, count);

        //fill with blank commands, but make sure we set the baseInstance field to the command index
        long addr = this.addr + index * this.stride;
        DrawElementsIndirectCommand command = new DrawElementsIndirectCommand();
        for (int i = index; i < index + count; i++) {
            command.baseInstance(index);
            for (int pass = 0; pass < this.passes; pass++, addr += DrawElementsIndirectCommand._SIZE) {
                command.store(addr);
            }
        }
        this.dirty = true;
    }

    @Override
    public void draw(int pass) {
        checkIndex(this.passes, pass);

        this.flush();

        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            if (this.needsBarrier) { //place a memory barrier
                this.needsBarrier = false;
                glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
            }

            glMultiDrawElementsIndirect(this.mode.gl(), this.type.gl(), pass * DrawElementsIndirectCommand._SIZE, this.capacity, toInt(this.stride));
        }
    }

    @Override
    public <B extends ShaderManager.AbstractShaderBuilder<B, S>, S extends ShaderProgram<S>> B configureShader(@NonNull B builder) {
        return builder.define("MULTIPASS_COMMAND_BUFFER_PASS_COUNT", this.passes)
                .define("MULTIPASS_COMMAND_BUFFER_TYPE_MULTIDRAW_INDIRECT");
    }

    @Override
    public void select(@NonNull ComputeShaderProgram computeShaderProgram) {
        this.flush();

        this.buffer.bindBase(GL_SHADER_STORAGE_BUFFER, 4);
        computeShaderProgram.dispatch(this.capacity);
        this.needsBarrier = true;
    }
}
