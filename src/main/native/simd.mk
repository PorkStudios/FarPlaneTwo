.PHONY: build .FORCE

include $(TOOLCHAIN_DIR)/simd/$(BASE_ARCH).mk

build: $(foreach ext,$(SIMD_EXTENSIONS),$(ext).simd_ext)
	@echo "built all simd targets for $(MODULE) on $(ARCH) ($(SIMD_EXTENSIONS))"

%.simd_ext: .FORCE
	@$(MAKE) --no-print-directory -C $(NATIVE_DIR) -f $(NATIVE_DIR)/module.mk SIMD_EXT=$* build
