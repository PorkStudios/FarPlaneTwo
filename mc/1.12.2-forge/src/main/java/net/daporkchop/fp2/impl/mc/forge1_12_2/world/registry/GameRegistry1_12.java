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

package net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public final class GameRegistry1_12 implements FGameRegistry {
    private static GameRegistry1_12 INSTANCE;

    public static GameRegistry1_12 get() {
        return INSTANCE;
    }

    public synchronized static void reload() {
        INSTANCE = new GameRegistry1_12();
    }

    private final Biome[] idsToBiomes;
    private final Reference2IntMap<Biome> biomesToIds;

    private final IBlockState[] idsToStates;
    private final Reference2IntMap<IBlockState> statesToIds;

    @Getter
    private final ExtendedBiomeRegistryData1_12 extendedBiomeRegistryData;
    @Getter
    private final ExtendedStateRegistryData1_12 extendedStateRegistryData;

    private GameRegistry1_12() {
        //ids -> biomes
        this.idsToBiomes = StreamSupport.stream(Biome.REGISTRY.spliterator(), false).toArray(Biome[]::new);

        //biomes -> ids
        this.biomesToIds = new Reference2IntOpenHashMap<>(this.idsToBiomes.length);
        this.biomesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToBiomes.length).forEach(id -> checkState(this.biomesToIds.put(this.idsToBiomes[id], id) < 0, "duplicate biome: %s", this.idsToBiomes[id]));

        //extended registry information
        this.extendedBiomeRegistryData = new ExtendedBiomeRegistryData1_12(this);

        //ids -> states
        this.idsToStates = StreamSupport.stream(Block.REGISTRY.spliterator(), false)
                .flatMap(block -> {
                    //Certain mods (i'm looking at you, IC² and Vintage-GT) have overridden BlockStateContainer#getValidStates() to not return every possible state, but instead
                    //  only a subset of the possible states. It's pretty cursed, and really shouldn't work at all, but it does actually occur in the wild. Thanks to that, what
                    //  should have been a single line of code is now this monstrosity.
                    Stream<IBlockState> allStates = Stream.of(block.getDefaultState());
                    for (IProperty<?> property : block.getBlockState().getProperties()) {
                        allStates = allStates.flatMap(state -> property.getAllowedValues().stream().map(value -> state.withProperty(property, uncheckedCast(value))));
                    }
                    return allStates;
                })
                .toArray(IBlockState[]::new);

        //states -> ids
        this.statesToIds = new Reference2IntOpenHashMap<>(this.idsToStates.length);
        this.statesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToStates.length).forEach(id -> checkState(this.statesToIds.put(this.idsToStates[id], id) < 0, "duplicate state: %s", this.idsToStates[id]));

        //extended registry information
        this.extendedStateRegistryData = new ExtendedStateRegistryData1_12(this);
    }

    @Override
    public byte[] registryToken() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            //serialize the registry
            buf.writeCharSequence(this.getClass().getTypeName(), StandardCharsets.UTF_8); //class name
            buf.writeByte(0);

            for (Biome biome : this.idsToBiomes) { //biomes
                buf.writeCharSequence(biome.getRegistryName().toString(), StandardCharsets.UTF_8);
                buf.writeByte(0);
            }
            for (IBlockState state : this.idsToStates) { //states
                buf.writeCharSequence(state.toString(), StandardCharsets.UTF_8);
                buf.writeByte(0);
            }

            //copy buffer contents to a byte[]
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    @Override
    public int biomesCount() {
        return this.idsToBiomes.length;
    }

    @Override
    public int biome2id(@NonNull Object biome) throws UnsupportedOperationException, ClassCastException {
        return this.biomesToIds.getInt((Biome) biome);
    }

    @Override
    public Biome id2biome(int biome) {
        return this.idsToBiomes[biome];
    }

    @Override
    public int statesCount() {
        return this.idsToStates.length;
    }

    @Override
    public int state2id(@NonNull Object state) throws UnsupportedOperationException, ClassCastException {
        return this.statesToIds.getInt((IBlockState) state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int state2id(@NonNull Object block, int meta) throws UnsupportedOperationException, ClassCastException {
        return this.statesToIds.getInt(((Block) block).getStateFromMeta(meta));
    }

    @Override
    public IBlockState id2state(int state) {
        return this.idsToStates[state];
    }
}
