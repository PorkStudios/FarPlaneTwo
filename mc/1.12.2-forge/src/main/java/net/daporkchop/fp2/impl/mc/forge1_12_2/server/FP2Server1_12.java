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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.generic.FChangedEvent;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.server.ATMinecraftServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.network.IMixinNetHandlerPlayServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.server.IMixinMinecraftServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.FColumn1_12_2;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Manages initialization of FP2 on the server.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Server1_12 {
    @NonNull
    private final FP2Forge1_12 fp2;

    public void preInit() {
        if (!PlatformInfo.IS_64BIT) { //require 64-bit
            this.fp2().unsupported("Your system or JVM is not 64-bit!\nRequired by FarPlaneTwo.");
        } else if (!PlatformInfo.IS_LITTLE_ENDIAN) { //require little-endian
            this.fp2().unsupported("Your system is not little-endian!\nRequired by FarPlaneTwo.");
        }

        System.setProperty("porklib.native.printStackTraces", "true");
        if (!Zstd.PROVIDER.isNative()) {
            this.fp2().log().alert("Native ZSTD could not be loaded! This will have SERIOUS performance implications!");
        }

        //register self to listen for events
        this.fp2().eventBus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void init() {
    }

    public void postInit() {
    }

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(FChangedEvent<FP2Config> event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) { //a server instance is currently present, update the serverConfig instance for every connected player
            server.addScheduledTask(() -> ((ATMinecraftServer1_12) server).getPlayerList().getPlayers().forEach(player -> ((IMixinNetHandlerPlayServer1_12) player.connection).fp2_farPlayerServer().fp2_IFarPlayer_serverConfig(this.fp2().globalConfig())));
        }
    }

    @FEventHandler
    protected void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        ((IMixinMinecraftServer1_12) server).fp2_initWorldServer();
    }

    @FEventHandler
    protected void onServerStopped(FMLServerStoppedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        ((IMixinMinecraftServer1_12) server).fp2_closeWorldServer();
    }

    //forge events

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            ((IMixinWorldServer1_12) event.getWorld()).fp2_initLevelServer();
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            ((IMixinWorldServer1_12) event.getWorld()).fp2_closeLevelServer();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            event.player.sendMessage(new TextComponentTranslation(MODID + ".playerJoinWarningMessage"));

            IFarPlayerServer player = ((IMixinNetHandlerPlayServer1_12) ((EntityPlayerMP) event.player).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_serverConfig(this.fp2().globalConfig());
            player.fp2_IFarPlayer_sendPacket(new SPacketHandshake());
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayerMP) {
            IFarPlayerServer player = ((IMixinNetHandlerPlayServer1_12) ((EntityPlayerMP) event.getEntity()).connection).fp2_farPlayerServer();

            //cubic chunks world data information has already been sent
            player.fp2_IFarPlayer_joinedWorld(((IMixinWorldServer1_12) event.getWorld()).fp2_levelServer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            IFarPlayerServer player = ((IMixinNetHandlerPlayServer1_12) ((EntityPlayerMP) event.player).connection).fp2_farPlayerServer();

            if (player != null) { //can happen if the player is kicked during the login sequence
                player.fp2_IFarPlayer_close();
            }
        }
    }

    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.END) {
            ((IMixinWorldServer1_12) event.world).fp2_levelServer().eventBus().fire(new TickEndEvent());

            event.world.playerEntities.forEach(player -> ((IMixinNetHandlerPlayServer1_12) ((EntityPlayerMP) player).connection).fp2_farPlayerServer().fp2_IFarPlayer_update());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        Chunk chunk = event.getChunk();
        ((IMixinWorldServer1_12) event.getWorld()).fp2_levelServer().eventBus().fire(new ColumnSavedEvent(Vec2i.of(chunk.x, chunk.z), new FColumn1_12_2(chunk), event.getData()));
    }
}
