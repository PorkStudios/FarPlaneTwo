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

package net.daporkchop.fp2.core.mode.voxel.client.struct;

import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.VectorType;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarTransform;

/**
 * @author DaPorkchop_
 */
@Attribute(name = "state", typeScalar = @ScalarType(int.class))
@Attribute(name = "light", typeVector = @VectorType(components = 2, componentType = @ScalarType(value = byte.class, interpret = {
        @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
        @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)
})))
@Attribute(name = "color", typeVector = @VectorType(components = 4, componentType = @ScalarType(value = byte.class, interpret = {
        @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
        @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = true)
})))
@Attribute(name = "pos", typeVector = @VectorType(components = 3, componentType = @ScalarType(value = byte.class, interpret = {
        @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED),
        @ScalarConvert(value = ScalarConvert.Type.TO_FLOAT, normalized = false)
})))
public interface VoxelLocalAttributes extends AttributeStruct {
    @AttributeSetter
    VoxelLocalAttributes state(int state);

    @AttributeSetter
    VoxelLocalAttributes light(int block, int sky);

    @AttributeSetter
    VoxelLocalAttributes color(@ScalarTransform(expand = @ScalarExpand(ScalarExpand.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA)) int argb8);

    @AttributeSetter
    VoxelLocalAttributes color(int r, int g, int b, int a);

    @AttributeSetter
    VoxelLocalAttributes pos(int x, int y, int z);
}
