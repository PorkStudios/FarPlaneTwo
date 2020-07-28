/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.util.threading.persist;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class PersistentTaskRegistry {
    private static final List<Class<? extends IPersistentTask>> CLASSES = new ArrayList<>();
    private static final ObjIntMap<Class<? extends IPersistentTask>> IDS = new ObjIntOpenHashMap<>();
    private static final List<Function<ByteBuf, ? extends IPersistentTask>> DESERIALIZERS = new ArrayList<>();

    public synchronized <T extends IPersistentTask> void register(@NonNull Class<T> clazz, @NonNull Function<ByteBuf, T> deserializer) {
        checkState(!IDS.containsKey(clazz));
        IDS.put(clazz, CLASSES.size());
        CLASSES.add(clazz);
        DESERIALIZERS.add(deserializer);
    }

    public <T extends IPersistentTask> T read(@NonNull ByteBuf buf) {
        int id = buf.readUnsignedByte();
        return uncheckedCast(DESERIALIZERS.get(id).apply(buf));
    }

    public void write(@NonNull IPersistentTask task, @NonNull ByteBuf buf) {
        int id = IDS.getOrDefault(task.getClass(), -1);
        checkArg(id >= 0, "unknown task %s", task);
        buf.writeByte(id);
        task.write(buf);
    }
}
