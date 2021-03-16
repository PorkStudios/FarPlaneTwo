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

package net.daporkchop.fp2.server.worldlistener;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.reference.ReferenceHandlerThread;
import net.daporkchop.fp2.util.reference.WeakEqualityForwardingReference;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Manages delegation of events to registered {@link IWorldChangeListener}s.
 * <p>
 * This could also be implemented by having the listeners register themselves to {@link MinecraftForge#EVENT_BUS} to listen for the relevant events, but
 * because the events are fired to every listener for every world, the runtime per event would be {@code O(<global_number_of_listeners>)} instead of
 * {@code O(<world_number_of_listeners>)}.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class WorldChangeListenerManager {
    private final Function<World, Set<ListenerWrapper>> COMPUTE_ARENA_FUNCTION = world -> new CopyOnWriteArraySet<>();

    private final Set<ListenerWrapper> GLOBAL_ARENA = COMPUTE_ARENA_FUNCTION.apply(null);
    private final Map<World, Set<ListenerWrapper>> ARENAS = new WeakHashMap<>();

    /**
     * Adds a new listener for events in all worlds.
     *
     * @param listener the {@link IWorldChangeListener} to add
     */
    public void addGlobal(@NonNull IWorldChangeListener listener) {
        GLOBAL_ARENA.add(new ListenerWrapper(listener, GLOBAL_ARENA));
    }

    /**
     * Removes a listener that was previously being notified for events in all worlds.
     *
     * @param listener the {@link IWorldChangeListener} to remove
     */
    public void removeGlobal(@NonNull IWorldChangeListener listener) {
        GLOBAL_ARENA.removeIf(wrapper -> wrapper.get() == listener);
    }

    /**
     * Adds a new listener for events in the given world.
     *
     * @param world    the {@link World} to listen for events in
     * @param listener the {@link IWorldChangeListener} to add
     */
    public void add(@NonNull World world, @NonNull IWorldChangeListener listener) {
        Set<ListenerWrapper> arena = ARENAS.computeIfAbsent(world, COMPUTE_ARENA_FUNCTION);
        arena.add(new ListenerWrapper(listener, arena));
    }

    /**
     * Removes a listener that was previously being notified for events in the given world.
     *
     * @param world    the {@link World} to remove the listener from
     * @param listener the {@link IWorldChangeListener} to remove
     */
    public void remove(@NonNull World world, @NonNull IWorldChangeListener listener) {
        Set<ListenerWrapper> arena = ARENAS.get(world);
        if (arena != null) { //arena exists
            arena.removeIf(wrapper -> wrapper.get() == listener);
        }
    }

    /**
     * @see IWorldChangeListener#onColumnSaved(World, int, int, NBTTagCompound)
     */
    public void fireColumnSave(@NonNull Chunk column, @NonNull NBTTagCompound nbt) {
        Consumer<ListenerWrapper> callback = wrapper -> {
            IWorldChangeListener listener = wrapper.get();
            if (listener != null) { //listener wasn't garbage collected
                listener.onColumnSaved(column.getWorld(), column.x, column.z, nbt);
            }
        };

        GLOBAL_ARENA.forEach(callback);
        Set<ListenerWrapper> arena = ARENAS.get(column.getWorld());
        if (arena != null) { //arena exists
            arena.forEach(callback);
        }
    }

    /**
     * @see IWorldChangeListener#onCubeSaved(World, int, int, int, NBTTagCompound)
     */
    public void fireCubeSave(@NonNull ICube cube, @NonNull NBTTagCompound nbt) {
        Consumer<ListenerWrapper> callback = wrapper -> {
            IWorldChangeListener listener = wrapper.get();
            if (listener != null) { //listener wasn't garbage collected
                listener.onCubeSaved(cube.getWorld(), cube.getX(), cube.getY(), cube.getZ(), nbt);
            }
        };

        GLOBAL_ARENA.forEach(callback);
        Set<ListenerWrapper> arena = ARENAS.get(cube.getWorld());
        if (arena != null) { //arena exists
            arena.forEach(callback);
        }
    }

    /**
     * @see IWorldChangeListener#onTickEnd()
     */
    @Deprecated
    public void fireTickEnd(@NonNull World world) {
        Consumer<ListenerWrapper> callback = wrapper -> {
            IWorldChangeListener listener = wrapper.get();
            if (listener != null) { //listener wasn't garbage collected
                listener.onTickEnd();
            }
        };

        GLOBAL_ARENA.forEach(callback);
        Set<ListenerWrapper> arena = ARENAS.get(world);
        if (arena != null) { //arena exists
            arena.forEach(callback);
        }
    }

    /**
     * Weak-referencing wrapper around an {@link IWorldChangeListener}.
     *
     * @author DaPorkchop_
     */
    private static class ListenerWrapper extends WeakEqualityForwardingReference<IWorldChangeListener> implements Runnable, Predicate<ListenerWrapper> {
        private final Set<ListenerWrapper> arena;

        public ListenerWrapper(@NonNull IWorldChangeListener referent, @NonNull Set<ListenerWrapper> arena) {
            super(referent, ReferenceHandlerThread.queue());

            this.arena = arena;
        }

        @Override
        public void run() { //called by the reference handler thread after the listener is garbage collected
            this.arena.removeIf(this);
        }

        /**
         * @deprecated internal API, do not touch!
         */
        @Override
        @Deprecated
        public boolean test(ListenerWrapper wrapper) { //used by #run() to remove this wrapper from this.arena by its identity
            return wrapper == this;
        }
    }
}
