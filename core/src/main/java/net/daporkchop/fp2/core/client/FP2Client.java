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

package net.daporkchop.fp2.core.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.generic.FChangedEvent;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.logging.Logger;

import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class FP2Client {
    private final ShaderMacros.Mutable globalShaderMacros = new ShaderMacros.Mutable();

    private ReloadableShaderRegistry reloadableShaderRegistry;

    @Setter(AccessLevel.PROTECTED)
    private Logger chat;
    private OpenGL gl;
    private GlobalRenderer globalRenderer;

    /**
     * Initializes this instance.
     * <p>
     * The following properties must be accessible (set to a non-null value) before calling this method:
     * <ul>
     *     <li>{@link #fp2()}</li>
     *     <li>{@link #chat()}</li>
     * </ul>
     *
     * @param clientThreadExecutor a {@link FutureExecutor} for scheduling tasks to be executed on the client thread
     */
    public void init(@NonNull FutureExecutor clientThreadExecutor) {
        checkState(this.fp2() != null, "fp2() must be set!");
        checkState(this.chat() != null, "chat() must be set!");

        this.reloadableShaderRegistry = new ReloadableShaderRegistry(this.fp2());

        //require at least OpenGL 4.5
        clientThreadExecutor.run(() -> {
            OpenGL gl = OpenGL.forCurrent();
            if (gl.version().compareTo(GLVersion.OpenGL45) < 0) {
                this.fp2().unsupported("Your system does not support OpenGL 4.5!\nRequired by FarPlaneTwo.");
                throw new UnsupportedOperationException("Your system does not support OpenGL 4.5!\nRequired by FarPlaneTwo.");
            }

            this.gl = gl;
            this.globalRenderer = new GlobalRenderer(this.fp2(), gl);
        }).join(); //TODO: don't block the thread???

        //update debug color macros
        if (FP2_DEBUG) {
            this.updateDebugColorMacros(this.fp2().globalConfig());
        }

        //register self to listen for events
        this.fp2().eventBus().register(this);
    }

    /**
     * @return the {@link FP2Core} instance which this {@link FP2Client} is used for
     */
    public abstract FP2Core fp2();

    /**
     * @return a {@link ResourceProvider} which can load game resources
     */
    public abstract ResourceProvider resourceProvider();

    /**
     * Opens a new {@link GuiScreen}.
     *
     * @param factory a factory for creating a new {@link GuiScreen}
     * @param <T>     the {@link GuiScreen}
     * @return the created {@link GuiScreen}
     * @throws UnsupportedOperationException if the active game distribution does not contain a client
     */
    public abstract <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory);

    /**
     * Creates a new {@link KeyCategory}.
     *
     * @param localeKey the locale key of the category name
     * @return the created {@link KeyCategory}
     */
    public abstract KeyCategory createKeyCategory(@NonNull String localeKey);

    /**
     * @return the current {@link IFarPlayerClient}, or an empty {@link Optional} if the client is not connected
     */
    public abstract Optional<? extends IFarPlayerClient> currentPlayer();

    //TODO: somehow move Reversed-Z stuff out of FP2Client, and preferably into :gl
    @Deprecated
    public abstract void enableReverseZ();

    @Deprecated
    public abstract void disableReverseZ();

    @Deprecated
    public abstract boolean isReverseZ();

    @Deprecated
    public abstract int vanillaRenderDistanceChunks();

    /**
     * Updates the debug color macros to reflect the state from the given {@link FP2Config} instance.
     *
     * @param config the current {@link FP2Config} instance
     */
    protected void updateDebugColorMacros(@NonNull FP2Config config) {
        this.globalShaderMacros()
                .define("FP2_DEBUG_COLORS_ENABLED", config.debug().debugColors().enable())
                .define("FP2_DEBUG_COLORS_MODE", config.debug().debugColors().ordinal());
    }

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(FChangedEvent<FP2Config> event) {
        if (FP2_DEBUG) {
            this.updateDebugColorMacros(event.next());
        }

        //send updated config to server
        this.currentPlayer().ifPresent(player -> player.send(CPacketClientConfig.create(event.next())));
    }
}
