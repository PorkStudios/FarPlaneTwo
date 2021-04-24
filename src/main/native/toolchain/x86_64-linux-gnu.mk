ifndef LDB_NATIVES_DEBUG
# release mode
export CFLAGS		:=	$(CFLAGS) -flto=$(NPROC) -fno-fat-lto-objects
export CXXFLAGS		:=	$(CXXFLAGS) -flto=$(NPROC) -fno-fat-lto-objects
export LDFLAGS		:=	$(LDFLAGS) -flto=$(NPROC) -fno-fat-lto-objects
else
# debug mode
export CFLAGS		:=	$(CFLAGS)
export CXXFLAGS		:=	$(CXXFLAGS)
export LDFLAGS		:=	$(LDFLAGS)
endif

export CC		:= x86_64-linux-gnu-gcc
export CXX		:= x86_64-linux-gnu-g++
export LD		:= x86_64-linux-gnu-g++
export STRIP	:= x86_64-linux-gnu-strip

export EXT	:=	so
