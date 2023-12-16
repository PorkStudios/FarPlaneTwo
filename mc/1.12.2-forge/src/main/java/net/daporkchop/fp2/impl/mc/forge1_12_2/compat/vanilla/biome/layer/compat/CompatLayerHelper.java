/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.gen.layer.GenLayer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static net.daporkchop.lib.common.util.PValidation.checkState;

/**
 * @author DaPorkchop_
 */
public class CompatLayerHelper {
    protected static final LoadingCache<Class<? extends GenLayer>, Function<GenLayer, GenLayer[]>> GET_LAYER_PARENTS_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(CompatLayerHelper::getLayerParents0));

    public static Function<GenLayer, GenLayer[]> getLayerParents(Class<? extends GenLayer> layerClass) {
        return GET_LAYER_PARENTS_CACHE.getUnchecked(layerClass);
    }

    private static Function<GenLayer, GenLayer[]> getLayerParents0(Class<? extends GenLayer> layerClass) {
        for (Class<?> clazz = layerClass; clazz != GenLayer.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                checkState(!GenLayer.class.isAssignableFrom(field.getType()), "can't get parent GenLayer(s) of %s (contains additional candidate parent field %s)", layerClass, field);
            }
        }
        return sourceLayer -> new GenLayer[]{((ATGenLayer1_12) sourceLayer).getParent()};
    }

    protected static final LoadingCache<Class<? extends GenLayer>, BiFunction<GenLayer, GenLayer[], GenLayer>> CLONE_LAYER_FUNC_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(CompatLayerHelper::cloneLayerFunc0));

    public static BiFunction<GenLayer, GenLayer[], GenLayer> cloneLayerFunc(Class<? extends GenLayer> layerClass) {
        return CLONE_LAYER_FUNC_CACHE.getUnchecked(layerClass);
    }

    private static BiFunction<GenLayer, GenLayer[], GenLayer> cloneLayerFunc0(Class<? extends GenLayer> layerClass) {
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

            ((ATGenLayer1_12) clonedLayer).setWorldGenSeed(((ATGenLayer1_12) sourceLayer).getWorldGenSeed());
            ((ATGenLayer1_12) clonedLayer).setParent(parents[0]);

            return clonedLayer;
        };
    }
}
