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

package net.daporkchop.fp2.client.gui;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.element.GuiEnumButton;
import net.daporkchop.fp2.client.gui.element.GuiSlider;
import net.daporkchop.fp2.client.gui.element.GuiSubmenuButton;
import net.daporkchop.fp2.client.gui.element.GuiToggleButton;
import net.daporkchop.fp2.client.gui.screen.DefaultConfigGuiScreen;
import net.daporkchop.fp2.config.Config;
import net.daporkchop.fp2.config.ConfigHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class GuiHelper {
    /**
     * Creates and displays a new config menu.
     *
     * @param menuName the name of the config menu
     * @param callback a callback function that will be called if the configuration has been modified once the menu is closed
     */
    public <V> void createAndDisplayGuiContext(@NonNull String menuName, @NonNull V defaultConfig, V serverConfig, @NonNull V currentConfig, @NonNull Consumer<V> callback) {
        new DefaultGuiContext(MODID + ".config." + menuName + '.', GuiObjectAccess.forValues(defaultConfig, serverConfig, currentConfig, ConfigHelper.cloneConfigObject(currentConfig)), callback);
    }

    /**
     * Creates a new {@link IConfigGuiScreen} for a given {@link IGuiContext} and config struct instance.
     *
     * @param context the current {@link IGuiContext}
     * @return a new {@link IConfigGuiScreen}
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public IConfigGuiScreen createConfigGuiScreen(@NonNull IGuiContext context, @NonNull GuiObjectAccess<?> access) {
        Config.GuiScreenClass guiScreenAnnotation = access.getAnnotation(Config.GuiScreenClass.class);
        if (guiScreenAnnotation != null) { //a specific gui screen class was requested, so let's use it
            Constructor<? extends IConfigGuiScreen> constructor = guiScreenAnnotation.value().getDeclaredConstructor(IGuiContext.class, GuiObjectAccess.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, access);
        }

        return new DefaultConfigGuiScreen(context, access);
    }

    /**
     * Creates a new {@link IConfigGuiElement} for a given {@link IGuiContext}, config struct instance and config property field.
     *
     * @param context the current {@link IGuiContext}
     * @return a new {@link IConfigGuiElement}
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public IConfigGuiElement createConfigGuiElement(@NonNull IGuiContext context, @NonNull GuiObjectAccess<?> access) {
        Config.GuiElementClass guiElementAnnotation = access.getAnnotation(Config.GuiElementClass.class);
        if (guiElementAnnotation != null) { //a specific gui element class was requested, so let's use it
            Constructor<? extends IConfigGuiElement> constructor = guiElementAnnotation.value().getDeclaredConstructor(IGuiContext.class, GuiObjectAccess.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, access);
        }

        Class<?> type = access.type();
        checkArg(type != char.class && type != Character.class
                 && type != byte.class && type != Byte.class
                 && type != short.class && type != Short.class
                 && (type.isPrimitive() || type.isEnum() || (type.getModifiers() & Modifier.ABSTRACT) == 0)
                 && !type.isInterface() && !type.isAnonymousClass(),
                "unsupported type for access: %s", access);

        if (type.isEnum()) {
            return new GuiEnumButton<>(context, uncheckedCast(access));
        } else if (type == boolean.class || type == Boolean.class) {
            return new GuiToggleButton(context, uncheckedCast(access));
        } else if (type == int.class || type == Integer.class
                   || type == long.class || type == Long.class
                   || type == float.class || type == Float.class
                   || type == double.class || type == Double.class) {
            return new GuiSlider(context, uncheckedCast(access));
        } else {
            return new GuiSubmenuButton<>(context, access);
        }
    }

    /**
     * Creates a new config GUI container for the given configuration category metadata and the {@link IConfigGuiElement}s to be displayed in it.
     *
     * @param categoryMeta the configuration category metadata
     * @param elements     the {@link IConfigGuiElement}s to be displayed
     * @return a {@link IConfigGuiElement} containing the given {@link IConfigGuiElement}s
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public IConfigGuiElement createConfigGuiContainer(@NonNull Config.CategoryMeta categoryMeta, @NonNull IGuiContext context, @NonNull GuiObjectAccess<?> access, @NonNull List<IConfigGuiElement> elements) {
        Constructor<? extends IConfigGuiElement> constructor = categoryMeta.containerClass().getDeclaredConstructor(IGuiContext.class, GuiObjectAccess.class, List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context, access, elements);
    }
}
