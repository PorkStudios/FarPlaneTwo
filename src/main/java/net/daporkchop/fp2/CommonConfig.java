/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2;

import net.daporkchop.fp2.strategy.RenderStrategy;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraftforge.common.config.Config;

import static java.lang.Math.max;

/**
 * @author DaPorkchop_
 */
@Config(modid = FP2.MODID)
public class CommonConfig {
    @Config.Comment({
            "The strategy that will be used for rendering distant terrain."
    })
    @Config.RequiresWorldRestart
    public static RenderStrategy renderStrategy = RenderStrategy.FLAT;

    @Config.Comment({
            "The far plane render distance (in blocks)"
    })
    public static int renderDistance = 512;

    @Config.Comment({
            "Toggles debug mode, which enables some features useful while developing the mod.",
            "Default: false"
    })
    @Config.RequiresMcRestart
    public static boolean debug = Boolean.parseBoolean(System.getProperty("fp2.debug", "false"));

    @Config.Comment({
            "The number of threads that will be used for generating far plane terrain data.",
            "Default: <cpu count> - 1 (and at least 1)"
    })
    @Config.RequiresWorldRestart
    public static int generationThreads = max(PorkUtil.CPU_COUNT - 1, 1);

    @Config.Comment({
            "The number of threads that will be used for loading and saving of far plane terrain data.",
            "Default: <cpu count>"
    })
    @Config.RequiresWorldRestart
    public static int ioThreads = PorkUtil.CPU_COUNT;
}
