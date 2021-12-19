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

package net.daporkchop.fp2.core.debug;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Container class for FP2 debug mode.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Debug {
    /**
     * Whether or not we are currently running in debug mode.
     */
    public static final boolean FP2_DEBUG = Boolean.parseBoolean(System.getProperty("fp2.debug", "false"));

    public void init(@NonNull FP2Core fp2) {
        if (fp2.hasClient()) {
            //register debug key bindings
            KeyCategory category = fp2.client().createKeyCategory(MODID + ".debug");

            category.addBinding("reloadShaders", "0", ReloadableShaderProgram::reloadAll);
            category.addBinding("dropTiles", "9", () -> fp2.client().currentPlayer().ifPresent(player -> {
                player.fp2_IFarPlayerClient_send(new CPacketClientConfig().config(null));
                player.fp2_IFarPlayerClient_send(new CPacketClientConfig().config(fp2.globalConfig()));
                fp2.client().chat().debug("§aReloading session");
            }));
            category.addBinding("reversedZ", "8", () -> {
                FP2Config config = fp2.globalConfig();
                fp2.globalConfig(config.withCompatibility(config.compatibility().withReversedZ(!config.compatibility().reversedZ())));
                fp2.client().chat().debug((fp2.globalConfig().compatibility().reversedZ() ? "§aEnabled" : "§cDisabled") + " reversed-Z projection");
            });
            category.addBinding("toggleVanillaRender", "7", () -> {
                FP2Config config = fp2.globalConfig();
                fp2.globalConfig(config.withDebug(config.debug().withVanillaTerrainRendering(!config.debug().vanillaTerrainRendering())));
                fp2.client().chat().debug((fp2.globalConfig().debug().vanillaTerrainRendering() ? "§aEnabled" : "§cDisabled") + " vanilla terrain");
            });
            category.addBinding("rebuildUVs", "6", TextureUVs::reloadAll);
            category.addBinding("toggleRenderMode", "5", () -> {
                String[] oldModes = fp2.globalConfig().renderModes();
                String[] newModes = new String[oldModes.length];
                newModes[0] = oldModes[oldModes.length - 1];
                System.arraycopy(oldModes, 0, newModes, 1, oldModes.length - 1);
                fp2.globalConfig(fp2.globalConfig().withRenderModes(newModes));
                fp2.client().chat().debug("§aSwitched render mode to §7" + newModes[0]);
            });
            category.addBinding("toggleLevel0", "4", () -> {
                FP2Config config = fp2.globalConfig();
                fp2.globalConfig(config.withDebug(config.debug().withLevelZeroRendering(!config.debug().levelZeroRendering())));
                fp2.client().chat().debug((fp2.globalConfig().debug().levelZeroRendering() ? "§aEnabled" : "§cDisabled") + " level-0 rendering");
            });
            category.addBinding("toggleDebugColors", "3", () -> {
                FP2Config config = fp2.globalConfig();
                FP2Config.Debug.DebugColorMode[] modes = FP2Config.Debug.DebugColorMode.values();
                fp2.globalConfig(config.withDebug(config.debug().withDebugColors(modes[(config.debug().debugColors().ordinal() + 1) % modes.length])));
                fp2.client().chat().debug("§aSwitched debug color mode to §7" + fp2.globalConfig().debug().debugColors());
            });
        }
    }
}
