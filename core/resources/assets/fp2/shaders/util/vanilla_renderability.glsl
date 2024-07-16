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
// BUFFERS
//
//

// Vanilla renderability index

//TODO: find a non-hacky way of implementing this
layout(std430) readonly restrict buffer VANILLA_RENDERABILITY_SSBO_NAME {
    ivec3 offset;
    ivec3 size;

    int _padding; //std430 layout is weird lol

    uint flags[];
} vanilla_renderability_state;

//
//
// UTILITIES
//
//

// vanilla renderability tests

bool isVanillaRenderableLevel0(in ivec3 chunkPos) {
    ivec3 tableOffset = vanilla_renderability_state.offset;
    ivec3 tableSize = vanilla_renderability_state.size;

    //offset the given chunk position by the table offset
    ivec3 offsetPos = chunkPos + tableOffset;

    //clamp coordinates to the table size (this is safe because the edges are always false)
    offsetPos = min(max(offsetPos, 0), tableSize - 1);

    //compute bit index in the table
    int idx = (offsetPos.x * tableSize.y + offsetPos.y) * tableSize.z + offsetPos.z;

    //extract the bit at the given index
    return (vanilla_renderability_state.flags[idx >> 5] & (1 << idx)) != 0;
}

#endif //UTIL_VANILLA_RENDERABILITY
