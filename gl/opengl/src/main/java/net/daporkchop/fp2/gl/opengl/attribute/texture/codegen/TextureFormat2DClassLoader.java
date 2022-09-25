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

package net.daporkchop.fp2.gl.opengl.attribute.texture.codegen;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureWriter2D;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannel;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameterFactory;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.input.ScalarArgumentMethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.transform.IntToARGBExpansionTransformMethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLSamplerType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;
import net.daporkchop.fp2.gl.opengl.attribute.texture.Texture2DImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureFormat2DImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureWriter2DImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.util.PValidation;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class TextureFormat2DClassLoader extends TextureFormatClassLoader<TextureFormat2DImpl> {
    public TextureFormat2DClassLoader(@NonNull OpenGL gl, @NonNull PixelFormatImpl pixelFormat) {
        super(gl, pixelFormat, 2);
    }

    @Override
    protected String dimensionName(int dimension) {
        switch (dimension) {
            case 0:
                return "width";
            case 1:
                return "height";
            default:
                throw new IllegalArgumentException(String.valueOf(dimension));
        }
    }

    @Override
    protected Class<?> baseFormatClass() {
        return TextureFormat2DImpl.class;
    }

    @Override
    protected byte[] generateFormatClass() {
        String superclassName = getInternalName(this.baseFormatClass());
        String className = this.formatClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(String.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);

            {
                String glslPrimitiveTypeName = GLSLPrimitiveType.INVALID.name();
                switch (this.pixelFormat.channelType()) {
                    case FLOATING_POINT:
                        glslPrimitiveTypeName = GLSLPrimitiveType.FLOAT.name();
                        break;
                    case INTEGER:
                        glslPrimitiveTypeName = GLSLPrimitiveType.INT.name();
                        break;
                    case UNSIGNED_INTEGER:
                        glslPrimitiveTypeName = GLSLPrimitiveType.UINT.name();
                        break;
                }

                mv.visitFieldInsn(GETSTATIC, getInternalName(GLSLPrimitiveType.class), glslPrimitiveTypeName, getDescriptor(GLSLPrimitiveType.class));
            }
            mv.visitLdcInsn(this.pixelFormat.channels().size());
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(GLSLTypeFactory.class), "sampler", getMethodDescriptor(getType(GLSLSamplerType.class), getType(GLSLPrimitiveType.class), INT_TYPE), false);

            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(String.class), getType(GLSLSamplerType.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //long size()
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "size", getMethodDescriptor(LONG_TYPE), null, null);

            mv.visitLdcInsn((long) this.pixelFormat.internalFormat().sizeBytes());
            mv.visitInsn(LRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //TextureWriter2D createWriter(int width, int height)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "createWriter", getMethodDescriptor(getType(TextureWriter2D.class), INT_TYPE, INT_TYPE), null, null);

            //return new ${ this.writerClassName() }(this, width, height);
            mv.visitTypeInsn(NEW, this.writerClassName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, this.writerClassName(), "<init>", getMethodDescriptor(VOID_TYPE, getObjectType(className), INT_TYPE, INT_TYPE), false);

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //Texture2D createTexture(int width, int height, int levels)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "createTexture", getMethodDescriptor(getType(Texture2D.class), INT_TYPE, INT_TYPE, INT_TYPE), null, null);

            //return new ${ this.textureClassName() }(this, width, height, levels);
            mv.visitTypeInsn(NEW, this.textureClassName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKESPECIAL, this.textureClassName(), "<init>", getMethodDescriptor(VOID_TYPE, getObjectType(className), INT_TYPE, INT_TYPE, INT_TYPE), false);

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    @Override
    protected Class<?> baseTextureClass() {
        return Texture2DImpl.class;
    }

    @Override
    protected byte[] generateTextureClass() {
        String superclassName = getInternalName(this.baseTextureClass());
        String className = this.textureClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getObjectType(this.formatClassName()), INT_TYPE, INT_TYPE, INT_TYPE), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(this.baseFormatClass()), INT_TYPE, INT_TYPE, INT_TYPE), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int gl_internalFormat()
            MethodVisitor mv = writer.visitMethod(ACC_PROTECTED, "gl_internalFormat", getMethodDescriptor(INT_TYPE), null, null);

            OpenGLConstants.visitGLConstant(mv, this.pixelFormat.internalFormat().glInternalFormat());
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int gl_format()
            MethodVisitor mv = writer.visitMethod(ACC_PROTECTED, "gl_format", getMethodDescriptor(INT_TYPE), null, null);

            OpenGLConstants.visitGLConstant(mv, this.pixelFormat.storageFormat().glFormat());
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //int gl_type()
            MethodVisitor mv = writer.visitMethod(ACC_PROTECTED, "gl_type", getMethodDescriptor(INT_TYPE), null, null);

            OpenGLConstants.visitGLConstant(mv, this.pixelFormat.storageType().glType());
            mv.visitInsn(IRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    @Override
    protected Class<?> baseWriterClass() {
        return TextureWriter2DImpl.class;
    }

    @Override
    protected byte[] generateWriterClass() {
        String superclassName = getInternalName(this.baseWriterClass());
        String className = this.writerClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getObjectType(this.formatClassName()), INT_TYPE, INT_TYPE), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitLdcInsn((long) this.pixelFormat.storageType().totalSizeBytes());
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(this.baseFormatClass()), INT_TYPE, INT_TYPE, LONG_TYPE), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //generate texel setter methods
        // as far as code quality is concerned, this isn't.
        Matcher matcher = Pattern.compile("^set(?<interpretation>Raw|Normalized)(?<sign>Unsigned)?(?:(?<depth>8|16|32)b)?(?<channels>[RGBA]{1,4}|ARGB8)$").matcher("");
        for (Method method : TextureWriter2D.class.getDeclaredMethods()) {
            if (!matcher.reset(method.getName()).matches()) { //not a setter method, skip it
                continue;
            }

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, method.getName(), getMethodDescriptor(method), null, null);

            //validate coordinate indices
            for (int dimension = 0; dimension < this.dimensions; dimension++) {
                //PValidation.checkIndex(this.${ this.dimensionName(dimension) }, ${ params[dimension + 1] });
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, superclassName, this.dimensionName(dimension), INT_TYPE.getDescriptor());
                mv.visitVarInsn(ILOAD, dimension + 1);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PValidation.class), "checkIndex", getMethodDescriptor(INT_TYPE, INT_TYPE, INT_TYPE), false);
                mv.visitInsn(POP);
            }

            boolean normalized = "Normalized".equals(matcher.group("interpretation"));
            boolean unsigned = matcher.group("sign") != null;

            String rawDepth = matcher.group("depth");
            String rawChannels = matcher.group("channels");
            assert rawChannels != null : "no channels are set";

            List<PixelFormatChannel> channels;
            MethodParameter parameter;

            //noinspection SwitchStatementWithTooFewBranches
            switch (rawChannels) {
                default: {
                    //decode channels part
                    {
                        PixelFormatChannel[] channelsArray = new PixelFormatChannel[rawChannels.length()];
                        EnumSet<PixelFormatChannel> set = EnumSet.noneOf(PixelFormatChannel.class);

                        for (int i = 0; i < rawChannels.length(); i++) {
                            switch (rawChannels.charAt(i)) {
                                case 'R':
                                    channelsArray[i] = PixelFormatChannel.RED;
                                    break;
                                case 'G':
                                    channelsArray[i] = PixelFormatChannel.GREEN;
                                    break;
                                case 'B':
                                    channelsArray[i] = PixelFormatChannel.BLUE;
                                    break;
                                case 'A':
                                    channelsArray[i] = PixelFormatChannel.ALPHA;
                                    break;
                                default:
                                    throw new IllegalArgumentException(String.valueOf(rawChannels.charAt(i)));
                            }

                            if (!set.add(channelsArray[i])) {
                                throw new IllegalArgumentException("duplicate channel: " + channelsArray[i]);
                            }
                        }

                        channels = ImmutableList.copyOf(channelsArray);
                    }

                    //figure out the component type for each parameter
                    ComponentType componentType;
                    Class<?> rawComponentType = method.getParameterTypes()[this.dimensions];
                    if (normalized) { //normalized texel setter
                        switch (Integer.parseUnsignedInt(rawDepth)) {
                            case Byte.SIZE:
                                componentType = unsigned ? ComponentType.UNSIGNED_BYTE : ComponentType.BYTE;
                                break;
                            case Short.SIZE:
                                componentType = unsigned ? ComponentType.UNSIGNED_SHORT : ComponentType.SHORT;
                                break;
                            case Integer.SIZE:
                                componentType = unsigned ? ComponentType.UNSIGNED_INT : ComponentType.INT;
                                break;
                            default:
                                throw new IllegalArgumentException(rawDepth);
                        }
                    } else { //raw texel setter
                        if (rawComponentType == float.class) {
                            componentType = ComponentType.FLOAT;
                        } else if (rawComponentType == int.class) {
                            componentType = unsigned ? ComponentType.UNSIGNED_INT : ComponentType.INT;
                        } else {
                            throw new IllegalArgumentException("raw texel setter with " + rawComponentType);
                        }
                    }

                    //decode parameters
                    MethodParameter[] parameters = new MethodParameter[channels.size()];
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = new ScalarArgumentMethodParameter(componentType, 1 + this.dimensions + i);
                    }
                    parameter = MethodParameterFactory.union(parameters);
                    break;
                }
                case "ARGB8": { //special case: ARGB8 color
                    assert rawDepth == null : "ARGB8 has a fixed depth of 8 bits";

                    //configure channels
                    channels = ImmutableList.of(PixelFormatChannel.ALPHA, PixelFormatChannel.RED, PixelFormatChannel.GREEN, PixelFormatChannel.BLUE);

                    //configure MethodParameter which unpacks the int ARGB8 argument
                    parameter = new IntToARGBExpansionTransformMethodParameter(
                            new ScalarArgumentMethodParameter(ComponentType.INT, 1 + this.dimensions),
                            IntToARGBExpansionTransformMethodParameter.UnpackOrder.ARGB,
                            !unsigned);
                    break;
                }
            }

            //store each component
            int lvtIndexAllocatorBase = 1 + Stream.of(method.getParameterTypes()).map(Type::getType).mapToInt(Type::getSize).sum();
            parameter.load(mv, lvtIndexAllocatorBase, (lvtIndexAllocatorLoad, loader) -> {
                //store destination base object
                mv.visitInsn(ACONST_NULL);

                //store destination offset
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, superclassName, "addr", LONG_TYPE.getDescriptor());
                mv.visitVarInsn(ILOAD, 1 + this.dimensions - 1);
                mv.visitInsn(I2L);
                for (int dimension = this.dimensions - 2; dimension >= 0; dimension--) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, superclassName, this.dimensionName(dimension), INT_TYPE.getDescriptor());
                    mv.visitInsn(I2L);
                    mv.visitInsn(LMUL);
                    mv.visitVarInsn(ILOAD, 1 + dimension);
                    mv.visitInsn(I2L);
                    mv.visitInsn(LADD);
                }
                mv.visitLdcInsn((long) this.pixelFormat.storageType().totalSizeBytes());
                mv.visitInsn(LMUL);
                mv.visitInsn(LADD);

                this.pixelFormat.storageType().store(mv, lvtIndexAllocatorLoad, (lvtIndexAllocatorStore, storer) -> {
                    for (int component = 0, components = parameter.components(); component < components; component++) {
                        //determine the index of the channel in the output type
                        PixelFormatChannel channel = channels.get(component);
                        int channelIndex = this.pixelFormat.storageFormat().channels().indexOf(channel);
                        if (channelIndex < 0) { //the storage format is missing the current channel, skip it
                            continue;
                        }

                        int parameterBitDepth = parameter.componentType().size() << 3;
                        int storageTypeBitDepth = this.pixelFormat.storageType().bitDepths().get(channelIndex);

                        ComponentType parameterComponentType = parameter.componentType();
                        PixelFormatChannelType storageTypeType = this.pixelFormat.storageType().genericType();
                        PixelFormatChannelType storageFormatType = this.pixelFormat.storageFormat().type();

                        loader.load(lvtIndexAllocatorStore, component);

                        if (normalized) { //parameters are integers which are being normalized
                            assert parameterComponentType.integer() : "parameter components aren't integers, but are being normalized";

                            if (storageTypeType != PixelFormatChannelType.FLOATING_POINT) { //values are stored as (unsigned) integers
                                if (storageFormatType == PixelFormatChannelType.FLOATING_POINT) { //stored values are interpreted as normalized floats
                                    int shift = abs(parameterBitDepth - storageTypeBitDepth);
                                    if (parameterBitDepth > storageTypeBitDepth) { //need to shift right
                                        if (!parameterComponentType.signed()) { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //unsigned -> unsigned, do a regular unsigned shift
                                                mv.visitLdcInsn(shift);
                                                mv.visitInsn(IUSHR);
                                            } else { //unsigned -> signed, do a regular unsigned shift but by 1 extra bit to prevent the sign bit from being set
                                                mv.visitLdcInsn(shift + 1);
                                                mv.visitInsn(IUSHR);
                                            }
                                        } else { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //signed -> unsigned
                                                //clamp to discard negative values

                                                { //Math.max(val, 0)
                                                    //this is a branchless implementation of:
                                                    //  (val < 0 ? 0 : val)
                                                    //implemented by:
                                                    //  (val & ~(val >> 31))

                                                    mv.visitInsn(DUP);
                                                    mv.visitLdcInsn(31);
                                                    mv.visitInsn(ISHR);
                                                    mv.visitLdcInsn(-1); //NOT
                                                    mv.visitInsn(IXOR);
                                                    mv.visitInsn(IAND);
                                                }

                                                //the argument is now effectively an unsigned integer with a bit depth of (parameterBitDepth - 1), do final shift if necessary
                                                if (shift - 1 != 0) {
                                                    mv.visitLdcInsn(shift - 1);
                                                    mv.visitInsn(IUSHR);
                                                }
                                            } else { //signed -> signed, do a regular shift with sign-extension
                                                mv.visitLdcInsn(shift);
                                                mv.visitInsn(ISHR);
                                            }
                                        }
                                    } else if (parameterBitDepth < storageTypeBitDepth) { //need to shift left
                                        int actualShiftLeft;

                                        if (!parameterComponentType.signed()) { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //unsigned -> unsigned, do a regular shift
                                                actualShiftLeft = shift;
                                            } else { //unsigned -> signed, do a regular shift but by 1 fewer bits to prevent the sign bit from being set
                                                actualShiftLeft = shift - 1;
                                            }
                                        } else { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //signed -> unsigned
                                                //clamp to discard negative values

                                                { //Math.max(val, 0)
                                                    //this is a branchless implementation of:
                                                    //  (val < 0 ? 0 : val)
                                                    //implemented by:
                                                    //  (val & ~(val >> 31))

                                                    mv.visitInsn(DUP);
                                                    mv.visitLdcInsn(31);
                                                    mv.visitInsn(ISHR);
                                                    mv.visitLdcInsn(-1); //NOT
                                                    mv.visitInsn(IXOR);
                                                    mv.visitInsn(IAND);
                                                }

                                                //the argument is now effectively an unsigned integer with a bit depth of (parameterBitDepth - 1), do final shift if necessary
                                                actualShiftLeft = shift + 1;
                                            } else { //signed -> signed, do a regular shift
                                                actualShiftLeft = shift;
                                            }
                                        }

                                        //shift left while doing some weird kind of inverse "sign-extension" on the least significant bit
                                        if (actualShiftLeft != 0) {
                                            mv.visitLdcInsn(actualShiftLeft);
                                            mv.visitInsn(ISHL);

                                            //TODO: LSB-extend (is this really even necessary?)
                                        }
                                    } else { //the values have the same bit depth, we probably don't need to do any shifting
                                        if (!parameterComponentType.signed()) { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //unsigned -> unsigned, no need to shift
                                                //no-op
                                            } else { //unsigned -> signed, unsigned shift right by one bit to prevent the sign bit from being set
                                                mv.visitLdcInsn(1);
                                                mv.visitInsn(IUSHR);
                                            }
                                        } else { //the input argument is unsigned
                                            if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //signed -> unsigned
                                                //clamp to discard negative values

                                                { //Math.max(val, 0)
                                                    //this is a branchless implementation of:
                                                    //  (val < 0 ? 0 : val)
                                                    //implemented by:
                                                    //  (val & ~(val >> 31))

                                                    mv.visitInsn(DUP);
                                                    mv.visitLdcInsn(31);
                                                    mv.visitInsn(ISHR);
                                                    mv.visitLdcInsn(-1); //NOT
                                                    mv.visitInsn(IXOR);
                                                    mv.visitInsn(IAND);
                                                }

                                                //the argument is now effectively an unsigned integer with a bit depth of (parameterBitDepth - 1), shift left by one
                                                int tmpLvtIndex = lvtIndexAllocatorStore++;
                                                mv.visitVarInsn(ISTORE, tmpLvtIndex);
                                                mv.visitVarInsn(ILOAD, tmpLvtIndex);
                                                mv.visitLdcInsn(1);
                                                mv.visitInsn(IAND);
                                                mv.visitVarInsn(ILOAD, tmpLvtIndex);
                                                mv.visitLdcInsn(1);
                                                mv.visitInsn(ISHL);
                                                mv.visitInsn(IOR);
                                                lvtIndexAllocatorStore--;
                                            } else { //signed -> signed, no need to shift
                                                //no-op
                                            }
                                        }
                                    }
                                } else { //stored values are interpreted as (unsigned) integers
                                    if (!parameterComponentType.signed()) { //the input argument is unsigned
                                        //unsigned -> unsigned/signed (doesn't make a difference): normalize and round towards -infinity to end up with either 0 or 1

                                        if (parameterBitDepth == Integer.SIZE) { //special handling for 32-bit unsigned integers, since java doesn't really support them
                                            Label case1 = new Label();
                                            Label tail = new Label();

                                            mv.visitLdcInsn(-1);
                                            mv.visitJumpInsn(IF_ICMPEQ, case1);
                                            //case 0
                                            mv.visitLdcInsn(0);
                                            mv.visitJumpInsn(GOTO, tail);
                                            //case 1
                                            mv.visitLabel(case1);
                                            mv.visitLdcInsn(1);
                                            mv.visitLabel(tail);
                                        } else { //branchless implementation for smaller integers
                                            if (false) {
                                                parameterComponentType.truncateInteger(mv);
                                                mv.visitLdcInsn((1 << parameterBitDepth) - 1);
                                                mv.visitInsn(ISUB);
                                                mv.visitLdcInsn(-1); //bitwise NOT
                                                mv.visitInsn(IXOR);
                                                mv.visitLdcInsn(31);
                                                mv.visitInsn(IUSHR);
                                            } else {
                                                mv.visitLdcInsn(1);
                                                mv.visitInsn(IADD);
                                                mv.visitLdcInsn(parameterBitDepth);
                                                mv.visitInsn(IUSHR);
                                                mv.visitLdcInsn(1);
                                                mv.visitInsn(IAND);
                                            }
                                        }
                                    } else { //the input argument is signed
                                        parameterComponentType.truncateInteger(mv);

                                        if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER) { //signed -> unsigned
                                            Label case1 = new Label();
                                            Label tail = new Label();

                                            mv.visitLdcInsn((1 << (parameterBitDepth - 1)) - 1); //(parameterBitDepth - 1) because parameter type is signed
                                            mv.visitJumpInsn(IF_ICMPEQ, case1);
                                            //case 0
                                            mv.visitLdcInsn(0);
                                            mv.visitJumpInsn(GOTO, tail);
                                            //case 1
                                            mv.visitLabel(case1);
                                            mv.visitLdcInsn(1);
                                            mv.visitLabel(tail);
                                        } else { //signed -> signed
                                            Label case1 = new Label();
                                            Label tail = new Label();

                                            mv.visitInsn(DUP);
                                            mv.visitLdcInsn((1 << (parameterBitDepth - 1)) - 1); //(parameterBitDepth - 1) because parameter type is signed
                                            mv.visitJumpInsn(IF_ICMPEQ, case1);
                                            //case 0
                                            mv.visitLdcInsn(31);
                                            mv.visitInsn(ISHR); //TODO: test all of this
                                            mv.visitJumpInsn(GOTO, tail);
                                            //case 1
                                            mv.visitLabel(case1);
                                            mv.visitInsn(POP);
                                            mv.visitLdcInsn(1);
                                            mv.visitLabel(tail);
                                        }
                                    }
                                }
                            } else { //values are stored as floats
                                if (this.pixelFormat.internalFormat().channelRange() == PixelFormatChannelRange.ZERO_TO_ONE //parameter values are normalized to [0, 1]
                                    && parameterComponentType.signed()) { //input values are signed, set any negative values to zero
                                    mv.visitInsn(DUP);
                                    mv.visitLdcInsn(31);
                                    mv.visitInsn(ISHR);
                                    mv.visitLdcInsn(-1); //NOT
                                    mv.visitInsn(IXOR);
                                    mv.visitInsn(IAND);
                                }

                                parameterComponentType.convertToFloat(mv);
                                mv.visitLdcInsn(parameterComponentType.normalizationFactor());
                                mv.visitInsn(FMUL);
                            }
                        } else { //parameters are either floats or integers, and aren't normalized
                            if (storageTypeType == PixelFormatChannelType.FLOATING_POINT) { //values are stored as floating-point values
                                if (parameterComponentType.integer()) { //convert integer parameters directly to floating-point values
                                    parameterComponentType.convertToFloat(mv);
                                }

                                //clamp values to range
                                switch (this.pixelFormat.internalFormat().channelRange()) {
                                    case ZERO_TO_ONE: //clamp to [0, 1]
                                        mv.visitLdcInsn(0.0f);
                                        mv.visitLdcInsn(1.0f);
                                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PMath.class), "clamp", getMethodDescriptor(FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), PMath.class.isInterface());
                                        break;
                                    case NEGATIVE_ONE_TO_ONE: //clamp to [-1, 1]
                                        mv.visitLdcInsn(-1.0f);
                                        mv.visitLdcInsn(1.0f);
                                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PMath.class), "clamp", getMethodDescriptor(FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), PMath.class.isInterface());
                                        break;
                                    case INFINITY: //no-op
                                        break;
                                    default:
                                        throw new IllegalArgumentException("unknown channel range: " + this.pixelFormat.internalFormat().channelRange());
                                }
                            } else { //values are stored as (unsigned) integers
                                if (!parameterComponentType.integer()) { //convert floating-point parameters to integers, rounding towards negative infinity
                                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PMath.class), "floorI", getMethodDescriptor(INT_TYPE, FLOAT_TYPE), PMath.class.isInterface());
                                }

                                //clamp values to range

                                //set negative values to zero if the storage type is unsigned
                                if (storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER //values stored as unsigned integers
                                    && parameterComponentType.signed()) { //parameter values are signed, set any negative values to zero
                                    //this is a branchless implementation of:
                                    //  (val < 0 ? 0 : val)
                                    //implemented by:
                                    //  (val & ~(val >> 31))

                                    mv.visitInsn(DUP);
                                    mv.visitLdcInsn(31);
                                    mv.visitInsn(ISHR);
                                    mv.visitLdcInsn(-1); //NOT
                                    mv.visitInsn(IXOR);
                                    mv.visitInsn(IAND);
                                }

                                //set any too high values to the maximum {
                                if (!(storageTypeBitDepth == Integer.SIZE && storageTypeType == PixelFormatChannelType.UNSIGNED_INTEGER)) { //don't do this for unsigned 32-bit integers, as it will break (thanks java)
                                    //there are other cases in which this could be skipped, but i'm too lazy to implement them (for now, at least)

                                    mv.visitLdcInsn((1 << storageTypeBitDepth - (storageTypeType == PixelFormatChannelType.INTEGER ? 1 : 0)) - 1);
                                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(Math.class), "min", getMethodDescriptor(INT_TYPE, INT_TYPE, INT_TYPE), Math.class.isInterface());
                                }
                            }
                        }

                        //store the resulting values
                        storer.storeComponent(channelIndex);
                        //mv.visitVarInsn(storageTypeType == PixelFormatChannelType.FLOATING_POINT ? FSTORE : ISTORE, lvtIndexAllocatorStore);
                    }
                });
            });

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }
}
