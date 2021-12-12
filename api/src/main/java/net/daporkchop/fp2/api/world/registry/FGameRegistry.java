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

package net.daporkchop.fp2.api.world.registry;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A registry for converting implementation-dependent game resources to their unique IDs.
 * <p>
 * In general, IDs should be considered arbitrary integers. They must not be persisted unless sufficient care is taken to ensure that all data is invalidated when the ID mappings
 * are changed (see {@link #registryToken()}). However, implementations must respect the following rules when assigning IDs:
 * <ul>
 *     <li>IDs must never be negative. If more than {@link Integer#MAX_VALUE} IDs would be required, an exception should be thrown when constructing the registry.</li>
 *     <li>The ID {@code 0} is always valid. For biomes, it may be assigned to any valid biome. For states, it must always be assigned to air.</li>
 *     <li>IDs should be allocated in a mostly sequential order. They do not necessarily have to be tightly packed, but users should be able to use them as simple array
 *     indices without wasting too much memory.</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public interface FGameRegistry {
    //
    // GENERAL
    //

    /**
     * Returns a {@code byte[]} which identifies the contents of this registry.
     * <p>
     * The {@code byte[]}'s contents should be considered arbitrary data. Users should not attempt to parse or otherwise read it, it is simply a unique token. The implementation
     * provides no guarantees as to the contents of the data, only that if two tokens are exactly equal (e.g. when compared using {@link Arrays#equals(byte[], byte[])}), it is safe
     * to assume that the two registries contain identical mappings. For instance, one could persistently save this token to disk alongside other data utilizing the same registry
     * mappings, and if the saved token is unchanged on the next load it would be safe to re-use the other saved data (the data would have to be erased and re-generated if the token
     * were to change).
     *
     * @return a {@code byte[]} which identifies the contents of this registry, or an empty {@link Optional} if none is available
     */
    Optional<byte[]> registryToken();

    //
    // BIOMES
    //

    /**
     * @return an {@link IntStream} over all the biomes in this registry
     */
    IntStream biomes();

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
     * @return a {@link FExtendedBiomeRegistryData} instance for the biomes in this registry
     */
    FExtendedBiomeRegistryData extendedBiomeRegistryData();

    //
    // (BLOCK) STATES
    //

    /**
     * @return an {@link IntStream} over all the states in this registry
     */
    IntStream states();

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

    /**
     * @return a {@link FExtendedStateRegistryData} instance for the states in this registry
     */
    FExtendedStateRegistryData extendedStateRegistryData();
}
