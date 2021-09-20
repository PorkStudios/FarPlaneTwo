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

package net.daporkchop.fp2.config.gui.element;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractConfigGuiElement<V> implements IConfigGuiElement {
    @NonNull
    protected final IGuiContext context;
    protected final Object instance;
    @NonNull
    protected final Field field;

    @Getter
    protected ElementBounds bounds = ElementBounds.ZERO;

    @Getter(lazy = true)
    private final String localizedName = I18n.format(this.context.localeKeyBase() + this.field.getName());

    @Getter(lazy = true)
    private final Optional<String[]> tooltipText = I18n.hasKey(this.langKey() + ".tooltip")
            ? Optional.of(I18n.format(this.langKey() + ".tooltip").split("\n"))
            : Optional.empty();

    protected String langKey() {
        return this.context.localeKeyBase() + this.field.getName();
    }

    @Override
    public void bounds(@NonNull ElementBounds bounds) {
        this.bounds = bounds;

        this.pack();
    }

    @SneakyThrows(IllegalAccessException.class)
    protected V get() {
        this.field.setAccessible(true);
        return uncheckedCast(this.field.get(this.instance));
    }

    @SneakyThrows(IllegalAccessException.class)
    protected void set(@NonNull V value) {
        this.field.setAccessible(true);
        this.field.set(this.instance, value);
    }

    @Override
    public Optional<String[]> getTooltip(int mouseX, int mouseY) {
        return this.bounds.contains(mouseX, mouseY) ? this.tooltipText() : Optional.empty();
    }
}
