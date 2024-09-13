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

import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;

import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
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

    private String[] getVertexAttributeNames() {
        this.checkOpen();

        if (!this.gl.supports(GLExtension.GL_ARB_program_interface_query)) {
            return this.getVertexAttributeNamesLegacy();
        }

        String[] result = this.getActiveResourceNames(GL_PROGRAM_INPUT);
        assert Arrays.equals(result, this.getVertexAttributeNamesLegacy()) //sanity check because i'm not entirely sure how reliable the old API is
                : "Program introspection: " + Arrays.toString(result) + ", Legacy: " + Arrays.toString(this.getVertexAttributeNamesLegacy());
        return result;
    }

    private String[] getVertexAttributeNamesLegacy() {
        this.checkOpen();

        int activeAttributes = this.gl.glGetProgrami(this.id, GL_ACTIVE_ATTRIBUTES);
        int bufSize = this.gl.glGetProgrami(this.id, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH);
        String[] result = new String[activeAttributes];
        for (int attributeIndex = 0; attributeIndex < activeAttributes; attributeIndex++) {
            int[] size = new int[1];
            int[] type = new int[1];
            result[attributeIndex] = this.gl.glGetActiveAttrib(this.id, attributeIndex, bufSize, size, type);
        }
        return result;
    }

    /**
     * @author DaPorkchop_
     */
    public static final class Builder extends ShaderProgram.Builder<DrawShaderProgram, Builder> {
        private final VertexAttributeBindings vertexAttributes = new VertexAttributeBindings();

        Builder(OpenGL gl) {
            super(gl,
                    EnumSet.of(ShaderType.VERTEX, ShaderType.TESSELLATION_CONTROL, ShaderType.TESSELLATION_EVALUATION, ShaderType.GEOMETRY, ShaderType.FRAGMENT),
                    EnumSet.of(ShaderType.VERTEX));
        }

        public Builder vertexShader(Shader vertexShader) {
            checkArg(vertexShader.type() == ShaderType.VERTEX, "not a vertex shader: %s", vertexShader.type());
            return this.addShader(vertexShader);
        }

        public Builder fragmentShader(Shader fragmentShader) {
            checkArg(fragmentShader.type() == ShaderType.FRAGMENT, "not a fragment shader: %s", fragmentShader.type());
            return this.addShader(fragmentShader);
        }

        public Builder vertexAttributes(AttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(attributeFormat, null);
        }

        public Builder vertexAttributesWithPrefix(String prefix, AttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(attributeFormat, prefix::concat);
        }

        public Builder vertexAttributes(AttributeFormat<?> attributeFormat, Function<String, String> nameFormatter) {
            checkArg(attributeFormat.supports(AttributeTarget.VERTEX_ATTRIBUTE), attributeFormat);
            this.vertexAttributes.add(this.gl.limits().maxVertexAttributes(), this.vertexAttributes.nextBindingIndex(), attributeFormat, nameFormatter, false);
            return this;
        }

        public Builder vertexAttributes(int bindingIndex, AttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(bindingIndex, attributeFormat, null);
        }

        public Builder vertexAttributesWithPrefix(int bindingIndex, String prefix, AttributeFormat<?> attributeFormat) {
            return this.vertexAttributes(bindingIndex, attributeFormat, prefix::concat);
        }

        public Builder vertexAttributes(int bindingIndex, AttributeFormat<?> attributeFormat, Function<String, String> nameFormatter) {
            checkArg(attributeFormat.supports(AttributeTarget.VERTEX_ATTRIBUTE), attributeFormat);
            this.vertexAttributes.add(this.gl.limits().maxVertexAttributes(), bindingIndex, attributeFormat, nameFormatter, true);
            return this;
        }

        @Override
        protected DrawShaderProgram build0() throws ShaderLinkageException {
            return new DrawShaderProgram(this);
        }

        @Override
        protected void configurePreLink(DrawShaderProgram program) {
            super.configurePreLink(program);
            this.vertexAttributes.configurePreLink(program);
        }

        @Override
        protected void configurePostLink(DrawShaderProgram program) {
            super.configurePostLink(program);
            this.vertexAttributes.configurePostLink(program);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static final class VertexAttributeBindings {
        private final BitSet occupiedBindingLocations = new BitSet();
        private final Map<Integer, AttributeFormat<?>> bindings = new TreeMap<>();
        private final Map<Integer, Function<String, String>> nameFormatters = new TreeMap<>();
        private Boolean usedExplicit;

        public int nextBindingIndex() {
            checkState(this.usedExplicit == null || !this.usedExplicit, "a vertex attribute was already registered with an explicit binding index");
            return this.occupiedBindingLocations.length();
        }

        public void add(int maxAttribBindings, int bindingIndex, AttributeFormat<?> format, Function<String, String> nameFormatter, Boolean explicit) {
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

        public void configurePreLink(DrawShaderProgram program) {
            this.bindings.forEach((bindingIndex, format) -> {
                Function<String, String> nameFormatter = this.nameFormatters.getOrDefault(bindingIndex, Function.identity());
                format.bindVertexAttributeLocations(program.id(), nameFormatter, bindingIndex);
            });
        }

        public void configurePostLink(DrawShaderProgram program) {
            //iterate over all the the interface blocks of this sort in the program
            Set<String> unboundAttributeNames = new TreeSet<>(Arrays.asList(program.getVertexAttributeNames()));

            //ignore OpenGL-controlled vertex attributes
            unboundAttributeNames.removeIf(name -> name.startsWith("gl_"));

            this.bindings.forEach((bindingIndex, format) -> {
                Function<String, String> nameFormatter = this.nameFormatters.getOrDefault(bindingIndex, Function.identity());

                //mark each vertex attribute name as bound
                for (String name : format.vertexAttributeNames()) {
                    unboundAttributeNames.remove(nameFormatter.apply(name));
                }
            });

            if (!unboundAttributeNames.isEmpty()) {
                //at least one vertex attribute location isn't bound
                throw new ShaderLinkageException("Program contains unbound vertex attributes: " + unboundAttributeNames);
            }

            //Note that the builder may have defined additional bindings for vertex attributes not present in the shader. Unfortunately, throwing
            //  an exception in this case doesn't make much sense, since the "non-existent" vertex attribute may actually have been defined in the
            //  GLSL source code and then subsequently optimized away by the driver. Since there isn't any way for the program introspection API
            //  to differentiate between something which has been optimized away and something which was never there in the first place, throwing
            //  or logging an error would result in a lot of false positives.
        }
    }
}
