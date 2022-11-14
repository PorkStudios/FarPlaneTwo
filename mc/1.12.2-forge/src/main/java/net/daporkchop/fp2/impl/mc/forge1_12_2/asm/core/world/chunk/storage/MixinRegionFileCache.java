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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.world.chunk.storage;

import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region.RegionByteBufOutput;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region.ThreadSafeRegionFileCache;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Makes {@link RegionFileCache} actually be thread-safe.
 *
 * @author DaPorkchop_
 */
@Mixin(RegionFileCache.class)
public abstract class MixinRegionFileCache {
    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static synchronized RegionFile createOrLoadRegionFile(File worldDir, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException("assets/fp2");
    }

    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static synchronized RegionFile getRegionFileIfExists(File worldDir, int chunkX, int chunkZ) {
        throw new UnsupportedOperationException("assets/fp2");
    }

    @Inject(method = "Lnet/minecraft/world/chunk/storage/RegionFileCache;clearRegionFileReferences()V",
            at = @At("HEAD"))
    private static void fp2_clearRegionFileReferences_redirectToThreadSafeRegionFileCache(CallbackInfo ci) {
        try {
            ThreadSafeRegionFileCache.INSTANCE.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static DataInputStream getChunkInputStream(File worldDir, int chunkX, int chunkZ) {
        try {
            return ThreadSafeRegionFileCache.INSTANCE.read(worldDir.toPath().resolve("region"), chunkX, chunkZ);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static DataOutputStream getChunkOutputStream(File worldDir, int chunkX, int chunkZ) {
        return new DataOutputStream(new RegionByteBufOutput(ThreadSafeRegionFileCache.INSTANCE, worldDir.toPath().resolve("region"), chunkX, chunkZ));
    }

    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static boolean chunkExists(File worldDir, int chunkX, int chunkZ) {
        try {
            return ThreadSafeRegionFileCache.INSTANCE.exists(worldDir.toPath().resolve("region"), chunkX, chunkZ);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
