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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Extension of {@link AttributeUsage} with additional usage types. Only used internally.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum InternalAttributeUsage {
    UNIFORM(AttributeUsage.UNIFORM, "u_"),
    UNIFORM_ARRAY(AttributeUsage.UNIFORM_ARRAY, "ua_"),
    DRAW_LOCAL(AttributeUsage.DRAW_LOCAL, "dl_"),
    DRAW_GLOBAL(AttributeUsage.DRAW_GLOBAL, "dg_"),
    TRANSFORM_INPUT(AttributeUsage.TRANSFORM_INPUT, "ti_"),
    TRANSFORM_OUTPUT(AttributeUsage.TRANSFORM_OUTPUT, "to_"),
    FRAGMENT_COLOR(null, "f_"),
    TEXTURE(null, "t_");

    private static final InternalAttributeUsage[] VALUES = values();

    static {
        InternalAttributeUsage[] internal = VALUES;
        AttributeUsage[] external = AttributeUsage.values();

        checkState(internal.length >= external.length);
        for (int i = 0; i < external.length; i++) {
            checkState(external[i].name().equals(internal[i].name()), "%s != %s", external[i].name(), internal[i].name());
        }
    }

    /**
     * Gets the {@link InternalAttributeUsage} equivalent to the given {@link AttributeUsage}.
     *
     * @param external the external {@link AttributeUsage}
     * @return the equivalent {@link InternalAttributeUsage}
     */
    public static InternalAttributeUsage fromExternal(AttributeUsage external) {
        return VALUES[external.ordinal()];
    }

    private final AttributeUsage external;
    private final String glslPrefix;
}
