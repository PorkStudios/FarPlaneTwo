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

package net.daporkchop.fp2.core.client.shader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A registry of {@link NewReloadableShaderProgram reloadable shader programs}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class ReloadableShaderRegistry {
    @NonNull
    private final FP2Core fp2;
    private final Map<Object, NewReloadableShaderProgram<?>> programs = Collections.synchronizedMap(new HashMap<>());

    /**
     * Gets a builder for a reloadable compute shader program which will be managed by this registry.
     *
     * @param key           a unique key to identify the program in this registry
     * @param macros        the macros defined in the shader
     * @param setupFunction a function for configuring additional settings necessary when linking the shader
     * @return a builder for the shader
     */
    public NewReloadableShaderProgram.ComputeBuilder createCompute(@NonNull Object key, @NonNull ShaderMacros macros, NewReloadableShaderProgram.SetupFunction<? super ComputeShaderProgram.Builder> setupFunction) {
        return new NewReloadableShaderProgram.ComputeBuilder(this, this.fp2, key, macros, setupFunction);
    }

    /**
     * Gets a builder for a reloadable draw shader program which will be managed by this registry.
     *
     * @param key           a unique key to identify the program in this registry
     * @param macros        the macros defined in the shader
     * @param setupFunction a function for configuring additional settings necessary when linking the shader
     * @return a builder for the shader
     */
    public NewReloadableShaderProgram.DrawBuilder createDraw(@NonNull Object key, @NonNull ShaderMacros macros, NewReloadableShaderProgram.SetupFunction<? super DrawShaderProgram.Builder> setupFunction) {
        return new NewReloadableShaderProgram.DrawBuilder(this, this.fp2, key, macros, setupFunction);
    }

    /**
     * Adds a new shader program to this registry.
     *
     * @param key     the unique key which identifies the program in the registry
     * @param program the shader program
     */
    void register(@NonNull Object key, @NonNull NewReloadableShaderProgram<?> program) {
        checkState(this.programs.putIfAbsent(key, program) == null, "this registry already contains a program with the key: %s", key);
    }

    /**
     * Removes an existing shader program from this registry.
     *
     * @param key     the unique key which identifies the program in the registry
     * @param program the shader program
     */
    boolean unregister(@NonNull Object key, @NonNull NewReloadableShaderProgram<?> program) {
        return this.programs.remove(key, program);
    }

    /**
     * Gets the registered shader program with the given key.
     *
     * @param key the unique key which identifies the program in the registry
     * @return the shader program with the given key
     * @throws RuntimeException if this registry does not contain a value with the given key
     */
    public <P extends ShaderProgram> NewReloadableShaderProgram<P> get(@NonNull Object key) {
        NewReloadableShaderProgram<?> program = this.programs.get(key);
        checkArg(program != null, "no such program with key: %s", key);
        return uncheckedCast(program);
    }

    /**
     * Reloads all registered shader programs.
     * <p>
     * If any shader fails to reload, all changes will be rolled back.
     *
     * @throws ShaderReloadFailedException if at least one of the shaders couldn't be reloaded
     */
    @CalledFromClientThread
    public void reload() throws ShaderReloadFailedException {
        int programCount = this.programs.size();

        OpenGL gl = this.fp2.client().gl();
        ResourceProvider resourceProvider = this.fp2.client().resourceProvider();

        List<NewReloadableShaderProgram<?>> reloadablePrograms = new ArrayList<>(this.programs.values());
        List<ShaderProgram> reloadedPrograms = new ArrayList<>(programCount);
        try (val ignored = PResourceUtil.lazyCloseAll(reloadedPrograms)) {
            ShaderReloadFailedException cause = null;
            int failCount = 0;
            for (val reloadableProgram : reloadablePrograms) {
                ShaderProgram newProgram;
                try {
                    newProgram = reloadableProgram.compile(gl, resourceProvider);
                } catch (ShaderCompilationException | ShaderLinkageException e) {
                    if (cause == null) {
                        cause = new ShaderReloadFailedException();
                    }
                    cause.addSuppressed(e);
                    failCount++;
                    continue;
                }

                reloadedPrograms.add(newProgram);
            }

            if (cause == null) {
                //all shaders were compiled successfully, replace them with the new ones
                for (int i = 0; i < reloadablePrograms.size(); i++) {
                    NewReloadableShaderProgram<?> reloadableProgram = reloadablePrograms.get(i);
                    //replace the element in reloadedPrograms with the old program so that it gets closed in the finally block
                    reloadableProgram.program = uncheckedCast(reloadedPrograms.set(i, reloadableProgram.program));
                }

                this.fp2.client().chat().success("§areloaded %d shader(s)", programCount);
            } else {
                this.fp2.log().error("shader reload failed", cause);
                this.fp2.client().chat().error("§c%d/%d shaders failed to reload (check log for info)", failCount, programCount);
                throw cause;
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShaderReloadFailedException extends Exception {
    }
}
