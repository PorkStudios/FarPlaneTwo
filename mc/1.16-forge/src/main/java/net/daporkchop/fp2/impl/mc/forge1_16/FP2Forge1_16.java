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

package net.daporkchop.fp2.impl.mc.forge1_16;

import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.debug.FP2Debug;
import net.daporkchop.fp2.core.log4j.util.log.Log4jAsPorkLibLogger;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.util.I18n;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.ATMinecraft1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.client.FP2Client1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.network.FP2Network1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.FP2Server1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.I18n1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.ParallelDispatchEventAsFutureExecutor1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * @author DaPorkchop_
 */
@Mod(MODID)
public final class FP2Forge1_16 extends FP2Core {
    public static boolean INITIALIZED = false; //TODO: this is a nasty hack

    public static Identifier getIdentifierForWorld(@NonNull World world) {
        return Identifier.from(world.dimension().location().toString());
    }

    private FP2Client1_16 client;
    private FP2Server1_16 server;

    public FP2Forge1_16() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void construct(FMLConstructModEvent event) {
        this.log(new Log4jAsPorkLibLogger(LogManager.getLogger(MODID)));
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        event.getIMCStream("early_register_events"::equals).forEach(msg -> msg.<Consumer<? super FEventBus>>getMessageSupplier().get().accept(this.eventBus()));

        this.init();
    }

    @SubscribeEvent
    public void dedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        this.commonSetup2(event);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        Minecraft minecraft = event.getMinecraftSupplier().get();

        this.client = new FP2Client1_16(this, minecraft);
        this.client.init(new ParallelDispatchEventAsFutureExecutor1_16(event, cause -> {
            //This ensures that if an exception is thrown by code running on the client thread, we actually crash the game.
            // Unfortunately the internal DeferredWorkQueue doesn't provide a nice way to achieve this, and will not
            // until 1.20: https://github.com/MinecraftForge/MinecraftForge/pull/9449.
            // By adding a task to the Minecraft#progressTasks queue which rethrows the exception, we can force the game to crash
            // even though every other mechanism to schedule tasks on the client thread will just silently swallow exceptions.
            ((ATMinecraft1_16) minecraft).getProgressTasks().add(() -> {
                throw new Error("Failed to initialize FP2 client state", cause);
            });
        }));

        this.commonSetup2(event);
    }

    /**
     * Common setup code again, because {@link FMLCommonSetupEvent} is fired before {@link FMLDedicatedServerSetupEvent}/{@link FMLClientSetupEvent}.
     */
    private void commonSetup2 /* electric boogaloo */(ParallelDispatchEvent event) {
        //server is present on both sides
        this.server = new FP2Server1_16(this);
        this.server.init(new ParallelDispatchEventAsFutureExecutor1_16(event, cause -> {
            this.log().fatal(new Error("Failed to initialize FP2 server state", cause));
            System.exit(1);
        }));

        FP2Network1_16.init(this);

        if (FP2_DEBUG) {
            FP2Debug.init(this);
        }

        INITIALIZED = true;
    }

    @SubscribeEvent
    public void enqueueIMC(InterModEnqueueEvent event) {
    }

    @SubscribeEvent
    public void processIMC(InterModProcessEvent event) {
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
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public I18n i18n() {
        return new I18n1_16();
    }
}
