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

#ifndef UTIL_FRUSTUM
#define UTIL_FRUSTUM

//
//
// UNIFORMS
//
//

uniform uint u_ClippingPlaneCount; //TODO: make this a constant and generate specialized shader variants
uniform vec4 u_ClippingPlanes[MAX_CLIPPING_PLANES];

//
//
// UTILITIES
//
//

/*bool testOutsideClippingPlaneOR(in vec3 mn, in vec3 mx, in vec4 plane) {
    return 0.0 >= min(
                    min(
                        min(
                            dot(plane, vec4(mn.x, mn.y, mn.z, 1.0)),
                            dot(plane, vec4(mn.x, mn.y, mx.z, 1.0))),
                        min(
                            dot(plane, vec4(mn.x, mx.y, mn.z, 1.0)),
                            dot(plane, vec4(mn.x, mx.y, mx.z, 1.0)))),
                    min(
                        min(
                            dot(plane, vec4(mx.x, mn.y, mn.z, 1.0)),
                            dot(plane, vec4(mx.x, mn.y, mx.z, 1.0))),
                        min(
                            dot(plane, vec4(mx.x, mx.y, mn.z, 1.0)),
                            dot(plane, vec4(mx.x, mx.y, mx.z, 1.0)))));
}*/

bool testOutsideClippingPlaneAND(in vec3 mn, in vec3 mx, in vec4 plane) {
    return 0.0 >= max(
                    max(
                        max(
                            dot(plane, vec4(mn.x, mn.y, mn.z, 1.0)),
                            dot(plane, vec4(mn.x, mn.y, mx.z, 1.0))),
                        max(
                            dot(plane, vec4(mn.x, mx.y, mn.z, 1.0)),
                            dot(plane, vec4(mn.x, mx.y, mx.z, 1.0)))),
                    max(
                        max(
                            dot(plane, vec4(mx.x, mn.y, mn.z, 1.0)),
                            dot(plane, vec4(mx.x, mn.y, mx.z, 1.0))),
                        max(
                            dot(plane, vec4(mx.x, mx.y, mn.z, 1.0)),
                            dot(plane, vec4(mx.x, mx.y, mx.z, 1.0)))));
}

/**
 * Checks to see whether or not the given AABB is partially contained by the frustum.
 *
 * @return false if the entire AABB is outside of the frustum, true otherwise
 */
bool isBoxInFrustum(in vec3 min, in vec3 max) {
    for (uint i = 0; i < u_ClippingPlaneCount; i++) {
        if (testOutsideClippingPlaneAND(min, max, u_ClippingPlanes[i])) {
            return false;
        }
    }
    return true;
}

#endif //UTIL_FRUSTUM
