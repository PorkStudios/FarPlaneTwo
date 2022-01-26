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

package net.daporkchop.fp2.gl.attribute;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * All things which an attribute format may be used for.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum AttributeUsage {
    /**
     * Buffers using this attribute format contain data which may be used as uniforms.
     */
    UNIFORM("u_", ""),
    /**
     * Buffers using this attribute format contain data which may be used as a uniform array.
     */
    UNIFORM_ARRAY("ua_", ""),
    /**
     * Buffers using this attribute format contain data which may be used as global vertex data when drawing.
     */
    DRAW_GLOBAL("dg_", ""),
    /**
     * Buffers using this attribute format contain data which may be used as local vertex data when drawing.
     */
    DRAW_LOCAL("dl_", ""),
    /**
     * Buffers using this attribute format may be used as the input for transform commands.
     */
    TRANSFORM_INPUT("ti_", ""),
    /**
     * Buffers using this attribute format may be used as the output for transform commands.
     */
    TRANSFORM_OUTPUT("to_", ""),
    /**
     * Buffers using this attribute format may be used as textures.
     */
    TEXTURE("t_", ""),
    /**
     * Buffers using this attribute format may be used as fragment colors.
     */
    FRAGMENT_COLOR("f_", "");

    @NonNull
    private final String defaultPrefix;
    @NonNull
    private final String defaultSuffix;
}
