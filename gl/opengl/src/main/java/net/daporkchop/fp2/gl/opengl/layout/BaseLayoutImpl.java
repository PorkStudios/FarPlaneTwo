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

package net.daporkchop.fp2.gl.opengl.layout;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseLayoutImpl implements BaseLayout {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final BiMap<String, AttributeImpl> allAttribsByName;
    protected final BiMap<String, AttributeImpl> uniformAttribsByName;
    protected final BiMap<String, AttributeImpl> globalAttribsByName;
    protected final BiMap<String, AttributeImpl> localAttribsByName;
    protected final BiMap<String, AttributeImpl> outputAttribsByName;

    protected final BiMap<String, AttributeFormatImpl> allFormatsByName;
    protected final BiMap<String, AttributeFormatImpl> uniformFormatsByName;
    protected final BiMap<String, AttributeFormatImpl> globalFormatsByName;
    protected final BiMap<String, AttributeFormatImpl> localFormatsByName;
    protected final BiMap<String, AttributeFormatImpl> outputFormatsByName;

    public BaseLayoutImpl(@NonNull BaseLayoutBuilderImpl builder) {
        this.gl = builder.gl;
        this.api = this.gl.api();

        {
            Collector<AttributeFormatImpl, ?, BiMap<String, AttributeFormatImpl>> formatToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(AttributeFormatImpl::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attribute formats into a single map (also ensures names are unique)
            this.allFormatsByName = Stream.of(builder.uniforms, builder.globals, builder.locals, builder.outputs).flatMap(Stream::of).collect(formatToMapCollector);

            //create maps for formats, separated by usage
            this.uniformFormatsByName = Stream.of(builder.uniforms).collect(formatToMapCollector);
            this.globalFormatsByName = Stream.of(builder.globals).collect(formatToMapCollector);
            this.localFormatsByName = Stream.of(builder.locals).collect(formatToMapCollector);
            this.outputFormatsByName = Stream.of(builder.outputs).collect(formatToMapCollector);
        }

        {
            Collector<AttributeImpl, ?, BiMap<String, AttributeImpl>> attribToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(AttributeImpl::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attributes into a single map (also ensures names are unique)
            this.allAttribsByName = Stream.of(builder.uniforms, builder.globals, builder.locals, builder.outputs).flatMap(Stream::of).map(AttributeFormatImpl::attribsArray).flatMap(Stream::of).collect(attribToMapCollector);

            //create maps for attributes, separated by usage
            this.uniformAttribsByName = Stream.of(builder.uniforms).map(AttributeFormatImpl::attribsArray).flatMap(Stream::of).collect(attribToMapCollector);
            this.globalAttribsByName = Stream.of(builder.globals).map(AttributeFormatImpl::attribsArray).flatMap(Stream::of).collect(attribToMapCollector);
            this.localAttribsByName = Stream.of(builder.locals).map(AttributeFormatImpl::attribsArray).flatMap(Stream::of).collect(attribToMapCollector);
            this.outputAttribsByName = Stream.of(builder.outputs).map(AttributeFormatImpl::attribsArray).flatMap(Stream::of).collect(attribToMapCollector);
        }
    }

    public abstract void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder);

    public abstract void configureProgramPreLink(int program);

    public abstract void configureProgramPostLink(int program);
}
