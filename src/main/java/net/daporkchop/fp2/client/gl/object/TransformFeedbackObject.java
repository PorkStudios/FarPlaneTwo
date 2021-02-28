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

package net.daporkchop.fp2.client.gl.object;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL40.*;

/**
 * @author DaPorkchop_
 */
public final class TransformFeedbackObject extends GLObject {
    protected static TransformFeedbackObject CURRENTLY_BOUND = null;

    public TransformFeedbackObject() {
        super(glGenTransformFeedbacks());
    }

    public TransformFeedbackObject bind() {
        checkState(CURRENTLY_BOUND == null, "a transform feedback object is already bound!");
        CURRENTLY_BOUND = this;

        glBindTransformFeedback(GL_TRANSFORM_FEEDBACK, this.id);
        return this;
    }

    @Override
    public void close() {
        checkState(CURRENTLY_BOUND == this, "not bound!");
        CURRENTLY_BOUND = null;

        glBindTransformFeedback(GL_TRANSFORM_FEEDBACK, 0);
    }

    @Override
    protected Runnable delete(int id) {
        return () -> glDeleteTransformFeedbacks(id);
    }

    public void draw(int mode) {
        glDrawTransformFeedback(mode, this.id);
    }
}
