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
     * @return the class type of the {@link IConfigGuiElement} to use for this setting, or {@link IConfigGuiElement} if it should be left as default
     */
    @SideOnly(Side.CLIENT)
    Class<? extends IConfigGuiElement> guiElementClass() default IConfigGuiElement.class;

    /**
     * @author DaPorkchop_
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @interface Range {
        Constant min() default @Constant;

        Constant max() default @Constant;
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

        /**
         * @return the type of {@link #field()} and {@link #method()}. Must be either a numeric primitive type or {@link Number}
         */
        Class<?> javaType() default double.class;
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
        Type value();

        /**
         * @author DaPorkchop_
         */
        enum Type {
            GAME,
            WORLD;
        }
    }
}
