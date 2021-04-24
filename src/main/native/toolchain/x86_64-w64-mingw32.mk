#windows gets no LTO because it's shit
#why do people use it
#seriously
export CFLAGS		:=	$(CFLAGS)
export CXXFLAGS		:=	$(CXXFLAGS)
export LDFLAGS		:=	$(LDFLAGS) -static-libgcc -static-libstdc++ -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive

export CC		:=	x86_64-w64-mingw32-gcc-posix
export CXX		:=	x86_64-w64-mingw32-g++-posix
export LD		:=	x86_64-w64-mingw32-g++-posix
export STRIP	:=	x86_64-w64-mingw32-strip

export EXT	:=	dll
