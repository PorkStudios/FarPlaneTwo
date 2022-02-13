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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.command.AbstractCommandBufferBuilder;
import net.daporkchop.fp2.gl.opengl.command.CodegenArgs;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperty;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class CompositeUop implements Uop {
    @NonNull
    protected final State state;
    @NonNull
    protected final List<Uop> children;

    @Override
    public Stream<StateProperty> depends() {
        return Stream.empty();
    }

    @Override
    public void emitCode(@NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
        MutableState state = this.state().mutableSnapshot();

        this.children.forEach(uop -> {
            uop.depends()
                    .filter(property -> property instanceof StateValueProperty)
                    .map(property -> (StateValueProperty<?>) property)
                    .distinct()
                    .forEach(property -> {
                        uop.state().get(property).ifPresent(value -> {
                            if (!Objects.equals(value, state.getOrDef(property))) {
                                state.set(property, uncheckedCast(value));
                                property.set(value, writer);
                            }
                        });
                    });

            uop.emitCode(builder, writer);
        });

        //reset to initial state
        this.state().properties().forEach(property -> {
            if (!Objects.equals(this.state().getOrDef(property), state.getOrDef(property))) {
                property.set(uncheckedCast(this.state().getOrDef(property)), writer);
            }
        });
    }
}
