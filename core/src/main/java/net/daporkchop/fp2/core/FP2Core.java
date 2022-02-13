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

package net.daporkchop.fp2.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.RegisterEvent;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.event.AbstractChangedEvent;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapConstants;
import net.daporkchop.fp2.core.mode.heightmap.HeightmapRenderMode;
import net.daporkchop.fp2.core.mode.voxel.VoxelConstants;
import net.daporkchop.fp2.core.mode.voxel.VoxelRenderMode;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.core.network.packet.debug.client.CPacketDebugDropAllTiles;
import net.daporkchop.fp2.core.network.packet.debug.server.SPacketDebugUpdateStatistics;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionEnd;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUpdateConfig;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.logging.Logger;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class FP2Core implements FP2 {
    public static final String MODID = FP2.MODID;

    /**
     * @return the current FP2 instance
     */
    public static FP2Core fp2() {
        return (FP2Core) FP2.fp2();
    }

    private final FEventBus eventBus = new EventBus();

    @Setter(AccessLevel.NONE)
    private FP2Config globalConfig;

    @NonNull
    private Logger log;

    @SneakyThrows
    protected FP2Core() {
        //initialize global fp2 instance
        Method method = PorkUtil.classForName(FP2.class.getCanonicalName() + "Holder").getDeclaredMethod("init", FP2.class);
        method.setAccessible(true);
        method.invoke(null, this);

        //register self as an event listener
        this.eventBus.register(this);
    }

    /**
     * Initializes this instance.
     * <p>
     * The following properties must be set before calling this method:
     * <ul>
     *     <li>{@link #log(Logger)}</li>
     * </ul>
     */
    protected void init() {
        //load global config
        this.globalConfig = FP2Config.load(this.configDir());
    }

    /**
     * @return a {@link ResourceProvider} which can load game resources
     */
    public abstract ResourceProvider resourceProvider();

    /**
     * @return whether or not the active game distribution contains a client
     */
    public abstract boolean hasClient();

    /**
     * @return whether or not the active game distribution contains a server
     */
    public abstract boolean hasServer();

    /**
     * @return the directory where fp2's config file is stored
     */
    protected abstract Path configDir();

    /**
     * @return the {@link I18n} instance used for localizing strings
     */
    public abstract I18n i18n();

    /**
     * @return the {@link FP2Client} instance for interacting with client features
     * @throws UnsupportedOperationException if the active game distribution does not contain a client
     */
    public abstract FP2Client client();

    /**
     * Opens a new {@link GuiScreen}.
     *
     * @param factory a factory for creating a new {@link GuiScreen}
     * @param <T>     the {@link GuiScreen}
     * @return the created {@link GuiScreen}
     * @throws UnsupportedOperationException if the active game distribution does not contain a client
     */
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return this.client().openScreen(factory);
    }

    @Deprecated
    public String[] renderModeNames() {
        return IFarRenderMode.REGISTRY.nameStream().toArray(String[]::new);
    }

    /**
     * Sets the global {@link FP2Config}.
     *
     * @param config the new {@link FP2Config} instance
     */
    public synchronized void globalConfig(@NonNull FP2Config config) {
        if (Objects.equals(this.globalConfig, config)) { //config is unchanged, do nothing
            return;
        }

        FP2Config.save(this.configDir(), config);
        FP2Config oldConfig = this.globalConfig;
        this.globalConfig = config;

        this.eventBus().fire(new AbstractChangedEvent<FP2Config>(oldConfig, config) {});
    }

    /**
     * Informs the user that the current system is unsupported by displaying the given message and subsequently crashing the game.
     *
     * @param message the message
     */
    public abstract void unsupported(@NonNull String message);

    @FEventHandler
    protected void registerDefaultRenderModes(RegisterEvent<IFarRenderMode<?, ?>> event) {
        event.registry()
                .addLast(VoxelConstants.NAME, new VoxelRenderMode())
                .addLast(HeightmapConstants.NAME, new HeightmapRenderMode());
    }

    @FEventHandler
    protected void registerPackets(@NonNull RegisterPacketsEvent event) {
        event.registerServerbound(CPacketClientConfig.class)
                .registerServerbound(CPacketDebugDropAllTiles.class);

        event.registerClientbound(SPacketHandshake.class)
                .registerClientbound(SPacketSessionBegin.class)
                .registerClientbound(SPacketSessionEnd.class)
                .registerClientbound(SPacketTileData.class)
                .registerClientbound(SPacketUnloadTile.class)
                .registerClientbound(SPacketUnloadTiles.class)
                .registerClientbound(SPacketUpdateConfig.Merged.class)
                .registerClientbound(SPacketUpdateConfig.Server.class)
                .registerClientbound(SPacketDebugUpdateStatistics.class);
    }
}
