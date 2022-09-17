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
 */

package net.daporkchop.fp2.gl.attribute.texture.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The channels which may be used in a pixel format.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum PixelFormatChannel {
    /**
     * The red color channel.
     * <p>
     * Does not depend on any other channels.
     */
    RED(Collections.emptySet()),
    /**
     * The green color channel.
     * <p>
     * Depends on the {@link #RED} channel.
     */
    GREEN(Collections.unmodifiableSet(EnumSet.of(RED))),
    /**
     * The blue color channel.
     * <p>
     * Depends on the {@link #RED} and {@link #GREEN} channels.
     */
    BLUE(Collections.unmodifiableSet(EnumSet.of(RED, GREEN))),
    /**
     * The alpha color channel.
     * <p>
     * Depends on the {@link #RED}, {@link #GREEN} and {@link #BLUE} channels.
     */
    ALPHA(Collections.unmodifiableSet(EnumSet.of(RED, GREEN, BLUE)));

    //TODO: add support for depth and/or stencil textures?

    /**
     * A set of {@link PixelFormatChannel} which must be present in a pixel format in order for this pixel format channel to be used.
     */
    private final Set<PixelFormatChannel> depends;
}
