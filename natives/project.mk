export SHELL		:=	/usr/bin/env bash

export NPROC		:=	$(shell nproc)

export ROOT_DIR		:=	$(CURDIR)
export NATIVE_DIR   :=  $(ROOT_DIR)/natives
export TOOLCHAIN_DIR	:=	$(NATIVE_DIR)/toolchain
export LIBS_DIR		:=	$(NATIVE_DIR)/lib
export COMPILE_DIR  :=  $(ROOT_DIR)/build/native
export OUTPUT_DIR   :=  $(ROOT_DIR)/src/main/resources/net/daporkchop/fp2

export OUTPUT_SRC_PATH	:=	src/main/native/
export OUTPUT_DST_PATH	:=	src/main/resources/net/daporkchop/fp2/

export CXXFLAGS		:=	-O2 -ffast-math -std=c++17 -Wno-attributes
export LDFLAGS		:=	$(CXXFLAGS) -shared

export MAKEFILES_	:=	$(wildcard $(NATIVE_DIR)/*.mk) $(wildcard $(TOOLCHAIN_DIR)/*.mk) $(wildcard $(TOOLCHAIN_DIR)/**/*.mk)
export HEADERFILES	:=	$(wildcard $(NATIVE_DIR)/*.h) $(wildcard $(NATIVE_DIR)/fp2/*.h) $(wildcard $(NATIVE_DIR)/fp2/**/*.h)
export INCLUDES		:=	$(NATIVE_DIR) $(JAVA_HOME)/include $(JAVA_HOME)/include/linux

export ARCHS		:=	x86_64-linux-gnu x86_64-w64-mingw32
export MODULES_STD	:=  src/main/native/compat/x86 src/main/native/client/gl/func/c
export MODULES_SIMD	:=  src/main/native/compat/cwg/noise src/main/native/compat/vanilla/biome/layer/c

export LIBS			:=  vectorclass-2.01.03
export LIB_TASKS	:=  $(addprefix $(LIBS_DIR)/,$(addsuffix .dl,$(LIBS)))

export WIN_LIB_DIR	:=	$(OUTPUT_DIR)/compat/windows
export WIN_LIBS		:=	$(addprefix $(WIN_LIB_DIR)/,libgcc_s_seh-1.dll libwinpthread-1.dll libstdc++-6.dll)

.PHONY: build clean .FORCE

build: $(foreach arch,$(ARCHS),$(arch).arch) $(LIB_TASKS) $(WIN_LIBS)

%.arch: .FORCE $(foreach module,$(subst /,_,$(MODULES_STD)),%/$(module).mod_std) $(foreach module,$(subst /,_,$(MODULES_SIMD)),%/$(module).mod_simd)
	@echo "built all libraries for $*"

%.mod_std: .FORCE $(LIB_TASKS)
	@$(MAKE) --no-print-directory -C $(ROOT_DIR) -f $(NATIVE_DIR)/module.mk ARCH=$(subst /,,$(dir $*)) BASE_ARCH=$(word 1,$(subst -, ,$(subst /,,$(dir $*)))) MODULE=$(subst _,/,$(notdir $*)) build

%.mod_simd: .FORCE $(LIB_TASKS)
	@$(MAKE) --no-print-directory -C $(ROOT_DIR) -f $(NATIVE_DIR)/simd.mk ARCH=$(subst /,,$(dir $*)) BASE_ARCH=$(word 1,$(subst -, ,$(subst /,,$(dir $*)))) MODULE=$(subst _,/,$(notdir $*)) build

clean: $(foreach module,$(subst $(OUTPUT_SRC_PATH),$(OUTPUT_DST_PATH),$(MODULES_STD) $(MODULES_SIMD)),$(ROOT_DIR)/$(module).clean) $(foreach dir,$(COMPILE_DIR) $(WIN_LIB_DIR),$(dir).clean)

%.clean: .FORCE
	@echo "deleting $*"
	@[ ! -d $* ] || rm -r $*

%.dl:
	@[ -d $(LIBS_DIR) ] || mkdir -p $(LIBS_DIR)
	@echo "downloading source for $(basename $(notdir $@))"
	@curl -o - https://cloud.daporkchop.net/programs/source/$(basename $(notdir $@)).tar.gz | tar zxf -
	@mv $(basename $(notdir $@))/ $(LIBS_DIR)
	@touch $@

$(WIN_LIB_DIR)/%:
	@[ -d $(WIN_LIB_DIR) ] || mkdir -p $(WIN_LIB_DIR)
	@echo "copying $*"
	@cp `x86_64-w64-mingw32-g++ -print-file-name=$*` $@

.FORCE:
