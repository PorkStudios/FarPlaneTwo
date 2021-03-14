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
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Manages delegation of events to registered {@link IWorldListener}s.
 * <p>
 * This could also be implemented by having the listeners register themselves to {@link MinecraftForge#EVENT_BUS} to listen for the relevant events, but
 * because the events are fired to every listener for every world, the runtime per event would be {@code O(<global_number_of_listeners>)} instead of
 * {@code O(<world_number_of_listeners>)}.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class WorldChangeListenerManager {
    private final Map<World, Set<ListenerWrapper>> ARENAS = new WeakHashMap<>();

    private final Function<World, Set<ListenerWrapper>> COMPUTE_ARENA_FUNCTION = world -> new CopyOnWriteArraySet<>();

    /**
     * Adds a new listener for events in the given world.
     *
     * @param world    the {@link World} to listen for events in
     * @param listener the {@link IWorldListener} to add
     */
    public void add(@NonNull World world, @NonNull IWorldListener listener) {
        Set<ListenerWrapper> arena = ARENAS.computeIfAbsent(world, COMPUTE_ARENA_FUNCTION);
        arena.add(new ListenerWrapper(listener, arena));
    }

    /**
     * Removes a listener that was previously being notified for events in the given world.
     *
     * @param world    the {@link World} to remove the listener from
     * @param listener the {@link IWorldListener} to remove
     */
    public void remove(@NonNull World world, @NonNull IWorldListener listener) {
        Set<ListenerWrapper> arena = ARENAS.get(world);
        if (arena != null) { //arena exists
            arena.removeIf(wrapper -> wrapper == listener);
        }
    }

    /**
     * @see IWorldListener#onColumnSaved(int, int)
     */
    public void fireColumnSave(@NonNull Chunk column) {
        Set<ListenerWrapper> arena = ARENAS.get(column.getWorld());
        if (arena != null) { //arena exists
            int x = column.x;
            int z = column.z;
            arena.forEach(wrapper -> {
                IWorldListener listener = wrapper.get();
                if (listener != null) { //listener wasn't garbage collected
                    listener.onColumnSaved(x, z);
                }
            });
        }
    }

    /**
     * @see IWorldListener#onCubeSaved(int, int, int)
     */
    public void fireCubeSave(@NonNull ICube cube) {
        Set<ListenerWrapper> arena = ARENAS.get(cube.getWorld());
        if (arena != null) { //arena exists
            int x = cube.getX();
            int y = cube.getY();
            int z = cube.getZ();
            arena.forEach(wrapper -> {
                IWorldListener listener = wrapper.get();
                if (listener != null) { //listener wasn't garbage collected
                    listener.onCubeSaved(x, y, z);
                }
            });
        }
    }

    /**
     * @see IWorldListener#onTickEnd()
     */
    public void fireTickEnd(@NonNull World world) {
        Set<ListenerWrapper> arena = ARENAS.get(world);
        if (arena != null) { //arena exists
            arena.forEach(wrapper -> {
                IWorldListener listener = wrapper.get();
                if (listener != null) { //listener wasn't garbage collected
                    listener.onTickEnd();
                }
            });
        }
    }

    /**
     * Weak-referencing wrapper around an {@link IWorldListener}.
     *
     * @author DaPorkchop_
     */
    private static class ListenerWrapper extends WeakEqualityForwardingReference<IWorldListener> implements Runnable, Predicate<ListenerWrapper> {
        private final Set<ListenerWrapper> arena;

        public ListenerWrapper(@NonNull IWorldListener referent, @NonNull Set<ListenerWrapper> arena) {
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
