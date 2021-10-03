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

package net.daporkchop.fp2.config.gui.access;

import lombok.NonNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Provides access to config property values.
 * <p>
 * Default, server and old values are read-only, current values are read-write.
 *
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public interface GuiObjectAccess<V> {
    static <V> GuiObjectAccess<V> forStaticField(@NonNull Field field) {
        return new ReflectiveGuiObjectAccess<>(field);
    }

    static <T, V> GuiObjectAccess<V> forMemberField(@NonNull T defaultInstance, T serverInstance, @NonNull T oldInstance, @NonNull T currentInstance, @NonNull Field field) {
        return new ReflectiveGuiObjectAccess<>(defaultInstance, serverInstance, oldInstance, currentInstance, field);
    }

    static <V> GuiObjectAccess<V> forValues(@NonNull V defaultInstance, V serverInstance, @NonNull V oldInstance, @NonNull V currentInstance) {
        return new ReferenceGuiObjectAccess<>(defaultInstance, serverInstance, oldInstance, currentInstance);
    }

    String name();

    <A extends Annotation> A getAnnotation(@NonNull Class<A> clazz);

    Class<V> type();

    V getDefault();

    V getServer();

    V getOld();

    V getCurrent();

    void setCurrent(@NonNull V value);

    default <NEW_V> GuiObjectAccess<NEW_V> childAccess(@NonNull Field field) {
        return forMemberField(this.getDefault(), this.getServer(), this.getOld(), this.getCurrent(), field);
    }
}
