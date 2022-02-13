/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.client.gui.GuiContainer;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.config.Config;
import net.daporkchop.fp2.core.config.ConfigHelper;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;
import net.daporkchop.fp2.core.config.gui.element.ConfigGuiEnumButton;
import net.daporkchop.fp2.core.config.gui.element.ConfigGuiSlider;
import net.daporkchop.fp2.core.config.gui.element.ConfigGuiSubmenuButton;
import net.daporkchop.fp2.core.config.gui.element.ConfigGuiToggleButton;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ConfigGuiHelper {
    /**
     * Creates and displays a new config menu.
     *
     * @param menuName the name of the config menu
     * @param callback a callback function that will be called if the configuration has been modified once the menu is closed
     */
    public <V> void createAndDisplayGuiContext(@NonNull String menuName, @NonNull V defaultConfig, V serverConfig, @NonNull V currentConfig, @NonNull Consumer<V> callback) {
        V newConfig = ConfigHelper.cloneConfigObject(currentConfig);
        fp2().openScreen(context -> new ConfigGuiScreen.Root<>(context,
                MODID + ".config." + menuName,
                ConfigGuiObjectAccess.forValues(defaultConfig, serverConfig, currentConfig, newConfig),
                callback));
    }

    /**
     * Creates a new {@link GuiElement} for a given {@link GuiContext}, {@link GuiElementProperties} and {@link ConfigGuiObjectAccess}.
     *
     * @param context the current {@link GuiContext}
     * @return a new {@link GuiElement}
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public GuiElement createConfigGuiElement(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull ConfigGuiObjectAccess<?> access) {
        Config.GuiElementClass guiElementAnnotation = access.getAnnotation(Config.GuiElementClass.class);
        if (guiElementAnnotation != null) { //a specific gui element class was requested, so let's use it
            Constructor<? extends GuiElement> constructor = guiElementAnnotation.value().getDeclaredConstructor(GuiContext.class, GuiElementProperties.class, ConfigGuiObjectAccess.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, properties, access);
        }

        Class<?> type = access.type();
        checkArg(type != char.class && type != Character.class
                 && type != byte.class && type != Byte.class
                 && type != short.class && type != Short.class
                 && (type.isPrimitive() || type.isEnum() || (type.getModifiers() & Modifier.ABSTRACT) == 0)
                 && !type.isInterface() && !type.isAnonymousClass(),
                "unsupported type for access: %s", access);

        if (type == boolean.class || type == Boolean.class) {
            return new ConfigGuiToggleButton(context, properties, uncheckedCast(access));
        } else if (type.isEnum()) {
            return new ConfigGuiEnumButton<>(context, properties, uncheckedCast(access));
        } else if (type == int.class || type == Integer.class
                   || type == long.class || type == Long.class
                   || type == float.class || type == Float.class
                   || type == double.class || type == Double.class) {
            return new ConfigGuiSlider(context, properties, uncheckedCast(access));
        } else {
            return new ConfigGuiSubmenuButton(context, properties, access);
        }
    }

    /**
     * Creates a new config GUI container for the given configuration category metadata and the {@link GuiElement}s to be displayed in it.
     *
     * @param categoryMeta the configuration category metadata
     * @param elements     the {@link GuiElement}s to be displayed
     * @return a {@link GuiContainer} containing the given {@link GuiElement}s
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public GuiContainer createConfigGuiContainer(@NonNull Config.CategoryMeta categoryMeta, @NonNull GuiContext context, @NonNull ConfigGuiObjectAccess<?> access, @NonNull List<GuiElement> elements) {
        try {
            Constructor<? extends GuiContainer> constructor = categoryMeta.containerClass().getDeclaredConstructor(GuiContext.class, List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, elements);
        } catch (NoSuchMethodException e) {
            Constructor<? extends GuiContainer> constructor = categoryMeta.containerClass().getDeclaredConstructor(GuiContext.class, ConfigGuiObjectAccess.class, List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, access, elements);
        }
    }
}
