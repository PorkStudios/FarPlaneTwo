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
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.lambda.IntIntObjConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A registry of {@link NewReloadableShaderProgram reloadable shader programs}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public final class ReloadableShaderRegistry {
    private final FP2Core fp2;
    private final Set<NewReloadableShaderProgram<?>> programs = Collections.newSetFromMap(new WeakHashMap<>());

    private boolean resourcesChanged = false;

    /**
     * Adds a new shader program to this registry.
     *
     * @param program the shader program
     */
    @CalledFromClientThread
    public void register(@NonNull NewReloadableShaderProgram<?> program) {
        checkState(this.programs.add(program), "already registered: %s", program);
    }

    /**
     * Removes an existing shader program from this registry.
     *
     * @param program the shader program
     */
    @CalledFromClientThread
    public void unregister(@NonNull NewReloadableShaderProgram<?> program) {
        checkState(this.programs.remove(program), "not registered: %s", program);
    }

    /**
     * Reloads all registered shader programs.
     * <p>
     * If any shader fails to reload, all changes will be rolled back.
     *
     * @param callback a callback function to run once the shaders have been reloaded. This will be called with the following arguments:
     *                 <ol>
     *                      <li>the total number of reloaded shaders</li>
     *                      <li>the number of shaders which failed to reload</li>
     *                      <li>a {@link Throwable} indicating the reason that shader reload failed (will be {@code null} if all shaders reloaded successfully)</li>
     *                 </ol>
     */
    @CalledFromClientThread
    public void reload(boolean resourcesChanged, IntIntObjConsumer<? super Throwable> callback) {
        //if the resources were changed, or they were changed prior to a previous shader reload which failed, we want to forcibly reload all
        //  shaders (otherwise we'd only reload the ones whose macros changed)
        resourcesChanged = (this.resourcesChanged |= resourcesChanged);

        int shaderCount = this.programs.size();

        OpenGL gl = this.fp2.client().gl();
        ResourceProvider resourceProvider = this.fp2.client().resourceProvider();

        List<NewReloadableShaderProgram<?>> reloadablePrograms = new ArrayList<>(shaderCount);
        List<ShaderMacros.Immutable> macrosSnapshots = new ArrayList<>(shaderCount);
        List<ShaderProgram> newPrograms = new ArrayList<>(shaderCount);
        try {
            Throwable cause = null;
            int failCount = 0;
            for (NewReloadableShaderProgram<? extends ShaderProgram> program : this.programs) {
                ShaderMacros.Immutable macrosSnapshot = program.macros.snapshot();
                if (!resourcesChanged && macrosSnapshot == program.macrosSnapshot) { //if neither the resources nor the macros have changed, we can skip reloading this program
                    continue;
                }

                ShaderProgram newProgram;
                try {
                    newProgram = program.compileFunction.apply(gl, resourceProvider, macrosSnapshot);
                } catch (Exception e) {
                    if (cause == null) {
                        cause = new RuntimeException("shader reload failed");
                    }
                    cause.addSuppressed(e);
                    failCount++;
                    continue;
                }

                reloadablePrograms.add(program);
                macrosSnapshots.add(macrosSnapshot);
                newPrograms.add(newProgram);
            }

            if (cause == null) { //all shaders were compiled successfully, replace them with the new ones
                for (int i = 0; i < reloadablePrograms.size(); i++) {
                    NewReloadableShaderProgram<?> reloadableProgram = reloadablePrograms.get(i);
                    reloadableProgram.macrosSnapshot = macrosSnapshots.get(i);
                    //replace the element in newProgram with the old program so that it gets closed in the finally block
                    reloadableProgram.program = uncheckedCast(newPrograms.set(i, reloadableProgram.program));
                }

                this.resourcesChanged = false;
            }

            callback.accept(reloadablePrograms.size(), failCount, cause);
        } finally {
            PorkUtil.closeAll(newPrograms);
        }
    }
}
