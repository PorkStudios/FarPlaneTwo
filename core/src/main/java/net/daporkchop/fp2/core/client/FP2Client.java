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
import net.daporkchop.fp2.core.client.render.RenderManager;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderRegistry;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.util.listener.ListenerList;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.GLProfile;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.logging.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class FP2Client {
    private ReloadableShaderRegistry reloadableShaderRegistry;

    private final ListenerList<TextureUVs.ReloadListener> textureUVsReloadListeners = ListenerList.create(TextureUVs.ReloadListener.class);

    @Setter(AccessLevel.PROTECTED)
    private Logger chat;
    private OpenGL gl;
    private GlobalRenderer globalRenderer;
    private RenderManager renderManager;

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

            //write OpenGL context info to the log, because surprisingly vanilla doesn't do this unless the game actually crashes
            this.fp2().log()
                    .info("OpenGL context: " + gl)
                    .info("OpenGL vendor: " + gl.glGetString(GL_VENDOR))
                    .info("OpenGL renderer: " + gl.glGetString(GL_RENDERER));

            if (FP2_DEBUG) {
                Path path = Paths.get(".fp2", "context_override.properties");
                if (Files.exists(path)) {
                    //load properties
                    Properties properties = new Properties();
                    try (InputStream in = Files.newInputStream(path)) {
                        properties.load(in);
                    }

                    //parse properties
                    GLVersion version = GLVersion.valueOf(Objects.requireNonNull(properties.getProperty("version"), "version"));
                    GLExtensionSet extensions = Stream.of(Objects.requireNonNull(properties.getProperty("extensions"), "extensions").split(","))
                            .map(GLExtension::valueOf)
                            .collect(GLExtensionSet.toExtensionSet());
                    GLProfile profile = GLProfile.valueOf(Objects.requireNonNull(properties.getProperty("profile"), "profile"));
                    boolean forwardCompatibility = Boolean.parseBoolean(Objects.requireNonNull(properties.getProperty("forwardCompatibility"), "forwardCompatibility"));

                    gl = gl.wrapAsLegacy(version, extensions, profile, forwardCompatibility);

                    this.fp2().log().info("Emulating OpenGL context: " + gl);
                }
            }

            GLVersion minimumVersion = GLVersion.OpenGL30;
            GLExtensionSet minimumRequiredExtensions = GLExtensionSet.empty()
                    .add(GLExtension.GL_ARB_uniform_buffer_object) //needed for select and draw state uniforms (this requirement will probably not be relaxed, too much hassle)
                    .add(GLExtension.GL_ARB_gpu_shader5) //needed for input and output blocks in shaders (this can be relaxed)
                    .add(GLExtension.GL_ARB_shader_storage_buffer_object) //needed for state -> texture UV mappings (this requirement can be relaxed)
                    .add(GLExtension.GL_ARB_program_interface_query); //needed by ShaderProgram if any SSBO bindings are added (this requirement can be removed with GL_ARB_shader_storage_buffer_object)

            Object unsupportedCause = null;
            if (gl.version().compareTo(minimumVersion) < 0) {
                unsupportedCause = minimumVersion;
            } else if (!gl.supports(minimumRequiredExtensions)) {
                unsupportedCause = minimumRequiredExtensions.removeAll(gl.allExtensions());
            }
            if (unsupportedCause != null) {
                String msg = "Your system does not support " + unsupportedCause + "!\nRequired by FarPlaneTwo.";
                this.fp2().unsupported(msg);
                throw new UnsupportedOperationException(msg);
            }

            this.gl = gl;
            this.globalRenderer = new GlobalRenderer(this.fp2(), gl);
            this.renderManager = this.createRenderManager();
        }); //TODO: without joining this, the initial render state *could* end up being uninitialized by the time we try to access it

        //register self to listen for events
        this.fp2().eventBus().register(this);
    }

    protected abstract RenderManager createRenderManager();

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

    @Deprecated
    public abstract int vanillaRenderDistanceChunks();

    public abstract int terrainTextureUnit();

    public abstract int lightmapTextureUnit();

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(FChangedEvent<FP2Config> event) {
        //send updated config to server
        this.currentPlayer().ifPresent(player -> player.send(CPacketClientConfig.create(event.next())));
    }
}
