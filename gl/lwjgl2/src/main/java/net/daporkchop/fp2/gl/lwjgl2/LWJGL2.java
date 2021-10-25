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

package net.daporkchop.fp2.gl.lwjgl2;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLModule;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.shader.GLShaders;
import net.daporkchop.lib.common.function.throwing.EPredicate;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Getter
public class LWJGL2 implements GL {
    protected final Set<GLVersion> versions;
    protected final GLVersion latestVersion;
    protected final Set<GLExtension> extensions;

    protected final ResourceArena resourceArena = new ResourceArena();

    protected final GLShaders shaders;
    protected final GLCompute compute;

    protected LWJGL2() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        this.versions = Stream.of(GLVersion.values())
                .filter((EPredicate<GLVersion>) version -> {
                    try {
                        return (boolean) ContextCapabilities.class.getDeclaredField("OpenGL" + version.major() + version.minor()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the version isn't supported by LWJGL2
                    }
                })
                .collect(Sets.toImmutableEnumSet());

        this.latestVersion = this.versions.stream().max(Comparator.naturalOrder()).get();

        this.extensions = Stream.of(GLExtension.values())
                .filter((EPredicate<GLExtension>) extension -> {
                    try {
                        return (boolean) ContextCapabilities.class.getDeclaredField(extension.name()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the extension isn't supported by LWJGL2
                    }
                })
                .collect(Sets.toImmutableEnumSet());

        this.shaders = GlobalProperties.<ModuleFactory<GLShaders>>getInstance("gl.lwjgl2.shaders.factory").create(this);
        this.compute = GlobalProperties.<ModuleFactory<GLCompute>>getInstance("gl.lwjgl2.compute.factory").create(this);
    }

    @Override
    public void close() {
        this.resourceArena.release();
    }

    /**
     * Factory for LWJGL2 module implementations.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface ModuleFactory<M extends GLModule> {
        /**
         * Creates a new {@link M} from the given {@link LWJGL2} context.
         *
         * @param gl the {@link LWJGL2} context
         * @return the new {@link M}
         */
        M create(@NonNull LWJGL2 gl);
    }
}
