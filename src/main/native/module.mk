include $(TOOLCHAIN_DIR)/$(ARCH).mk

CXXFLAGS        :=  $(CXXFLAGS) -DFP2_MODULE=$(subst /,_,$(MODULE)) $(addprefix -I,$(INCLUDES) $(CURDIR))

HEADERFILES	    :=	$(HEADERFILES) $(wildcard $(MODULE)/*.h) $(wildcard $(MODULE)/**/*.h)
SRCFILES		:=	$(wildcard $(MODULE)/*.cpp) $(wildcard $(MODULE)/**/*.cpp)
OFILES			:=	$(addprefix $(COMPILE_DIR)/$(ARCH)/,$(addsuffix .o,$(SRCFILES)))
OUTPUT_FILE     :=  $(OUTPUT_DIR)/$(MODULE)/$(ARCH).$(EXT)

build: $(OUTPUT_FILE)
	@echo "built $(MODULE) for $(ARCH)"

$(OUTPUT_FILE): $(OFILES)
	@[ -d $(dir $(OUTPUT_FILE)) ] || mkdir -p $(dir $(OUTPUT_FILE))
	@echo "linking $(OUTPUT_FILE)"
	@$(LD) $(LDFLAGS) -o $(OUTPUT_FILE) $(OFILES)
	@echo "stripping $(OUTPUT_FILE)"
	@$(STRIP) $(OUTPUT_FILE)

$(COMPILE_DIR)/$(ARCH)/%.cpp.o: %.cpp $(HEADERFILES)
	@[ -d $(dir $@) ] || mkdir -p $(dir $@)
	@echo "building $@"
	@$(CXX) $(CXXFLAGS) -c $*.cpp -o $@
