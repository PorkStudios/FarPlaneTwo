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
 *
 */

package net.daporkchop.fp2.core.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.compression.zstd.Zstd;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class FP2Server {
    /**
     * Initializes this instance.
     * <p>
     * The following properties must be accessible (set to a non-null value) before calling this method:
     * <ul>
     *     <li>{@link #fp2()}</li>
     * </ul>
     *
     * @param serverThreadExecutor a {@link FutureExecutor} for scheduling tasks to be executed on the server thread
     */
    public void init(@NonNull FutureExecutor serverThreadExecutor) {
        checkState(this.fp2() != null, "fp2() must be set!");

        if (!PlatformInfo.IS_64BIT) { //require 64-bit
            this.fp2().unsupported("Your system or JVM is not 64-bit!\nRequired by FarPlaneTwo.");
        } else if (!PlatformInfo.IS_LITTLE_ENDIAN) { //require little-endian
            this.fp2().unsupported("Your system is not little-endian!\nRequired by FarPlaneTwo.");
        }

        System.setProperty("porklib.native.printStackTraces", "true");
        if (!Zstd.PROVIDER.isNative()) {
            this.fp2().log().alert("Native ZSTD could not be loaded! This will have SERIOUS performance implications!");
        }

        //register self to listen for events
        this.fp2().eventBus().register(this);
    }

    /**
     * @return the {@link FP2Core} instance which this {@link FP2Server} is used for
     */
    public abstract FP2Core fp2();
}
