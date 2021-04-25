export CC		:= x86_64-linux-gnu-gcc -flto=$(NPROC) -fno-fat-lto-objects
export CXX		:= x86_64-linux-gnu-g++ -flto=$(NPROC) -fno-fat-lto-objects
export LD		:= x86_64-linux-gnu-g++ -flto=$(NPROC) -fno-fat-lto-objects
export STRIP	:= x86_64-linux-gnu-strip

export EXT	:=	so
