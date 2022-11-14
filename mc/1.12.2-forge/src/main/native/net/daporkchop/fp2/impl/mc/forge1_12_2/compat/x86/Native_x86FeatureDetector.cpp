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

#include <fp2.h>
#include <vectorclass-2.01.03/instrset_detect.cpp>

FP2_JNI(jstring, Native_1x86FeatureDetector, maxSupportedVectorExtension)(JNIEnv* env, jobject obj) FP2_JNI_HEAD
    auto instrset = instrset_detect();
    switch (instrset) {
        case 1:
            return env->NewStringUTF("SSE");
        case 2:
            return env->NewStringUTF("SSE2");
        case 3:
            return env->NewStringUTF("SSE3");
        case 4:
            return env->NewStringUTF("SSSE3");
        case 5:
            return env->NewStringUTF("SSE4.1");
        case 6:
            return env->NewStringUTF("SSE4.2");
        case 7:
            if (!hasFMA3()) {
                return env->NewStringUTF("AVX");
            } else {
                return env->NewStringUTF("AVX_FMA");
            }
        case 8:
            if (!hasFMA3()) {
                throw fp2::error("the CPU reports that it has AVX2, but not FMA3!");
            } else {
                return env->NewStringUTF("AVX2");
            }
        case 9:
            if (!hasFMA3()) {
                throw fp2::error("the CPU reports that it has AVX512F, but not FMA3!");
            } else {
                return env->NewStringUTF("AVX512F");
            }
        case 10:
            if (!hasFMA3()) {
                throw fp2::error("the CPU reports that it has AVX512VL_BW_DQ, but not FMA3!");
            } else {
                return env->NewStringUTF("AVX512VL_BW_DQ");
            }
        default: //none or unknown
            return env->NewStringUTF("");
    }
FP2_JNI_TAIL
