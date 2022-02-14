/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.ResourceProvider1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
        this.chat(new ChatLogger1_16(this.mc));

        super.init(clientThreadExecutor);

        //register self to listen for events
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected boolean checkGL45() {
        return GL.getCapabilities().OpenGL45;
    }

    @Override
    public ResourceProvider resourceProvider() {
        return new ResourceProvider1_16(this.mc);
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public KeyCategory createKeyCategory(@NonNull String localeKey) {
        return (name, defaultKey, handler) -> {
            int keyCode = InputMappings.getKey("key.keyboard." + defaultKey.toLowerCase(Locale.ROOT)).getValue();
            KeyBinding binding = new KeyBinding("key." + localeKey + '.' + name, keyCode, "key.categories." + localeKey);
            ClientRegistry.registerKeyBinding(binding);
            this.keyBindings.put(binding, handler);
        };
    }

    @Override
    public Optional<IFarPlayerClient> currentPlayer() {
        throw new UnsupportedOperationException(); //TODO
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

    //forge events

    @SubscribeEvent
    public void keyInput(InputEvent.KeyInputEvent event) {
        this.keyBindings.forEach((binding, handler) -> {
            if (binding.consumeClick()) {
                handler.run();
            }
        });
    }
}
