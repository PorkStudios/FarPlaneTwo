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

package net.daporkchop.fp2.core.mode.heightmap.client.struct;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.gl.attribute.Attribute;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public class HeightmapLocalAttributes {
    @Attribute
    public int a_state;

    @Attribute(
            vectorAxes = { "Block", "Sky" },
            convert = { Attribute.Conversion.TO_UNSIGNED, Attribute.Conversion.TO_NORMALIZED_FLOAT })
    public byte a_lightBlock;
    public byte a_lightSky;

    @Attribute(
            transform = Attribute.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGB,
            convert = Attribute.Conversion.TO_NORMALIZED_FLOAT)
    public int a_color;

    @Attribute(
            vectorAxes = { "X", "Z" },
            convert = Attribute.Conversion.TO_UNSIGNED)
    public byte a_posHorizX;
    public byte a_posHorizZ;

    @Attribute(convert = Attribute.Conversion.TO_UNSIGNED)
    public int a_heightInt;

    @Attribute(convert = { Attribute.Conversion.TO_UNSIGNED, Attribute.Conversion.TO_FLOAT })
    public byte a_heightFrac;
}
