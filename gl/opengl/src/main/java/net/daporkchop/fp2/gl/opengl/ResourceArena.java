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

package net.daporkchop.fp2.gl.opengl;

import lombok.NonNull;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Keeps track of allocated GL resources in order to implement automatic cleanup.
 *
 * @author DaPorkchop_
 */
public class ResourceArena extends AbstractRefCounted {
    protected final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    protected final Map<Object, CleanupReference> referenceMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    protected void doRelease() {
        //run all cleanup references which are still present in the map
        this.referenceMap.forEach((key, reference) -> reference.run());
    }

    /**
     * Deletes any resources whose reference objects were garbage-collected since the last call.
     */
    public void clean() {
        for (CleanupReference reference; (reference = (CleanupReference) this.queue.poll()) != null; ) {
            reference.run();
        }
    }

    /**
     * Registers the given resource.
     *
     * @param referent       the container object which references the resource
     * @param id             the resource's OpenGL id
     * @param deleteFunction a function which will delete the resource
     */
    public void register(@NonNull Object referent, int id, @NonNull IntConsumer deleteFunction) {
        checkState(this.referenceMap.putIfAbsent(referent, new CleanupReference(referent, this.queue, deleteFunction, id)) == null, "already registered: %s", referent);
    }

    /**
     * Unregisters the given resource without deleting it.
     *
     * @param referent the container object which references the resource
     */
    public void unregister(@NonNull Object referent) {
        CleanupReference reference = this.referenceMap.remove(referent);
        checkState(reference != null, "not registered: %s", referent);
    }

    /**
     * Unregisters and deletes the given resource.
     *
     * @param referent the container object which references the resource
     */
    public void delete(@NonNull Object referent) {
        CleanupReference reference = this.referenceMap.remove(referent);
        checkState(reference != null, "not registered: %s", referent);
        reference.run();
    }

    /**
     * A reference to an OpenGL resource container which is able to delete the underlying resource.
     *
     * @author DaPorkchop_
     */
    protected static class CleanupReference extends PhantomReference<Object> implements Runnable {
        protected final IntConsumer deleteFunction;
        protected final int id;

        public CleanupReference(@NonNull Object referent, @NonNull ReferenceQueue<Object> q, @NonNull IntConsumer deleteFunction, int id) {
            super(referent, q);

            this.deleteFunction = deleteFunction;
            this.id = id;
        }

        @Override
        public void run() {
            this.deleteFunction.accept(this.id);
        }
    }
}
