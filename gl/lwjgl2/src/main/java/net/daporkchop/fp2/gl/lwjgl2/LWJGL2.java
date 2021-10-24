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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.lib.common.function.throwing.EPredicate;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@Getter
public class LWJGL2 implements GL {
    protected final GLVersion version;
    protected final Set<GLExtension> extensions;

    protected LWJGL2() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        this.version = Lists.reverse(Arrays.asList(GLVersion.values())).stream()
                .filter((EPredicate<GLVersion>) version -> {
                    try {
                        return (boolean) ContextCapabilities.class.getDeclaredField("OpenGL" + version.major() + version.minor()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the version isn't supported by LWJGL2
                    }
                })
                .findFirst().get();

        this.extensions = Stream.of(GLExtension.values())
                .filter((EPredicate<GLExtension>) extension -> {
                    try {
                        return (boolean) ContextCapabilities.class.getDeclaredField(extension.name()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the extension isn't supported by LWJGL2
                    }
                })
                .collect(Sets.toImmutableEnumSet());
    }

    @Override
    public void close() {
    }
}
