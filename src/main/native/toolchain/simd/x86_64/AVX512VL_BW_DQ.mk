include $(TOOLCHAIN_DIR)/simd/x86_64/AVX512F.mk
export CXX					:= $(CXX) -mavx512vl -mavx512bw -mavx512dq