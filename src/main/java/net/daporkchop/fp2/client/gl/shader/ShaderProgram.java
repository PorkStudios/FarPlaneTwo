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

package net.daporkchop.fp2.client.gl.shader;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicInteger;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Basic wrapper around a shader.
 *
 * @author DaPorkchop_
 */
@Getter
abstract class ShaderProgram<I extends ShaderProgram<I>> implements AutoCloseable {
    protected final String name;
    protected int id;

    //stores the id in an object that isn't this object to allow this to be garbage collected
    //the value is used by the cleaner to delete the shader program after this is GC'd
    protected final AtomicInteger idReference = new AtomicInteger();

    /**
     * Creates a new shader program by linking the given shaders with each other.
     *
     * @param name the program's name
     * @param vert the vertex shader
     * @param geom the geometry shader
     * @param frag the fragment shader
     * @param comp the compute shader
     */
    protected ShaderProgram(@NonNull String name, Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying) {
        this.name = name;

        //allocate program
        this.idReference.set(this.id = glCreateProgram());

        //allow garbage-collection of program
        AtomicInteger idReference = this.idReference;
        PCleaner.cleaner(this, () -> Minecraft.getMinecraft().addScheduledTask(() -> glDeleteProgram(idReference.get())));

        this.link(this.id, vert, geom, frag, comp, xfb_varying);
    }

    private void link(int id, Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying) {
        if (vert != null) {
            checkArg(vert.type == ShaderType.VERTEX, "vert must be a VERTEX shader (%s)", vert.type);
        }
        if (geom != null) {
            checkArg(geom.type == ShaderType.GEOMETRY, "geom must be a GEOMETRY shader (%s)", geom.type);
        }
        if (frag != null) {
            checkArg(frag.type == ShaderType.FRAGMENT, "frag must be a FRAGMENT shader (%s)", frag.type);
        }
        if (comp != null) {
            checkArg(comp.type == ShaderType.COMPUTE, "comp must be a COMPUTE shader (%s)", comp.type);
        }

        //attach shaders
        if (vert != null) {
            glAttachShader(id, vert.id);
        }
        if (geom != null) {
            glAttachShader(id, geom.id);
        }
        if (frag != null) {
            glAttachShader(id, frag.id);
        }
        if (comp != null) {
            glAttachShader(id, comp.id);
        }

        //register transform feedback varyings
        if (xfb_varying != null) {
            glTransformFeedbackVaryings(id, xfb_varying, GL_INTERLEAVED_ATTRIBS);
        }

        //link and validate
        glLinkProgram(id);
        ShaderManager.validateProgramLink(this.name, id);
        glValidateProgram(id);
        ShaderManager.validateProgramValidate(this.name, id);
    }

    protected void reload(Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying) {
        //attempt to link new code
        int newId = glCreateProgram();
        try {
            this.link(newId, vert, geom, frag, comp, xfb_varying);
        } catch (Exception e) {
            glDeleteProgram(newId);
            throw e;
        }

        //replace old shader program with newly linked one
        int oldId = this.id;
        this.idReference.set(this.id = newId);

        //delete old program
        glDeleteProgram(oldId);
    }

    /**
     * Gets the location of a uniform value in the fragment shader.
     *
     * @param name the uniform's name
     * @return the uniform's location
     */
    public int uniformLocation(@NonNull String name) {
        return glGetUniformLocation(this.id, name);
    }

    /**
     * Binds this shader for use when rendering.
     * <p>
     * This method returns itself, for use in a try-with-resources block.
     */
    public I use() {
        glUseProgram(this.id);
        return uncheckedCast(this);
    }

    @Override
    public void close() {
        glUseProgram(0);
    }
}
