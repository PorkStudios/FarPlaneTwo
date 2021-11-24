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

package net.daporkchop.fp2.core.event;

import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.ReloadEvent;
import net.daporkchop.fp2.core.FP2Core;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Implementation of {@link ReloadEvent}.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractReloadEvent<T> implements ReloadEvent<T> {
    private final List<Throwable> failureCauses = new ArrayList<>();
    private int total;

    /**
     * Fires this event on {@link FP2Core#fp2()}'s {@link FEventBus}.
     */
    public void fire() {
        fp2().eventBus().fire(this);

        if (this.failureCauses.isEmpty()) {
            this.handleSuccess(this.total);
        } else {
            Throwable cause = new RuntimeException();
            this.failureCauses.forEach(cause::addSuppressed);

            this.handleFailure(this.failureCauses.size(), this.total, cause);
        }
    }

    protected abstract void handleSuccess(int total);

    protected abstract void handleFailure(int failed, int total, @NonNull Throwable cause);

    @Override
    public void notifySuccess() {
        this.total++;
    }

    @Override
    public void notifyFailure(@NonNull Throwable cause) {
        this.failureCauses.add(cause);
        this.total++;
    }
}
