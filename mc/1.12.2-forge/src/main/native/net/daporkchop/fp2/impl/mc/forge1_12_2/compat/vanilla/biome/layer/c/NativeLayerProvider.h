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

#include "NativeFastLayer.h"

FP2_JNI(void, NativeLayerProvider, reload0) (JNIEnv* env, jobject obj,
        jint count, jintArray ids, jbyteArray flags, jbyteArray equals, jintArray mutations) FP2_JNI_HEAD
    int32_t ids_length = env->GetArrayLength(ids);
    if (ids_length * sizeof(int32_t) != sizeof(fp2::biome::fastlayer::biome_ids)) {
        throw fp2::error("invalid array length", ids_length);
    }

    env->GetIntArrayRegion(ids, 0, ids_length, (jint*) &fp2::biome::fastlayer::biome_ids);

    fp2::biome::fastlayer::biomes.reload(env, count, flags, equals, mutations);
FP2_JNI_TAIL
