/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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
import lombok.val;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.key.KeyModifier;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.config.FP2Config;
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
                    fp2.client().globalRenderer().shaderRegistry.reload();
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
                    fp2.client().chat().debug("§aRegenerating all tiles");
                    player.send(CPacketDebugDropAllTiles.create(context.sessionId()));
                }
            }));
            category.addBinding("reloadRenderer", "9", KeyModifier.SHIFT, () -> fp2.client().currentPlayer().ifPresent(player -> {
                IFarClientContext context = player.activeContext();
                if (context != null) {
                    fp2.client().chat().debug("§aReloading renderer");
                    context.reloadRenderer();
                }
            }));
            category.addBinding("rebakeTiles", "9", KeyModifier.ALT, () -> fp2.client().currentPlayer().ifPresent(player -> {
                IFarClientContext context = player.activeContext();
                if (context != null) {
                    fp2.client().chat().debug("§aRe-baking tiles");
                    context.rebakeTiles();
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
            category.addBinding("rebuildUVs", "6", () -> TextureUVs.reloadAll(fp2.client()));
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

            List<String> list = new ArrayList<>();

            {
                list.add("");
                list.add("§lFarPlaneTwo (Client):");

                IFarClientContext context = player.activeContext();
                if (context != null) {
                    FarTileCache tileCache = context.tileCache();
                    if (tileCache != null) {
                        val stats = tileCache.stats();
                        list.add("TileCache: "
                                 + numberFormat.format(stats.tileCountWithData()) + '/' + numberFormat.format(stats.tileCount())
                                 + ' ' + i18n.formatByteCount(stats.totalSize())
                                 + " (" + i18n.formatPercentOf(stats.totalSize(), stats.uncompressedSize()) + " -> " + i18n.formatByteCount(stats.uncompressedSize()) + ')');
                    } else {
                        list.add("§oNo TileCache active");
                    }

                    AbstractFarRenderer renderer = context.renderer();
                    if (renderer != null) {
                        val rendererStats = renderer.stats();
                        val bakeStorageStats = rendererStats.bakeStorage();
                        val renderIndexStats = rendererStats.renderIndex();

                        /*list.add("Baked Tiles: "
                                 + numberFormat.format(stats.bakedTiles()) + "T "
                                 + numberFormat.format(stats.bakedTilesWithData()) + "D "
                                 + numberFormat.format(stats.bakedTiles() - stats.bakedTilesWithData()) + 'E');*/

                        list.add("Render index: " + renderIndexStats.implName());
                        boolean addCulledTiles = renderIndexStats.selectedTiles() >= 0 && renderIndexStats.indexedTiles() >= 0;
                        boolean addCulledCommands = renderIndexStats.selectedCommands() >= 0 && renderIndexStats.indexedCommands() >= 0;
                        if (addCulledTiles || addCulledCommands) {
                            val builder = new StringBuilder().append("Culled: ");
                            if (addCulledTiles) {
                                val culledTiles = renderIndexStats.indexedTiles() - renderIndexStats.selectedTiles();
                                builder.append(i18n.formatPercentOf(culledTiles, renderIndexStats.indexedTiles()))
                                        .append(' ').append(numberFormat.format(culledTiles)).append('/').append(numberFormat.format(renderIndexStats.indexedTiles()))
                                        .append('T');
                            }
                            if (addCulledCommands) {
                                if (addCulledTiles) {
                                    builder.append(", ");
                                }

                                val culledCommands = renderIndexStats.indexedCommands() - renderIndexStats.selectedCommands();
                                builder.append(i18n.formatPercentOf(culledCommands, renderIndexStats.indexedCommands()))
                                        .append(' ').append(numberFormat.format(culledCommands)).append('/').append(numberFormat.format(renderIndexStats.indexedCommands()))
                                        .append('C');
                            }
                            list.add(builder.toString());
                        }

                        long allocatedVRAM = (long) bakeStorageStats.allocatedIndices() * bakeStorageStats.indexSize() + (long) bakeStorageStats.allocatedVertices() * bakeStorageStats.vertexSize();
                        long totalVRAM = (long) bakeStorageStats.totalIndices() * bakeStorageStats.indexSize() + (long) bakeStorageStats.totalVertices() * bakeStorageStats.vertexSize();

                        list.add("All VRAM: "
                                 + i18n.formatPercentOf(allocatedVRAM, totalVRAM)
                                 + " (" + i18n.formatByteCount(allocatedVRAM) + '/' + i18n.formatByteCount(totalVRAM) + ')');
                        list.add("Indices: "
                                 + i18n.formatPercentOf(bakeStorageStats.allocatedIndices(), bakeStorageStats.totalIndices())
                                 + ' ' + numberFormat.format(bakeStorageStats.allocatedIndices()) + '/' + numberFormat.format(bakeStorageStats.totalIndices())
                                 + '@' + i18n.formatByteCount(bakeStorageStats.indexSize())
                                 + " (" + i18n.formatByteCount(bakeStorageStats.allocatedIndices() * bakeStorageStats.indexSize()) + '/' + i18n.formatByteCount(bakeStorageStats.totalIndices() * bakeStorageStats.indexSize()) + ')');
                        list.add("Vertices: "
                                 + i18n.formatPercentOf(bakeStorageStats.allocatedVertices(), bakeStorageStats.totalVertices())
                                 + ' ' + numberFormat.format(bakeStorageStats.allocatedVertices()) + '/' + numberFormat.format(bakeStorageStats.totalVertices())
                                 + '@' + i18n.formatByteCount(bakeStorageStats.vertexSize())
                                 + " (" + i18n.formatByteCount(bakeStorageStats.allocatedVertices() * bakeStorageStats.vertexSize()) + '/' + i18n.formatByteCount(bakeStorageStats.totalVertices() * bakeStorageStats.vertexSize()) + ')');
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

                val trackerStats = player.debugTrackerStats();
                if (trackerStats != null) {
                    list.add("Tracker: "
                             + numberFormat.format(trackerStats.tilesTrackedGlobal()) + "G "
                             + numberFormat.format(trackerStats.tilesTotal()) + "T " + numberFormat.format(trackerStats.tilesLoaded()) + "L "
                             + numberFormat.format(trackerStats.tilesLoading()) + "P " + numberFormat.format(trackerStats.tilesQueued()) + 'Q');
                    list.add("Updates: " + i18n.formatDuration(trackerStats.avgUpdateDuration()) + " avg, " + i18n.formatDuration(trackerStats.lastUpdateDuration()) + " last");
                } else {
                    list.add("§oTracking data not available");
                }
            }

            return list;
        }).orElse(Collections.emptyList());
    }
}
