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

#include <"fp2:shaders/util/bvec_helper.glsl"> // bvec_and()
#include <"fp2:shaders/util/camera_state_uniforms.glsl"> // u_modelViewProjectionMatrix, u_positionFrac, u_positionFloor

void aabbCornersToClipSpace(in vec3 bmin, in vec3 bsize, out vec4 clipSpaceCorners[8]) {
    vec4 SX = u_modelViewProjectionMatrix * vec4(bsize.x, 0.0, 0.0, 0.0);
    vec4 SY = u_modelViewProjectionMatrix * vec4(0.0, bsize.y, 0.0, 0.0);
    vec4 SZ = u_modelViewProjectionMatrix * vec4(0.0, 0.0, bsize.z, 0.0);

    vec4 P0 = u_modelViewProjectionMatrix * vec4(bmin.x, bmin.y, bmin.z, 1.0);
    vec4 P1 = P0 + SZ;
    vec4 P2 = P0 + SY;
    vec4 P3 = P2 + SZ;
    vec4 P4 = P0 + SX;
    vec4 P5 = P4 + SZ;
    vec4 P6 = P4 + SY;
    vec4 P7 = P6 + SZ;

    clipSpaceCorners[0] = P0;
    clipSpaceCorners[1] = P1;
    clipSpaceCorners[2] = P2;
    clipSpaceCorners[3] = P3;
    clipSpaceCorners[4] = P4;
    clipSpaceCorners[5] = P5;
    clipSpaceCorners[6] = P6;
    clipSpaceCorners[7] = P7;
}

#if FP2_REVERSEDZ
const bool CLIP_CONTROL_ZERO_TO_ONE = true; //TODO: make this changeable?
#else
const bool CLIP_CONTROL_ZERO_TO_ONE = false; //TODO: make this changeable?
#endif

const bool DEPTH_CLAMP_ENABLED = false; //TODO: make this changeable?

bool isClippedNear(in vec4 c) {
    float zm = CLIP_CONTROL_ZERO_TO_ONE ? 0.0 : -c.w;
    return !DEPTH_CLAMP_ENABLED && zm > c.z;
}

bool isClippedFar(in vec4 c) {
    return c.z > c.w;
}

bool isClippedSides(in vec4 c) {
    return -c.w > c.x || c.x > c.w ||
           -c.w > c.y || c.y > c.w;
}

bool clipTestSinglePoint(in vec4 c) {
    float zm = CLIP_CONTROL_ZERO_TO_ONE ? 0.0 : -c.w;
    return -c.w <= c.x && c.x <= c.w &&
           -c.w <= c.y && c.y <= c.w &&
           (DEPTH_CLAMP_ENABLED || (zm <= c.z && c.z <= c.w));
}

/**
 * Checks to see whether or not the given AABB is partially contained by the frustum.
 *
 * @return false if the entire AABB is outside of the frustum, true otherwise
 */
bool isBoxInFrustum(in vec3 min, in vec3 size, out vec4 clipSpacePositions[8]) {
    aabbCornersToClipSpace(min, size, clipSpacePositions);

    // check how the bounding box resides regarding to the view frustum
    uint outOfBound[6] = uint[6](0u, 0u, 0u, 0u, 0u, 0u);

    for (uint i = 0; i < 8u; i++) {
        if (clipSpacePositions[i].x >  clipSpacePositions[i].w) outOfBound[0]++;
        if (clipSpacePositions[i].x < -clipSpacePositions[i].w) outOfBound[1]++;
        if (clipSpacePositions[i].y >  clipSpacePositions[i].w) outOfBound[2]++;
        if (clipSpacePositions[i].y < -clipSpacePositions[i].w) outOfBound[3]++;
        if (!DEPTH_CLAMP_ENABLED) {
            if (clipSpacePositions[i].z >  clipSpacePositions[i].w) outOfBound[4]++;
            if (clipSpacePositions[i].z < (CLIP_CONTROL_ZERO_TO_ONE ? 0.0 : -clipSpacePositions[i].w)) outOfBound[5]++;
        }
    }

    for (uint i = 0; i < 6u; i++) {
        if (outOfBound[i] == 8u) {
            return false;
        }
    }
    return true;

    //TODO: this code should be identical, but isn't

    //perform clip space test against all points in the AABB.
    //  there are 6 inequalities which must all be true for a point to pass the clip test:
    //    - -w <= x <= w
    //    - -w <= y <= w
    //    - -w <= z <= w
    //  if any of these inequalities is false for all 8 corners, the AABB is entirely outside the view frustum.

    //notes:
    //  - if glClipControl() has set the depth clip mode to GL_ZERO_TO_ONE, the z clipping equations are
    //    changed to '0.0 <= z <= w'.
    //  - if GL_DEPTH_CLAMP is enabled, the z clipping equations are ignored.

    bvec3 allFailed_min = bvec3(true);
    bvec3 allFailed_max = bvec3(true);
    for (uint i = 0; i < 8u; i++) {
        vec4 clipSpacePosition = clipSpacePositions[i];

        bvec3 passed_min = lessThanEqual(vec3(-clipSpacePosition.w), clipSpacePosition.xyz);
        bvec3 passed_max = lessThanEqual(clipSpacePosition.xyz, vec3(clipSpacePosition.w));

        if (CLIP_CONTROL_ZERO_TO_ONE) {
            passed_min.z = 0.0 <= clipSpacePosition.z;
        }
        if (DEPTH_CLAMP_ENABLED) {
            passed_min.z = true;
            passed_max.z = true;
        }

        bvec3 failed_min = not(passed_min);
        bvec3 failed_max = not(passed_max);

        allFailed_min = bvec_and(allFailed_min, failed_min);
        allFailed_max = bvec_and(allFailed_max, failed_max);
    }

    if (any(allFailed_min) || any(allFailed_max)) {
        return false;
    }

    return true;
}
