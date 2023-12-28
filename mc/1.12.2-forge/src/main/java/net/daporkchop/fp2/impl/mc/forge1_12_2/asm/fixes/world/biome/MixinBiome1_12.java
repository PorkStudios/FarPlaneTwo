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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.fixes.world.biome;

import com.google.common.collect.ImmutableMap;
import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Some optional optimizations for biome color calculation.
 *
 * @author DaPorkchop_
 */
@Mixin(Biome.class)
public abstract class MixinBiome1_12 {
    @Unique
    private static final long EVENT_ISCANCELED_OFFSET = PUnsafe.pork_getOffset(Event.class, "isCanceled");
    @Unique
    private static final long EVENT_RESULT_OFFSET = PUnsafe.pork_getOffset(Event.class, "result");
    @Unique
    private static final long EVENT_PHASE_OFFSET = PUnsafe.pork_getOffset(Event.class, "phase");
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

    static {
        //sanity checks to make sure the event classes don't have any additional fields which we aren't aware of - if they do, our hackery could cause incorrect results.

        for (ImmutableTriple<Class<?>, Class<?>, Map<String, Class<?>>> entry : Arrays.<ImmutableTriple<Class<?>, Class<?>, Map<String, Class<?>>>>asList(
                ImmutableTriple.of(BiomeEvent.GetWaterColor.class, BiomeEvent.BiomeColor.class, Collections.emptyMap()),
                ImmutableTriple.of(BiomeEvent.GetGrassColor.class, BiomeEvent.BiomeColor.class, Collections.emptyMap()),
                ImmutableTriple.of(BiomeEvent.GetFoliageColor.class, BiomeEvent.BiomeColor.class, Collections.emptyMap()),
                ImmutableTriple.of(BiomeEvent.BiomeColor.class, BiomeEvent.class, ImmutableMap.of(
                        "originalColor", int.class,
                        "newColor", int.class)),
                ImmutableTriple.of(BiomeEvent.class, Event.class, ImmutableMap.of(
                        "biome", Biome.class)),
                ImmutableTriple.of(Event.class, Object.class, ImmutableMap.of(
                        "isCanceled", boolean.class,
                        "result", Event.Result.class,
                        "phase", EventPriority.class)))) {
            checkState(entry.getLeft().getSuperclass() == entry.getMiddle(),
                    "expected %s's superclass to be %s, but found %s", entry.getLeft(), entry.getMiddle(), entry.getLeft().getSuperclass());

            Map<String, Class<?>> fields = Stream.of(entry.getLeft().getDeclaredFields())
                            .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                            .collect(Collectors.toMap(Field::getName, Field::getType));
            checkState(entry.getRight().equals(fields),
                    "expected %s to contain fields %s, but found %s", entry.getLeft(), entry.getRight(), fields);
        }
    }

    @Unique
    private static void fp2_resetEventInstance(BiomeEvent.BiomeColor event, Object biome, int original) {
        //configure event
        PUnsafe.putBoolean(event, EVENT_ISCANCELED_OFFSET, false); //reset event#isCanceled to its default value
        PUnsafe.putObject(event, EVENT_RESULT_OFFSET, Event.Result.DEFAULT); //reset event#result to its default value
        PUnsafe.putObject(event, EVENT_PHASE_OFFSET, null); //reset event#phase to its default value
        PUnsafe.putObject(event, BIOMEEVENT_BIOME_OFFSET, biome); //set event#biome (final field)
        PUnsafe.putInt(event, BIOMEEVENT$BIOMECOLOR_ORIGINALCOLOR_OFFSET, original); //set event#originalColor (final field)
        event.setNewColor(original); //set event#newColor

        //based on jdk8 implementation of MethodHandle#updateForm(LambdaForm), this is needed in order to ensure changes are made visible after writing to a final field with Unsafe
        PUnsafe.fullFence();
    }

    /**
     * This prevents allocation of a new event instance.
     *
     * @author DaPorkchop_
     * @reason optimization
     */
    @Overwrite(remap = false) //don't remap: this is a forge method
    public int getWaterColorMultiplier() {
        Recycler<BiomeEvent.GetWaterColor> recycler = GETWATERCOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetWaterColor event = recycler.allocate();

        //configure event
        fp2_resetEventInstance(event, this, this.waterColor);

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
    @Overwrite(remap = false) //don't remap: this is a forge method
    public int getModdedBiomeGrassColor(int original) {
        Recycler<BiomeEvent.GetGrassColor> recycler = GETGRASSCOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetGrassColor event = recycler.allocate();

        //configure event
        fp2_resetEventInstance(event, this, original);

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
    @Overwrite(remap = false) //don't remap: this is a forge method
    public int getModdedBiomeFoliageColor(int original) {
        Recycler<BiomeEvent.GetFoliageColor> recycler = GETFOLIAGECOLOR_EVENT_RECYCLER.get();
        BiomeEvent.GetFoliageColor event = recycler.allocate();

        //configure event
        fp2_resetEventInstance(event, this, original);

        //fire event
        MinecraftForge.EVENT_BUS.post(event);

        //return new color from event
        int newColor = event.getNewColor();
        recycler.release(event);
        return newColor;
    }
}
