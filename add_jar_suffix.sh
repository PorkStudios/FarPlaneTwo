#!/bin/bash

if [ ! -d build/libs/ ]; then echo "build/libs/ does not exist!"; exit 1; fi

cd build/libs/
for f in *.jar; do
    mv "$f" "`echo $f $1 | perl -pe 's/^((?:[^-]*?-){2}[^-]*?)((?:-sources)?\.jar) (.+)$/$1-$3$2/g' \
                         | perl -pe 's/^(.*?) (.+)$/$1/g'`"
done
