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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.debug.client;

import net.daporkchop.fp2.impl.mc.forge1_12_2.debug.resources.DebugResourcePack1_12;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft1_12 {
    @Shadow @Final public List<IResourcePack> defaultResourcePacks;

    @Inject(method = "Lnet/minecraft/client/Minecraft;init()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IReloadableResourceManager;registerReloadListener(Lnet/minecraft/client/resources/IResourceManagerReloadListener;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0),
            require = 1, allow = 1)
    private void fp2_debug_init_registerDebugResourcePack(CallbackInfo ci) {
        //inject fp2 resources into classpath
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
            this.defaultResourcePacks.add(new DebugResourcePack1_12());
        }
    }
}