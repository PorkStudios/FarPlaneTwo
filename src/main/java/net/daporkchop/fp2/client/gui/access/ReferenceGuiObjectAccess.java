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

package net.daporkchop.fp2.client.gui.access;

import lombok.NonNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.Annotation;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
final class ReferenceGuiObjectAccess<V> implements GuiObjectAccess<V> {
    protected final V defaultInstance;
    protected final V serverInstance;
    protected final V oldInstance;
    protected V currentInstance;

    protected final Class<V> clazz;

    public ReferenceGuiObjectAccess(@NonNull V defaultInstance, V serverInstance, @NonNull V oldInstance, @NonNull V currentInstance) {
        Class<V> clazz = uncheckedCast(defaultInstance.getClass());
        checkArg(clazz == oldInstance.getClass() && clazz == currentInstance.getClass() && (serverInstance == null || clazz == serverInstance.getClass()),
                "all parameters must of the same class! (defaultInstance=%s, serverInstance=%s, oldInstance=%s, currentInstance=%s)",
                className(defaultInstance), className(serverInstance), className(oldInstance), className(currentInstance));

        this.defaultInstance = defaultInstance;
        this.serverInstance = serverInstance;
        this.oldInstance = oldInstance;
        this.currentInstance = currentInstance;
        this.clazz = clazz;
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends Annotation> A getAnnotation(@NonNull Class<A> clazz) {
        return this.clazz.getAnnotation(clazz);
    }

    @Override
    public Class<V> type() {
        return this.clazz;
    }

    @Override
    public V getDefault() {
        return this.defaultInstance;
    }

    @Override
    public V getServer() {
        return this.serverInstance;
    }

    @Override
    public V getOld() {
        return this.oldInstance;
    }

    @Override
    public V getCurrent() {
        return this.currentInstance;
    }

    @Override
    public void setCurrent(@NonNull V value) {
        checkArg(this.clazz == value.getClass(), "invalid class type for new value (expected=%s, given=%s)", className(value), this.clazz.getTypeName());

        this.currentInstance = value;
    }
}
