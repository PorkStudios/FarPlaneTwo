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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.server;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.generic.FChangedEvent;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.network.play.IMixinServerPlayNetHandler1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.server.IMixinMinecraftServer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.FColumn1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor1_16;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FP2Server1_16 extends FP2Server {
    @NonNull
    private final FP2Forge1_16 fp2;

    @Override
    public void init(@NonNull FutureExecutor serverThreadExecutor) {
        super.init(serverThreadExecutor);

        //register self to listen for events
        MinecraftForge.EVENT_BUS.register(this);
    }

    //fp2 events

    @FEventHandler
    protected void onConfigChanged(FChangedEvent<FP2Config> event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) { //a server instance is currently present, update the serverConfig instance for every connected player
            server.submitAsync(() -> server.getPlayerList()
                    .getPlayers()
                    .forEach(player -> ((IMixinServerPlayNetHandler1_16) player.connection).fp2_farPlayerServer().fp2_IFarPlayer_serverConfig(this.fp2().globalConfig())));
        }
    }

    //forge events

    @SubscribeEvent
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        ((IMixinMinecraftServer1_16) event.getServer()).fp2_initWorldServer();
    }

    @SubscribeEvent
    protected void onServerStopped(FMLServerStoppedEvent event) {
        try {
            ((IMixinMinecraftServer1_16) event.getServer()).fp2_closeWorldServer();

            ServerThreadMarkedFutureExecutor1_16.getFor(event.getServer()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ((IMixinServerWorld1_16) event.getWorld()).fp2_initLevelServer();
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof ServerWorld) {
            ((IMixinServerWorld1_16) event.getWorld()).fp2_closeLevelServer();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            event.getPlayer().sendMessage(new TranslationTextComponent(MODID + ".playerJoinWarningMessage"), Util.NIL_UUID);

            IFarPlayerServer player = ((IMixinServerPlayNetHandler1_16) ((ServerPlayerEntity) event.getPlayer()).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_serverConfig(this.fp2().globalConfig());
            player.fp2_IFarPlayer_sendPacket(new SPacketHandshake());
        }
    }

    @SubscribeEvent
    public void onPlayerJoinedWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            IFarPlayerServer player = ((IMixinServerPlayNetHandler1_16) ((ServerPlayerEntity) event.getEntity()).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_joinedWorld(((IMixinServerWorld1_16) event.getWorld()).fp2_levelServer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            IFarPlayerServer player = ((IMixinServerPlayNetHandler1_16) ((ServerPlayerEntity) event.getPlayer()).connection).fp2_farPlayerServer();
            player.fp2_IFarPlayer_close();
        }
    }

    @SubscribeEvent
    public void onWorldTickEnd(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof ServerWorld) {
            ((IMixinServerWorld1_16) event.world).fp2_levelServer().eventBus().fire(new TickEndEvent());

            ((ServerWorld) event.world).players().forEach(player -> ((IMixinServerPlayNetHandler1_16) player.connection).fp2_farPlayerServer().fp2_IFarPlayer_update());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        IChunk chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        ((IMixinServerWorld1_16) event.getWorld()).fp2_levelServer().eventBus().fire(new ColumnSavedEvent(Vec2i.of(pos.x, pos.z), new FColumn1_16(chunk), event.getData()));
    }
}
