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

package net.daporkchop.fp2.gl.opengl.command.state;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import org.objectweb.asm.MethodVisitor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link StateProperty} with an associated value.
 * <p>
 * Property values are always {@link Optional}. An empty {@link Optional} indicates that we don't care what the property is set to.
 *
 * @author DaPorkchop_
 */
public interface StateValueProperty<T> extends StateProperty {
    /**
     * @return the property's default value
     */
    T def();

    /**
     * Generates JVM bytecode for setting the property to the given value.
     *
     * @param value       the value
     * @param mv          the {@link MethodVisitor} to which code should be written
     * @param apiLvtIndex the index of the {@link GLAPI} instance in the LVT
     */
    void set(@NonNull T value, @NonNull MethodVisitor mv, int apiLvtIndex);

    void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator);

    void restore(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, int lvtIndexBase);
}
