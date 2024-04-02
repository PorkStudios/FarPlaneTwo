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

package net.daporkchop.fp2.gl.shader.source;

import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.daporkchop.fp2.api.util.Identifier;

/**
 * A single line of source code, along with metadata describing its location.
 *
 * @author DaPorkchop_
 */
@Data
public final class SourceLine {
    @With
    @NonNull
    private final String text;

    @NonNull
    private final Identifier location;
    private final int lineNumber;

    private final SourceLine includedFrom;

    @Override
    public String toString() {
        return this.toString(null, true);
    }

    public String toString(String prefix) {
        return this.toString(prefix, true);
    }

    public String toString(String prefix, boolean includeText) {
        StringBuilder builder = new StringBuilder();
        if (prefix != null) {
            builder.append(prefix).append(' ');
        }
        builder.append("at <").append(this.location).append(' ').append(this.lineNumber).append('>');
        if (includeText) {
            builder.append(": ").append(this.text);
        }
        if (this.includedFrom != null) {
            builder.append("\nincluded from: ").append(this.includedFrom.toString(prefix, includeText));
        }
        return builder.toString();
    }
}
