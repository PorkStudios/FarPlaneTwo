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

package net.daporkchop.fp2.impl.mc.forge1_16.client;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.config.gui.ConfigGuiHelper;
import net.daporkchop.fp2.core.minecraft.util.log.ChatLogger;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.gui.screen.ATScreen1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.client.network.play.IMixinClientPlayNetHandler1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.gui.GuiContext1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.ResourceProvider1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Client1_16 extends FP2Client {
    @NonNull
    private final FP2Forge1_16 fp2;
    @NonNull
    private final Minecraft mc;

    private final Map<KeyBinding, Runnable> keyBindings = new IdentityHashMap<>();

    private boolean reverseZ = false;

    @Override
    public void init(@NonNull FutureExecutor clientThreadExecutor) {
        this.chat(new ChatLogger(message -> {
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new StringTextComponent(message), Util.NIL_UUID);
                return true;
            } else {
                return false;
            }
        }));

        super.init(clientThreadExecutor);

        //register self to listen for events
        MinecraftForge.EVENT_BUS.register(this);

        //enable stencil buffer
        this.mc.getMainRenderTarget().enableStencil();

        //register resource reload listener
        ((IReloadableResourceManager) this.mc.getResourceManager()).registerReloadListener(new ResourceReloadListener1_16(this));
    }

    @Override
    public ResourceProvider resourceProvider() {
        return new ResourceProvider1_16(this.mc);
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return new GuiContext1_16().createScreenAndOpen(this.mc, factory);
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

            int keyCode = InputMappings.getKey("key.keyboard." + defaultKey.toLowerCase(Locale.ROOT)).getValue();
            KeyBinding binding = new KeyBinding("key." + localeKey + '.' + name, KeyConflictContext.UNIVERSAL, defaultModifier, InputMappings.Type.KEYSYM.getOrCreate(keyCode), "key.categories." + localeKey);
            ClientRegistry.registerKeyBinding(binding);
            this.keyBindings.put(binding, handler);
        };
    }

    @Override
    public Optional<? extends IFarPlayerClient> currentPlayer() {
        ClientPlayNetHandler netHandler = this.mc.getConnection();
        return netHandler != null
                ? ((IMixinClientPlayNetHandler1_16) netHandler).fp2_playerClient()
                : Optional.empty();
    }

    @Override
    public void enableReverseZ() {
        if (this.fp2.globalConfig().compatibility().reversedZ()) {
            this.reverseZ = true;

            RenderSystem.depthFunc(GL_LEQUAL);
            glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
            RenderSystem.clearDepth(0.0d);
        }
    }

    @Override
    public void disableReverseZ() {
        if (this.reverseZ) {
            this.reverseZ = false;

            glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            RenderSystem.depthFunc(GL_LEQUAL);
            RenderSystem.clearDepth(1.0d);
        }
    }

    @Override
    public boolean isReverseZ() {
        return this.reverseZ;
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        return this.mc.options.renderDistance;
    }

    @Override
    public int terrainTextureUnit() {
        return 0;
    }

    @Override
    public int lightmapTextureUnit() {
        return 2;
    }

    //forge events

    @SubscribeEvent
    public void initGui(GuiScreenEvent.InitGuiEvent.Post event) {
        Button button = new Button(0, 0, 40, 20, new TranslationTextComponent(MODID + ".gui.buttonFP2Options"), b -> {
            FP2Config defaultConfig = FP2Config.DEFAULT_CONFIG;
            FP2Config serverConfig = this.mc.isLocalServer() ? null : this.currentPlayer().map(IFarPlayerClient::serverConfig).orElse(null);
            FP2Config clientConfig = this.fp2().globalConfig();

            ConfigGuiHelper.createAndDisplayGuiContext("menu", defaultConfig, serverConfig, clientConfig, this.fp2()::globalConfig);
        });
        boolean addButton = true;

        Screen gui = event.getGui();
        if (gui instanceof VideoSettingsScreen) {
            button.x = gui.width / 2 + 165;
            button.y = 32 + 4;
        } else if (FP2_DEBUG && gui instanceof MainMenuScreen) {
            button.x = gui.width / 2 + 104;
            button.y = gui.height / 4 + 48;
        } else if (FP2_DEBUG && gui instanceof IngameMenuScreen) {
            button.x = gui.width / 2 + 104;
            button.y = gui.height / 4 + 8;
        } else {
            addButton = false;
        }

        if (addButton) { //add the button to the gui
            ((ATScreen1_16) gui).getButtons().add(0, button);
            ((ATScreen1_16) gui).getChildren().add(0, button);
        }
    }

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        this.keyBindings.forEach((binding, handler) -> {
            if (binding.consumeClick()) {
                handler.run();
            }
        });
    }

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        this.disableReverseZ();
    }
}
