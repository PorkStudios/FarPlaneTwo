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

package net.daporkchop.fp2.compat.x86;

import net.daporkchop.fp2.compat.windows.WindowsDLLDependencyInjector;
import net.daporkchop.lib.common.system.OperatingSystem;
import net.daporkchop.lib.common.system.PlatformInfo;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * Native implementation of {@link x86FeatureDetector} used on supported x86 systems.
 *
 * @author DaPorkchop_
 */
final class Native_x86FeatureDetector implements x86FeatureDetector {
    protected static String initAndGetClassName() {
        if (PlatformInfo.OPERATING_SYSTEM == OperatingSystem.Windows) {
            //ensure windows DLLs are injected before anything else happens
            WindowsDLLDependencyInjector.inject();
        }

        return Native_x86FeatureDetector.class.getCanonicalName();
    }

    public Native_x86FeatureDetector() {
        fp2().log().info("using x86 vector extension: {}", this.maxSupportedVectorExtension());
    }

    @Override
    public native String maxSupportedVectorExtension();

    @Override
    public boolean isNative() {
        return true;
    }
}
