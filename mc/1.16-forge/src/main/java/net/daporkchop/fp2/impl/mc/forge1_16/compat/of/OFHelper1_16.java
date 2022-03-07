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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.of;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Method;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Optifine compatibility code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class OFHelper1_16 {
    //TODO: make intellij stop rearranging this class somehow

    public static final boolean OF;
    public static final String OF_VERSION;

    static { //adapted from io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinLoader
        String ofVersion = null;
        try {
            Class<?> optifineInstallerClass = Class.forName("optifine.Installer");
            Method getVersionHandler = optifineInstallerClass.getMethod("getOptiFineVersion");
            ofVersion = ((String) getVersionHandler.invoke(null));
            ofVersion = ofVersion.replace("_pre", "");
            ofVersion = ofVersion.substring(ofVersion.length() - 2);

            LogManager.getLogger("fp2_optifine").info("Detected OptiFine version: {}", ofVersion);
        } catch (ClassNotFoundException e) {
            LogManager.getLogger("fp2_optifine").info("No OptiFine detected");
        } catch (Exception e) {
            ofVersion = "G6";
            LogManager.getLogger("fp2_optifine").error("OptiFine detected, but could not detect version. It may not work. Assuming OptiFine G6...", e);
        } finally {
            OF_VERSION = ofVersion;
            OF = ofVersion != null;
        }
    }
}
