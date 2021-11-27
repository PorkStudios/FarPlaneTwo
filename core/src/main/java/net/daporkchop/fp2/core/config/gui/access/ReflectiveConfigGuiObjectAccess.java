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

package net.daporkchop.fp2.core.config.gui.access;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
final class ReflectiveConfigGuiObjectAccess<T, V> implements ConfigGuiObjectAccess<V> {
    protected final T defaultInstance;
    protected final T serverInstance;
    protected final T oldInstance;
    protected final T currentInstance;

    @Getter
    protected final Field field;

    public ReflectiveConfigGuiObjectAccess(@NonNull Field field) {
        checkArg((field.getModifiers() & Modifier.STATIC) != 0, "field must be static: %s", field);

        this.defaultInstance = null;
        this.serverInstance = null;
        this.oldInstance = null;
        this.currentInstance = null;
        this.field = field;
    }

    public ReflectiveConfigGuiObjectAccess(@NonNull T defaultInstance, T serverInstance, @NonNull T oldInstance, @NonNull T currentInstance, @NonNull Field field) {
        checkArg((field.getModifiers() & Modifier.STATIC) == 0, "field must be non-static: %s", field);

        Class<?> clazz = defaultInstance.getClass();
        checkArg(clazz == oldInstance.getClass() && clazz == currentInstance.getClass() && (serverInstance == null || clazz == serverInstance.getClass()),
                "all parameters must of the same class! (defaultInstance=%s, serverInstance=%s, oldInstance=%s, currentInstance=%s)",
                className(defaultInstance), className(serverInstance), className(oldInstance), className(currentInstance));

        this.defaultInstance = defaultInstance;
        this.serverInstance = serverInstance;
        this.oldInstance = oldInstance;
        this.currentInstance = currentInstance;
        this.field = field;
    }

    @Override
    public String name() {
        return this.field.getName();
    }

    @Override
    public <A extends Annotation> A getAnnotation(@NonNull Class<A> clazz) {
        return this.field.getAnnotation(clazz);
    }

    @Override
    public Class<V> type() {
        return uncheckedCast(this.field.getType());
    }

    @Override
    @SneakyThrows(IllegalAccessException.class)
    public V getDefault() {
        return uncheckedCast(this.field.get(this.defaultInstance));
    }

    @Override
    @SneakyThrows(IllegalAccessException.class)
    public V getServer() {
        return this.serverInstance != null //allowing static access to a server field doesn't make any sense...
                ? uncheckedCast(this.field.get(this.serverInstance))
                : null;
    }

    @Override
    @SneakyThrows(IllegalAccessException.class)
    public V getOld() {
        return uncheckedCast(this.field.get(this.oldInstance));
    }

    @Override
    @SneakyThrows(IllegalAccessException.class)
    public V getCurrent() {
        return uncheckedCast(this.field.get(this.currentInstance));
    }

    @Override
    @SneakyThrows(IllegalAccessException.class)
    public void setCurrent(@NonNull V value) {
        this.field.set(this.currentInstance, value);
    }
}
