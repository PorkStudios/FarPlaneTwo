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

package net.daporkchop.fp2.config.gui;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Provides access to config property values.
 * <p>
 * Default and old values are read-only, current values are read-write.
 *
 * @author DaPorkchop_
 */
public class GuiObjectAccess<V> {
    protected static final GuiObjectAccess<?> NULL = new GuiObjectAccess<>(null, null, null);

    public static <V> GuiObjectAccess<V> forStatic() {
        return uncheckedCast(NULL);
    }

    protected final V defaultInstance;
    protected final V oldInstance;
    protected final V newInstance;

    @Getter
    protected final Class<V> clazz;

    public GuiObjectAccess(V defaultInstance, V oldInstance, V newInstance) {
        checkArg((defaultInstance == null) == (oldInstance == null) && (oldInstance == null) == (newInstance == null), "all parameters must either be null or non-null");
        checkArg(defaultInstance == null || (defaultInstance.getClass() == oldInstance.getClass() && oldInstance.getClass() == newInstance.getClass()), "all parameters must of the same class type!");

        this.defaultInstance = defaultInstance;
        this.oldInstance = oldInstance;
        this.newInstance = newInstance;
        this.clazz = defaultInstance != null ? uncheckedCast(defaultInstance.getClass()) : null;
    }

    @SneakyThrows(IllegalAccessException.class)
    public <R> R getDefault(@NonNull Field field) {
        if ((field.getModifiers() & Modifier.STATIC) == 0) {
            checkArg(this.defaultInstance != null, "defaultInstance must be non-null for %s", field);
        } else {
            checkArg(this.defaultInstance == null, "defaultInstance must be null for %s", field);
        }

        return uncheckedCast(field.get(this.defaultInstance));
    }

    @SneakyThrows(IllegalAccessException.class)
    public <R> R getOld(@NonNull Field field) {
        if ((field.getModifiers() & Modifier.STATIC) == 0) {
            checkArg(this.oldInstance != null, "oldInstance must be non-null for %s", field);
        } else {
            checkArg(this.oldInstance == null, "oldInstance must be null for %s", field);
        }

        return uncheckedCast(field.get(this.oldInstance));
    }

    @SneakyThrows(IllegalAccessException.class)
    public <R> R get(@NonNull Field field) {
        if ((field.getModifiers() & Modifier.STATIC) == 0) {
            checkArg(this.newInstance != null, "newInstance must be non-null for %s", field);
        } else {
            checkArg(this.newInstance == null, "newInstance must be null for %s", field);
        }

        return uncheckedCast(field.get(this.newInstance));
    }

    @SneakyThrows(IllegalAccessException.class)
    public <P> void set(@NonNull Field field, P value) {
        if ((field.getModifiers() & Modifier.STATIC) == 0) {
            checkArg(this.newInstance != null, "newInstance must be non-null for %s", field);
        } else {
            checkArg(this.newInstance == null, "newInstance must be null for %s", field);
        }

        field.set(this.newInstance, value);
    }

    public <T> GuiObjectAccess<T> child(@NonNull Field field) {
        return new GuiObjectAccess<>(this.getDefault(field), this.getOld(field), this.get(field));
    }
}
