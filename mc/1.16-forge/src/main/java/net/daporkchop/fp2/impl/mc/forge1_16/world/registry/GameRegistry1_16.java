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

package net.daporkchop.fp2.impl.mc.forge1_16.world.registry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class GameRegistry1_16 implements FGameRegistry {
    private final Biome[] idsToBiomes;
    private final Reference2IntMap<Biome> biomesToIds;

    private final BlockState[] idsToStates;
    private final Reference2IntMap<BlockState> statesToIds;

    @Getter
    private final ExtendedBiomeRegistryData1_16 extendedBiomeRegistryData;
    @Getter
    private final ExtendedStateRegistryData1_16 extendedStateRegistryData;

    public GameRegistry1_16(@NonNull World world) {
        //ids -> biomes
        this.idsToBiomes = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).stream().toArray(Biome[]::new);

        //biomes -> ids
        this.biomesToIds = new Reference2IntOpenHashMap<>(this.idsToBiomes.length);
        this.biomesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToBiomes.length).forEach(id -> checkState(this.biomesToIds.putIfAbsent(this.idsToBiomes[id], id) < 0, "duplicate biome: %s", this.idsToBiomes[id]));

        //extended registry information
        this.extendedBiomeRegistryData = new ExtendedBiomeRegistryData1_16(this);

        //ids -> states
        //  ForgeRegistry#spliterator() wraps iterator(), which iterates in ID order using BitSet#nextSetBit. if i were using something like values().stream(), it would be a stream
        //  over a guava HashBiMap, which would obviously be bad because its order is unpredictable
        this.idsToStates = StreamSupport.stream(ForgeRegistries.BLOCKS.spliterator(), false)
                .flatMap(block -> block.getStateDefinition().getPossibleStates().stream())
                .toArray(BlockState[]::new);

        //states -> ids
        this.statesToIds = new Reference2IntOpenHashMap<>(this.idsToStates.length);
        this.statesToIds.defaultReturnValue(-1);
        IntStream.range(0, this.idsToStates.length).forEach(id -> checkState(this.statesToIds.putIfAbsent(this.idsToStates[id], id) < 0, "duplicate state: %s", this.idsToStates[id]));

        //extended registry information
        this.extendedStateRegistryData = new ExtendedStateRegistryData1_16(this);
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
            for (BlockState state : this.idsToStates) { //states
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
    public Biome id2biome(int biome) throws UnsupportedOperationException {
        return this.idsToBiomes[biome];
    }

    @Override
    public int statesCount() {
        return this.idsToStates.length;
    }

    @Override
    public int state2id(@NonNull Object state) throws UnsupportedOperationException, ClassCastException {
        return this.statesToIds.getInt((BlockState) state);
    }

    @Override
    public int state2id(@NonNull Object block, int meta) throws UnsupportedOperationException, ClassCastException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockState id2state(int state) throws UnsupportedOperationException {
        return this.idsToStates[state];
    }
}
