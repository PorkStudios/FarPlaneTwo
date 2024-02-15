/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.api.world.registry;

import net.daporkchop.fp2.api.world.level.BlockLevelConstants;

/**
 * Extended information describing the attributes of individual states using IDs from a {@link FGameRegistry}.
 *
 * @author DaPorkchop_
 */
public interface FExtendedStateRegistryData {
    /**
     * @return the {@link FGameRegistry} containing the state ID mappings
     */
    FGameRegistry registry();

    /**
     * Gets the block type for the given state ID.
     * <p>
     * The resulting value will be one of the block types defined in {@link BlockLevelConstants}:
     * <ul>
     *     <li>{@link BlockLevelConstants#BLOCK_TYPE_INVISIBLE}</li>
     *     <li>{@link BlockLevelConstants#BLOCK_TYPE_TRANSPARENT}</li>
     *     <li>{@link BlockLevelConstants#BLOCK_TYPE_OPAQUE}</li>
     * </ul>
     *
     * @param state the state ID
     * @return the block type
     * @throws IndexOutOfBoundsException if {@code state} is not a valid state ID
     */
    int type(int state) throws IndexOutOfBoundsException;

    /**
     * Gets the light emission level for the given state ID.
     * <p>
     * The resulting value will an integer greater than or equal to {@code 0} and less than or equal to {@link BlockLevelConstants#MAX_LIGHT}.
     *
     * @param state the state ID
     * @return the light emission level
     * @throws IndexOutOfBoundsException if {@code state} is not a valid state ID
     */
    int lightEmission(int state) throws IndexOutOfBoundsException;

    /**
     * Gets the light opacity level for the given state ID.
     * <p>
     * The resulting value will an integer greater than or equal to {@code 0} and less than or equal to {@link BlockLevelConstants#MAX_LIGHT}.
     *
     * @param state the state ID
     * @return the light opacity level
     * @throws IndexOutOfBoundsException if {@code state} is not a valid state ID
     */
    int lightOpacity(int state) throws IndexOutOfBoundsException;
}
