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

#define _GLIBCXX_DEBUG

#include <cstdint>
#include <jni.h>

#ifndef FP2_MODULE
#error FP2_MODULE must be set!
#endif

//stupid preprocessor workaround
#define FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) \
    extern "C" __attribute__((visibility("default"),flatten)) JNIEXPORT RETURN_TYPE JNICALL Java_net_daporkchop_fp2_ ## MODULE ## _ ## CLASS ## _ ## METHOD_NAME
#define FP2_JNI_EVALUATOR(RETURN_TYPE, MODULE, CLASS, METHOD_NAME) FP2_JNI_PASTER(RETURN_TYPE, MODULE, CLASS, METHOD_NAME)
#define FP2_JNI(RETURN_TYPE, CLASS, METHOD_NAME) FP2_JNI_EVALUATOR(RETURN_TYPE, FP2_MODULE, CLASS, METHOD_NAME)

#define FP2_JNI_HEAD {try{
#define FP2_JNI_TAIL }catch(const fp2::error& err) {fp2::throwException(env, err);}}

namespace fp2 {
    //
    //"throw exception from jni" helper methods
    //

    struct error {
    public:
        const char* _msg;
        jint _code;

        error(const char* msg): _msg(msg), _code(-1) {}

        error(jint code): _msg("<no message>"), _code(code) {}

        error(const char* msg, jint code): _msg(msg), _code(code) {}
    };

    inline void throwException(JNIEnv* env, const error& err)  {
        jclass clazz = env->FindClass("net/daporkchop/lib/natives/NativeException");

        env->Throw((jthrowable) env->NewObject(
            clazz,
            env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;I)V"),
            env->NewStringUTF(err._msg),
            err._code
        ));
    }

    inline void throwException(JNIEnv* env, const char* c, jint i)  {
        throwException(env, error(c, i));
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
        JNIEnv* _env;

    public:
        pinned_array(JNIEnv* env, const JAVA& java): _java(java), _env(env) {
            if (!(_ptr = (T*) _env->GetPrimitiveArrayCritical(_java, nullptr))) {
                throw "unable to pin array!";
            }
        }

        ~pinned_array() {
            _env->ReleasePrimitiveArrayCritical(_java, _ptr, 0);
        }

        T& operator[](const std::size_t idx) {
            return _ptr[idx];
        }
    };

    using pinned_byte_array = pinned_array<jbyteArray, jbyte>;
    using pinned_int_array = pinned_array<jintArray, jint>;
    using pinned_float_array = pinned_array<jfloatArray, jfloat>;
    using pinned_double_array = pinned_array<jdoubleArray, jdouble>;

    /**
     * stupid helper method which simply casts one type to another (for use as a template parameter)
     */
    template <typename A, typename B> B cast(const A a) {
        return (B) a;
    }
}

#endif //FP2_H
