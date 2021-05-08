# workaround for gcc bug: https://stackoverflow.com/questions/43152633/invalid-register-for-seh-savexmm-in-cygwin
export CXX		:=	x86_64-w64-mingw32-g++ -ffixed-xmm16 -ffixed-xmm17 -ffixed-xmm18 -ffixed-xmm19 -ffixed-xmm20 -ffixed-xmm21 -ffixed-xmm22 -ffixed-xmm23 -ffixed-xmm24 -ffixed-xmm25 -ffixed-xmm26 -ffixed-xmm27 -ffixed-xmm28 -ffixed-xmm29 -ffixed-xmm30 -ffixed-xmm31
export LD		:=	x86_64-w64-mingw32-g++ -static-libgcc -static-libstdc++ -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive

export EXT	:=	dll
