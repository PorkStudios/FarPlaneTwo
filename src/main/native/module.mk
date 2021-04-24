include $(TOOLCHAIN_DIR)/$(ARCH).mk

SRCFILES		:=	$(wildcard $(MODULE)/*.cpp) $(wildcard $(MODULE)/**/*.cpp)
OFILES			:=	$(addprefix $(COMPILE_DIR)/$(ARCH)/,$(addsuffix .o,$(SRCFILES)))

INCLUDE			:=	$(addprefix -I,$(INCLUDES) $(CURDIR))

OUTPUT_FILE     :=  $(OUTPUT_DIR)/$(MODULE)/$(ARCH).$(EXT)

build: $(OUTPUT_FILE)
	@echo "built $(MODULE) for $(ARCH)"

$(OUTPUT_FILE): $(OFILES)
	@[ -d $(dir $(OUTPUT_FILE)) ] || mkdir -p $(dir $(OUTPUT_FILE))
	@echo "linking $(OUTPUT_FILE)"
	@$(LD) $(LDFLAGS) $(INCLUDE) -o $(OUTPUT_FILE) $(OFILES)
	@echo "stripping $(OUTPUT_FILE)"
	@$(STRIP) $(OUTPUT_FILE)

$(COMPILE_DIR)/$(ARCH)/%.cpp.o: %.cpp
	@[ -d $(dir $@) ] || mkdir -p $(dir $@)
	@echo "building $@"
	@$(CXX) $(CXXFLAGS) $(INCLUDE) -c $*.cpp -o $@
