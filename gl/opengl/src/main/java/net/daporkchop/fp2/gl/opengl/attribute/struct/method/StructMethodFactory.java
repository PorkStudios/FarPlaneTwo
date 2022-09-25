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

package net.daporkchop.fp2.gl.opengl.attribute.struct.method;

import com.google.common.collect.Iterators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameterFactory;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Constructs {@link StructMethod}s from {@link Method}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StructMethodFactory {
    public static StructMethod createFromMethod(@NonNull AttributeType.Fields property, @NonNull Method method) {
        checkArg(method.getReturnType() == method.getDeclaringClass(), "method %s must have the same return type as the class in which it is declared!", method);
        checkArg((method.getModifiers() & Modifier.STATIC) == 0, "method %s may not be static!", method);

        if (method.isAnnotationPresent(AttributeSetter.class)) {
            AttributeSetter annotation = method.getAnnotation(AttributeSetter.class);
            String attributeName = annotation.value();
            if (attributeName.isEmpty()) { //fall back to method name if attribute name is unset
                attributeName = method.getName();
            }

            int fieldIndex = property.fieldNameIndex(attributeName);

            return new ChildSpecificSetter(digestParametersForSetter(property.fieldProperty(fieldIndex), 1, Iterators.forArray(method.getParameters())), fieldIndex);
        }

        throw new UnsupportedOperationException(method.toString());
    }

    private static AbstractSetter digestParametersForSetter(@NonNull AttributeType attributeType, int lvtIndex, @NonNull Iterator<Parameter> iterator) {
        Parameter parameter = iterator.next();
        if (parameter.isAnnotationPresent(ArrayIndex.class)) { //the parameter is an array index
            checkArg(parameter.getType() == int.class, "parameter annotated as @%s must be int", ArrayIndex.class);

            checkArg(attributeType instanceof AttributeType.Elements, "%s attribute doesn't have elements, cannot be used with @%s", PorkUtil.className(attributeType), ArrayIndex.class);
            AttributeType.Elements realAttributeType = (AttributeType.Elements) attributeType;

            return new IndexedSetterImpl(digestParametersForSetter(realAttributeType.componentType(), lvtIndex + 1, iterator), lvtIndex);
        }

        //consume all the remaining parameters and gather them into an array
        List<MethodParameter> methodParameters = new ArrayList<>();
        do {
            methodParameters.add(MethodParameterFactory.createFromType(parameter.getAnnotatedType(), lvtIndex));
            lvtIndex += Type.getType(parameter.getType()).getSize();
        } while (iterator.hasNext() && (parameter = iterator.next()) != null);

        //group all the method parameters into a single virtual parameter containing all the components
        MethodParameter methodParameter = MethodParameterFactory.union(methodParameters.toArray(new MethodParameter[0]));

        return new BasicSetterImpl(methodParameter, lvtIndex);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class ChildSpecificSetter implements StructMethod.Setter {
        @NonNull
        protected final StructMethod.Setter delegate;
        protected final int childIndex;

        @Override
        public int lvtIndexStart() {
            return this.delegate.lvtIndexStart();
        }

        @Override
        public <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> void visit(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull M rootMember, @NonNull Callback<M, C> callback) {
            this.delegate.visit(mv, lvtIndexAllocator, rootMember.child(this.childIndex), callback);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class AbstractSetter implements StructMethod.Setter {
        protected abstract MethodParameter parameter();

        @Override
        public <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> void visit(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                  @NonNull M rootMember,
                                                                                                  @NonNull Callback<M, C> callback) {
            this.parameter().load(mv, lvtIndexAllocatorIn, (lvtIndexAllocator, loader) -> {
                int lastComponentIndex = this.visit0(mv, lvtIndexAllocator, 0, rootMember, callback, loader);
                int expectedComponentIndex = this.parameter().components();

                checkState(lastComponentIndex == expectedComponentIndex, "only processed %d/%d components", lastComponentIndex, expectedComponentIndex);
            });
        }

        protected abstract <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visit0(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                              int globalComponentIndexAllocatorIn, @NonNull M rootMember,
                                                                                                              @NonNull Callback<M, C> callback,
                                                                                                              @NonNull MethodParameter.Loader loader);

        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visitComponentFixed(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                                  int globalComponentIndexAllocatorIn,
                                                                                                                  @NonNull M parent, int localComponentIndex, @NonNull C component,
                                                                                                                  @NonNull Callback<M, C> callback,
                                                                                                                  @NonNull MethodParameter.Loader loader) {
            callback.visitComponentFixed(lvtIndexAllocatorIn, parent, localComponentIndex, component, this.parameter().componentType(),
                    lvtIndexAllocator -> loader.load(lvtIndexAllocator, globalComponentIndexAllocatorIn));
            return globalComponentIndexAllocatorIn + 1;
        }

        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visitComponentIndexed(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                                    int globalComponentIndexAllocatorIn,
                                                                                                                    @NonNull M parent,
                                                                                                                    @NonNull BitSet possibleLocalComponentIndices, @NonNull LoaderCallback localComponentIndexLoader,
                                                                                                                    @NonNull Callback<M, C> callback,
                                                                                                                    @NonNull MethodParameter.Loader loader) {
            throw new UnsupportedOperationException();
        }

        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visitChildFixed(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                              int globalComponentIndexAllocatorIn,
                                                                                                              @NonNull M parent, int localChildIndex, @NonNull M child,
                                                                                                              @NonNull InternalChildCallback<M, C> childCallback,
                                                                                                              @NonNull Callback<M, C> callback,
                                                                                                              @NonNull MethodParameter.Loader loader) {
            @AllArgsConstructor
            class State implements ChildCallback<M, C> {
                int globalComponentIndexAllocator;

                @Override
                public void visitChild(int lvtIndexAllocator, @NonNull Callback<M, C> next) {
                    this.globalComponentIndexAllocator = childCallback.visitChild(lvtIndexAllocator, this.globalComponentIndexAllocator, next);
                }
            }

            State state = new State(globalComponentIndexAllocatorIn);
            callback.visitChildFixed(lvtIndexAllocatorIn, parent, localChildIndex, child, state);
            return state.globalComponentIndexAllocator;
        }

        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visitChildIndexed(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                                int globalComponentIndexAllocatorIn,
                                                                                                                @NonNull M parent, @NonNull BitSet possibleLocalChildIndices, @NonNull LoaderCallback localChildIndexLoader,
                                                                                                                @NonNull InternalChildCallback<M, C> childCallback,
                                                                                                                @NonNull Callback<M, C> callback,
                                                                                                                @NonNull MethodParameter.Loader loader) {
            @AllArgsConstructor
            class State implements ChildCallback<M, C> {
                int globalComponentIndexAllocator;

                @Override
                public void visitChild(int lvtIndexAllocator, @NonNull Callback<M, C> next) {
                    this.globalComponentIndexAllocator = childCallback.visitChild(lvtIndexAllocator, this.globalComponentIndexAllocator, next);
                }
            }

            State state = new State(globalComponentIndexAllocatorIn);
            callback.visitChildIndexed(lvtIndexAllocatorIn, parent, possibleLocalChildIndices, localChildIndexLoader, state);
            return state.globalComponentIndexAllocator;
        }

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface InternalChildCallback<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
            int visitChild(int lvtIndexAllocatorIn, int globalComponentIndexAllocatorIn, @NonNull Callback<M, C> next);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class BasicSetterImpl extends AbstractSetter {
        @NonNull
        @Getter
        private final MethodParameter parameter;
        @Getter
        private final int lvtIndexStart;

        @Override
        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visit0(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                     int globalComponentIndexAllocatorIn, @NonNull M rootMember,
                                                                                                     @NonNull Callback<M, C> callback,
                                                                                                     @NonNull MethodParameter.Loader loader) {
            //recurse into all children
            for (int localChildIndex = 0; localChildIndex < rootMember.children(); localChildIndex++) {
                M child = rootMember.child(localChildIndex);

                globalComponentIndexAllocatorIn = this.visitChildFixed(mv, lvtIndexAllocatorIn, globalComponentIndexAllocatorIn,
                        rootMember, localChildIndex, child,
                        (lvtIndexAllocator, globalComponentIndexAllocator, next) -> this.visit0(mv, lvtIndexAllocator, globalComponentIndexAllocator, child, next, loader),
                        callback, loader);
            }

            //process all components
            for (int localComponentIndex = 0; localComponentIndex < rootMember.components(); localComponentIndex++) {
                C component = rootMember.component(localComponentIndex);

                globalComponentIndexAllocatorIn = this.visitComponentFixed(mv, lvtIndexAllocatorIn, globalComponentIndexAllocatorIn,
                        rootMember, localComponentIndex, component,
                        callback, loader);
            }

            return globalComponentIndexAllocatorIn;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class IndexedSetterImpl extends AbstractSetter {
        @NonNull
        protected final AbstractSetter delegate;

        protected final int indexLvtIndex;

        @Override
        protected MethodParameter parameter() {
            return this.delegate.parameter();
        }

        @Override
        public int lvtIndexStart() {
            return this.delegate.lvtIndexStart();
        }

        @Override
        protected <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int visit0(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn,
                                                                                                     int globalComponentIndexAllocatorIn, @NonNull M rootMember,
                                                                                                     @NonNull Callback<M, C> callback,
                                                                                                     @NonNull MethodParameter.Loader loader) {
            checkArg(rootMember.components() == 0, "member may not have any components");

            //recurse into a single child
            BitSet possibleLocalChildIndices = new BitSet(rootMember.children());
            possibleLocalChildIndices.set(0, rootMember.children());

            globalComponentIndexAllocatorIn = this.visitChildIndexed(mv, lvtIndexAllocatorIn, globalComponentIndexAllocatorIn,
                    rootMember, possibleLocalChildIndices,
                    lvtIndexAllocator -> mv.visitVarInsn(ILOAD, this.indexLvtIndex),
                    (lvtIndexAllocator, globalComponentIndexAllocator, next) -> this.delegate.visit0(mv, lvtIndexAllocator, globalComponentIndexAllocator, rootMember.child(0), next, loader),
                    callback, loader);

            return globalComponentIndexAllocatorIn;
        }
    }
}
