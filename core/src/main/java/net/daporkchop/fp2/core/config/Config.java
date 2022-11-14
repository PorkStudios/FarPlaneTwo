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

package net.daporkchop.fp2.core.config;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.core.client.gui.GuiContainer;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.gui.container.ColumnsContainer;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class Config {
    /**
     * @author DaPorkchop_
     */
    public enum Requirement {
        NONE,
        WORLD,
        GAME;
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface GuiElementClass {
        /**
         * @return the class type of the {@link GuiElement} to use for this setting
         */
        Class<? extends GuiElement> value();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    public @interface GuiCategories {
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
    public @interface CategoryMeta {
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
        Class<? extends GuiContainer> containerClass() default ColumnsContainer.class;
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface GuiCategory {
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
    public @interface GuiShowServerValue {
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface GuiRange {
        Constant min();

        Constant max();

        /**
         * If present, causes the slider's values to be snapped to multiples of this constant.
         * <p>
         * Note that if this constant is defined, both {@link #min()} and {@link #max()} must be multiples of it.
         */
        Constant snapTo() default @Constant(field = "<null>");
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    public @interface Range {
        Constant min() default @Constant(field = "<null>");

        Constant max() default @Constant(field = "<null>");
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target({})
    public @interface Constant {
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
    public @interface RestartRequired {
        /**
         * @return the requirement for what needs to be restarted
         */
        Requirement value();
    }
}
