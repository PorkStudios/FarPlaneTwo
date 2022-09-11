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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameterFactory;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Constructs {@link StructMethod}s from {@link Method}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StructMethodFactory {
    public static StructMethod createFromMethod(@NonNull StructProperty.Fields property, @NonNull Method method) {
        checkArg(method.getReturnType() == method.getDeclaringClass(), "method %s must have the same return type as the class in which it is declared!", method);
        checkArg((method.getModifiers() & Modifier.STATIC) == 0, "method %s may not be static!", method);

        if (method.isAnnotationPresent(AttributeSetter.class)) {
            AttributeSetter annotation = method.getAnnotation(AttributeSetter.class);
            String attributeName = annotation.value();
            if (attributeName.isEmpty()) { //fall back to method name if attribute name is unset
                attributeName = method.getName();
            }

            int fieldIndex = property.fieldNameIndex(attributeName);

            return digestParametersForSetter(property.fieldProperty(fieldIndex), fieldIndex, 1, Iterators.forArray(method.getParameters()));
        }

        throw new UnsupportedOperationException(method.toString());
    }

    private static StructMethod.Setter digestParametersForSetter(@NonNull StructProperty property, int memberIndex, int lvtIndex, @NonNull Iterator<Parameter> iterator) {
        Parameter parameter = iterator.next();
        if (parameter.isAnnotationPresent(ArrayIndex.class)) { //the parameter is an array index
            checkArg(parameter.getType() == int.class, "parameter annotated as @%s must be int", ArrayIndex.class);

            throw new UnsupportedOperationException(); //TODO
        }

        //consume all the remaining parameters and gather them into an array
        List<MethodParameter> methodParameters = new ArrayList<>();
        do {
            methodParameters.add(MethodParameterFactory.createFromType(parameter.getAnnotatedType(), lvtIndex));
            lvtIndex += Type.getType(parameter.getType()).getSize();
        } while (iterator.hasNext() && (parameter = iterator.next()) != null);

        //group all the method parameters into a single virtual parameter containing all the components
        MethodParameter methodParameter = MethodParameterFactory.union(methodParameters.toArray(new MethodParameter[0]));

        return new SetterImpl(property, methodParameter, lvtIndex, memberIndex);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class SetterImpl implements StructMethod.Setter {
        @NonNull
        private final StructProperty property;
        @NonNull
        private final MethodParameter parameter;
        @Getter
        private final int lvtIndexStart;
        private final int memberIndex;

        @Override
        public <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> void forEachComponent(@NonNull MethodVisitor mv, int lvtIndexAllocator0, @NonNull M containingMember, @NonNull ComponentCallback<C> callback) {
            this.parameter.load(mv, lvtIndexAllocator0, (lvtIndexAllocator1, loader) -> {
                int lastComponent = this.forEachComponent(mv, lvtIndexAllocator1, containingMember.child(this.memberIndex), callback, loader, 0);
                checkState(lastComponent == this.parameter.components());
            });
        }

        private <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> int forEachComponent(@NonNull MethodVisitor mv, int lvtIndexAllocator0, @NonNull M member, @NonNull ComponentCallback<C> callback, @NonNull MethodParameter.Loader loader, int componentIndex) {
            //recurse into all children
            for (int i = 0; i < member.children(); i++) {
                componentIndex = this.forEachComponent(mv, lvtIndexAllocator0, member.child(i), callback, loader, componentIndex);
            }

            //process all components
            for (int i = 0; i < member.components(); i++) {
                int realComponentIndex = componentIndex++;
                callback.withComponent(lvtIndexAllocator0, member.component(i), this.parameter.componentType(),
                        lvtIndexAllocator1 -> loader.load(lvtIndexAllocator1, realComponentIndex));
            }

            return componentIndex;
        }
    }
}
