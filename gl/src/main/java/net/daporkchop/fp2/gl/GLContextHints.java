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

package net.daporkchop.fp2.gl;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

/**
 * Arguments used when creating a new OpenGL context.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@With
@Builder(toBuilder = true)
public final class GLContextHints {
    public static final GLContextHints DEFAULT = builder().build();

    /**
     * The OpenGL version to request.
     * <p>
     * If {@code null}, any arbitrary version (likely the latest supported by the driver) will be used.
     */
    private final GLVersion version;

    /**
     * The OpenGL profile to request.
     * <p>
     * Defaults to {@link GLProfile#COMPAT}.
     */
    @Builder.Default
    @NonNull
    private final GLProfile profile = GLProfile.COMPAT;

    /**
     * Whether or not a forward compatibility context should be requested.
     */
    @Builder.Default
    private final boolean forwardCompatibility = false;
}
