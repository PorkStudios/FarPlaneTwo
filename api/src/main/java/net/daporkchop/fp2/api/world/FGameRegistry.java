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

package net.daporkchop.fp2.api.world;

import lombok.NonNull;

/**
 * A registry for converting implementation-dependent game resources to their unique IDs.
 *
 * @author DaPorkchop_
 */
public interface FGameRegistry {
    /**
     * Gets the unique ID corresponding to the biome described by the given {@link Object}.
     *
     * @param biome an {@link Object} which describes a biome
     * @return the biome's unique ID
     * @throws UnsupportedOperationException if the implementation cannot describe a biome using a single {@link Object}
     * @throws ClassCastException            if {@code biome} is not of the type used by the implementation to describe a biome
     */
    int biome2id(@NonNull Object biome) throws UnsupportedOperationException, ClassCastException;

    /**
     * Gets the unique ID corresponding to the state described by the given {@link Object}.
     *
     * @param state an {@link Object} which describes a state
     * @return the state's unique ID
     * @throws UnsupportedOperationException if the implementation cannot describe a state using a single {@link Object}
     * @throws ClassCastException            if {@code state} is not of the type used by the implementation to describe a state
     */
    int state2id(@NonNull Object state) throws UnsupportedOperationException, ClassCastException;

    /**
     * Gets the unique ID corresponding to the state described by the given {@link Object} and the given {@code int}.
     *
     * @param block an {@link Object} which can be combined with {@code meta} to describe a state
     * @param meta  an {@code int} which can be combined with {@code block} to describe a state
     * @return the state's unique ID
     * @throws UnsupportedOperationException if the implementation cannot describe a state using an {@link Object} and an {@code int}
     * @throws ClassCastException            if {@code block} is not of the type used by the implementation to describe a state
     */
    int state2id(@NonNull Object block, int meta) throws UnsupportedOperationException, ClassCastException;
}
