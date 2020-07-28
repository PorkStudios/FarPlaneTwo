/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.FP2ResourceReloadListener;
import net.daporkchop.fp2.client.GlobalInfo;
import net.daporkchop.fp2.client.KeyBindings;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.net.client.CPacketDropAllPieces;
import net.daporkchop.fp2.net.client.CPacketRenderMode;
import net.daporkchop.fp2.net.server.SPacketPieceData;
import net.daporkchop.fp2.net.server.SPacketReady;
import net.daporkchop.fp2.net.server.SPacketRenderingStrategy;
import net.daporkchop.fp2.net.server.SPacketUnloadPiece;
import net.daporkchop.fp2.server.FarWorldBlockChangeListener;
import net.daporkchop.fp2.server.ServerConstants;
import net.daporkchop.fp2.strategy.common.IFarContext;
import net.daporkchop.fp2.strategy.common.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderer;
import net.daporkchop.fp2.util.IFarPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = MODID,
        useMetadata = true,
        acceptedMinecraftVersions = "1.12.2")
public class FP2 {
    public static final String MODID = "fp2";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);

        this.registerPackets();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new FP2ResourceReloadListener());
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            KeyBindings.register();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            HeightmapRenderer.reloadHeightShader(false); //load HeightmapRenderer on client thread
        }
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        ServerConstants.init();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        ServerConstants.shutdown();
    }

    protected void registerPackets() {
        int id = 0;
        NETWORK_WRAPPER.registerMessage(CPacketRenderMode.Handler.class, CPacketRenderMode.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(CPacketDropAllPieces.Handler.class, CPacketDropAllPieces.class, id++, Side.SERVER);
        NETWORK_WRAPPER.registerMessage(SPacketReady.Handler.class, SPacketReady.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketRenderingStrategy.Handler.class, SPacketRenderingStrategy.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketPieceData.Handler.class, SPacketPieceData.class, id++, Side.CLIENT);
        NETWORK_WRAPPER.registerMessage(SPacketUnloadPiece.Handler.class, SPacketUnloadPiece.class, id++, Side.CLIENT);
    }

    //
    // events
    //

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            //server
            ((IFarContext) event.getWorld()).init(FP2Config.renderMode);
            event.getWorld().addEventListener(new FarWorldBlockChangeListener(((IFarContext) event.getWorld()).world()));
        } else {
            //client
            GlobalInfo.reloadUVs();
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && ((IFarContext) event.getWorld()).isInitialized()) {
            try {
                ((IFarContext) event.getWorld()).world().close();
                LOGGER.info("Closed far world storage in dimension {}.", event.getWorld().provider.getDimension());
            } catch (IOException e) {
                LOGGER.fatal("Unable to close far world storage!", e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        LOGGER.debug("Handling login for player {}", event.player.getName());
        NETWORK_WRAPPER.sendTo(new SPacketReady(), (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LOGGER.debug("Handling logout for player {}", event.player.getName());
        ((IFarContext) event.player.world).tracker().playerRemove((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.player.world.isRemote && ((IFarPlayer) event.player).isReady()) {
            ((IFarContext) event.player.getServer().getWorld(event.fromDim)).tracker().playerRemove((EntityPlayerMP) event.player);
            ((IFarContext) event.player.getServer().getWorld(event.toDim)).tracker().playerAdd((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote) {
            IFarPlayerTracker tracker = ((IFarContext) event.world).tracker();
            event.world.playerEntities.stream()
                    .map(EntityPlayerMP.class::cast)
                    .filter(player -> ((IFarPlayer) player).isReady())
                    .forEach(tracker::playerMove);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void connectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClientConstants.init();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void disconnectedFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientConstants.shutdown();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void keyInput(InputEvent.KeyInputEvent event) {
        if (KeyBindings.RELOAD_SHADERS.isPressed()) {
            ShaderManager.reload();
        } else if (KeyBindings.DROP_PIECES.isPressed()) {
            NETWORK_WRAPPER.sendToServer(new CPacketDropAllPieces());
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(FP2.MODID)) {
            ConfigManager.sync(FP2.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
        }
    }
}
