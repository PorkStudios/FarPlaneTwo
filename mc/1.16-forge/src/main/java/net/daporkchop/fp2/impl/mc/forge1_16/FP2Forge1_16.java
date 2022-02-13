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

package net.daporkchop.fp2.impl.mc.forge1_16;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.util.I18n;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mod(MODID)
public final class FP2Forge1_16 extends FP2Core {
    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void dedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void enqueueIMC(InterModEnqueueEvent event) {
    }

    @SubscribeEvent
    public void processIMC(InterModProcessEvent event) {
    }

    @Override
    public ResourceProvider resourceProvider() {
        return null;
    }

    @Override
    public boolean hasClient() {
        return false;
    }

    @Override
    public boolean hasServer() {
        return false;
    }

    @Override
    protected Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public I18n i18n() {
        return null;
    }

    @Override
    public FP2Client client() {
        return null;
    }

    @Override
    public void unsupported(@NonNull String message) {
        throw new UnsupportedOperationException(message);
    }
}
