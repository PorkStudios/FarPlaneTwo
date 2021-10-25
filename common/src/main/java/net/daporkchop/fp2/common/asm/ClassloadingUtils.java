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

package net.daporkchop.fp2.common.asm;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Caches classes which are generated at runtime.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ClassloadingUtils {
    /**
     * Extracts a class name from its bytecode.
     *
     * @param code the bytecode
     * @return the class name
     */
    public String extractClassNameFromCode(@NonNull byte[] code) {
        class State extends ClassVisitor {
            String name;

            public State() {
                super(Opcodes.ASM5);
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.name = name;
                throw new RuntimeException(); //break out immediately to avoid having to read the rest of the class
            }
        }

        State state = new State();
        try {
            new ClassReader(code).accept(state, 0);
            throw new IllegalStateException();
        } catch (RuntimeException e) {
            if (state.name != null) {
                return state.name.replace('/', '.');
            }

            throw new IllegalArgumentException("unable to find class name", e);
        }
    }

    /**
     * Defines a class which will use the classpath of the given {@link ClassLoader}, but not be visible to existing classes inside it.
     *
     * @param loader the {@link ClassLoader}
     * @param code   the bytecode
     * @return the defined class
     */
    public Class<?> defineHiddenClass(@NonNull ClassLoader loader, @NonNull byte[] code) {
        return new DelegatingClassLoader(loader).defineClass(extractClassNameFromCode(code), code);
    }
}
