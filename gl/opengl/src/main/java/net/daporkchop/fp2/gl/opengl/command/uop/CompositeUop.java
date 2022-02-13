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

package net.daporkchop.fp2.gl.opengl.command.uop;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.command.AbstractCommandBufferBuilder;
import net.daporkchop.fp2.gl.opengl.command.CodegenArgs;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperty;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public class CompositeUop extends Uop {
    protected final List<Uop> children;

    public CompositeUop(@NonNull List<Uop> children) {
        super(children.get(0).state());

        this.children = children;
    }

    @Override
    protected Stream<StateProperty> dependsFirst() {
        return this.children.stream().flatMap(Uop::dependsFirst).distinct();
    }

    @Override
    public void emitCode(@NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
        this.children.forEach(uop -> uop.emitCode(builder, writer));
    }
}
