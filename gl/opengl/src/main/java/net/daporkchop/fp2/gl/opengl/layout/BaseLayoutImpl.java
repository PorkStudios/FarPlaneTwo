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
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import java.util.List;
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

    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> allFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> uniformFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> globalFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> localFormatsByName;

    protected final BiMap<String, GLSLField> allAttribsByName;
    protected final BiMap<String, GLSLField> uniformAttribsByName;
    protected final BiMap<String, GLSLField> globalAttribsByName;
    protected final BiMap<String, GLSLField> localAttribsByName;

    public BaseLayoutImpl(@NonNull BaseLayoutBuilderImpl<?> builder) {
        this.gl = builder.gl;
        this.api = this.gl.api();

        {
            Collector<BaseAttributeFormatImpl<?, ?>, ?, BiMap<String, BaseAttributeFormatImpl<?, ?>>> formatToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(BaseAttributeFormatImpl::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attribute formats into a single map (also ensures names are unique)
            this.allFormatsByName = Stream.of(builder.uniforms, builder.globals, builder.locals).flatMap(List::stream).collect(formatToMapCollector);

            //create maps for formats, separated by usage
            this.uniformFormatsByName = builder.uniforms.stream().collect(formatToMapCollector);
            this.globalFormatsByName = builder.globals.stream().collect(formatToMapCollector);
            this.localFormatsByName = builder.locals.stream().collect(formatToMapCollector);
        }

        {
            Collector<GLSLField, ?, BiMap<String, GLSLField>> attribToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(GLSLField::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attributes into a single map (also ensures names are unique)
            this.allAttribsByName = Stream.of(builder.uniforms, builder.globals, builder.locals).flatMap(List::stream).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);

            //create maps for attributes, separated by usage
            this.uniformAttribsByName = builder.uniforms.stream().map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.globalAttribsByName = builder.globals.stream().map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.localAttribsByName = builder.locals.stream().map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
        }
    }

    public abstract void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder);

    public abstract void configureProgramPreLink(int program);

    public abstract void configureProgramPostLink(int program);
}
