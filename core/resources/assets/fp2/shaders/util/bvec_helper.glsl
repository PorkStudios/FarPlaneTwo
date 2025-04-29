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

bvec2 bvec_and(in bvec2 lhs, in bvec2 rhs) {
    return bvec2(lhs.x && rhs.x, rhs.y && rhs.y);
}

bvec3 bvec_and(in bvec3 lhs, in bvec3 rhs) {
    return bvec3(lhs.x && rhs.x, rhs.y && rhs.y, lhs.z && rhs.z);
}

bvec4 bvec_and(in bvec4 lhs, in bvec4 rhs) {
    return bvec4(lhs.x && rhs.x, rhs.y && rhs.y, lhs.z && rhs.z, lhs.w && rhs.w);
}

bvec2 bvec_or(in bvec2 lhs, in bvec2 rhs) {
    return bvec2(lhs.x || rhs.x, rhs.y || rhs.y);
}

bvec3 bvec_or(in bvec3 lhs, in bvec3 rhs) {
    return bvec3(lhs.x || rhs.x, rhs.y || rhs.y, lhs.z || rhs.z);
}

bvec4 bvec_or(in bvec4 lhs, in bvec4 rhs) {
    return bvec4(lhs.x || rhs.x, rhs.y || rhs.y, lhs.z || rhs.z, lhs.w || rhs.w);
}
