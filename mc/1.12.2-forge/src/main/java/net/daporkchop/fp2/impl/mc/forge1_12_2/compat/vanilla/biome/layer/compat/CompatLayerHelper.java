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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.asm.FieldIdentifier;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
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
import java.util.List;
import java.util.Optional;
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
    }

    @RequiredArgsConstructor
    private static final class CompatInfo {
        public final List<Function<GenLayer, GenLayer>> parents;

        public final MethodHandle constructor;
        public final List<BiFunction<GenLayer, GenLayer[], Object>> constructorParameters;
    }

    private static final LoadingCache<Class<? extends GenLayer>, Optional<CompatInfo>> KNOWN_COMPAT_INFO_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.<Class<? extends GenLayer>, Optional<CompatInfo>>from(layerClass -> {
                JsonCompatInfo jsonInfo;
                try (InputStream in = CompatLayerHelper.class.getResourceAsStream(layerClass.getName() + ".json")) {
                    if (in == null) {
                        return Optional.empty();
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

                return Optional.of(new CompatInfo(parentFields, cloneConstructor, cloneConstructorParameters));
            }));

    private static final LoadingCache<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_LAYER_PARENTS_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(CompatLayerHelper::getLayerParents0));

    public static Function<GenLayer, GenLayer[]> getLayerParents(Class<? extends GenLayer> layerClass) {
        return GET_LAYER_PARENTS_CACHE.getUnchecked(layerClass);
    }

    private static Function<GenLayer, GenLayer[]> getLayerParents0(Class<? extends GenLayer> layerClass) {
        CompatInfo compatInfo = KNOWN_COMPAT_INFO_CACHE.get(layerClass).orElse(null);
        if (compatInfo != null) {
            return sourceLayer -> compatInfo.parents.stream()
                    .map(parent -> parent.apply(sourceLayer))
                    .collect(Collectors.toList())
                    .toArray(new GenLayer[0]);
        }

        for (Class<?> clazz = layerClass; clazz != GenLayer.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                checkState(!GenLayer.class.isAssignableFrom(field.getType()), "can't get parent GenLayer(s) of %s (contains additional candidate parent field %s)", layerClass, field);
            }
        }
        return sourceLayer -> new GenLayer[]{ ((ATGenLayer1_12) sourceLayer).getParent() };
    }

    private static final LoadingCache<Class<? extends GenLayer>, BiFunction<GenLayer, GenLayer[], GenLayer>> CLONE_LAYER_FUNC_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(CompatLayerHelper::cloneLayerFunc0));

    public static BiFunction<GenLayer, GenLayer[], GenLayer> cloneLayerFunc(Class<? extends GenLayer> layerClass) {
        return CLONE_LAYER_FUNC_CACHE.getUnchecked(layerClass);
    }

    private static BiFunction<GenLayer, GenLayer[], GenLayer> cloneLayerFunc0(Class<? extends GenLayer> layerClass) {
        CompatInfo compatInfo = KNOWN_COMPAT_INFO_CACHE.get(layerClass).orElse(null);
        if (compatInfo != null) {
            return (sourceLayer, parents) -> (GenLayer) compatInfo.constructor.invokeWithArguments(compatInfo.constructorParameters.stream()
                    .map(parameter -> parameter.apply(sourceLayer, parents))
                    .toArray());
        }

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

        return (sourceLayer, parents) -> {
            GenLayer clonedLayer = PUnsafe.allocateInstance(layerClass);
            try {
                for (Field field : fields) {
                    field.set(clonedLayer, field.get(sourceLayer));
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("failed to clone GenLayer", e);
            }

            ((ATGenLayer1_12) clonedLayer).setParent(parents[0]);

            return clonedLayer;
        };
    }
}
