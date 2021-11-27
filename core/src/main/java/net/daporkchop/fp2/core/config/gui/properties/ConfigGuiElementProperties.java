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

package net.daporkchop.fp2.core.config.gui.properties;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.element.properties.AbstractGuiElementProperties;
import net.daporkchop.fp2.core.config.Config;
import net.daporkchop.fp2.core.config.ConfigHelper;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ConfigGuiElementProperties<V> extends AbstractGuiElementProperties {
    protected final ConfigGuiObjectAccess<V> access;
    protected final String localeKey;

    protected final Optional<Number> min;
    protected final Optional<Number> max;

    protected final Optional<Number> step;

    public ConfigGuiElementProperties(@NonNull String localeKeyBase, @NonNull ConfigGuiObjectAccess<V> access) {
        this.access = access;
        this.localeKey = localeKeyBase + '.' + access.name();

        Config.Range range = this.access.getAnnotation(Config.Range.class);
        Config.GuiRange guiRange = this.access.getAnnotation(Config.GuiRange.class);
        if (range != null || guiRange != null) {
            this.min = Optional.ofNullable(ConfigHelper.evaluate(guiRange != null ? guiRange.min() : range.min()));
            this.max = Optional.ofNullable(ConfigHelper.evaluate(guiRange != null ? guiRange.max() : range.max()));
        } else {
            this.min = this.max = Optional.empty();
        }

        this.step = guiRange != null
                ? Optional.ofNullable(ConfigHelper.evaluate(guiRange.snapTo()))
                : Optional.empty();
    }

    @Override
    public String text() {
        String formatKey = this.localeKey + ".propertyFormat";
        return fp2().i18n().format(fp2().i18n().hasKey(formatKey) ? formatKey : MODID + ".config.property.format", this.name(), this.localizeValue(this.access.getCurrent()));
    }

    protected String localizeValue(Object val) {
        if (val instanceof Boolean) {
            return fp2().i18n().format(MODID + ".config.boolean." + val);
        } else if (val instanceof Enum) {
            Enum e = (Enum) val;
            return fp2().i18n().format(e.getDeclaringClass().getTypeName() + '#' + e.name());
        } else {
            String formatKey = this.localeKey() + ".valueFormat";
            return fp2().i18n().hasKey(formatKey) ? fp2().i18n().format(formatKey, val) : Objects.toString(val);
        }
    }

    @Override
    protected void computeTooltipText0(@NonNull StringJoiner joiner) {
        super.computeTooltipText0(joiner);

        { //indicator for this option's old/default values
            String text = Stream.of(
                    Optional.ofNullable(this.localizeValue(this.access.getOld())).map(val -> fp2().i18n().format(MODID + ".config.old.tooltip", val)).orElse(null),
                    Optional.ofNullable(this.localizeValue(this.access.getDefault())).map(val -> fp2().i18n().format(MODID + ".config.default.tooltip", val)).orElse(null),
                    this.access.getAnnotation(Config.GuiShowServerValue.class) != null ? Optional.ofNullable(this.access.getServer()).map(this::localizeValue).map(val -> fp2().i18n().format(MODID + ".config.server.tooltip", val)).orElse(null) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));

            if (!text.isEmpty()) {
                joiner.add(text);
            }
        }

        //indicator for this option's value range
        this.min.ifPresent(min -> {
            joiner.add(fp2().i18n().format(MODID + ".config.range.tooltip", this.localizeValue(min), this.localizeValue(this.max.get())));
        });

        { //indicator for whether this option requires a restart
            Config.RestartRequired restartRequired = this.access.getAnnotation(Config.RestartRequired.class);
            if (restartRequired != null) {
                joiner.add(fp2().i18n().format(MODID + ".config.restartRequired.tooltip." + restartRequired.value()));
            }
        }

        //indicator to open "more options" menu
        //joiner.add(fp2().i18n().format(MODID + ".config.moreOptions.tooltip"));
    }
}
