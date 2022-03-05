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

package net.daporkchop.fp2.core.client.render;

import net.daporkchop.fp2.gl.attribute.annotation.ArrayTransform;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.FieldsAsArrayAttribute;

/**
 * @author DaPorkchop_
 */
public class GlobalUniformAttributes {
    //
    // camera
    //

    @Attribute
    public final float @ArrayType(length = 16, transform = @ArrayTransform(value = ArrayTransform.Type.TO_MATRIX, matrixCols = 4, matrixRows = 4)) [] modelViewProjectionMatrix = new float[16];

    @FieldsAsArrayAttribute(
            attribute = @Attribute(name = "positionFloor"),
            names = { "positionFloorX", "positionFloorY", "positionFloorZ" },
            transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
    public int positionFloorX;
    public int positionFloorY;
    public int positionFloorZ;

    @FieldsAsArrayAttribute(
            attribute = @Attribute(name = "positionFrac"),
            names = { "positionFracX", "positionFracY", "positionFracZ" },
            transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
    public float positionFracX;
    public float positionFracY;
    public float positionFracZ;

    //
    // fog
    //

    @FieldsAsArrayAttribute(
            attribute = @Attribute(name = "fogColor"),
            names = { "fogColorR", "fogColorG", "fogColorB", "fogColorA" },
            transform = @ArrayTransform(ArrayTransform.Type.TO_VECTOR))
    public float fogColorR;
    public float fogColorG;
    public float fogColorB;
    public float fogColorA;

    @Attribute
    public int fogMode;

    @Attribute
    public float fogDensity;

    @Attribute
    public float fogStart;

    @Attribute
    public float fogEnd;

    @Attribute
    public float fogScale;

    //
    // misc. GL state
    //

    @Attribute
    public float alphaRefCutout;
}
