export CXX		:=	x86_64-w64-mingw32-g++-posix
export LD		:=	x86_64-w64-mingw32-g++-posix -static-libgcc -static-libstdc++ -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive
export STRIP	:=	x86_64-w64-mingw32-strip

export EXT	:=	dll
