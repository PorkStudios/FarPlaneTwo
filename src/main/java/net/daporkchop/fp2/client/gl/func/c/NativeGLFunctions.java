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

package net.daporkchop.fp2.client.gl.func.c;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.client.gl.func.GLFunctions;
import org.lwjgl.opengl.GLContext;

import java.lang.reflect.Method;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class NativeGLFunctions implements GLFunctions {
    @SneakyThrows(Exception.class)
    protected static long getFunctionAddress(@NonNull String name) {
        Method method = GLContext.class.getDeclaredMethod("getFunctionAddress", String.class);
        method.setAccessible(true);
        long addr = (Long) method.invoke(null, name);
        checkState(addr != 0L, "unable to find function %s", name);
        return addr;
    }

    protected final long glDrawElementsIndirect = getFunctionAddress("glDrawElementsIndirect");
    protected final long glMultiDrawElements = getFunctionAddress("glMultiDrawElements");
    protected final long glMultiDrawElementsBaseVertex = getFunctionAddress("glMultiDrawElementsBaseVertex");

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public void glDrawElementsIndirect(int mode, int type, long indirect) {
        this.glDrawElementsIndirect0(mode, type, indirect, this.glDrawElementsIndirect);
    }

    protected native void glDrawElementsIndirect0(int mode, int type, long indirect, long functionPointer);

    @Override
    public void glMultiDrawElements(int mode, long count, int type, long indices, int drawcount) {
        this.glMultiDrawElements0(mode, count, type, indices, drawcount, this.glMultiDrawElements);
    }

    protected native void glMultiDrawElements0(int mode, long count, int type, long indices, int drawcount, long functionPointer);

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        this.glMultiDrawElementsBaseVertex0(mode, count, type, indices, drawcount, basevertex, this.glMultiDrawElementsBaseVertex);
    }

    protected native void glMultiDrawElementsBaseVertex0(int mode, long count, int type, long indices, int drawcount, long basevertex, long functionPointer);
}
