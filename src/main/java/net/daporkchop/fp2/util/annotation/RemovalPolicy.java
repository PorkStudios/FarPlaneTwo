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

package net.daporkchop.fp2.util.annotation;

/**
 * Defines how to handle removal of an element which is being deleted at runtime by another annotation.
 *
 * @author DaPorkchop_
 * @see DebugOnly
 */
public enum RemovalPolicy {
    /**
     * The element will be removed from the class bytecode, but all references to it will be left as-is.
     */
    DELETE,
    /**
     * The element will be removed from the class bytecode. Any bytecode references to it will cause an exception to be thrown during class loading.
     */
    ERROR_LOAD,
    /**
     * The element will be removed from the class bytecode. Any bytecode references to it will be replaced with bytecode equivalent to {@code throw new AssertionError();}.
     */
    ERROR_RUNTIME,
    /**
     * The element will be removed from the class bytecode, and all references to it will be silently discarded.
     * <p>
     * Note that this WILL cause issues if the member's (return) value is used.
     */
    DROP;
}
