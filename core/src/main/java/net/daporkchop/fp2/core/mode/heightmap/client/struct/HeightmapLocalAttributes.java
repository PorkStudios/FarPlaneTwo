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

package net.daporkchop.fp2.core.mode.heightmap.client.struct;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayTransform;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.FieldsAsArrayAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarTransform;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public class HeightmapLocalAttributes {
    @Attribute
    public int state;

    @FieldsAsArrayAttribute(
            attribute = @Attribute(name = "light"),
            names = { "lightBlock", "lightSky" },
            scalarType = @ScalarTransform(interpret = {
                    @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
                    @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)
            }),
            transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
    public byte lightBlock;
    public byte lightSky;

    @Attribute
    @ScalarTransform(expand = @ScalarExpand(
            value = ScalarExpand.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA, alpha = false,
            thenConvert = @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)))
    public int color;

    @FieldsAsArrayAttribute(
            attribute = @Attribute(name = "posHoriz"),
            names = { "posHorizX", "posHorizZ" },
            scalarType = @ScalarTransform(interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED)),
            transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
    public byte posHorizX;
    public byte posHorizZ;

    @Attribute
    @ScalarTransform(interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED))
    public int heightInt;

    @Attribute
    @ScalarTransform(interpret = {
            @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
            @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false)
    })
    public byte heightFrac;
}
