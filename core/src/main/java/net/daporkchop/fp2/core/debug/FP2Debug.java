/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.debug;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.key.KeyModifier;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.engine.client.FarTileCache;
import net.daporkchop.fp2.core.network.packet.debug.client.CPacketDebugDropAllTiles;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.util.I18n;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public static final boolean FP2_DEBUG = Boolean.getBoolean("fp2.debug");

    public void init(@NonNull FP2Core fp2) {
        fp2.log().alert("FarPlaneTwo debug mode enabled!");

        if (fp2.hasClient()) {
            //register debug key bindings
            KeyCategory category = fp2.client().createKeyCategory(MODID + ".debug");

            category.addBinding("reloadShaders", "0", () -> {
                try {
                    fp2.client().reloadableShaderRegistry().reload();
                } catch (ReloadableShaderRegistry.ShaderReloadFailedException e) {
                    // swallow exception, it's already been logged and i don't want to crash the game every time i make a typo while prototyping a new shader
                }
            });
            category.addBinding("dropTiles", "9", () -> fp2.client().currentPlayer().ifPresent(player -> {
                player.send(CPacketClientConfig.create(null));
                player.send(CPacketClientConfig.create(fp2.globalConfig()));
                fp2.client().chat().debug("§aReloading session");
            }));
            category.addBinding("regenerateTiles", "9", KeyModifier.CONTROL, () -> fp2.client().currentPlayer().ifPresent(player -> {
                IFarClientContext context = player.activeContext();
                if (context != null) {
                    player.send(CPacketDebugDropAllTiles.create(context.sessionId()));
                    fp2.client().chat().debug("§aRegenerating all tiles");
                }
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

    /**
     * Gets a list of strings containing debug information to be displayed on the client.
     *
     * @param fp2 the active {@link FP2Core} instance
     * @return list of strings containing debug information to be displayed on the client
     */
    public List<String> clientDebugInfo(@NonNull FP2Core fp2) {
        return fp2.client().currentPlayer().map(player -> {
            I18n i18n = fp2.i18n();

            NumberFormat numberFormat = i18n.numberFormat();
            NumberFormat percentFormat = i18n.percentFormat();

            List<String> list = new ArrayList<>();

            {
                list.add("");
                list.add("§lFarPlaneTwo (Client):");

                IFarClientContext context = player.activeContext();
                if (context != null) {
                    FarTileCache tileCache = context.tileCache();
                    if (tileCache != null) {
                        DebugStats.TileCache stats = tileCache.stats();
                        list.add("TileCache: " + numberFormat.format(stats.tileCountWithData()) + '/' + numberFormat.format(stats.tileCount())
                                 + ' ' + percentFormat.format((stats.allocatedSpace() | stats.totalSpace()) != 0L ? stats.allocatedSpace() / (double) stats.totalSpace() : 1.0d)
                                 + ' ' + i18n.formatByteCount(stats.allocatedSpace()) + '/' + i18n.formatByteCount(stats.totalSpace())
                                 + " (" + percentFormat.format((stats.allocatedSpace() | stats.uncompressedSize()) != 0L ? stats.allocatedSpace() / (double) stats.uncompressedSize() : 1.0d) + " -> " + i18n.formatByteCount(stats.uncompressedSize()) + ')');
                    } else {
                        list.add("§oNo TileCache active");
                    }

                    AbstractFarRenderer renderer = context.renderer();
                    if (renderer != null) {
                        DebugStats.Renderer stats = renderer.stats();
                        list.add("Baked Tiles: " + numberFormat.format(stats.bakedTiles()) + "T " + numberFormat.format(stats.bakedTilesWithData()) + "D "
                                 + numberFormat.format(stats.bakedTiles() - stats.bakedTilesWithData()) + 'E');
                        if (stats.selectedTiles() >= 0L) {
                            list.add("Culled: " + numberFormat.format(stats.indexedTiles() - stats.selectedTiles()) + '/' + numberFormat.format(stats.indexedTiles())
                                     + " (" + percentFormat.format((stats.indexedTiles() - stats.selectedTiles()) / (double) stats.indexedTiles()) + ')');
                        }
                        list.add("All VRAM: " + percentFormat.format(stats.allocatedVRAM() / (double) stats.totalVRAM())
                                 + ' ' + i18n.formatByteCount(stats.allocatedVRAM()) + '/' + i18n.formatByteCount(stats.totalVRAM()));
                        list.add("Indices: " + percentFormat.format(stats.allocatedIndices() / (double) stats.totalIndices())
                                 + ' ' + numberFormat.format(stats.allocatedIndices()) + '/' + numberFormat.format(stats.totalIndices())
                                 + '@' + i18n.formatByteCount(stats.indexSize())
                                 + " (" + i18n.formatByteCount(stats.allocatedIndices() * stats.indexSize()) + '/' + i18n.formatByteCount(stats.totalIndices() * stats.indexSize()) + ')');
                        list.add("Vertices: " + percentFormat.format(stats.allocatedVertices() / (double) stats.totalVertices())
                                 + ' ' + numberFormat.format(stats.allocatedVertices()) + '/' + numberFormat.format(stats.totalVertices())
                                 + '@' + i18n.formatByteCount(stats.vertexSize())
                                 + " (" + i18n.formatByteCount(stats.allocatedVertices() * stats.vertexSize()) + '/' + i18n.formatByteCount(stats.totalVertices() * stats.vertexSize()) + ')');
                    } else {
                        list.add("§oNo renderer active");
                    }
                } else {
                    list.add("§oNo context active");
                }
            }

            {
                list.add("");
                list.add("§lFarPlaneTwo (Server):");

                DebugStats.Tracking trackingStats = player.debugServerStats();
                if (trackingStats != null) {
                    list.add("Tracker: " + numberFormat.format(trackingStats.tilesTrackedGlobal()) + "G "
                             + numberFormat.format(trackingStats.tilesTotal()) + "T " + numberFormat.format(trackingStats.tilesLoaded()) + "L "
                             + numberFormat.format(trackingStats.tilesLoading()) + "P " + numberFormat.format(trackingStats.tilesQueued()) + 'Q');
                    list.add("Updates: " + i18n.formatDuration(trackingStats.avgUpdateDuration()) + " avg, " + i18n.formatDuration(trackingStats.lastUpdateDuration()) + " last");
                } else {
                    list.add("§oTracking data not available");
                }
            }

            return list;
        }).orElse(Collections.emptyList());
    }
}
