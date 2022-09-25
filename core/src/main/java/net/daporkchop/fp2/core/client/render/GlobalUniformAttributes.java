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

package net.daporkchop.fp2.core.client.render;

import net.daporkchop.fp2.gl.attribute.annotation.AAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.AMatrixType;
import net.daporkchop.fp2.gl.attribute.annotation.AScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.AVectorType;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayLength;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;

/**
 * @author DaPorkchop_
 */
//camera
@AAttribute(name = "modelViewProjectionMatrix", typeMatrix = @AMatrixType(cols = 4, rows = 4, componentType = @AScalarType(float.class)))
@AAttribute(name = "positionFloor", typeVector = @AVectorType(components = 3, componentType = @AScalarType(int.class)))
@AAttribute(name = "positionFrac", typeVector = @AVectorType(components = 3, componentType = @AScalarType(float.class)))
//fog
@AAttribute(name = "fogColor", typeVector = @AVectorType(components = 4, componentType = @AScalarType(float.class)))
@AAttribute(name = "fogMode", typeScalar = @AScalarType(int.class))
@AAttribute(name = "fogDensity", typeScalar = @AScalarType(float.class))
@AAttribute(name = "fogStart", typeScalar = @AScalarType(float.class))
@AAttribute(name = "fogEnd", typeScalar = @AScalarType(float.class))
@AAttribute(name = "fogScale", typeScalar = @AScalarType(float.class))
//misc. GL state
@AAttribute(name = "alphaRefCutout", typeScalar = @AScalarType(float.class))
public interface GlobalUniformAttributes {
    //
    // camera
    //

    @AttributeSetter
    GlobalUniformAttributes modelViewProjectionMatrix(float @ArrayLength(16) [] modelViewProjectionMatrix);

    @AttributeSetter
    GlobalUniformAttributes positionFloor(int x, int y, int z);

    @AttributeSetter
    GlobalUniformAttributes positionFrac(float x, float y, float z);

    //
    // fog
    //

    @AttributeSetter
    GlobalUniformAttributes fogColor(float r, float g, float b, float a);

    @AttributeSetter
    GlobalUniformAttributes fogMode(int fogMode);

    @AttributeSetter
    GlobalUniformAttributes fogDensity(float fogDensity);

    @AttributeSetter
    GlobalUniformAttributes fogStart(float fogStart);

    @AttributeSetter
    GlobalUniformAttributes fogEnd(float fogEnd);

    @AttributeSetter
    GlobalUniformAttributes fogScale(float fogScale);

    //
    // misc. GL state
    //

    @AttributeSetter
    GlobalUniformAttributes alphaRefCutout(float alphaRefCutout);
}
