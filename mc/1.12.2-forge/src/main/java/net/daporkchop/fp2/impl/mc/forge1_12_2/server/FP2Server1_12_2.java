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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.ChangedEvent;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.network.IMixinNetHandlerPlayServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.FColumn1_12_2;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * Manages initialization of FP2 on the server.
 *
 * @author DaPorkchop_
 */
@Getter
public class FP2Server1_12_2 extends FP2Server {
    private final FP2Forge1_12_2 fp2;

    public FP2Server1_12_2(@NonNull FP2Forge1_12_2 fp2) {
        this.fp2 = fp2;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void init(@NonNull FutureExecutor serverThreadExecutor) {
        super.init(serverThreadExecutor);

        //register self to listen for events
        MinecraftForge.EVENT_BUS.register(this);

        //if we're in debug mode in a dev environment on the dedicated server, inject locale data into the language map
        if (FP2_DEBUG && FMLLaunchHandler.isDeobfuscatedEnvironment() && FMLCommonHandler.instance().getSide() == Side.SERVER) {
            try (InputStream in = FResources.findForDebug("minecraft_pack_format_v3").getResource(Paths.get("assets/" + MODID + "/lang/en_us.lang")).get().getThrowing()) {
                LanguageMap.inject(in);
            }
        }
    }

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(ChangedEvent<FP2Config> event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) { //a server instance is currently present, update the serverConfig instance for every connected player
            server.addScheduledTask(() -> server.playerList.getPlayers().forEach(player -> ((IMixinNetHandlerPlayServer) player.connection).fp2_farPlayerServer().fp2_IFarPlayer_serverConfig(this.fp2().globalConfig())));
        }
    }

    //forge events

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            ((IMixinWorldServer) event.getWorld()).fp2_farWorldServer().fp2_IFarWorldServer_init();
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            ((IMixinWorldServer) event.getWorld()).fp2_farWorldServer().fp2_IFarWorld_close();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            event.player.sendMessage(new TextComponentTranslation(MODID + ".playerJoinWarningMessage"));

            IFarPlayerServer player = ((IMixinNetHandlerPlayServer) ((EntityPlayerMP) event.player).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_serverConfig(this.fp2().globalConfig());
            player.fp2_IFarPlayer_sendPacket(new SPacketHandshake());
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayerMP) {
            IFarPlayerServer player = ((IMixinNetHandlerPlayServer) ((EntityPlayerMP) event.getEntity()).connection).fp2_farPlayerServer();

            //cubic chunks world data information has already been sent
            player.fp2_IFarPlayer_joinedWorld(((IMixinWorldServer) event.getWorld()).fp2_farWorldServer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            IFarPlayerServer player = ((IMixinNetHandlerPlayServer) ((EntityPlayerMP) event.player).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_close();
        }
    }

    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.END) {
            ((IMixinWorldServer) event.world).fp2_farWorldServer().fp2_IFarWorldServer_eventBus().fire(new TickEndEvent());

            event.world.playerEntities.forEach(player -> ((IMixinNetHandlerPlayServer) ((EntityPlayerMP) player).connection).fp2_farPlayerServer().fp2_IFarPlayer_update());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        Chunk chunk = event.getChunk();
        ((IMixinWorldServer) event.getWorld()).fp2_farWorldServer().fp2_IFarWorldServer_eventBus().fire(new ColumnSavedEvent(Vec2i.of(chunk.x, chunk.z), new FColumn1_12_2(chunk), event.getData()));
    }
}
