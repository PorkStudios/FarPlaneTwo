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

package net.daporkchop.fp2.api.event;

import java.util.Optional;

/**
 * A special type of event whose handlers may have a return value.
 * <p>
 * Unlike handlers for other event types, which must always return {@code void}, handlers for a {@link ReturningEvent} of type {@link T} may have one of three return value types:
 * <ul>
 *     <li>if {@link Optional<T>}, the {@link Optional}'s value will be added to the event output if present, and discarded otherwise</li>
 *     <li>if {@link T}, the behavior will be conceptually the same as if the value were first wrapped into an {@link Optional} as if by {@link Optional#of(Object)}
 *     and subsequently handled according to the first rule</li>
 *     <li>if {@code void}, the handler has no return value, and so the "return value" will be discarded in any case</li>
 * </ul>
 * Monitor handlers must always have a return type of {@code void} in any case.
 *
 * @author DaPorkchop_
 */
public interface ReturningEvent<T> {
}
