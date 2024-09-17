/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-$today.year DaPorkchop_
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

#pragma once

/*
 * Helper for ARB_shader_draw_parameters which automatically switches between the ARB and core variable names.
 */

//
//
// UTILITIES
//
//

#if __VERSION__ >= 460
    //we can use the variables from the core profile
    int get_gl_DrawID() { return int(gl_DrawID); }
    int get_gl_BaseVertex() { return int(gl_BaseVertex); }
    int get_gl_BaseInstance() { return int(gl_BaseInstance); }
#elif defined(GL_ARB_shader_draw_parameters)
    //we can use the variables from the extension
    int get_gl_DrawID() { return int(gl_DrawIDARB); }
    int get_gl_BaseVertex() { return int(gl_BaseVertexARB); }
    int get_gl_BaseInstance() { return int(gl_BaseInstanceARB); }
#else
#   error "GL_ARB_shader_draw_parameters must be supported!"
#endif
