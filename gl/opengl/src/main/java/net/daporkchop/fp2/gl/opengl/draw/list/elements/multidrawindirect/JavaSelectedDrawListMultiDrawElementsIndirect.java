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

package net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect;

import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.SimpleDrawListImpl;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;
import java.util.function.IntPredicate;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect.MultiDrawElementsIndirect.*;

/**
 * @author DaPorkchop_
 */
public class JavaSelectedDrawListMultiDrawElementsIndirect extends DrawListMultiDrawElementsIndirect implements SimpleDrawListImpl.JavaSelected<DrawCommandIndexed> {
    public JavaSelectedDrawListMultiDrawElementsIndirect(@NonNull DrawListBuilderImpl builder) {
        super(builder);
    }

    @Override
    public Map<StateValueProperty<?>, Object> configureStateForDrawSelected0(@NonNull State state) {
        return this.configureStateForDraw0(state);
    }

    @Override
    public void drawSelected0(GLAPI api, int mode, IntPredicate selector) {
        long dstCommands = PUnsafe.allocateMemory(this.capacity * _SIZE);
        try {
            { //copy all commands, setting the instance count to 0 if the selector wants it disabled
                long srcCommandAddr = this.commandsAddr;
                long dstCommandAddr = dstCommands;
                for (int i = 0; i < this.capacity; i++, srcCommandAddr += _SIZE, dstCommandAddr += _SIZE) {
                    _firstIndex(dstCommandAddr, _firstIndex(srcCommandAddr));
                    _count(dstCommandAddr, _count(srcCommandAddr));
                    _baseVertex(dstCommandAddr, _baseVertex(srcCommandAddr));
                    _baseInstance(dstCommandAddr, _baseInstance(srcCommandAddr));
                    _instanceCount(dstCommandAddr, selector.test(i) ? _instanceCount(srcCommandAddr) : 0);
                }
            }

            api.glBufferData(GL_DRAW_INDIRECT_BUFFER, this.capacity * _SIZE, dstCommands, GL_STREAM_DRAW);
            api.glMultiDrawElementsIndirect(mode, this.indexType, 0L, this.capacity, 0);
        } finally {
            PUnsafe.freeMemory(dstCommands);
        }
    }
}
