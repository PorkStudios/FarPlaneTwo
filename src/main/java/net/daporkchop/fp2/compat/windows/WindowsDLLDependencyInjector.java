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

package net.daporkchop.fp2.compat.windows;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.system.OperatingSystem;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.natives.NativeFeature;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Forcibly injects a few DLLs into the native library search path in order to allow Windows to load native libs compiled with GCC correctly.
 * <p>
 * While it would be possible to statically link all of these libraries into the final DLL, that would mean packaging dozens of copies of each of them into the fp2 jar, thus yielding
 * massively increased file sizes.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class WindowsDLLDependencyInjector {
    private static final String[] LIB_NAMES = {
            "libgcc_s_seh-1.dll",
            "libwinpthread-1.dll",
            "libstdc++-6.dll"
    };

    private static boolean RUN = false;

    /**
     * Injects the native library dependencies into the search path.
     */
    @SneakyThrows(Throwable.class)
    public synchronized void inject() {
        if (PlatformInfo.OPERATING_SYSTEM != OperatingSystem.Windows) {
            return; //do nothing if we're not on windows
        } else if (RUN) {
            return; //do nothing if the dependencies have already been injected
        }
        RUN = true;

        //create temporary directory for storing libraries in
        Path tempDir = Files.createTempDirectory("fp2-windows-natives_" + UUID.randomUUID() + '_' + System.currentTimeMillis() + '_' + System.nanoTime());

        for (String lib : LIB_NAMES) {
            Path libFile = tempDir.resolve(lib);

            //copy library to temp directory
            try (InputStream in = WindowsDLLDependencyInjector.class.getResourceAsStream(lib)) {
                if (in == null) { //the dll couldn't be found - skip it, and let porklib:natives handle falling back to the pure-java impl once the actual module dlls fail to load
                    continue;
                }

                Files.copy(in, libFile);
            }

            //load library manually
            NativeFeature.loadNativeLibrary(WindowsDLLDependencyInjector.class, libFile.toFile());
        }
    }
}
