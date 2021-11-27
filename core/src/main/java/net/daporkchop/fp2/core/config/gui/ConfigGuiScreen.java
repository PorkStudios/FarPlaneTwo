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

package net.daporkchop.fp2.core.config.gui;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.container.ScrollingContainer;
import net.daporkchop.fp2.core.client.gui.element.GuiTitle;
import net.daporkchop.fp2.core.client.gui.element.properties.SimpleGuiElementProperties;
import net.daporkchop.fp2.core.client.gui.screen.AbstractGuiScreen;
import net.daporkchop.fp2.core.config.Config;
import net.daporkchop.fp2.core.config.ConfigHelper;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;
import net.daporkchop.fp2.core.config.gui.properties.ConfigGuiElementProperties;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiScreen extends AbstractGuiScreen {
    protected final ConfigGuiObjectAccess<?> access;

    public ConfigGuiScreen(@NonNull GuiContext context, @NonNull String localeKey, @NonNull ConfigGuiObjectAccess<?> access) {
        super(context, localeKey, new ScrollingContainer(context, new ArrayList<>()));

        this.access = access;

        Config.GuiCategories categories = access.type().getAnnotation(Config.GuiCategories.class);
        if (categories == null) { //create default categories
            //dummy class to allow us to access the default value for the {@link Setting.GuiCategories} annotation without needing to implement it manually
            @Config.GuiCategories(@Config.CategoryMeta(name = "default"))
            class DummyClass {
            }

            categories = DummyClass.class.getAnnotation(Config.GuiCategories.class);
        }

        checkArg(categories.value().length != 0, "%s has no GUI categories!", access);
        Map<String, Config.CategoryMeta> categoriesByName = Stream.of(categories.value())
                .reduce(new LinkedHashMap<>(),
                        (map, meta) -> {
                            checkState(map.putIfAbsent(meta.name(), meta) == null, "duplicate category name: %s", meta.name());
                            return map;
                        },
                        (a, b) -> null);

        Map<Config.CategoryMeta, List<GuiElement>> elementsByCategory = ConfigHelper.getConfigPropertyFields(access.type())
                .collect(Collectors.groupingBy(
                        field -> {
                            String categoryName = Optional.ofNullable(field.getAnnotation(Config.GuiCategory.class)).map(Config.GuiCategory::value).orElse("default");
                            Config.CategoryMeta meta = categoriesByName.get(categoryName);
                            checkArg(meta != null, "no such category: %s", categoryName);
                            return meta;
                        },
                        Collectors.mapping(field -> {
                            ConfigGuiObjectAccess<?> childAccess = access.childAccess(field);
                            return ConfigGuiHelper.createConfigGuiElement(context, new ConfigGuiElementProperties<>(localeKey, childAccess), childAccess);
                        }, Collectors.toList())));

        ((ScrollingContainer) this.child).elements().addAll(categoriesByName.values().stream()
                .map(meta -> new AbstractMap.SimpleEntry<>(meta, elementsByCategory.get(meta)))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .flatMap(entry -> Stream.of(
                        elementsByCategory.size() == 1 || !entry.getKey().title() ? null : new GuiTitle(context, new SimpleGuiElementProperties(localeKey + '.' + entry.getKey().name() + ".category")),
                        ConfigGuiHelper.createConfigGuiContainer(entry.getKey(), context, access, entry.getValue())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @Override
    public void close() {
        fp2().log().info("config gui closed");
        fp2().log().info("def=%s", this.access.getDefault());
        fp2().log().info("old=%s", this.access.getOld());
        fp2().log().info("new=%s", this.access.getCurrent());
    }

    @Override
    protected String localizedTitleString() {
        return fp2().i18n().format(this.localeKey + ".title");
    }
}
