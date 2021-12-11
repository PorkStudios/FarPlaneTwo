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

package net.daporkchop.fp2.core.client.render;

import net.daporkchop.fp2.gl.attribute.Attribute;

/**
 * @author DaPorkchop_
 */
public class GlobalUniformAttributes {
    @Attribute(
            transform = Attribute.Transformation.ARRAY_TO_MATRIX,
            matrixDimension = @Attribute.MatrixDimension(columns = 4, rows = 4))
    public final float[] u_modelViewProjectionMatrix = new float[16];

    @Attribute(vectorAxes = { "X", "Y", "Z" })
    public int u_positionFloorX;
    public int u_positionFloorY;
    public int u_positionFloorZ;

    @Attribute(vectorAxes = { "X", "Y", "Z" })
    public float u_positionFracX;
    public float u_positionFracY;
    public float u_positionFracZ;

    @Attribute(vectorAxes = { "R", "G", "B", "A" })
    public float u_fogColorR;
    public float u_fogColorG;
    public float u_fogColorB;
    public float u_fogColorA;

    @Attribute
    public float u_fogDensity;

    @Attribute
    public float u_fogStart;

    @Attribute
    public float u_fogEnd;

    @Attribute
    public float u_fogScale;
}
