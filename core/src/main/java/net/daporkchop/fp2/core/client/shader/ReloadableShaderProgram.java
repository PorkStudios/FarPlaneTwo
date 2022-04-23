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

package net.daporkchop.fp2.core.client.shader;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.capability.CloseableResource;
import net.daporkchop.fp2.core.event.AbstractReloadEvent;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import java.util.function.Supplier;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public interface ReloadableShaderProgram<P extends BaseShaderProgram<?>> extends Supplier<P>, CloseableResource {
    static ReloadableShaderProgram<DrawShaderProgram> draw(@NonNull GL gl, @NonNull DrawLayout layout, @NonNull ShaderMacros macros, @NonNull Identifier vertexShaderSource, @NonNull Identifier fragmentShaderSource) {
        return new AbstractReloadableShaderProgram<DrawShaderProgram>(macros) {
            @Override
            protected void reload(@NonNull ShaderMacros.Immutable macrosSnapshot) throws ShaderCompilationException, ShaderLinkageException {
                try (
                        VertexShader vertexShader = gl.createVertexShader(layout)
                                .defineAll(macrosSnapshot.macros())
                                .include(vertexShaderSource)
                                .build();
                        FragmentShader fragmentShader = gl.createFragmentShader(layout)
                                .defineAll(macrosSnapshot.macros())
                                .include(fragmentShaderSource)
                                .build()) {
                    this.program = gl.createDrawShaderProgram(layout)
                            .addShader(vertexShader)
                            .addShader(fragmentShader)
                            .build();
                }
            }
        };
    }

    static void reloadAll() {
        new AbstractReloadEvent<ReloadableShaderProgram<?>>() {
            @Override
            protected void handleSuccess(int total) {
                fp2().client().chat().success("§areloaded %d shader(s)", total);
            }

            @Override
            protected void handleFailure(int failed, int total, @NonNull Throwable cause) {
                fp2().log().error("shader reload failed", cause);
                fp2().client().chat().error("§c%d/%d shaders failed to reload (check log for info)", failed, total);
            }
        }.fire();
    }
}
