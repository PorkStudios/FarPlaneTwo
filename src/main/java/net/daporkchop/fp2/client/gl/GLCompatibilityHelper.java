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

package net.daporkchop.fp2.client.gl;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.config.FP2Config;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL33;

import java.util.Locale;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class GLCompatibilityHelper {
    /**
     * This is a workaround for an issue with the official AMD driver which results in horrible performance when a vertex attribute doesn't
     * have a 4-byte alignment.
     */
    public final boolean WORKAROUND_AMD_VERTEX_ATTRIBUTE_PADDING = FP2Config.compatibility.workaroundAmdVertexPadding.shouldEnable(isOfficialAmdDriver());

    /**
     * This is a workaround for an issue with the official AMD driver which results unpredictable behavior when any of the vertex attributes
     * use the {@link GL33#GL_INT_2_10_10_10_REV} type.
     */
    public final boolean WORKAROUND_AMD_INT_2_10_10_10_REV = FP2Config.compatibility.workaroundAmdInt2_10_10_10_REV.shouldEnable(isOfficialAmdDriver());

    public final int EFFECTIVE_VERTEX_ATTRIBUTE_ALIGNMENT = WORKAROUND_AMD_VERTEX_ATTRIBUTE_PADDING ? INT_SIZE : 1;

    static {
        FP2_LOG.info("{}enabling AMD vertex attribute padding workaround", WORKAROUND_AMD_VERTEX_ATTRIBUTE_PADDING ? "" : "not ");
        FP2_LOG.info("{}enabling AMD GL_INT_2_10_10_10_REV workaround", WORKAROUND_AMD_INT_2_10_10_10_REV ? "" : "not ");
    }

    private boolean isOfficialAmdDriver() {
        String brand = glGetString(GL_VENDOR) + ' ' + glGetString(GL_VERSION) + ' ' + glGetString(GL_RENDERER);

        return (brand.contains("AMD") || brand.contains("ATI")) && !brand.contains("Mesa");
    }
}
