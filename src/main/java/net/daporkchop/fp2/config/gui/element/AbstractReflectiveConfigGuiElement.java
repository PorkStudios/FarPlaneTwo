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
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.Setting;
import net.daporkchop.fp2.config.gui.GuiObjectAccess;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.StringJoiner;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractReflectiveConfigGuiElement<T, V> extends AbstractConfigGuiElement {
    @NonNull
    protected final GuiObjectAccess<T> access;
    @NonNull
    protected final Field field;

    public AbstractReflectiveConfigGuiElement(@NonNull IGuiContext context, @NonNull GuiObjectAccess<T> access, @NonNull Field field) {
        super(context);

        this.access = access;
        this.field = field;
    }

    @Override
    protected String langKey() {
        return this.context.localeKeyBase() + this.field.getName();
    }

    protected String text() {
        return I18n.format(MODID + ".config.property.format", this.localizedName(), this.localizeValue(this.get()));
    }

    protected abstract String localizeValue(V value);

    @Override
    protected void computeTooltipText0(@NonNull StringJoiner joiner) {
        super.computeTooltipText0(joiner);

        { //indicator for this option's old/default values
            String oldValue = this.localizeValue(this.access.getOld(this.field));
            String defaultValue = this.localizeValue(this.access.getDefault(this.field));

            String key = oldValue == null
                    ? defaultValue == null ? null : "default"
                    : defaultValue == null ? "old" : "oldDefault";

            if (key != null) {
                joiner.add(I18n.format(MODID + ".config." + key + ".tooltip", oldValue, defaultValue));
            }
        }

        { //indicator for this option's value range
            Setting.Range range = this.field.getAnnotation(Setting.Range.class);
            Setting.GuiRange guiRange = this.field.getAnnotation(Setting.GuiRange.class);
            if (range != null || guiRange != null) {
                V min = uncheckedCast(ConfigHelper.evaluate(guiRange != null ? guiRange.min() : range.min()));
                V max = uncheckedCast(ConfigHelper.evaluate(guiRange != null ? guiRange.max() : range.max()));

                joiner.add(I18n.format(MODID + ".config.range.tooltip", this.localizeValue(min), this.localizeValue(max)));
            }
        }

        { //indicator for whether this option requires a restart
            Setting.RestartRequired restartRequired = this.field.getAnnotation(Setting.RestartRequired.class);
            if (restartRequired != null) {
                joiner.add(I18n.format(MODID + ".config.restartRequired.tooltip." + restartRequired.value()));
            }
        }
    }

    protected V get() {
        return this.access.get(this.field);
    }

    protected void set(@NonNull V value) {
        this.access.set(this.field, value);
    }
}
