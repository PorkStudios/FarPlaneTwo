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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.player.IFarPlayerServer;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Manages initialization of FP2 on the server.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class FP2Server {
    /**
     * Called during {@link FMLPreInitializationEvent}.
     */
    public void preInit() {
        if (!PlatformInfo.IS_64BIT) { //require 64-bit
            fp2().unsupported("Your system or JVM is not 64-bit!\nRequired by FarPlaneTwo.");
        } else if (!PlatformInfo.IS_LITTLE_ENDIAN) { //require little-endian
            fp2().unsupported("Your system is not little-endian!\nRequired by FarPlaneTwo.");
        }

        System.setProperty("porklib.native.printStackTraces", "true");
        if (!Zstd.PROVIDER.isNative()) {
            fp2().log().alert("Native ZSTD could not be loaded! This will have SERIOUS performance implications!");
        }

        ServerEvents.register();

        ConfigListenerManager.add(() -> {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) { //a server instance is currently present, update the serverConfig instance for every connected player
                server.addScheduledTask(() -> server.playerList.getPlayers().forEach(player -> ((IFarPlayerServer) player.connection).fp2_IFarPlayer_serverConfig(fp2().globalConfig())));
            }
        });
    }

    /**
     * Called during {@link FMLInitializationEvent}.
     */
    public void init() {
    }

    /**
     * Called during {@link FMLPostInitializationEvent}.
     */
    public void postInit() {
        PUnsafe.ensureClassInitialized(IFarRenderMode.class);
    }
}
