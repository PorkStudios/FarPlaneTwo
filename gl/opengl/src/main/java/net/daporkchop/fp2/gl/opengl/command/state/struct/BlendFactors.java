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

package net.daporkchop.fp2.gl.opengl.command.state.struct;

import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.daporkchop.fp2.gl.command.BlendFactor;

/**
 * @author DaPorkchop_
 */
@Data
@With
public final class BlendFactors {
    private static boolean usesUserColor(BlendFactor factor) {
        switch (factor) {
            case CONSTANT_COLOR:
            case ONE_MINUS_CONSTANT_COLOR:
            case CONSTANT_ALPHA:
            case ONE_MINUS_CONSTANT_ALPHA:
                return true;
            default:
                return false;
        }
    }

    @NonNull
    private final BlendFactor srcRGB;
    @NonNull
    private final BlendFactor srcA;
    @NonNull
    private final BlendFactor dstRGB;
    @NonNull
    private final BlendFactor dstA;

    /**
     * @return whether or not any of the blend factors uses the user-defined constant color
     */
    public boolean usesUserColor() {
        return usesUserColor(this.srcRGB) || usesUserColor(this.srcA) || usesUserColor(this.dstRGB) || usesUserColor(this.dstA);
    }
}
