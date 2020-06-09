/* Copyright (c) 2020 LibJ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.libj.math;

import static org.junit.Assert.*;
import static org.libj.math.LongDecimal.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.libj.lang.Buffers;
import org.libj.lang.Numbers;
import org.libj.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongDecimalCodecTest extends LongDecimalTest {
  private static final Logger logger = LoggerFactory.getLogger(LongDecimalCodecTest.class);

  private static void testEncodeDecode(final long value, final short scale, final byte bits, final long[] time) {
    final long maxValue = pow2[63 - bits];
    final boolean expectOverflow = value < 0 ? value < -maxValue : value > maxValue;

    if (debug) {
      logger.info("Value: " + Buffers.toString(value) + " " + value);
      logger.info("Scale: " + Buffers.toString(scale).substring(Buffers.toString(scale).length() - bits) + " " + scale + " " + bits);
    }

    final long defaultValue = random.nextLong();
    long ts = System.nanoTime();
    final long encoded = encode(value, scale, bits, defaultValue);
    time[0] += System.nanoTime() - ts;
    if (expectOverflow) {
      if (encoded != defaultValue)
        fail("Expected IllegalArgumentException: " + value + ", " + scale + ", " + bits);

      return;
    }

    if (debug)
      logger.info("Encod: " + Buffers.toString(encoded));

    ts = System.nanoTime();
    final long decodedValue = decodeValue(encoded, bits);
    final short decodedScale = decodeScale(encoded, bits);
    time[1] += System.nanoTime() - ts;

    if (debug) {
      logger.info("DeVal: " + Buffers.toString(decodedValue));
      logger.info("DeSca: " + Buffers.toString(decodedScale));
    }

    assertEquals("value=" + value + ", scale=" + scale + ", bits=" + bits, value, decodedValue);
    assertEquals("value=" + value + ", scale=" + scale + ", bits=" + bits, scale, decodedScale);
  }

  private static String formatOverrunPoint(final BigDecimal val, final int cut) {
    final String str = val.toString();
    if (str.length() <= cut)
      return str;

    final String a = str.substring(0, cut);
    final String b = str.substring(cut);
    return a + "." + b + " " + b.length() + " " + (FastMath.log2(b.length()) + 2);
  }

  @Test
  public void testMulOverrunPoints() {
    boolean flip = false;
    for (int i = 63, j = 63; i >= 1;) {
      final BigDecimal a = BigDecimals.TWO.pow(i);
      final BigDecimal b = BigDecimals.TWO.pow(j);
      logger.info(Strings.padLeft(i + " + " + j, 8) + " " + Strings.padLeft(String.valueOf(i + j), 4) + " " + formatOverrunPoint(a.multiply(b), i <= 55 ? 18 : 17));
      if (flip)
        --i;
      else
        --j;

      flip = !flip;
    }
  }

  @Test
  public void testEncodeDecode() {
    final long[] time = new long[2];
    final long value = 415720947668033403L;
    final short scale = -1;
    final byte bits = 3;
    testEncodeDecode(value, scale, bits, time);

    int count = 0;
    for (byte b = 0; b < 16; ++b)
      for (short s = 0; s <= maxScale[b]; s += Math.random() * 10)
        for (int i = 0; i < numTests / 100; ++i, ++count)
          testEncodeDecode(random.nextLong(), s, b, time);

    logger.info("LongDecimal.testEncodeDecode(): encode=" + (time[0] / count) + "ns, decode=" + (count / time[1]) + "/ns");
  }

  private static final int count = 2000000;

  @Test
  public void testSwap() {
    long a = 0L;
    long b = 0L;

    long ts;
    long tmp;

    long time1 = 0;
    long time2 = 0;
    for (int j = 0; j < 20; ++j) {
      if (j % 2 == 0) {
        a = random.nextLong();
        b = random.nextLong();
      }

      for (int i = 0; i < count; ++i) {
        if (j % 2 == 0) {
          ts = System.nanoTime();
          tmp = a;
          a = b;
          b = tmp;
          time1 += System.nanoTime() - ts;
        }
        else {
          ts = System.nanoTime();
          a ^= b;
          b ^= a;
          a ^= b;
          time2 += System.nanoTime() - ts;
        }
      }

      if (j < 6) {
        time1 = 0;
        time2 = 0;
      }
    }

    logger.info("tmp: " + time1);
    logger.info("xor: " + time2);
//    assertTrue(time1 < time2);
  }

  @Test
  public void testMaxValue() {
    final long[] maxValue = new long[LongDecimal.minScale.length];
    for (byte b = 0; b < maxValue.length; ++b)
      maxValue[b] = LongDecimal.maxValue(b);

    long ts;
    long tmp = 0L;

    long time1 = 0;
    long time2 = 0;
    for (int j = 0; j < 20; ++j) {
      for (int i = 0; i < count; ++i) {
        if (j % 2 == 0) {
          ts = System.nanoTime();
          tmp = maxValue[(byte)(i % maxValue.length)];
          time1 += System.nanoTime() - ts;
        }
        else {
          ts = System.nanoTime();
          tmp = LongDecimal.maxValue((byte)(i % maxValue.length));
          time2 += System.nanoTime() - ts;
        }
      }

      if (j < 6) {
        time1 = 0;
        time2 = 0;
      }
    }

    assertNotEquals(0, tmp);
    logger.info("array: " + time1);
    logger.info("shift: " + time2);
    assertTrue(time1 > time2);
  }

  @Test
  public void testMaxDigits() {
    final byte[] maxDigits = new byte[LongDecimal.minScale.length];
    for (byte b = 0; b < maxValue.length; ++b)
      maxDigits[b] = Numbers.precision(LongDecimal.maxValue(b));

    long ts;
    byte tmp = 0;

    long time1 = 0;
    long time2 = 0;
    for (int j = 0; j < 20; ++j) {
      for (int i = 0; i < count; ++i) {
        if (j % 2 == 0) {
          final byte b = (byte)(i % maxValue.length);
          ts = System.nanoTime();
          tmp = maxDigits[b];
          time1 += System.nanoTime() - ts;
        }
        else {
          final byte b = (byte)(i % maxValue.length);
          final long maxValue = LongDecimal.maxValue(b);
          ts = System.nanoTime();
          tmp = Numbers.precision(maxValue);
          time2 += System.nanoTime() - ts;
        }
      }

      if (j < 6) {
        time1 = 0;
        time2 = 0;
      }
    }

    assertNotEquals(0, tmp);
    logger.info("array: " + time1);
    logger.info("funct: " + time2);
    assertTrue(time1 < time2);
  }
}