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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.gen.layer.ATGenLayer1_12;
import net.daporkchop.lib.common.function.plain.TriFunction;
import net.daporkchop.lib.primitive.lambda.LongObjObjFunction;
import net.daporkchop.lib.primitive.lambda.ObjLongObjFunction;
import net.minecraft.world.gen.layer.GenLayer;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public interface GenLayerFunctions {
    /**
     * Extracts the parent {@link GenLayer}s of the given {@link GenLayer}.
     *
     * @param layer the {@link GenLayer}
     * @return an array containing the given {@link GenLayer}'s parents
     */
    GenLayer[] getParents(@NonNull GenLayer layer);

    /**
     * Creates a clone of the given {@link GenLayer} instance, replacing its parents with the given {@link GenLayer}s.
     *
     * @param layer   the {@link GenLayer} to clone
     * @param parents the parent {@link GenLayer}s of the cloned layer, in the same order as they would be returned by {@link #getParents(GenLayer)}
     * @return the cloned {@link GenLayer}
     */
    GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents);

    /**
     * Converts the given {@link GenLayer} instance to an equivalent {@link IFastLayer}.
     *
     * @param provider the {@link FastLayerProvider} being used
     * @param layer    the {@link GenLayer} to convert
     * @param parents  the parent {@link IFastLayer}s of the cloned layer, in the same order as they would be returned by {@link #getParents(GenLayer)}
     * @return the converted {@link IFastLayer}
     */
    IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents);

    /**
     * Can be implemented by a {@link GenLayer} directly to provide these functions.
     *
     * @author DaPorkchop_
     */
    interface IGenLayerFunctions {
        /**
         * Extracts the parent {@link GenLayer}s of this {@link GenLayer}.
         *
         * @return an array containing this {@link GenLayer}'s parents
         */
        GenLayer[] fp2_getParents();

        /**
         * Creates a clone of this {@link GenLayer} instance, replacing its parents with the given {@link GenLayer}s.
         *
         * @param clonedParents the parent {@link GenLayer}s of the cloned layer, in the same order as they would be returned by {@link #fp2_getParents()}
         * @return the cloned {@link GenLayer}
         */
        GenLayer fp2_cloneLayer(GenLayer @NonNull [] clonedParents);

        /**
         * Converts this {@link GenLayer} instance to an equivalent {@link IFastLayer}.
         *
         * @param provider the {@link FastLayerProvider} being used
         * @param parents  the parent {@link IFastLayer}s of the cloned layer, in the same order as they would be returned by {@link #fp2_getParents()}
         * @return the converted {@link IFastLayer}
         */
        IFastLayer fp2_makeFast(@NonNull FastLayerProvider provider, IFastLayer @NonNull [] parents);

        /**
         * Returns a {@link GenLayerFunctions} which forwards function calls to an implementation of {@link IGenLayerFunctions}.
         *
         * @param layerClass the {@link GenLayer} implementation class
         * @return a {@link GenLayerFunctions}
         */
        static GenLayerFunctions proxy(@NonNull Class<? extends GenLayer> layerClass) {
            return new GenLayerFunctions() {
                @Override
                public GenLayer[] getParents(@NonNull GenLayer layer) {
                    checkArg(layer.getClass() == layerClass, "expected: %s, found: %s", layerClass, layer.getClass());
                    return ((IGenLayerFunctions) layer).fp2_getParents();
                }

                @Override
                public GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
                    checkArg(layer.getClass() == layerClass, "expected: %s, found: %s", layerClass, layer.getClass());
                    return ((IGenLayerFunctions) layer).fp2_cloneLayer(parents);
                }

                @Override
                public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
                    checkArg(layer.getClass() == layerClass, "expected: %s, found: %s", layerClass, layer.getClass());
                    return ((IGenLayerFunctions) layer).fp2_makeFast(provider, parents);
                }
            };
        }
    }

    /**
     * @author DaPorkchop_
     */
    abstract class NoParent implements GenLayerFunctions {
        @Override
        public final GenLayer[] getParents(@NonNull GenLayer layer) {
            return new GenLayer[0];
        }

        @Override
        public final GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
            checkArg(parents.length == 0, "expected exactly zero parents: %s", Arrays.asList(parents));
            return this.cloneLayer(layer);
        }

        public abstract GenLayer cloneLayer(@NonNull GenLayer layer);

        @Override
        public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
            checkArg(parents.length == 0, "expected exactly zero parents: %s", Arrays.asList(parents));
            return this.makeFast(provider, layer);
        }

        public abstract IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer);
    }

    /**
     * @author DaPorkchop_
     */
    abstract class SingleParentVanilla implements GenLayerFunctions {
        @Override
        public final GenLayer[] getParents(@NonNull GenLayer layer) {
            return new GenLayer[]{ ((ATGenLayer1_12) layer).getParent() };
        }

        @Override
        public final GenLayer cloneLayer(@NonNull GenLayer layer, GenLayer @NonNull [] parents) {
            checkArg(parents.length == 1, "expected exactly one parent: %s", Arrays.asList(parents));
            return this.cloneLayer(layer, parents[0]);
        }

        public abstract GenLayer cloneLayer(@NonNull GenLayer layer, @NonNull GenLayer parent);

        @Override
        public IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, IFastLayer @NonNull [] parents) {
            checkArg(parents.length == 1, "expected exactly one parent: %s", Arrays.asList(parents));
            return this.makeFast(provider, layer, parents[0]);
        }

        public abstract IFastLayer makeFast(@NonNull FastLayerProvider provider, @NonNull GenLayer layer, @NonNull IFastLayer parent);
    }
}
