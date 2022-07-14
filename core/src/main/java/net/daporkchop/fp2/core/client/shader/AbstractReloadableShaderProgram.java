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
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.generic.FReloadEvent;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Base implementation of {@link ReloadableShaderProgram}.
 *
 * @author DaPorkchop_
 */
abstract class AbstractReloadableShaderProgram<P extends BaseShaderProgram<?>> implements ReloadableShaderProgram<P> {
    protected final ShaderMacros macros;

    protected ShaderMacros.Immutable macrosSnapshot;
    protected P program;

    @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
    public AbstractReloadableShaderProgram(@NonNull ShaderMacros macros) {
        this.macros = macros;

        //initially compile the shader
        this.macrosSnapshot = macros.snapshot();
        this.reload(this.macrosSnapshot);

        //register this program to receive reload events
        fp2().eventBus().registerWeak(this);
    }

    protected abstract void reload(@NonNull ShaderMacros.Immutable macrosSnapshot) throws ShaderCompilationException, ShaderLinkageException;

    @Override
    @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
    public P get() {
        ShaderMacros.Immutable macrosSnapshot = this.macros.snapshot();
        if (macrosSnapshot != this.macrosSnapshot) {
            this.reload(macrosSnapshot);

            this.macrosSnapshot = macrosSnapshot;
        }

        return this.program;
    }

    @Override
    public void close() {
        //stop receiving reload events
        fp2().eventBus().unregister(this);

        this.program.close();
    }

    @FEventHandler
    protected void onReload(@NonNull FReloadEvent<ReloadableShaderProgram<?>> event) {
        event.doReload(() -> this.reload(this.macrosSnapshot));
    }
}
