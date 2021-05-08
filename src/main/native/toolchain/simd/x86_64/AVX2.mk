include $(TOOLCHAIN_DIR)/simd/x86_64/AVX.mk
export CXX					:= $(CXX) -mavx2 -mfma