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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.fixes.world.biome;

import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.BiomeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Some optional optimizations for biome color calculation.
 *
 * @author DaPorkchop_
 */
@Mixin(Biome.class)
public abstract class MixinBiome1_12 {
    @Unique
    private static final long BIOMEEVENT_BIOME_OFFSET = PUnsafe.pork_getOffset(BiomeEvent.class, "biome");
    @Unique
    private static final long BIOMEEVENT$BIOMECOLOR_ORIGINALCOLOR_OFFSET = PUnsafe.pork_getOffset(BiomeEvent.BiomeColor.class, "originalColor");

    @Unique
    private static final TL<Recycler<BiomeEvent.GetWaterColor>> GETWATERCOLOR_EVENT_RECYCLER = TL.initializedWith(() -> Recycler.unbounded(() -> new BiomeEvent.GetWaterColor(null, 0)));
    @Unique
    private static final TL<Recycler<BiomeEvent.GetGrassColor>> GETGRASSCOLOR_EVENT_RECYCLER = TL.initializedWith(() -> Recycler.unbounded(() -> new BiomeEvent.GetGrassColor(null, 0)));
    @Unique
    private static final TL<Recycler<BiomeEvent.GetFoliageColor>> GETFOLIAGECOLOR_EVENT_RECYCLER = TL.initializedWith(() -> Recycler.unbounded(() -> new BiomeEvent.GetFoliageColor(null, 0)));

    @Shadow
    @Final
    private int waterColor;

    /**
     * This prevents allocation of a new event instance.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public int getWaterColorMultiplier() {
        Recycler<BiomeEvent.GetWaterColor> recycler = GETWATERCOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetWaterColor event = recycler.allocate();

        //configure event
        PUnsafe.putObject(event, BIOMEEVENT_BIOME_OFFSET, this); //set event#biome (final field)
        PUnsafe.putInt(event, BIOMEEVENT$BIOMECOLOR_ORIGINALCOLOR_OFFSET, this.waterColor); //set event#originalColor (final field)
        event.setNewColor(this.waterColor); //set event#newColor

        //based on jdk8 implementation of MethodHandle#updateForm(LambdaForm), this is needed in order to ensure changes are made visible after writing to a final field with Unsafe
        PUnsafe.fullFence();

        //fire event
        MinecraftForge.EVENT_BUS.post(event);

        //return new color from event
        int newColor = event.getNewColor();
        recycler.release(event);
        return newColor;
    }

    /**
     * This prevents allocation of a new event instance.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public int getModdedBiomeGrassColor(int original) {
        Recycler<BiomeEvent.GetGrassColor> recycler = GETGRASSCOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetGrassColor event = recycler.allocate();

        //configure event
        PUnsafe.putObject(event, BIOMEEVENT_BIOME_OFFSET, this); //set event#biome (final field)
        PUnsafe.putInt(event, BIOMEEVENT$BIOMECOLOR_ORIGINALCOLOR_OFFSET, original); //set event#originalColor (final field)
        event.setNewColor(original); //set event#newColor

        //based on jdk8 implementation of MethodHandle#updateForm(LambdaForm), this is needed in order to ensure changes are made visible after writing to a final field with Unsafe
        PUnsafe.fullFence();

        //fire event
        MinecraftForge.EVENT_BUS.post(event);

        //return new color from event
        int newColor = event.getNewColor();
        recycler.release(event);
        return newColor;
    }

    /**
     * This prevents allocation of a new event instance.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite
    public int getModdedBiomeFoliageColor(int original) {
        Recycler<BiomeEvent.GetFoliageColor> recycler = GETFOLIAGECOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetFoliageColor event = recycler.allocate();

        //configure event
        PUnsafe.putObject(event, BIOMEEVENT_BIOME_OFFSET, this); //set event#biome (final field)
        PUnsafe.putInt(event, BIOMEEVENT$BIOMECOLOR_ORIGINALCOLOR_OFFSET, original); //set event#originalColor (final field)
        event.setNewColor(original); //set event#newColor

        //based on jdk8 implementation of MethodHandle#updateForm(LambdaForm), this is needed in order to ensure changes are made visible after writing to a final field with Unsafe
        PUnsafe.fullFence();

        //fire event
        MinecraftForge.EVENT_BUS.post(event);

        //return new color from event
        int newColor = event.getNewColor();
        recycler.release(event);
        return newColor;
    }
}
