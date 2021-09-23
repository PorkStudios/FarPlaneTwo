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

package net.daporkchop.fp2.config;

import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IConfigGuiScreen;
import net.daporkchop.fp2.config.gui.container.ColumnsContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * @author DaPorkchop_
 */
@Retention(RUNTIME)
@Target({ FIELD, TYPE })
public @interface Setting {
    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    @SideOnly(Side.CLIENT)
    @interface GuiScreenClass {
        /**
         * @return the class type of the {@link IConfigGuiScreen} to use for this object
         */
        Class<? extends IConfigGuiScreen> value();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @SideOnly(Side.CLIENT)
    @interface GuiElementClass {
        /**
         * @return the class type of the {@link IConfigGuiElement} to use for this setting
         */
        Class<? extends IConfigGuiElement> value();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    @SideOnly(Side.CLIENT)
    @interface GuiCategories {
        /**
         * @return a {@link CategoryMeta} for each available category
         */
        CategoryMeta[] value();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target({})
    @SideOnly(Side.CLIENT)
    @interface CategoryMeta {
        /**
         * @return the name of the category
         */
        String name();

        /**
         * @return whether to add a title to this category if multiple categories are present
         */
        boolean title() default true;

        /**
         * @return the class of the container which will store the GUI components used by this category
         */
        Class<? extends IConfigGuiElement> containerClass() default ColumnsContainer.class;
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @SideOnly(Side.CLIENT)
    @interface GuiCategory {
        /**
         * @return the name of the category which this setting should be added to
         */
        String value();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @interface Range {
        Constant min();

        Constant max();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @SideOnly(Side.CLIENT)
    @interface GuiRange {
        Constant min();

        Constant max();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target({})
    @interface Constant {
        /**
         * @return the value of this constant field, or {@link Double#NaN} if it isn't set
         */
        double value() default Double.NaN;

        /**
         * @return a field reference in the form of {@code package.name.ClassName#fieldName}, referencing a static field which contains this constant field's value
         */
        String field() default "";

        /**
         * @return a method reference in the form of {@code package.name.ClassName#methodName}, referencing a zero-argument static method which returns this constant field's value
         */
        String method() default "";
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @interface RestartRequired {
        /**
         * @return the requirement for what needs to be restarted
         */
        Requirement value();
    }

    /**
     * @author DaPorkchop_
     */
    enum Requirement {
        NONE,
        WORLD,
        GAME;
    }
}
