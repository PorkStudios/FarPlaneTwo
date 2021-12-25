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

package net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.asyncblockaccess;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.reference.WeakSelfRemovingReference;
import net.daporkchop.fp2.core.util.threading.futurecache.AsyncCacheBase;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.ref.Reference;
import java.util.Map;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Extension of {@link AsyncCacheBase} which caches NBT data for values.
 *
 * @author DaPorkchop_
 */
public abstract class AsyncCacheNBTBase<K, P, V> extends AsyncCacheBase<K, V> {
    protected static final Object DEFAULT_PARAM = new Object();

    protected final Map<K, Reference<NBTTagCompound>> nbt = new ObjObjConcurrentHashMap<>();

    /**
     * Notifies the cache that the NBT data for the entry with the given key has changed.
     *
     * @param key the key
     * @param nbt the new NBT data
     */
    public void notifyUpdate(@NonNull K key, @NonNull NBTTagCompound nbt) {
        this.nbt.put(key, WeakSelfRemovingReference.create(nbt, key, this.nbt));
        super.invalidate(key);
    }

    @Override
    @Deprecated
    public void invalidate(@NonNull K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected V load(@NonNull K key, boolean allowGeneration) {
        P param = this.getParamFor(key, allowGeneration);
        if (param == null) { //parameter couldn't be loaded
            checkState(!allowGeneration, "allowGeneration was true, but the parameter for %s wasn't generated!", key);
            return null;
        }

        while (true) {
            Reference<NBTTagCompound> reference = this.nbt.get(key);
            if (reference != null) { //get NBT from cache
                NBTTagCompound nbt = reference.get();
                if (nbt != null) { //attempt to parse value from cached NBT
                    V value = this.parseNBT(key, param, nbt);
                    if (value != null) {
                        return this.bakeValue(key, param, value);
                    }
                }
            }

            V value = this.loadFromDisk(key, param);
            if (value != null || !allowGeneration) {
                return value != null ? this.bakeValue(key, param, value) : null;
            }

            this.triggerGeneration(key, param);
        }
    }

    protected P getParamFor(@NonNull K key, boolean allowGeneration) {
        return uncheckedCast(DEFAULT_PARAM);
    }

    protected abstract V parseNBT(@NonNull K key, @NonNull P param, @NonNull NBTTagCompound nbt);

    protected abstract V loadFromDisk(@NonNull K key, @NonNull P param);

    protected abstract void triggerGeneration(@NonNull K key, @NonNull P param);

    protected V bakeValue(@NonNull K key, @NonNull P param, @NonNull V value) {
        return value;
    }
}
