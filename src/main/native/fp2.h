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

#include <jni.h>

#ifndef FP2_MODULE
#error FP2_MODULE must be set!
#endif

//stupid preprocessor workaround
#define FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) \
    extern "C" __attribute__((visibility("default"))) JNIEXPORT RETURN_TYPE JNICALL Java_net_daporkchop_fp2_ ## MODULE ## _ ## CLASS ## _ ## METHOD_NAME
#define FP2_JNI_EVALUATOR(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME)
#define FP2_JNI(RETURN_TYPE, CLASS, METHOD_NAME) FP2_JNI_EVALUATOR(RETURN_TYPE, FP2_MODULE, CLASS, METHOD_NAME)

namespace fp2 {
    static inline JNIEnv* ENV;

    inline void init(JNIEnv* env) {
        fp2::ENV = env;
    }

    //
    //"throw exception from jni" helper methods
    //

    inline void throwException(const char* msg)  {
        jclass clazz = ENV->FindClass("net/daporkchop/lib/natives/NativeException");

        ENV->Throw((jthrowable) ENV->NewObject(
            clazz,
            ENV->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V"),
            ENV->NewStringUTF(msg)
        ));
        throw 0;
    }

    inline void throwException(const char* msg, jint err)  {
        jclass clazz = ENV->FindClass("net/daporkchop/lib/natives/NativeException");

        ENV->Throw((jthrowable) ENV->NewObject(
            clazz,
            ENV->GetMethodID(clazz, "<init>", "(Ljava/lang/String;I)V"),
            ENV->NewStringUTF(msg),
            err
        ));
        throw 0;
    }

    inline void throwException(const char* msg, jlong err)  {
        jclass clazz = ENV->FindClass("net/daporkchop/lib/natives/NativeException");

        ENV->Throw((jthrowable) ENV->NewObject(
            clazz,
            ENV->GetMethodID(clazz, "<init>", "(Ljava/lang/String;J)V"),
            ENV->NewStringUTF(msg),
            err
        ));
        throw 0;
    }

    /**
     * Fancy helper class for JNI array pinning.
     *
     * @author DaPorkchop_
     */
    template<typename JAVA, typename T> class pinned_array {
    private:
        T* _ptr;
        const JAVA& _java;

    public:
        pinned_array(const JAVA& java): _java(java) {
            if (!(_ptr = (T*) ENV->GetPrimitiveArrayCritical(_java, nullptr))) {
                throwException("unable to pin array!");
            }
        }

        ~pinned_array() {
            ENV->ReleasePrimitiveArrayCritical(_java, _ptr, 0);
        }

        T& operator[](const std::size_t idx) {
            return _ptr[idx];
        }
    };

    using pinned_int_array = pinned_array<jintArray, jint>;
}

#endif //FP2_H
