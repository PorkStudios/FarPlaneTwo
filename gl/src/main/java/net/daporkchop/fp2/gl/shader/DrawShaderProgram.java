/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.gl.shader;

import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A linked OpenGL shader program for drawing.
 *
 * @author DaPorkchop_
 */
public final class DrawShaderProgram extends ShaderProgram {
    public static Builder builder(OpenGL gl) {
        return new Builder(gl);
    }

    DrawShaderProgram(Builder builder) throws ShaderLinkageException {
        super(builder);
    }

    /**
     * @author DaPorkchop_
     */
    public static final class Builder extends ShaderProgram.Builder<DrawShaderProgram, Builder> {
        private boolean vertexShader;
        private boolean fragmentShader;

        private final VertexAttributeBindings vertexAttributes = new VertexAttributeBindings();

        Builder(OpenGL gl) {
            super(gl);
        }

        public Builder vertexShader(Shader vertexShader) {
            checkArg(vertexShader.type() == ShaderType.VERTEX, "not a vertex shader: %s", vertexShader.type());
            checkState(!this.vertexShader, "a vertex shader was already added");
            this.vertexShader = true;
            this.shaders.add(vertexShader);
            return this;
        }

        public Builder fragmentShader(Shader fragmentShader) {
            checkArg(fragmentShader.type() == ShaderType.FRAGMENT, "not a fragment shader: %s", fragmentShader.type());
            checkState(!this.fragmentShader, "a fragment shader was already added");
            this.fragmentShader = true;
            this.shaders.add(fragmentShader);
            return this;
        }

        public Builder vertexAttributes(NewAttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(attributeFormat, null);
        }

        public Builder vertexAttributesWithPrefix(String prefix, NewAttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(attributeFormat, prefix::concat);
        }

        public Builder vertexAttributes(NewAttributeFormat<?> attributeFormat, Function<String, String> nameFormatter) {
            checkArg(attributeFormat.supports(AttributeTarget.VERTEX_ATTRIBUTE), attributeFormat);
            this.vertexAttributes.add(this.gl.limits().maxVertexAttributes(), this.vertexAttributes.nextBindingIndex(), attributeFormat, nameFormatter, false);
            return this;
        }

        public Builder vertexAttributes(int bindingIndex, NewAttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(bindingIndex, attributeFormat, null);
        }

        public Builder vertexAttributesWithPrefix(int bindingIndex, String prefix, NewAttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(bindingIndex, attributeFormat, prefix::concat);
        }

        public Builder vertexAttributes(int bindingIndex, NewAttributeFormat<?> attributeFormat, Function<String, String> nameFormatter) {
            checkArg(attributeFormat.supports(AttributeTarget.VERTEX_ATTRIBUTE), attributeFormat);
            this.vertexAttributes.add(this.gl.limits().maxVertexAttributes(), bindingIndex, attributeFormat, nameFormatter, true);
            return this;
        }

        @Override
        public DrawShaderProgram build() throws ShaderLinkageException {
            checkState(this.vertexShader, "missing vertex shader");
            checkState(this.fragmentShader, "missing fragment shader");
            return new DrawShaderProgram(this);
        }

        @Override
        protected void configurePreLink(int program) {
            super.configurePreLink(program);
            this.vertexAttributes.configurePreLink(this.gl, program);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class VertexAttributeBindings {
        protected final BitSet occupiedBindingLocations = new BitSet();
        protected final Map<Integer, NewAttributeFormat<?>> bindings = new TreeMap<>();
        protected final Map<Integer, Function<String, String>> nameFormatters = new TreeMap<>();
        protected Boolean usedExplicit;

        public int nextBindingIndex() {
            checkState(this.usedExplicit == null || !this.usedExplicit, "a vertex attribute was already registered with an explicit binding index");
            return this.occupiedBindingLocations.length();
        }

        public void add(int maxAttribBindings, int bindingIndex, NewAttributeFormat<?> format, Function<String, String> nameFormatter, Boolean explicit) {
            if (this.usedExplicit != null) {
                checkState(this.usedExplicit == explicit, "a vertex attribute was already registered with an %s binding index", explicit ? "explicit" : "implicit");
            } else {
                this.usedExplicit = explicit;
            }

            int occupiedVertexAttributes = format.occupiedVertexAttributes();
            checkRange(maxAttribBindings, bindingIndex, bindingIndex + occupiedVertexAttributes);
            BitSet alreadyUsedBindingLocations = this.occupiedBindingLocations.get(bindingIndex, bindingIndex + occupiedVertexAttributes);
            checkArg(alreadyUsedBindingLocations.isEmpty(), "binding index %s is already configured", alreadyUsedBindingLocations);
            //TODO: check that the given binding index range is valid

            this.occupiedBindingLocations.set(bindingIndex, bindingIndex + occupiedVertexAttributes);
            this.bindings.put(bindingIndex, format);
            if (nameFormatter != null) {
                this.nameFormatters.put(bindingIndex, nameFormatter);
            }
        }

        public void configurePreLink(OpenGL gl, int program) {
            if (this.bindings.isEmpty()) {
                return;
            }

            this.bindings.forEach((bindingIndex, format) -> format.bindVertexAttributeLocations(program, this.nameFormatters.getOrDefault(bindingIndex, Function.identity()), bindingIndex));
        }
    }
}
