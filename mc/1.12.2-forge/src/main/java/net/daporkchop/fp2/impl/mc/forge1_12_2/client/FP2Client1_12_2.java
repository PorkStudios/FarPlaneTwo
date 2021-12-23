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
import net.daporkchop.fp2.api.event.ChangedEvent;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.client.FP2ResourceReloadListener;
import net.daporkchop.fp2.client.GuiButtonFP2Options;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui.GuiContext1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.TextureUVs1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.log.ChatAsPorkLibLogger;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GLContext;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Client1_12_2 extends FP2Client {
    private final Minecraft mc = Minecraft.getMinecraft();

    @NonNull
    private final FP2 fp2;

    private final Map<KeyBinding, Runnable> keyBindings = new IdentityHashMap<>();

    private boolean reverseZ = false;

    public void preInit() {
        this.chat(new ChatAsPorkLibLogger(this.mc));

        if (!GLContext.getCapabilities().OpenGL45) { //require at least OpenGL 4.5
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

        //register self to listen for events
        this.fp2().eventBus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void init() {
        if (FP2_DEBUG) {
            this.updateDebugColorMacros(this.fp2().globalConfig());
        }
    }

    public void postInit() {
        TextureUVs1_12_2.initDefault();

        this.mc.resourceManager.registerReloadListener(new FP2ResourceReloadListener());
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return new GuiContext1_12_2().createScreenAndOpen(this.mc, factory);
    }

    @Override
    public KeyCategory createKeyCategory(@NonNull String localeKey) {
        return (name, defaultKey, handler) -> {
            KeyBinding binding = new KeyBinding("key." + localeKey + '.' + name, Keyboard.getKeyIndex(defaultKey), "key.categories." + localeKey);
            ClientRegistry.registerKeyBinding(binding);
            this.keyBindings.put(binding, handler);
        };
    }

    @Override
    public Optional<IFarPlayerClient> currentPlayer() {
        return this.mc.player != null
                ? Optional.of((IFarPlayerClient) this.mc.player.connection)
                : Optional.empty();
    }

    @Override
    public void enableReverseZ() {
        if (this.fp2.globalConfig().compatibility().reversedZ()) {
            this.reverseZ = true;

            GlStateManager.depthFunc(GL_LEQUAL);
            glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
            GlStateManager.clearDepth(0.0d);
        }
    }

    @Override
    public void disableReverseZ() {
        if (this.reverseZ) {
            this.reverseZ = false;

            glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GlStateManager.depthFunc(GL_LEQUAL);
            GlStateManager.clearDepth(1.0d);
        }
    }

    @Override
    public boolean isReverseZ() {
        return this.reverseZ;
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        return this.mc.gameSettings.renderDistanceChunks;
    }

    protected void updateDebugColorMacros(@NonNull FP2Config config) {
        this.globalShaderMacros()
                .define("FP2_DEBUG_COLORS_ENABLED", config.debug().debugColors().enable())
                .define("FP2_DEBUG_COLORS_MODE", config.debug().debugColors().ordinal());
    }

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(ChangedEvent<FP2Config> event) {
        if (FP2_DEBUG) {
            this.updateDebugColorMacros(event.next());
        }

        //send updated config to server
        if (this.mc.player != null && this.mc.player.connection != null) {
            ((IFarPlayerClient) this.mc.player.connection).fp2_IFarPlayerClient_send(new CPacketClientConfig().config(this.fp2().globalConfig()));
        }
    }

    //forge events

    @SubscribeEvent(priority = EventPriority.LOW)
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        net.minecraft.client.gui.GuiScreen gui = event.getGui();
        if (gui instanceof GuiVideoSettings) { //add fp2 button to video settings menu
            gui.buttonList.add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 165, gui.height / 6 - 12, gui));
        } else if (FP2_DEBUG) { //we're in debug mode, also add it to the main menu and pause menu
            if (gui instanceof GuiMainMenu) {
                gui.buttonList.add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 48, gui));
            } else if (gui instanceof GuiIngameMenu) {
                gui.buttonList.add(new GuiButtonFP2Options(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 8, gui));
            }
        }
    }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        this.keyBindings.forEach((binding, handler) -> {
            if (binding.isPressed()) {
                handler.run();
            }
        });
    }

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        this.disableReverseZ();
    }
}
