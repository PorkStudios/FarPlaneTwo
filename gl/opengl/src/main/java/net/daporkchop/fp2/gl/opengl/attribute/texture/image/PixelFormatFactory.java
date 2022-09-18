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

package net.daporkchop.fp2.gl.opengl.attribute.texture.image;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.OpenGL;

/**
 * @author DaPorkchop_
 */
@Getter
public class PixelFormatFactory {
    protected final OpenGL gl;

    protected final PixelStorageFormats storageFormats;
    protected final PixelStorageTypes storageTypes;
    protected final PixelInternalFormats internalFormats;

    public PixelFormatFactory(@NonNull OpenGL gl) {
        this.gl = gl;

        this.storageFormats = new PixelStorageFormats(gl);
        this.storageTypes = new PixelStorageTypes(gl);
        this.internalFormats = new PixelInternalFormats(gl);
    }

    public PixelFormatImpl build(@NonNull PixelFormatBuilderImpl builder) {
        PixelInternalFormat internalFormat = this.internalFormats.getOptimalInternalFormatFor(builder);
        PixelStorageFormat storageFormat = this.storageFormats.getOptimalStorageFormatFor(builder);
        PixelStorageType storageType = this.storageTypes.getOptimalStorageTypeFor(builder, storageFormat);

        return new PixelFormatImpl(this.gl, internalFormat, storageFormat, storageType);
    }
}
