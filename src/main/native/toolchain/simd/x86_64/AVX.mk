include $(TOOLCHAIN_DIR)/simd/x86_64/SSE4.2.mk
export CXX					:= $(CXX) -mavx -fabi-version=0
export SIMD_REGISTER_WIDTH	:= 256