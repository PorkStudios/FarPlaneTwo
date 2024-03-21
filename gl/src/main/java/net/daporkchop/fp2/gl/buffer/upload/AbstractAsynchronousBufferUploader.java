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

package net.daporkchop.fp2.gl.buffer.upload;

import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.OpenGL;

import java.util.ArrayDeque;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
abstract class AbstractAsynchronousBufferUploader extends BufferUploader {
    private final ArrayDeque<Long> pendingSyncs = new ArrayDeque<>();
    private boolean anyQueued;

    public AbstractAsynchronousBufferUploader(OpenGL gl) {
        super(gl);
    }

    protected final long currentToken() {
        this.anyQueued = true;
        return this.currentToken;
    }

    @Override
    public void tick() {
        if (this.anyQueued) { //some uploads are queued, add a new sync
            this.anyQueued = false;
            this.pendingSyncs.addLast(this.gl.glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
        }

        while (!this.pendingSyncs.isEmpty()) {
            long sync = this.pendingSyncs.peekFirst();
            if (this.gl.glGetSync(sync, GL_SYNC_STATUS) == GL_SIGNALED) { //the
                this.gl.glDeleteSync(sync);
                this.pendingSyncs.removeFirst();
            } else {
                break;
            }
        }
    }

    @Override
    public void close() {
        super.close();

        for (Long sync; (sync = this.pendingSyncs.poll()) != null; ) {
            this.gl.glDeleteSync(sync);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class SyncPoint {
        protected final long sync;
    }
}
