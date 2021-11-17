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

package net.daporkchop.fp2.client.gl.shader.reload;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.capability.CloseableResource;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.VertexShader;

import java.util.function.Supplier;

/**
 * @author DaPorkchop_
 */
public interface ReloadableShaderProgram<P extends BaseShaderProgram> extends Supplier<P>, CloseableResource {
    static ReloadableShaderProgram<DrawShaderProgram> draw(@NonNull GL gl, @NonNull DrawLayout layout, @NonNull ShaderMacros macros, @NonNull Identifier vertexShaderSource, @NonNull Identifier fragmentShaderSource) {
        return new AbstractReloadableShaderProgram<DrawShaderProgram>(macros) {
            @Override
            protected void reload(@NonNull ShaderMacros.Immutable macrosSnapshot) throws ShaderCompilationException, ShaderLinkageException {
                try (
                        VertexShader vertexShader = gl.createVertexShader()
                                .forLayout(layout)
                                .include(vertexShaderSource).endSource()
                                .defineAll(macrosSnapshot.macros()).endDefines()
                                .build();
                        FragmentShader fragmentShader = gl.createFragmentShader()
                                .forLayout(layout)
                                .include(fragmentShaderSource).endSource()
                                .defineAll(macrosSnapshot.macros()).endDefines()
                                .build()) {
                    this.program = gl.linkShaderProgram(layout, vertexShader, fragmentShader);
                }
            }
        };
    }
}
