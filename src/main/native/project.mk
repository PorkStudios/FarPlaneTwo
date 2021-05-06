export SHELL		:=	/bin/bash

export NPROC		:=	$(shell nproc)

export ROOT_DIR		:=	$(CURDIR)
export NATIVE_DIR   :=  $(ROOT_DIR)/src/main/native
export TOOLCHAIN_DIR	:=	$(NATIVE_DIR)/toolchain
export LIBS_DIR		:=	$(NATIVE_DIR)/lib
export COMPILE_DIR  :=  $(ROOT_DIR)/build/native
export OUTPUT_DIR   :=  $(ROOT_DIR)/src/main/resources/net/daporkchop/fp2

export CFLAGS		:=	-O2 -ffast-math -fPIC -ffunction-sections -fdata-sections -fvisibility=hidden -msse4.1
export CXXFLAGS		:=	$(CFLAGS) -std=c++17
export LDFLAGS		:=	$(CFLAGS) -shared -Wl,--gc-sections

export HEADERFILES	:=	$(wildcard $(NATIVE_DIR)/*.h) $(wildcard $(NATIVE_DIR)/fp2/*.h) $(wildcard $(NATIVE_DIR)/fp2/**/*.h)
export INCLUDES		:=	$(NATIVE_DIR) $(JAVA_HOME)include $(JAVA_HOME)include/linux

export ARCHS		:=	x86_64-linux-gnu x86_64-w64-mingw32
export MODULES		:=  compat/cwg/noise compat/vanilla/biome/layer/c

export LIBS			:=  vectorclass-2.01.03
export LIB_TASKS	:=  $(addprefix $(LIBS_DIR)/,$(addsuffix .dl,$(LIBS)))

.PHONY: build clean .FORCE

build: $(foreach arch,$(ARCHS),build.$(arch)) $(LIB_TASKS)

build.%: .FORCE $(foreach module,$(MODULES),%,$(module).lib)
	@echo built libraries for $(shell echo '$@' | perl -n -e '/build\.(.+)/ && print $$1')!

%.lib: .FORCE $(LIB_TASKS)
	@_PRJ_NAME=$(shell echo "$@" | perl -n -e '/,(.*?)\.lib$$/ && print $$1') && \
		_ARCH=$(shell echo "$@" | perl -n -e '/^([^,]*?),.*?\.lib$$/ && print $$1') && \
		$(MAKE) --no-print-directory -C $(NATIVE_DIR) -f $(NATIVE_DIR)/module.mk ARCH=$$_ARCH MODULE=$$_PRJ_NAME build \

clean:
	@[ ! -d $(COMPILE_DIR) ] || rm -r $(COMPILE_DIR)
	@for f in $(MODULES); do [ ! -d $(OUTPUT_DIR)/$$f ] || rm -r $(OUTPUT_DIR)/$$f; done

%.dl:
	@[ -d $(LIBS_DIR) ] || mkdir -p $(LIBS_DIR)
	@echo "downloading source for $(basename $(notdir $@))"
	@curl -o - https://cloud.daporkchop.net/programs/source/$(basename $(notdir $@)).tar.gz | tar zxf -
	@mv $(basename $(notdir $@))/ $(LIBS_DIR)
	@touch $@

.FORCE:
