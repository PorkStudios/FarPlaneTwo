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

//this file is intentionally not #pragma once because it's included conditionally
#ifndef FP2_HELPER_ARB_texture_buffer_object
#define FP2_HELPER_ARB_texture_buffer_object

/*
 * Helper for ARB_texture_buffer_object which automatically switches between the various functions for sampling texture buffers
 */

//
//
// UTILITIES
//
//

#if __VERSION__ >= 140
    //we can use texelFetch() from core GLSL
#   define texelFetchBuffer texelFetch
#elif defined(GL_EXT_gpu_shader4)
    //we can use texelFetchBuffer() from GL_EXT_gpu_shader4
#   define texelFetchBuffer texelFetchBuffer
#else
#   error "GLSL 1.40 or GL_EXT_gpu_shader4 must be supported!"
#endif

#endif //FP2_HELPER_ARB_texture_buffer_object
