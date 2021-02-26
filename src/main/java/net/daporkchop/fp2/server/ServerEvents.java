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

package net.daporkchop.fp2.server;

import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.net.server.SPacketReady;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.IFarPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class ServerEvents {
    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            //TODO: make this better
            ((IFarWorldServer) event.getWorld()).forEachContext(context -> event.getWorld().addEventListener(new FarWorldBlockChangeListener(context.world())));
        }
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            ((IFarWorldServer) event.getWorld()).close();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Constants.LOGGER.debug("Handling login for player {}", event.player.getName());
        NETWORK_WRAPPER.sendTo(new SPacketReady(), (EntityPlayerMP) event.player);
        event.player.sendMessage(new TextComponentString("§c§lFarPlaneTwo pre-pre-pre-alpha build: use at your own risk!"));
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Constants.LOGGER.debug("Handling logout for player {}", event.player.getName());
        IFarRenderMode<?, ?> mode = ((IFarPlayer) event.player).activeMode();
        if (mode != null) {
            ((IFarWorldServer) event.player.world).contextFor(mode).world().tracker().playerRemove((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        IFarRenderMode<?, ?> mode;
        if (!event.player.world.isRemote && (mode = ((IFarPlayer) event.player).activeMode()) != null) {
            ((IFarWorldServer) event.player.getServer().getWorld(event.fromDim)).contextFor(mode).world().tracker().playerRemove((EntityPlayerMP) event.player);
            ((IFarWorldServer) event.player.getServer().getWorld(event.toDim)).contextFor(mode).world().tracker().playerAdd((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.END) {
            long time = event.world.getTotalWorldTime();
            if (time % 20L == 0L) {
                event.world.playerEntities.forEach(player -> {
                    IFarRenderMode<?, ?> mode = ((IFarPlayer) player).activeMode();
                    if (mode != null) {
                        ((IFarWorldServer) event.world).contextFor(mode).world().tracker().playerMove((EntityPlayerMP) player);
                    }
                });
            }
        }
    }
}
