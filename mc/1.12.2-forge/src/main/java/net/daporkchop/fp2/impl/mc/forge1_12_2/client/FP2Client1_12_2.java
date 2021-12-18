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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.client.ClientEvents;
import net.daporkchop.fp2.client.FP2ResourceReloadListener;
import net.daporkchop.fp2.client.KeyBindings;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.debug.client.DebugClientEvents;
import net.daporkchop.fp2.debug.client.DebugKeyBindings;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui.GuiContext1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.TextureUVs1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.log.ChatAsPorkLibLogger;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.Function;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.core.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Client1_12_2 extends FP2Client {
    private final Minecraft mc = Minecraft.getMinecraft();

    @NonNull
    private final FP2 fp2;

    public void preInit() {
        this.chat(new ChatAsPorkLibLogger(this.mc));

        if (FP2_DEBUG) {
            ConfigListenerManager.add(() -> this.globalShaderMacros()
                    .define("FP2_DEBUG_COLORS_ENABLED", this.fp2().globalConfig().debug().debugColors().enable())
                    .define("FP2_DEBUG_COLORS_MODE", this.fp2().globalConfig().debug().debugColors().ordinal()));

            MinecraftForge.EVENT_BUS.register(new DebugClientEvents());
        }

        if (!OPENGL_45) { //require at least OpenGL 4.5
            this.fp2().unsupported("Your system does not support OpenGL 4.5!\nRequired by FarPlaneTwo.");
        }

        if (!this.mc.getFramebuffer().isStencilEnabled() && !this.mc.getFramebuffer().enableStencil()) {
            if (OF && (PUnsafe.getBoolean(this.mc.gameSettings, OF_FASTRENDER_OFFSET) || PUnsafe.getInt(this.mc.gameSettings, OF_AALEVEL_OFFSET) > 0)) {
                this.fp2().unsupported("FarPlaneTwo was unable to enable the OpenGL stencil buffer!\n"
                                       + "Please launch the game without FarPlaneTwo and disable\n"
                                       + "  OptiFine's \"Fast Render\" and \"Antialiasing\", then\n"
                                       + "  try again.");
            } else {
                this.fp2().unsupported("Unable to enable the OpenGL stencil buffer!\nRequired by FarPlaneTwo.");
            }
        }

        ClientEvents.register();

        ConfigListenerManager.add(() -> {
            if (this.mc.player != null && this.mc.player.connection != null) {
                ((IFarPlayerClient) this.mc.player.connection).fp2_IFarPlayerClient_send(new CPacketClientConfig().config(this.fp2().globalConfig()));
            }
        });
    }

    public void init() {
        KeyBindings.register();

        if (FP2_DEBUG) {
            DebugKeyBindings.register();
        }
    }

    public void postInit() {
        //TODO: move this to core?
        this.globalShaderMacros().define("T_SHIFT", T_SHIFT).define("RENDER_PASS_COUNT", RENDER_PASS_COUNT);

        TextureUVs1_12_2.initDefault();

        this.mc.resourceManager.registerReloadListener(new FP2ResourceReloadListener());
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return new GuiContext1_12_2().createScreenAndOpen(this.mc, factory);
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        return this.mc.gameSettings.renderDistanceChunks;
    }
}
