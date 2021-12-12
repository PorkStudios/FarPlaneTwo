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

package net.daporkchop.fp2;

import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.RegisterEvent;
import net.daporkchop.fp2.client.ClientEvents;
import net.daporkchop.fp2.client.FP2ResourceReloadListener;
import net.daporkchop.fp2.client.KeyBindings;
import net.daporkchop.fp2.client.TextureUVs;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.compat.x86.x86FeatureDetector;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.core.network.packet.standard.client.CPacketClientConfig;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketSessionBegin;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketTileData;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTile;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketUnloadTiles;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.fp2.debug.client.DebugClientEvents;
import net.daporkchop.fp2.debug.client.DebugKeyBindings;
import net.daporkchop.fp2.impl.mc.forge1_12_2.I18n1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui.GuiContext1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.log.ChatAsPorkLibLogger;
import net.daporkchop.fp2.impl.mc.forge1_12_2.log.Log4jAsPorkLibLogger;
import net.daporkchop.fp2.mode.heightmap.HeightmapRenderMode;
import net.daporkchop.fp2.mode.voxel.VoxelRenderMode;
import net.daporkchop.fp2.net.FP2Network;
import net.daporkchop.fp2.server.FP2Server;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.event.IdMappingsChangedEvent;
import net.daporkchop.fp2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.core.client.FP2Client.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = MODID,
        useMetadata = true,
        dependencies = "required-after:forgerocks@[6.20.3-1.12.2,);after:cubicchunks@[1.12.2-0.0.1188.0,)",
        acceptedMinecraftVersions = "1.12.2")
public class FP2 extends FP2Core implements ResourceProvider {
    private final Minecraft mc = Minecraft.getMinecraft();

    private String version = "";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.log(new Log4jAsPorkLibLogger(event.getModLog()));
        this.chat(new ChatAsPorkLibLogger(this.mc));

        Constants.FP2_LOG = event.getModLog();
        this.version = event.getModMetadata().version;

        this.log().info("Detected x86 SIMD extension: %s", x86FeatureDetector.INSTANCE.maxSupportedVectorExtension());

        FP2Network.preInit();

        if (FP2_DEBUG) {
            bigWarning("FarPlaneTwo debug mode enabled!");

            if (this.hasClient()) {
                ConfigListenerManager.add(() -> GLOBAL_SHADER_MACROS
                        .define("FP2_DEBUG_COLORS_ENABLED", fp2().globalConfig().debug().debugColors().enable())
                        .define("FP2_DEBUG_COLORS_MODE", fp2().globalConfig().debug().debugColors().ordinal()));

                MinecraftForge.EVENT_BUS.register(new DebugClientEvents());
            }
        }

        FP2Server.preInit();

        if (this.hasClient()) {
            if (!OPENGL_45) { //require at least OpenGL 4.5
                unsupported("Your system does not support OpenGL 4.5!\nRequired by FarPlaneTwo.");
            }

            if (!MC.getFramebuffer().isStencilEnabled() && !MC.getFramebuffer().enableStencil()) {
                if (OF && (PUnsafe.getBoolean(MC.gameSettings, OF_FASTRENDER_OFFSET) || PUnsafe.getInt(MC.gameSettings, OF_AALEVEL_OFFSET) > 0)) {
                    unsupported("FarPlaneTwo was unable to enable the OpenGL stencil buffer!\n"
                                + "Please launch the game without FarPlaneTwo and disable\n"
                                + "  OptiFine's \"Fast Render\" and \"Antialiasing\", then\n"
                                + "  try again.");
                } else {
                    unsupported("Unable to enable the OpenGL stencil buffer!\nRequired by FarPlaneTwo.");
                }
            }

            ClientEvents.register();

            ConfigListenerManager.add(() -> {
                if (MC.player != null && MC.player.connection != null) {
                    ((IFarPlayerClient) MC.player.connection).fp2_IFarPlayerClient_send(new CPacketClientConfig().config(fp2().globalConfig()));
                }
            });
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FP2Server.init();

        if (this.hasClient()) {
            KeyBindings.register();
        }

        if (FP2_DEBUG && this.hasClient()) {
            DebugKeyBindings.register();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ConfigListenerManager.fire();
        FP2Server.postInit();

        if (this.hasClient()) {
            //TODO: move this to core?
            GLOBAL_SHADER_MACROS.define("T_SHIFT", T_SHIFT).define("RENDER_PASS_COUNT", RENDER_PASS_COUNT);

            TextureUVs.initDefault();

            MC.resourceManager.registerReloadListener(new FP2ResourceReloadListener());
        }
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        try {
            ServerThreadMarkedFutureExecutor.getFor(FMLCommonHandler.instance().getMinecraftServerInstance()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Mod.EventHandler
    public void onIdsChanged(FMLModIdMappingEvent event) {
        this.eventBus().fire(new IdMappingsChangedEvent());

        FastRegistry.reload();
    }

    @NetworkCheckHandler
    public boolean checkCanConnectWithMods(Map<String, String> modVersions, Side remoteSide) {
        String remoteVersion = modVersions.get(MODID);

        return FP2_DEBUG //accept any version in debug mode
               || remoteVersion == null //fp2 isn't present on the remote side, so it doesn't matter whether or not it's compatible
               || this.version.equals(remoteVersion); //if fp2 is present, the versions must match
    }

    @FEventHandler
    public void registerDefaultRenderModes(RegisterEvent<IFarRenderMode<?, ?>> event) {
        event.registry()
                .addLast("voxel", new VoxelRenderMode())
                .addLast("heightmap", new HeightmapRenderMode());
    }

    //
    // ResourceProvider
    //

    @Override
    public ResourceProvider resourceProvider() {
        return this;
    }

    @Override
    public InputStream provideResourceAsStream(@NonNull Identifier id) throws IOException {
        try {
            return FMLClientHandler.instance().getClient().getResourceManager().getResource(new ResourceLocation(id.toString())).getInputStream();
        } catch (FileNotFoundException e) {
            throw new ResourceNotFoundException(id, e);
        }
    }

    //
    // FP2Core
    //

    @Override
    public boolean hasClient() {
        return FMLCommonHandler.instance().getSide() == Side.CLIENT;
    }

    @Override
    public boolean hasServer() {
        return true; //the server is always present, be it integrated or dedicated
    }

    @Override
    protected Path configDir() {
        return Loader.instance().getConfigDir().toPath();
    }

    @Override
    public I18n i18n() {
        return new I18n1_12_2();
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        return new GuiContext1_12_2().createScreenAndOpen(this.mc, factory);
    }

    @Override
    public String[] renderModeNames() {
        return IFarRenderMode.REGISTRY.nameStream().toArray(String[]::new);
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        return this.mc.gameSettings.renderDistanceChunks;
    }

    @Override
    protected void registerPackets(@NonNull RegisterPacketsEvent event) { //TODO: remove this once all packets have been moved to :core module
        super.registerPackets(event);

        event.registerClientbound(SPacketSessionBegin.class)
                .registerClientbound(SPacketTileData.class)
                .registerClientbound(SPacketUnloadTile.class)
                .registerClientbound(SPacketUnloadTiles.class);
    }
}
