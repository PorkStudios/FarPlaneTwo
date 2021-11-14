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

import net.daporkchop.fp2.gl.attribute.Attrib;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructFormatGenerator;
import net.daporkchop.fp2.gl.opengl.attribute.struct.VertexAttributeLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;

/**
 * @author DaPorkchop_
 */
public class StructCodegenTest {
    @Attrib(convert = Attrib.Conversion.TO_FLOAT)
    public int attrIntAsFloat;

    @Attrib(convert = Attrib.Conversion.TO_NORMALIZED_FLOAT)
    public int attrIntAsNormalizedFloat;

    @Attrib(convert = Attrib.Conversion.TO_UNSIGNED)
    public short attrUnsignedShortAsInt;

    @Attrib(convert = Attrib.Conversion.TO_NORMALIZED_FLOAT)
    public byte attrByteAsNormalizedFloat;

    @Attrib(convert = { Attrib.Conversion.TO_UNSIGNED, Attrib.Conversion.TO_FLOAT })
    public byte attrByteAsUnsignedFloat;

    @Attrib(vectorAxes = { "X", "_Y", "Z" }, convert = Attrib.Conversion.TO_NORMALIZED_FLOAT)
    public short attrVecShortAsNormalizedIntX;
    public short attrVecShortAsNormalizedInt_Y;
    public short attrVecShortAsNormalizedIntZ;

    @Attrib(transform = Attrib.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGB, convert = Attrib.Conversion.TO_NORMALIZED_FLOAT)
    public int attrColorRGB;

    @Attrib(transform = Attrib.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGBA, convert = Attrib.Conversion.TO_NORMALIZED_FLOAT)
    public int attrColorARGB;

    @Attrib(transform = Attrib.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGB, convert = Attrib.Conversion.TO_FLOAT)
    public int attrColorRGB_notNormalized;

    @Attrib(transform = Attrib.Transformation.INT_ARGB8_TO_BYTE_VECTOR_RGB)
    public int attrColorRGB_int;

    @Attrib(transform = Attrib.Transformation.ARRAY_TO_MATRIX, matrixDimension = @Attrib.MatrixDimension(columns = 4, rows = 4))
    public float[] fMatrix = new float[16];

    @Attrib(transform = Attrib.Transformation.ARRAY_TO_MATRIX,
            convert = { Attrib.Conversion.TO_UNSIGNED, Attrib.Conversion.TO_NORMALIZED_FLOAT },
            matrixDimension = @Attrib.MatrixDimension(columns = 4, rows = 4))
    public short[] normalizedUshortMatrix = new short[16];

    public static void main(String... args) throws Throwable {
        StructInfo<StructCodegenTest> info = new StructInfo<>(StructCodegenTest.class);

        {
            StringBuilder builder = new StringBuilder();
            info.glslStructDefinition(builder);
            System.out.println(builder);
        }

        InterleavedStructFormat<StructCodegenTest> formatStd140 = new StructFormatGenerator().getInterleaved(GLSLBlockMemoryLayout.STD140.layout(info));
        InterleavedStructFormat<StructCodegenTest> formatVA = new StructFormatGenerator().getInterleaved(VertexAttributeLayout.interleaved(info));
    }
}
