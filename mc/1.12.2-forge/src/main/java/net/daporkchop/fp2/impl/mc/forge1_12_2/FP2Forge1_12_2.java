/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2;

import lombok.NonNull;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.debug.FP2Debug;
import net.daporkchop.fp2.core.log4j.Log4jAsPorkLibLogger;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.fp2.core.util.threading.futureexecutor.ImmediateFutureExecutor;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.FP2Client1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.impl.mc.forge1_12_2.network.FP2Network1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.FP2Server1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.I18n1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.event.IdMappingsChangedEvent;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.Map;

import static net.daporkchop.fp2.core.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = FP2.MODID,
        useMetadata = true,
        dependencies = "required-after:forgerocks@[6.28.2,);after:cubicchunks@[1.12.2-0.0.1255.0,)",
        acceptedMinecraftVersions = "1.12.2")
public final class FP2Forge1_12_2 extends FP2Core {
    public static Identifier getIdentifierForWorld(@NonNull World world) {
        DimensionType dimensionType = world.provider.getDimensionType();

        //sanity check because i'm not entirely sure what kind of crazy shit mods do with dimension types, and i want to be sure not to screw anything up
        for (DimensionType otherType : DimensionType.values()) {
            if (otherType != dimensionType) {
                checkState(!dimensionType.getName().equals(otherType.getName()), "multiple dimension types with name '%s' exist, ids=[%d, %d]", dimensionType.getId(), otherType.getId());
            }
        }

        return Identifier.fromLenient("minecraft", dimensionType.getName());
    }

    public static int getDimensionForWorldIdentifier(@NonNull Identifier id) {
        return DimensionType.byName(id.path()).getId();
    }

    private FP2Client1_12_2 client;
    private FP2Server1_12_2 server;

    @Mod.Metadata
    private ModMetadata metadata;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        this.log(new Log4jAsPorkLibLogger(LogManager.getLogger(MODID)));
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.init();

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            this.client = new FP2Client1_12_2(this, Minecraft.getMinecraft());
            this.client.init(ImmediateFutureExecutor.INSTANCE);
        }

        this.server = new FP2Server1_12_2(this);
        this.server.init(ImmediateFutureExecutor.INSTANCE);

        FP2Network1_12_2.init();

        if (FP2_DEBUG) {
            FP2Debug.init(this);
        }
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        this.eventBus().fire(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        try {
            this.eventBus().fire(event);

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
               || this.metadata.version.equals(remoteVersion); //if fp2 is present, the versions must match
    }

    //
    // FP2Core
    //

    @Override
    public boolean hasClient() {
        return this.client != null;
    }

    @Override
    public FP2Client client() {
        if (this.client != null) {
            return this.client;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean hasServer() {
        return true; //the server is always present, be it integrated or dedicated
    }

    @Override
    public FP2Server server() {
        return this.server;
    }

    @Override
    protected Path configDir() {
        return Loader.instance().getConfigDir().toPath();
    }

    @Override
    public I18n i18n() {
        return new I18n1_12_2();
    }
}
