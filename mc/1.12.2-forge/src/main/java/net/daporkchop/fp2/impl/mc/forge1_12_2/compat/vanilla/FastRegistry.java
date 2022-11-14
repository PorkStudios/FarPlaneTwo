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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Faster, but otherwise functionally equivalent alternatives to methods like {@link Block#getStateById(int)} and {@link Biome#getBiome(int)}.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@Deprecated
public class FastRegistry {
    private Reference2IntMap<Biome> BIOMES_TO_IDS;
    private Biome[] IDS_TO_BIOMES;

    private Reference2IntMap<IBlockState> BLOCK_STATES_TO_IDS;
    private IBlockState[] IDS_TO_BLOCK_STATES;

    private final Set<ReloadListener> RELOAD_LISTENERS = new CopyOnWriteArraySet<>();

    static {
        reload();
    }

    /**
     * Reloads all cached registries.
     */
    public synchronized void reload() {
        // biomes -> ids
        Reference2IntMap<Biome> biomesToIds = new Reference2IntOpenHashMap<>();
        Biome.REGISTRY.forEach(biome -> biomesToIds.put(biome, Biome.getIdForBiome(biome)));
        BIOMES_TO_IDS = biomesToIds;

        // ids -> biomes
        Biome[] idsToBiomes = new Biome[biomesToIds.values().stream().mapToInt(Integer::intValue).max().getAsInt() + 1];
        IntStream.range(0, idsToBiomes.length).forEach(id -> idsToBiomes[id] = Biome.getBiome(id));
        IDS_TO_BIOMES = idsToBiomes;

        // block states -> ids
        Reference2IntMap<IBlockState> blockstatesToIds = new Reference2IntOpenHashMap<>();
        Block.REGISTRY.forEach(block -> block.getBlockState().getValidStates().forEach(state -> blockstatesToIds.put(state, Block.getStateId(state))));
        BLOCK_STATES_TO_IDS = blockstatesToIds;

        // ids -> block states
        IBlockState[] idsToBlockStates = new IBlockState[blockstatesToIds.values().stream().mapToInt(Integer::intValue).max().getAsInt() + 1];
        IntStream.range(0, idsToBlockStates.length).forEach(id -> {
            try {
                idsToBlockStates[id] = Block.getStateById(id);
            } catch (Exception ignored) {
                //an exception means the id isn't valid
                //this is probably the worst possible way of handling this, but i don't want to hard-code the mechanics of Block.getStateById(int) in case a mod changes it
            }
        });
        IDS_TO_BLOCK_STATES = idsToBlockStates;

        //notify reload listeners
        RELOAD_LISTENERS.forEach(ReloadListener::onFastRegistryReload);
    }

    /**
     * Adds a new reload listener.
     *
     * @param listener the reload listener to add
     */
    public static void addReloadListener(@NonNull ReloadListener listener) {
        checkState(RELOAD_LISTENERS.add(listener), "reload listener %s already present", listener);
        listener.onFastRegistryReload();
    }

    /**
     * Removes a previously added reload listener.
     *
     * @param listener the reload listener to remove
     */
    public static void removeReloadListener(@NonNull ReloadListener listener) {
        checkState(RELOAD_LISTENERS.remove(listener), "reload listener %s not present", listener);
    }

    /**
     * @see Biome#getIdForBiome(Biome)
     */
    public int getId(@NonNull Biome biome) {
        return BIOMES_TO_IDS.getInt(biome);
    }

    /**
     * @see Biome#getBiome(int)
     */
    public Biome getBiome(int id) {
        return id >= 0 && id < IDS_TO_BIOMES.length ? IDS_TO_BIOMES[id] : null;
    }

    /**
     * @see Biome#getBiome(int, Biome)
     */
    public Biome getBiome(int id, Biome fallback) {
        return PorkUtil.fallbackIfNull(getBiome(id), fallback);
    }

    /**
     * @see Block#getStateId(IBlockState)
     */
    public int getId(@NonNull IBlockState state) {
        return BLOCK_STATES_TO_IDS.getInt(state);
    }

    /**
     * @see Block#getStateById(int)
     */
    public IBlockState getBlockState(int id) {
        return id >= 0 && id < IDS_TO_BLOCK_STATES.length ? IDS_TO_BLOCK_STATES[id] : null;
    }

    /**
     * A function which is run whenever {@link FastRegistry} is reloaded.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface ReloadListener {
        void onFastRegistryReload();
    }
}
