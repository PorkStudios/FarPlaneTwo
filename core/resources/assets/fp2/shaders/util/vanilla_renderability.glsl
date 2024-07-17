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

#ifndef UTIL_VANILLA_RENDERABILITY
#define UTIL_VANILLA_RENDERABILITY

//
//
// UNIFORMS
//
//

layout(std140) uniform U_VanillaRenderability {
    uvec4 offset; //this is actually a uvec3
    uvec4 size; //this is actually a uvec3
} u_VanillaRenderability;

//
//
// BUFFERS
//
//

layout(std430) readonly restrict buffer B_VanillaRenderability {
    uint flags[];
} b_VanillaRenderability;

//
//
// UTILITIES
//
//

// vanilla renderability tests

bool isVanillaRenderableLevel0(in ivec3 chunkPos) {
    uvec3 tableOffset = u_VanillaRenderability.offset.xyz;
    uvec3 tableSize = u_VanillaRenderability.size.xyz;

    //offset the given chunk position by the table offset
    uvec3 offsetPos = uvec3(chunkPos) + tableOffset;

    //check if the chunk's coordinates are in-bounds (if not, the chunk is obviously not renderable by vanilla and can be skipped).
    //  this is equivalent to (offsetPos < 0 || offsetPos >= tableSize).
    if (any(greaterThanEqual(offsetPos, tableSize))) {
        return false;
    }

    //compute bit index in the table
    uint idx = (offsetPos.x * tableSize.y + offsetPos.y) * tableSize.z + offsetPos.z;

    //extract the bit at the given index
    return (b_VanillaRenderability.flags[idx >> 5u] & (1u << idx)) != 0u;
}

#endif //UTIL_VANILLA_RENDERABILITY
