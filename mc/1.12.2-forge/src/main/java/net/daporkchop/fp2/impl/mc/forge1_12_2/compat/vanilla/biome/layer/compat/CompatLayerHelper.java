/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat;

import com.google.gson.GsonBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.asm.FieldIdentifier;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.FastLayerProvider;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.GenLayerFunctions;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.gen.layer.GenLayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public final class CompatLayerHelper {
    private static final class JsonCompatInfo {
        public List<Parent> parents;

        public static final class Parent {
            public boolean vanillaParent;
            public FieldIdentifier field;
        }

        public String constructorSignature;
        public List<ConstructorParameter> constructorParameters;

        public static final class ConstructorParameter {
            public boolean seed;
            public Integer parent;
        }

        public CompatLayerType type = CompatLayerType.PADDED;
    }

    private enum CompatLayerType {
        PADDED,
    }

    @RequiredArgsConstructor
    private static final class CompatInfo {
        public final List<Function<GenLayer, GenLayer>> parents;

        public final MethodHandle constructor;
        public final List<BiFunction<GenLayer, GenLayer[], Object>> constructorParameters;

        public final CompatLayerType type;
    }

    private static CompatInfo loadCompatInfo(Class<? extends GenLayer> layerClass) {
        JsonCompatInfo jsonInfo;
        try (InputStream in = CompatLayerHelper.class.getResourceAsStream(layerClass.getName() + ".json")) {
            if (in == null) {
                return null;
            }

            jsonInfo = new GsonBuilder().setLenient().create().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonCompatInfo.class);
        }

        List<Function<GenLayer, GenLayer>> parentFields = new ArrayList<>();
        for (JsonCompatInfo.Parent parentInfo : jsonInfo.parents) {
            if (parentInfo.vanillaParent) {
                parentFields.add(layer -> ((ATGenLayer1_12) layer).getParent());
            } else if (parentInfo.field != null) {
                Class<?> ownerClass = Class.forName(parentInfo.field.owner.replace('/', '.'), false, layerClass.getClassLoader());
                Field field = ownerClass.getDeclaredField(parentInfo.field.name);
                field.setAccessible(true);
                parentFields.add(uncheckedCast((Function<GenLayer, Object>) field::get));
            } else {
                throw new IllegalArgumentException(layerClass.getName());
            }
        }

        MethodHandle cloneConstructor = MethodHandles.lookup().findConstructor(
                layerClass,
                MethodType.fromMethodDescriptorString(jsonInfo.constructorSignature, layerClass.getClassLoader()));

        List<BiFunction<GenLayer, GenLayer[], Object>> cloneConstructorParameters = new ArrayList<>();
        for (JsonCompatInfo.ConstructorParameter parameterInfo : jsonInfo.constructorParameters) {
            if (parameterInfo.seed) {
                cloneConstructorParameters.add((layer, parents) -> ((ATGenLayer1_12) layer).getWorldGenSeed());
            } else if (parameterInfo.parent != null) {
                int parentIndex = parameterInfo.parent;
                cloneConstructorParameters.add((layer, parents) -> parents[parentIndex]);
            } else {
                throw new IllegalArgumentException(layerClass.getName());
            }
        }

        return new CompatInfo(parentFields, cloneConstructor, cloneConstructorParameters, jsonInfo.type);
    }

    public static GenLayerFunctions getDefaultGenLayerFunctions(Class<? extends GenLayer> layerClass) {
        CompatInfo compatInfo = loadCompatInfo(layerClass);
        if (compatInfo != null) {
            //we have known compatibility information for this class, use it!
            //  this is clearly not optimal code, but i don't really care
            return new GenLayerFunctions() {
                @Override
                public GenLayer[] getParents(@NonNull GenLayer layer) {
                    return compatInfo.parents.stream()
                            .map(parent -> parent.apply(layer))
                            .collect(Collectors.toList())
                            .toArray(new GenLayer[0]);
                }

                @Override
                public GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
                    return (GenLayer) compatInfo.constructor.invokeWithArguments(compatInfo.constructorParameters.stream()
                            .map(parameter -> parameter.apply(layer, parents))
                            .toArray());
                }

                @Override
                public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
                    assert compatInfo.type == CompatLayerType.PADDED;
                    checkArg(parents.length == 1, "expected exactly one parent: %s", Arrays.asList(parents));
                    return new CompatPaddedLayerWrapper(layer, parents[0]);
                }
            };
        }

        //fall back to creating a default implementation as best we can

        //ensure that the GenLayer only has one parent
        for (Class<?> clazz = layerClass; clazz != GenLayer.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                checkState(!GenLayer.class.isAssignableFrom(field.getType()), "can't get parent GenLayer(s) of %s (contains additional candidate parent field %s)", layerClass, field);
            }
        }

        //find the fields we need to clone
        List<Field> fields = new ArrayList<>();
        for (Class<?> clazz = layerClass; clazz != GenLayer.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                checkState(field.getType().isPrimitive() || field.getType().isEnum(), "can't clone GenLayer of %s (contains non-cloneable field %s)", layerClass, field);

                field.setAccessible(true);
                fields.add(field);
            }
        }

        return new GenLayerFunctions.SingleParentVanilla() {
            @Override
            public GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent) {
                GenLayer clonedLayer = PUnsafe.allocateInstance(layerClass);
                try {
                    for (Field field : fields) {
                        field.set(clonedLayer, field.get(layer));
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("failed to clone GenLayer", e);
                }

                ((ATGenLayer1_12) clonedLayer).setParent(parent);

                return clonedLayer;
            }

            @Override
            public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent) {
                //TODO: auto-detect which kind of layer it is and use an appropriate wrapper class
                return new CompatPaddedLayerWrapper(layer, parent);
            }
        };
    }
}
