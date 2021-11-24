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

package net.daporkchop.fp2.impl.mc.forge1_12_2;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.world.FGameRegistry;
import net.daporkchop.fp2.util.event.IdMappingsChangedEvent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.util.stream.IntStream;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public class GameRegistry1_12_2 implements FGameRegistry {
    private static GameRegistry1_12_2 INSTANCE = new GameRegistry1_12_2();

    static {
        fp2().eventBus().registerStatic(GameRegistry1_12_2.class);
    }

    @FEventHandler
    private static void recreateWhenChanged(IdMappingsChangedEvent event) {
        INSTANCE = new GameRegistry1_12_2();
    }

    private final Reference2IntMap<Biome> biomesToIds;
    private final Biome[] idsToBiomes;

    private final Reference2IntMap<IBlockState> statesToIds;
    private final IBlockState[] idsToStates;

    private GameRegistry1_12_2() {
        this.biomesToIds = new Reference2IntOpenHashMap<>();
        Biome.REGISTRY.forEach(biome -> this.biomesToIds.put(biome, Biome.getIdForBiome(biome)));

        // ids -> biomes
        this.idsToBiomes = new Biome[this.biomesToIds.values().stream().mapToInt(Integer::intValue).max().getAsInt() + 1];
        IntStream.range(0, this.idsToBiomes.length).forEach(id -> this.idsToBiomes[id] = Biome.getBiome(id));

        // block states -> ids
        this.statesToIds = new Reference2IntOpenHashMap<>();
        Block.REGISTRY.forEach(block -> block.getBlockState().getValidStates().forEach(state -> this.statesToIds.put(state, Block.getStateId(state))));

        // ids -> block states
        this.idsToStates = new IBlockState[this.statesToIds.values().stream().mapToInt(Integer::intValue).max().getAsInt() + 1];
        IntStream.range(0, this.idsToStates.length).forEach(id -> {
            try {
                this.idsToStates[id] = Block.getStateById(id);
            } catch (Exception ignored) {
                //an exception means the id isn't valid
                //this is probably the worst possible way of handling this, but i don't want to hard-code the mechanics of Block.getStateById(int) in case a mod changes it
            }
        });
    }

    @Override
    public int biome2id(@NonNull Object biome) throws UnsupportedOperationException, ClassCastException {
        return this.biomesToIds.getInt((Biome) biome);
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
}
