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

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.Setting;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.StringJoiner;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractReflectiveConfigGuiElement<V> extends AbstractConfigGuiElement {
    protected final Object instance;
    @NonNull
    protected final Field field;

    public AbstractReflectiveConfigGuiElement(@NonNull IGuiContext context, Object instance, @NonNull Field field) {
        super(context);

        this.instance = instance;
        this.field = field;
    }

    @Override
    protected String langKey() {
        return this.context.localeKeyBase() + this.field.getName();
    }

    @Override
    protected void computeTooltipText0(@NonNull StringJoiner joiner) {
        super.computeTooltipText0(joiner);

        { //indicator for this option's value range
            Setting.Range range = this.field.getAnnotation(Setting.Range.class);
            if (range != null) {
                joiner.add(I18n.format(MODID + ".config.range.tooltip", ConfigHelper.evaluate(range.min()), ConfigHelper.evaluate(range.max())));
            }
        }

        { //indicator for whether this option requires a restart
            Setting.RestartRequired restartRequired = this.field.getAnnotation(Setting.RestartRequired.class);
            if (restartRequired != null) {
                joiner.add(I18n.format(MODID + ".config.restartRequired.tooltip." + restartRequired.value()));
            }
        }
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
}
