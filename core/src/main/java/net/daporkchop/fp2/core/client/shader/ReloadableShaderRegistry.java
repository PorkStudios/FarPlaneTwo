/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.Shader;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A registry of {@link ReloadableShaderProgram reloadable shader programs}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ReloadableShaderRegistry implements AutoCloseable {
    /**
     * @return a new {@link Builder} instance
     */
    public static ReloadableShaderRegistry.Builder builder(@NonNull FP2Core fp2, @NonNull OpenGL gl) {
        return new Builder(fp2, gl);
    }

    private final @NonNull FP2Core fp2;
    private final @NonNull OpenGL gl;
    private final @NonNull ImmutableMap<Object, ReloadableShaderProgram<?>> programs;

    private transient Map<Identifier, byte[]> lastResourceHashes;

    /**
     * Gets the registered shader program with the given key.
     *
     * @param key the unique key which identifies the program in the registry
     * @return the shader program with the given key
     * @throws RuntimeException if this registry does not contain a value with the given key
     */
    public <P extends ShaderProgram> ReloadableShaderProgram<P> get(@NonNull Object key) {
        ReloadableShaderProgram<?> program = this.programs.get(key);
        checkArg(program != null, "no registered program with key: %s", key);
        return uncheckedCast(program);
    }

    /**
     * Gets the registered shader programs with the given keys.
     *
     * @param keys the unique keys which identify the programs in the registry
     * @return the shader programs with the given keys
     * @throws RuntimeException if this registry does not contain a value with one or more of the given keys
     */
    public <K, P extends ShaderProgram> ReloadableShaderPrograms<K, P> getAll(@NonNull Collection<K> keys) {
        ImmutableMap.Builder<K, ReloadableShaderProgram<P>> builder = ImmutableMap.builder();
        for (K key : keys) {
            builder.put(key, this.get(key));
        }
        return new ReloadableShaderPrograms<>(builder.build());
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(program -> {
            PResourceUtil.close(program.program);
            program.program = null;
        }, this.programs.values());
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

        boolean parallelCompileEnabled = this.fp2.globalConfig().performance().parallelShaderCompile();
        boolean GL_ARB_parallel_shader_compile = gl.supports(GLExtension.GL_ARB_parallel_shader_compile);
        boolean GL_KHR_parallel_shader_compile = gl.supports(GLExtension.GL_KHR_parallel_shader_compile);

        ResourceProvider cachingResourceProvider = ResourceProvider.caching(this.fp2.client().resourceProvider());
        ResourceProvider.AccessTracker trackingResourceProvider = new ResourceProvider.AccessTracker(cachingResourceProvider);

        if (this.lastResourceHashes != null && //cache is up-to-date
                this.lastResourceHashes.entrySet().parallelStream().allMatch(entry -> {
                    Identifier id = entry.getKey();
                    byte[] previousHash = entry.getValue();

                    try {
                        byte[] newHash = hashResource(cachingResourceProvider, id);
                        return Arrays.equals(previousHash, newHash);
                    } catch (ResourceNotFoundException ignored) {
                        //the resource has been removed since the last reload, we need to reload the shaders!
                        return false;
                    }
                })) {

            //all resources are unchanged, exit early
            this.fp2.client().chat().success("§aNot reloading %d shader(s) (already up-to-date)", programCount);
            return;
        }

        //the state for a single shader program as its being reloaded
        @RequiredArgsConstructor
        final class ReloadState implements AutoCloseable {
            final ReloadableShaderProgram<?> reloadableProgram;

            List<Shader.CompileTask> compileTasks;
            List<Shader> compiledShaders;
            ShaderProgram.LinkTask<?> linkTask;
            ShaderProgram newProgram;
            ShaderProgram oldProgram;

            Throwable failureCause;

            @Override
            public void close() {
                PResourceUtil.closeAll(
                        PResourceUtil.lazyCloseAll(this.compileTasks),
                        PResourceUtil.lazyCloseAll(this.compiledShaders),
                        this.linkTask,
                        this.newProgram,
                        this.oldProgram);
            }
        }

        long startTime = System.nanoTime();

        List<ReloadState> reloadStates = new ArrayList<>(programCount);
        try (val ignored = PResourceUtil.lazyCloseAll(reloadStates)) {
            for (ReloadableShaderProgram<?> reloadableProgram : this.programs.values()) {
                reloadStates.add(new ReloadState(reloadableProgram));
            }

            int failCount = 0;

            //save the previous compile thread count and set the new one
            int oldCompileThreadsCount = 0;
            int newCompileThreadsCount = parallelCompileEnabled ? -1 : 0;
            if (GL_ARB_parallel_shader_compile) {
                oldCompileThreadsCount = gl.glGetInteger(GL_MAX_SHADER_COMPILER_THREADS_ARB);
                gl.glMaxShaderCompilerThreadsARB(newCompileThreadsCount);
            } else if (GL_KHR_parallel_shader_compile) {
                oldCompileThreadsCount = gl.glGetInteger(GL_MAX_SHADER_COMPILER_THREADS_KHR);
                gl.glMaxShaderCompilerThreadsKHR(newCompileThreadsCount);
            }

            if (parallelCompileEnabled) {
                //parallel compilation is enabled! we will run each step for every registered program before advancing, so that capable drivers can compile/link shaders in parallel. for drivers
                //  which don't support that, this will be slightly slower and use a bit more memory than the serial approach, but the added overhead should be pretty insignificant compared to the
                //  time it takes for the actual compilation and linking.

                //begin compiling all the shaders
                for (ReloadState reloadState : reloadStates) {
                    try {
                        reloadState.compileTasks = reloadState.reloadableProgram.compileAsync(gl, trackingResourceProvider);
                    } catch (Exception e) { //save exception for later and continue
                        reloadState.failureCause = e;
                        failCount++;
                    }
                }

                //finish compiling and begin linking all the programs
                for (ReloadState reloadState : reloadStates) {
                    if (reloadState.failureCause == null) {
                        try {
                            //wait for all the shaders to finish compiling
                            reloadState.compiledShaders = new ArrayList<>(reloadState.compileTasks.size());
                            for (Shader.CompileTask compileTask : reloadState.compileTasks) {
                                reloadState.compiledShaders.add(compileTask.join());
                            }

                            //begin linking the program
                            reloadState.linkTask = reloadState.reloadableProgram.linkAsync(gl, reloadState.compiledShaders);
                        } catch (Exception e) { //save exception for later and continue
                            reloadState.failureCause = e;
                            failCount++;
                        }
                    }
                }

                //wait for all the programs to finish linking
                for (ReloadState reloadState : reloadStates) {
                    if (reloadState.failureCause == null) {
                        try {
                            reloadState.newProgram = reloadState.linkTask.join();
                        } catch (Exception e) { //save exception for later and continue
                            reloadState.failureCause = e;
                            failCount++;
                        }
                    }
                }
            } else {
                //parallel compilation is explicitly disabled, compile the shaders serially
                for (ReloadState reloadState : reloadStates) {
                    try {
                        reloadState.newProgram = reloadState.reloadableProgram.compileSync(gl, trackingResourceProvider);
                    } catch (Exception e) { //save exception for later and continue
                        reloadState.failureCause = e;
                        failCount++;
                    }
                }
            }

            //restore old compile thread count
            if (GL_ARB_parallel_shader_compile) {
                gl.glMaxShaderCompilerThreadsARB(oldCompileThreadsCount);
            } else if (GL_KHR_parallel_shader_compile) {
                gl.glMaxShaderCompilerThreadsKHR(oldCompileThreadsCount);
            }

            if (failCount == 0) {
                //all shaders were compiled successfully, replace them with the new ones
                for (ReloadState reloadState : reloadStates) {
                    reloadState.oldProgram = reloadState.reloadableProgram.program; //store the old program in the reload state so it gets closed
                    reloadState.reloadableProgram.program = uncheckedCast(reloadState.newProgram);
                    reloadState.newProgram = null; //set the linked program in the reload state to null since the ownership has been transferred to the ReloadableShaderProgram instance
                }

                this.fp2.client().chat().success("§areloaded %d shader(s) in %.3fs", programCount, (System.nanoTime() - startTime) / (1000.0d * 1000.0d * 1000.0d));

                //hash all the files that were accessed so we can detect if anything changed on a subsequent reload
                this.lastResourceHashes = trackingResourceProvider.accessedResourceIdentifiers().parallelStream()
                        .collect(Collectors.toConcurrentMap(
                                Function.identity(),
                                id -> hashResource(cachingResourceProvider, id)));
            } else {
                //collect all the exceptions
                ShaderReloadFailedException cause = new ShaderReloadFailedException();
                for (ReloadState reloadState : reloadStates) {
                    if (reloadState.failureCause != null) {
                        cause.addSuppressed(reloadState.failureCause);
                    }
                }

                this.fp2.log().error("shader reload failed", cause);
                this.fp2.client().chat().error("§c%d/%d shaders failed to reload (check log for info)", failCount, programCount);
                throw cause;
            }
        }
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private static byte[] hashResource(ResourceProvider resourceProvider, Identifier id) throws IOException, ResourceNotFoundException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); //should be available by default

        digest.update(resourceProvider.provideResourceAsBytes(id));
        return digest.digest();
    }

    /**
     * @author DaPorkchop_
     */
    public static class ShaderReloadFailedException extends Exception {
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder {
        final @NonNull FP2Core fp2;
        final @NonNull OpenGL gl;

        final Set<Object> programKeys = new HashSet<>();
        final List<ReloadableShaderProgram.Builder<?, ?, ?>> programBuilders = new ArrayList<>();

        private void registerProgram(@NonNull Object key, @NonNull ReloadableShaderProgram.Builder<?, ?, ?> programBuilder) {
            if (!this.programKeys.add(key)) {
                throw new IllegalStateException("this registry already contains a program with the key: " + key);
            }
            this.programBuilders.add(programBuilder);
        }

        /**
         * Gets a builder for a reloadable compute shader program which will be managed by this registry.
         *
         * @param key           a unique key to identify the program in this registry
         * @param macros        the macros defined in the shader
         * @param setupFunction a function for configuring additional settings necessary when linking the shader
         * @return a builder for the shader
         */
        public ReloadableShaderProgram.ComputeBuilder registerCompute(@NonNull Object key, @NonNull ShaderMacros macros, ReloadableShaderProgram.SetupFunction<? super ComputeShaderProgram.Builder> setupFunction) {
            val programBuilder = new ReloadableShaderProgram.ComputeBuilder(key, macros, setupFunction);
            this.registerProgram(key, programBuilder);
            return programBuilder;
        }

        /**
         * Gets a builder for a reloadable draw shader program which will be managed by this registry.
         *
         * @param key           a unique key to identify the program in this registry
         * @param macros        the macros defined in the shader
         * @param setupFunction a function for configuring additional settings necessary when linking the shader
         * @return a builder for the shader
         */
        public ReloadableShaderProgram.DrawBuilder registerDraw(@NonNull Object key, @NonNull ShaderMacros macros, ReloadableShaderProgram.SetupFunction<? super DrawShaderProgram.Builder> setupFunction) {
            val programBuilder = new ReloadableShaderProgram.DrawBuilder(key, macros, setupFunction);
            this.registerProgram(key, programBuilder);
            return programBuilder;
        }

        /**
         * Finishes building this registry and performs the initial compilation of all shaders.
         *
         * @return the constructed {@link ReloadableShaderRegistry}
         * @throws ShaderReloadFailedException if at least one of the shaders couldn't be loaded
         */
        public ReloadableShaderRegistry build() throws ShaderReloadFailedException {
            //build all the ReloadableShaderProgram instances.
            //  we don't need to worry about cleaning up resources here if an exception is closed, as none of the ReloadableShaderProgram instances actually contain a program instance yet
            ImmutableMap.Builder<Object, ReloadableShaderProgram<?>> programsBuilder = ImmutableMap.builder();
            for (val programBuilder : this.programBuilders) {
                programsBuilder.put(programBuilder.key, programBuilder.build());
            }

            //actually construct the registry instance and load the shaders
            ReloadableShaderRegistry result = new ReloadableShaderRegistry(this.fp2, this.gl, programsBuilder.build());
            try {
                result.reload();
                return result;
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, result);
            }
        }
    }
}
