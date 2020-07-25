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

import net.daporkchop.fp2.strategy.RenderMode;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@Config(modid = FP2.MODID)
public class FP2Config {
    @Config.Comment({
            "The mode that will be used for rendering distant terrain."
    })
    @Config.LangKey("config.fp2.renderMode")
    @Config.RequiresWorldRestart
    public static RenderMode renderMode = RenderMode.HEIGHTMAP;

    @Config.Comment({
            "The far plane render distance (in blocks)"
    })
    @Config.RangeInt(min = 1)
    @Config.LangKey("config.fp2.renderDistance")
    public static int renderDistance = 512;

    @Config.Comment({
            "The number of LoD levels to use."
    })
    @Config.RangeInt(min = 1, max = 32)
    @Config.SlidingOption
    @Config.LangKey("config.fp2.maxLevels")
    @Config.RequiresWorldRestart
    public static int maxLevels = 8;

    @Config.Comment({
            "The distance (in blocks) between LoD transitions.",
            "Note that this value is doubled for each level, so a setting of 64 will mean 64 blocks for the first layer, 128 blocks for the second layer, etc."
    })
    @Config.RangeInt(min = 16)
    @Config.LangKey("config.fp2.levelCutoffDistance")
    public static int levelCutoffDistance = 64;

    @Config.Comment({
            "The number of threads that will be used for generating far plane terrain data.",
            "Default: <cpu count> - 1 (and at least 1)"
    })
    @Config.LangKey("config.fp2.generationThreads")
    @Config.RequiresWorldRestart
    public static int generationThreads = max(PorkUtil.CPU_COUNT - 1, 1);

    @Config.Comment({
            "The number of threads that will be used for downscaling far plane terrain data.",
            "Default: <cpu count> - 1 (and at least 1)"
    })
    @Config.LangKey("config.fp2.scaleThreads")
    @Config.RequiresWorldRestart
    public static int scaleThreads = max(PorkUtil.CPU_COUNT - 1, 1);

    @Config.Comment({
            "Config options available only on the client."
    })
    @Config.LangKey("config.fp2.client")
    @SideOnly(Side.CLIENT)
    public static Client client = new Client();

    @Config.Comment({
            "Options for storage of far terrain tiles."
    })
    @Config.LangKey("config.fp2.storage")
    public static Storage storage = new Storage();

    @Config.Comment({
            "Config options useful while developing the mod."
    })
    @Config.LangKey("config.fp2.debug")
    public static Debug debug = new Debug();

    /**
     * @author DaPorkchop_
     */
    @SideOnly(Side.CLIENT)
    public static class Client {
        @Config.Comment({
                "The number of threads that will be used for preparing far plane terrain data for rendering.",
                "Default: <cpu count> - 1 (and at least 1)"
        })
        @Config.LangKey("config.fp2.client.renderThreads")
        @Config.RequiresWorldRestart
        public int renderThreads = max(PorkUtil.CPU_COUNT - 1, 1);
    }

    /**
     * @author DaPorkchop_
     */
    public static class Storage {
        @Config.Comment({
                "The options used by the leveldb databases for storing far terrain data.",
                "Don't touch this unless you know what you're doing!"
        })
        @Config.LangKey("config.fp2.storage.leveldb")
        @Config.RequiresWorldRestart
        public Options leveldbOptions = new Options()
                .compressionType(CompressionType.NONE);
    }

    /**
     * @author DaPorkchop_
     */
    public static class Debug {
        @Config.Comment({
                "Toggles debug mode, which enables some features useful while developing the mod.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.debug")
        @Config.RequiresMcRestart
        public boolean debug = Boolean.parseBoolean(System.getProperty("fp2.debug", "false"));

        @Config.Comment({
                "If true, disables reading of heightmap tiles from disk.",
                "Overridden by disablePersistence.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disableRead")
        public boolean disableRead = false;

        @Config.Comment({
                "If true, disables writing of heightmap tiles to disk.",
                "Overridden by disablePersistence.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disableWrite")
        public boolean disableWrite = false;

        @Config.Comment({
                "If true, completely disables disk persistence.",
                "Far terrain data will never be written to or read from disk.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disablePersistence")
        public boolean disablePersistence = false;
    }
}
