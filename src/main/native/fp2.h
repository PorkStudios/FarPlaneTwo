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

#ifndef FP2_H
#define FP2_H

#include <fp2/fastmod.h>

#include <bits/stdc++.h>
#include <jni.h>

#ifndef FP2_MODULE
#error FP2_MODULE must be set!
#endif

//stupid preprocessor workaround
#define FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) \
    extern "C" __attribute__((visibility("default"))) JNIEXPORT RETURN_TYPE JNICALL Java_net_daporkchop_fp2_ ## MODULE ## _ ## CLASS ## _ ## METHOD_NAME
#define FP2_JNI_EVALUATOR(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME)
#define FP2_JNI(RETURN_TYPE, CLASS, METHOD_NAME) FP2_JNI_EVALUATOR(RETURN_TYPE, FP2_MODULE, CLASS, METHOD_NAME)

//"throw exception from jni" helper methods
namespace fp2 {
    static jint throwException(JNIEnv* env, const char* msg)  {
        jclass clazz = env->FindClass("net/daporkchop/lib/natives/NativeException");

        return env->Throw((jthrowable) env->NewObject(
            clazz,
            env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V"),
            env->NewStringUTF(msg)
        ));
    }

    static jint throwException(JNIEnv* env, const char* msg, jint err)  {
        jclass clazz = env->FindClass("net/daporkchop/lib/natives/NativeException");

        return env->Throw((jthrowable) env->NewObject(
            clazz,
            env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;I)V"),
            env->NewStringUTF(msg),
            err
        ));
    }

    static jint throwException(JNIEnv* env, const char* msg, jlong err)  {
        jclass clazz = env->FindClass("net/daporkchop/lib/natives/NativeException");

        return env->Throw((jthrowable) env->NewObject(
            clazz,
            env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;J)V"),
            env->NewStringUTF(msg),
            err
        ));
    }
}

#endif //FP2_H
