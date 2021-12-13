include $(TOOLCHAIN_DIR)/simd/x86_64/AVX2.mk
export CXX					:= $(CXX) -mavx512f -mfma
export SIMD_REGISTER_WIDTH	:= 512
