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

package net.daporkchop.fp2.client.gl.commandbuffer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import static net.daporkchop.fp2.client.gl.OpenGL.*;

/**
 * A draw command used by {@link GL40#glDrawArraysIndirect(int, long)} and {@link GL43#glMultiDrawArraysIndirect(int, long, int, int)}.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DrawArraysIndirectCommand implements DrawIndirectCommand {
    public static final int COMMAND_SIZE = 4;
    public static final int COMMAND_BYTES = COMMAND_SIZE * INT_SIZE;

    protected int count;
    protected int instanceCount;
    protected int first;
    protected int baseInstance;

    @Override
    public long size() {
        return COMMAND_BYTES;
    }

    @Override
    public void load(long addr) {
        this.count = PUnsafe.getInt(addr + 0 * INT_SIZE);
        this.instanceCount = PUnsafe.getInt(addr + 1 * INT_SIZE);
        this.first = PUnsafe.getInt(addr + 2 * INT_SIZE);
        this.baseInstance = PUnsafe.getInt(addr + 3 * INT_SIZE);
    }

    @Override
    public void store(long addr) {
        PUnsafe.putInt(addr + 0 * INT_SIZE, this.count);
        PUnsafe.putInt(addr + 1 * INT_SIZE, this.instanceCount);
        PUnsafe.putInt(addr + 2 * INT_SIZE, this.first);
        PUnsafe.putInt(addr + 3 * INT_SIZE, this.baseInstance);
    }
}
