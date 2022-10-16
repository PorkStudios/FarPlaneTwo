/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.api.event.generic;

import lombok.NonNull;
import net.daporkchop.lib.common.function.exception.ERunnable;

/**
 * Fired when all resources of a certain type should be reloaded.
 *
 * @author DaPorkchop_
 */
public interface FReloadEvent<T> {
    /**
     * Notify the event that the listener was able to be reloaded successfully.
     */
    void notifySuccess();

    /**
     * Notify the event that the listener could not be reloaded.
     *
     * @param cause the {@link Throwable} which caused the reload to to fail
     */
    void notifyFailure(@NonNull Throwable cause);

    /**
     * Tries to run the given {@link ERunnable} which will reload the resource.
     *
     * @param reloadAction the {@link ERunnable}
     */
    default void doReload(@NonNull ERunnable reloadAction) {
        try {
            reloadAction.runThrowing();
            this.notifySuccess();
        } catch (Throwable cause) {
            this.notifyFailure(cause);
        }
    }
}
