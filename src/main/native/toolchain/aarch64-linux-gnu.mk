export CC		:= aarch64-linux-gnu-gcc -flto=$(NPROC) -fno-fat-lto-objects
export CXX		:= aarch64-linux-gnu-g++ -flto=$(NPROC) -fno-fat-lto-objects
export LD		:= aarch64-linux-gnu-g++ -flto=$(NPROC) -fno-fat-lto-objects
export STRIP	:= aarch64-linux-gnu-strip

export EXT	:=	so
