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
 */

package net.daporkchop.fp2.core.config;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.function.exception.ESupplier;
import net.daporkchop.lib.common.misc.Tuple;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ConfigHelper {
    protected static final long FIELD_MODIFIERS_OFFSET = ((ESupplier<Long>) () -> PUnsafe.objectFieldOffset(Field.class.getDeclaredField("modifiers"))).get();

    protected static final Set<Class<?>> SIMPLE_COPYABLE_TYPES = ImmutableSet.of(
            Boolean.class,
            Byte.class, Short.class, Character.class, Integer.class, Long.class,
            Float.class, Double.class,
            String.class);

    protected Tuple<Class<?>, String> parseMemberId(@NonNull String id) {
        try {
            String[] split = id.split("#");
            checkArg(!split[0].isEmpty(), "class name may not be empty!");
            checkArg(!split[1].isEmpty(), "member name may not be empty!");

            return new Tuple<>(Class.forName(split[0]), split[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("unable to parse member id: " + id, e);
        }
    }

    /**
     * Extracts the numeric value from a {@link Config.Constant} annotation.
     *
     * @param constant the annotation
     * @return the numeric value
     */
    @SneakyThrows({ IllegalAccessException.class, InvocationTargetException.class, NoSuchFieldException.class, NoSuchMethodException.class })
    public Number evaluate(@NonNull Config.Constant constant) {
        double value = constant.value();
        String fieldId = constant.field();
        String methodId = constant.method();

        if ("<null>".equals(fieldId)) { //annotation has specifically requested to return null
            return null;
        }

        checkArg((Double.isNaN(value) ? 0 : 1) + (fieldId.isEmpty() ? 0 : 1) + (methodId.isEmpty() ? 0 : 1) == 1,
                "invalid @Constant annotation: exactly one of 'value', 'field' and 'method' must be set!");

        if (!Double.isNaN(value)) {
            return value;
        } else if (!fieldId.isEmpty()) {
            Tuple<Class<?>, String> tuple = parseMemberId(fieldId);

            Field field = tuple.getA().getDeclaredField(tuple.getB());
            checkState((field.getModifiers() & Modifier.STATIC) != 0, "not a static field: %s", fieldId);
            field.setAccessible(true);

            return (Number) field.get(null);
        } else {
            Tuple<Class<?>, String> tuple = parseMemberId(methodId);

            Method method = tuple.getA().getDeclaredMethod(tuple.getB());
            checkState((method.getModifiers() & Modifier.STATIC) != 0, "not a static method: %s", fieldId);
            method.setAccessible(true);

            return (Number) method.invoke(null);
        }
    }

    /**
     * Gets all of the fields in the given {@link Class} which contain config properties.
     *
     * @param clazz the {@link Class}
     * @return all of the fields in the given {@link Class} which contain properties
     * @throws IllegalArgumentException if any fields in the given {@link Class} are not valid configuration properties
     */
    public Stream<Field> getConfigPropertyFields(@NonNull Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> curr = clazz; curr != Object.class; curr = curr.getSuperclass()) {
            fields.addAll(Arrays.asList(curr.getDeclaredFields()));
        }

        return fields.stream()
                .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                .peek(field -> {
                    checkValidPropertyType(field);

                    //make the field accessible and writeable
                    field.setAccessible(true);
                    PUnsafe.putInt(field, FIELD_MODIFIERS_OFFSET, field.getModifiers() & ~Modifier.FINAL);
                });
    }

    /**
     * Duplicates a configuration object.
     *
     * @param srcInstance the object to clone
     * @return the cloned instance
     */
    @SneakyThrows(IllegalAccessException.class)
    public <T> T cloneConfigObject(@NonNull T srcInstance) {
        Class<?> srcClass = srcInstance.getClass();

        if (isSimpleCopyableType(srcClass)) { //simple copyable type, nothing to do
            return srcInstance;
        } else if (srcClass.isArray()) { //array type
            Class<?> componentType = srcInstance.getClass().getComponentType();
            int length = Array.getLength(srcInstance);

            T dstInstance = uncheckedCast(Array.newInstance(componentType, length));
            if (isSimpleCopyableType(componentType)) { //we can just copy the elements
                System.arraycopy(srcInstance, 0, dstInstance, 0, length);
            } else { //do a deep copy
                for (int i = 0; i < length; i++) {
                    Array.set(dstInstance, i, cloneConfigObject(Array.get(srcInstance, i)));
                }
            }
            return dstInstance;
        } else { //assume a struct type
            T dstInstance = uncheckedCast(PUnsafe.allocateInstance(srcInstance.getClass()));
            for (Field field : getConfigPropertyFields(srcInstance.getClass()).toArray(Field[]::new)) {
                Object value = field.get(srcInstance);
                field.set(dstInstance, isSimpleCopyableType(field.getType()) ? value : cloneConfigObject(value));
            }
            return dstInstance;
        }
    }

    /**
     * Computes the difference between the two configuration objects and checks if anything needs to be restarted for the changes to take effect.
     *
     * @param oldInstance the old configuration object
     * @param newInstance the new configuration object
     * @return the {@link Config.Requirement}
     */
    @SneakyThrows(IllegalAccessException.class)
    public <T> Config.Requirement restartRequirement(@NonNull T oldInstance, @NonNull T newInstance) {
        checkArg(oldInstance.getClass() == newInstance.getClass(), "%s != %s", oldInstance.getClass(), newInstance.getClass());

        Config.Requirement requirement = Config.Requirement.NONE;
        for (Field field : getConfigPropertyFields(oldInstance.getClass()).toArray(Field[]::new)) {
            Object oldValue = field.get(oldInstance);
            Object newValue = field.get(newInstance);

            if (!Objects.deepEquals(oldValue, newValue)) {
                Config.RestartRequired restartRequiredAnnotation = field.getAnnotation(Config.RestartRequired.class);
                if (restartRequiredAnnotation != null && restartRequiredAnnotation.value().ordinal() > requirement.ordinal()) {
                    requirement = restartRequiredAnnotation.value();
                }

                if (!isSimpleCopyableType(field.getType())) {
                    Config.Requirement subRequirement = restartRequirement(oldValue, newValue);
                    if (subRequirement.ordinal() > requirement.ordinal()) {
                        requirement = subRequirement;
                    }
                }
            }
        }

        return requirement;
    }

    /**
     * Ensures that the given configuration object's values are valid.
     *
     * @param instance the configuration object
     */
    @SneakyThrows(IllegalAccessException.class)
    public <T> T validateConfig(@NonNull T instance) {
        for (Field field : getConfigPropertyFields(instance.getClass()).toArray(Field[]::new)) {
            Object value = field.get(instance);

            checkArg(value != null, "invalid value for %s: null", field);

            if (!isSimpleCopyableType(field.getType())) {
                validateConfig(value);
            } else if (Number.class.isAssignableFrom(value.getClass())) {
                Config.Range range = field.getAnnotation(Config.Range.class);
                if (range != null) {
                    double min = evaluate(range.min()).doubleValue();
                    double max = evaluate(range.max()).doubleValue();
                    double number = ((Number) value).doubleValue();

                    checkArg(number >= min && number <= max, "invalid value for %s: %s (min=%s, max=%s)", field, number, min, max);
                }
            }
        }

        return instance;
    }

    protected void checkValidPropertyType(@NonNull Field field) {
        checkState((field.getModifiers() & Modifier.TRANSIENT) == 0, "transient field not allowed in config class: %s", field);
        checkState(!field.isSynthetic(), "synthetic field not allowed in config class: %s", field);
    }

    protected boolean isSimpleCopyableType(@NonNull Class<?> type) {
        return type.isEnum() || type.isPrimitive() || SIMPLE_COPYABLE_TYPES.contains(type);
    }
}
