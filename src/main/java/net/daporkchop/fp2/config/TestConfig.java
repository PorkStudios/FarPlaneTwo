/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author DaPorkchop_
 */
@ToString
@EqualsAndHashCode
public class TestConfig {
    public Submenu submenu = new Submenu();
    public TestEnum e = TestEnum.FIRST;

    public boolean boolOption0 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption1 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption2 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption3 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption4 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption5 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption6 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption7 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption8 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOption9 = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionA = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionB = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionC = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionD = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionE = ThreadLocalRandom.current().nextBoolean();
    public boolean boolOptionF = ThreadLocalRandom.current().nextBoolean();

    enum TestEnum {
        FIRST,
        SECOND,
        THIRD;
    }

    @ToString
    @EqualsAndHashCode
    public class Submenu {
        public boolean boolOption0 = false;
        public boolean boolOption1 = true;
    }
}
