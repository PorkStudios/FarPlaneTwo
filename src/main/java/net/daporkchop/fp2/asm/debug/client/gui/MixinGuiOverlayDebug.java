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

package net.daporkchop.fp2.asm.debug.client.gui;

import net.daporkchop.fp2.client.gui.GuiHelper;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.text.NumberFormat;
import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Mixin(GuiOverlayDebug.class)
public abstract class MixinGuiOverlayDebug extends Gui {
    @Inject(method = "Lnet/minecraft/client/gui/GuiOverlayDebug;getDebugInfoRight()Ljava/util/List;",
            at = @At("RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void fp2_getDebugInfoRight_injectFP2DebugInfo(CallbackInfoReturnable<List<String>> ci,
                                                          long maxMemory, long totalMemory, long freeMemory, long usedMemory, List<String> list) {
        IFarPlayerClient player = (IFarPlayerClient) MC.getConnection();
        if (player == null) { //connection is null while the F3 screen is visible? this shouldn't be possible...
            return;
        }

        I18n i18n = fp2().i18n();

        NumberFormat numberFormat = i18n.numberFormat();
        NumberFormat percentFormat = i18n.percentFormat();

        {
            list.add("");
            list.add("§lFarPlaneTwo (Client):");

            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            if (context != null) {
                IFarTileCache<?, ?> tileCache = context.tileCache();
                if (tileCache != null) {
                    DebugStats.TileCache stats = tileCache.stats();
                    list.add("TileCache: " + numberFormat.format(stats.tileCountWithData()) + '/' + numberFormat.format(stats.tileCount())
                             + ' ' + percentFormat.format((stats.allocatedSpace() | stats.totalSpace()) != 0L ? stats.allocatedSpace() / (double) stats.totalSpace() : 1.0d)
                             + ' ' + i18n.formatByteCount(stats.allocatedSpace()) + '/' + i18n.formatByteCount(stats.totalSpace())
                             + " (" + percentFormat.format((stats.allocatedSpace() | stats.uncompressedSize()) != 0L ? stats.allocatedSpace() / (double) stats.uncompressedSize() : 1.0d) + " -> " + i18n.formatByteCount(stats.uncompressedSize()) + ')');
                } else {
                    list.add("§oNo TileCache active");
                }

                IFarRenderer renderer = context.renderer();
                if (renderer != null) {
                    DebugStats.Renderer stats = renderer.stats();
                    list.add("Baked Tiles: " + numberFormat.format(stats.bakedTiles()) + "T " + numberFormat.format(stats.bakedTilesWithData()) + "D "
                             + numberFormat.format(stats.bakedTiles() - stats.bakedTilesWithData()) + 'E');
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

            DebugStats.Tracking trackingStats = player.fp2_IFarPlayerClient_debugServerStats();
            if (trackingStats != null) {
                list.add("Tracker: " + numberFormat.format(trackingStats.tilesTrackedGlobal()) + "G "
                         + numberFormat.format(trackingStats.tilesTotal()) + "T " + numberFormat.format(trackingStats.tilesLoaded()) + "L "
                         + numberFormat.format(trackingStats.tilesLoading()) + "P " + numberFormat.format(trackingStats.tilesQueued()) + 'Q');
                list.add("Updates: " + i18n.formatDuration(trackingStats.avgUpdateDuration()) + " avg, " + i18n.formatDuration(trackingStats.lastUpdateDuration()) + " last");
            } else {
                list.add("§oTracking data not available");
            }
        }
    }
}
