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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.minecraft.util.log.ChatLogger;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.ATMinecraft1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.network.IMixinNetHandlerPlayClient1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui.GuiContext1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.ResourceProvider1_12;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.of.OFHelper1_12.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Client1_12 extends FP2Client {
    @NonNull
    private final FP2Forge1_12 fp2;
    @NonNull
    private final Minecraft mc;

    private final Map<KeyBinding, Runnable> keyBindings = new IdentityHashMap<>();

    private boolean reverseZ = false;

    @Override
    public void init(@NonNull FutureExecutor clientThreadExecutor) {
        //set chat logger
        this.chat(new ChatLogger(message -> {
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(message));
                return true;
            } else {
                return false;
            }
        }));

        super.init(clientThreadExecutor);

        //register self to listen for events
        MinecraftForge.EVENT_BUS.register(this);

        //enable stencil buffer
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

        //register resource reload listener
        ((ATMinecraft1_12) this.mc).getResourceManager().registerReloadListener(new ResourceReloadListener1_12());
    }

    @Override
    public ResourceProvider resourceProvider() {
        return new ResourceProvider1_12(this.mc);
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return new GuiContext1_12().createScreenAndOpen(this.mc, factory);
    }

    @Override
    public KeyCategory createKeyCategory(@NonNull String localeKey) {
        return (name, defaultKey, defaultModifiers, handler) -> {
            net.minecraftforge.client.settings.KeyModifier defaultModifier;
            if (defaultModifiers.isEmpty()) {
                defaultModifier = net.minecraftforge.client.settings.KeyModifier.NONE;
            } else {
                checkArg(defaultModifiers.size() <= 1, "at most one modifier may be given!");
                defaultModifier = net.minecraftforge.client.settings.KeyModifier.valueOf(defaultModifiers.iterator().next().name());
            }

            KeyBinding binding = new KeyBinding("key." + localeKey + '.' + name, KeyConflictContext.UNIVERSAL, defaultModifier, Keyboard.getKeyIndex(defaultKey), "key.categories." + localeKey);
            ClientRegistry.registerKeyBinding(binding);
            this.keyBindings.put(binding, handler);
        };
    }

    @Override
    public Optional<? extends IFarPlayerClient> currentPlayer() {
        NetHandlerPlayClient connection = this.mc.getConnection();
        return connection != null
                ? ((IMixinNetHandlerPlayClient1_12) connection).fp2_playerClient()
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

    @Override
    public int terrainTextureUnit() {
        return 0;
    }

    @Override
    public int lightmapTextureUnit() {
        return 1;
    }

    //forge events

    @SubscribeEvent(priority = EventPriority.LOW)
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        net.minecraft.client.gui.GuiScreen gui = event.getGui();
        if (gui instanceof GuiVideoSettings) { //add fp2 button to video settings menu
            event.getButtonList().add(new GuiButtonFP2Options1_12(0xBEEF, gui.width / 2 + 165, gui.height / 6 - 12, gui));
        } else if (FP2_DEBUG) { //we're in debug mode, also add it to the main menu and pause menu
            if (gui instanceof GuiMainMenu) {
                event.getButtonList().add(new GuiButtonFP2Options1_12(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 48, gui));
            } else if (gui instanceof GuiIngameMenu) {
                event.getButtonList().add(new GuiButtonFP2Options1_12(0xBEEF, gui.width / 2 + 104, gui.height / 4 + 8, gui));
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
