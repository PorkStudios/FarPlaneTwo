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

package net.daporkchop.fp2.mode.common.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.AllocatedGLBuffer;
import net.daporkchop.lib.primitive.lambda.LongLongConsumer;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * A command buffer containing tasks to be executed on the client thread in order to finish baking a tile.
 *
 * @author DaPorkchop_
 */
public final class BakeOutput {
    private static final BakeOutput EMPTY = new BakeOutput(0L);

    public static BakeOutput empty() {
        return EMPTY;
    }

    public final long renderData;
    public final long size;

    protected final List<Task> tasks = new ArrayList<>();

    public BakeOutput(long renderDataSize) {
        this.renderData = PUnsafe.allocateMemory(this, this.size = renderDataSize);
    }

    public void submit(@NonNull Task task) {
        checkState(!this.isEmpty(), "cannot submit execution task to empty BakeOutput!");

        this.tasks.add(task);
    }

    public void uploadAndStoreAddress(@NonNull ByteBuf data, @NonNull AllocatedGLBuffer bufferIn, @NonNull LongLongConsumer setAddressCallback) {
        this.submit(renderData -> {
            try (AllocatedGLBuffer buffer = bufferIn.bind(GL_COPY_WRITE_BUFFER)) {
                long addr = buffer.alloc(data.readableBytes());
                setAddressCallback.accept(renderData, addr);
                buffer.uploadRange(addr, data);
            } finally {
                data.release();
            }
        });
    }

    public void uploadAndStoreAddress(@NonNull ByteBuf data, @NonNull AllocatedGLBuffer buffer, @NonNull LongLongConsumer setAddressCallback, int divisor) {
        this.uploadAndStoreAddress(data, buffer, (renderData, addr) -> setAddressCallback.accept(renderData, addr / divisor));
    }

    /**
     * Executes the bake commands.
     * <p>
     * Must be called from the client thread.
     */
    public void execute() {
        checkState(!this.isEmpty(), "cannot execute empty BakeOutput!");

        try {
            for (int i = 0, size = this.tasks.size(); i < size; i++) {
                this.tasks.get(i).run(this.renderData);
            }
        } finally {
            this.tasks.clear();
        }
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @FunctionalInterface
    public interface Task {
        void run(long renderData);
    }
}
