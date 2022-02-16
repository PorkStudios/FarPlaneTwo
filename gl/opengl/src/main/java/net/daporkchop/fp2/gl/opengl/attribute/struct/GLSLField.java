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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Data
@With
@SuppressWarnings("UnstableApiUsage")
public final class GLSLField<T extends GLSLType> {
    private static final Interner<GLSLField<?>> INTERNER = Interners.newWeakInterner();

    @NonNull
    protected final T type;
    @NonNull
    protected final String name;

    /**
     * @return the GLSL declaration for this field
     */
    public String declaration() {
        return this.type.declaration(this.name);
    }

    public GLSLField<T> intern() {
        return uncheckedCast(INTERNER.intern(this.withType(uncheckedCast(this.type.intern())).withName(this.name.intern())));
    }

    public Stream<GLSLField<? extends GLSLBasicType>> basicFields() {
        return this.type.basicFields(this.name);
    }
}
