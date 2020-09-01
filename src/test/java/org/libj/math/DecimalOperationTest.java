/* Copyright (c) 2020 Seva Safris, LibJ
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

import static org.libj.lang.Strings.Align.*;
import static org.libj.math.Decimal.*;
import static org.libj.math.FixedPoint.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Random;

import org.libj.console.Ansi;
import org.libj.console.Ansi.Color;
import org.libj.lang.Strings;
import org.libj.test.TestAide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DecimalOperationTest {
  static final Logger logger = LoggerFactory.getLogger(DecimalOperationTest.class);

  static final BigDecimal DEFAULT = BigDecimal.valueOf(Long.MAX_VALUE);
  static final BigDecimal EXPECTED_DEFAULT = BigDecimal.valueOf(Long.MAX_VALUE);
  static final BigDecimal UNEXPECTED_DEFAULT = BigDecimal.valueOf(Long.MAX_VALUE);
  static final BigDecimal EXCEPTION = BigDecimal.valueOf(Long.MAX_VALUE);

  private static final byte maxBits = Decimal.MAX_SCALE_BITS;

  static final Random random = new Random();
  static final int numTests = 10000;
  static final MathContext precision16 = MathContext.DECIMAL64;
  static final DecimalFormat expectedFormatter = new DecimalFormat("0E0");
  static final DecimalFormat epsilonFormatter = new DecimalFormat("0E0");
  public static final long[] pow2 = new long[Long.SIZE];
  public static final BigInteger[] minValue = new BigInteger[Long.SIZE];
  public static final BigInteger[] maxValue = new BigInteger[Long.SIZE];

  private static final long[] preliminary = {0, 1, -1, 10, -10, 100, -100, Long.MAX_VALUE, Long.MIN_VALUE};
  private static final long boundedMin = -100000000;
  private static final long boundedMax = 100000000;
  private static final short boundedScale = 8;
  private static final byte boundedBits = 9;

  private static final BigDecimal errorThreshold = new BigDecimal("1E-2");
  private static final byte restrictScaleBits = -1;

  private static byte testBits = -1;
  private static long testD1 = -1;
  private static long testD2 = -1;

  private static final Path errorPath = Paths.get("target/generated-test-resources/DecimalTest.txt");
  private static final File errorFile;

  static {
    for (byte v = 0; v < minValue.length; ++v) {
      minValue[v] = BigInteger.valueOf(Decimal.minValue(v));
      maxValue[v] = BigInteger.valueOf(Decimal.maxValue(v));
    }

    expectedFormatter.setRoundingMode(RoundingMode.FLOOR);
    expectedFormatter.setMinimumFractionDigits(18);
    expectedFormatter.setPositivePrefix("");
    epsilonFormatter.setRoundingMode(RoundingMode.CEILING);
    epsilonFormatter.setMinimumFractionDigits(0);
    expectedFormatter.setPositivePrefix("");
    for (int i = 0; i < pow2.length; ++i)
      pow2[i] = (long)Math.pow(2, i);

    if (Files.exists(errorPath)) {
      errorFile = errorPath.toFile();
      readErrorFile();
    }
    else {
      errorFile = null;
    }
  }

  private static void clearErrorFile() {
    if (errorFile != null)
      errorFile.delete();
  }

  private static void readErrorFile() {
    try (final BufferedReader in = new BufferedReader(new FileReader(errorFile))) {
      testD1 = Long.parseLong(in.readLine());
      testD2 = Long.parseLong(in.readLine());
      testBits = Byte.parseByte(in.readLine());
      logVariables(testD1, testD2, testBits);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void logVariables(final long d1, final long d2, final byte scaleBits) {
    logger.info(c(Color.RED, "----------------------------------------\nd1: (value = " + d1 + "L, scale = " + scale(d1, scaleBits) + ") d2: (value = " + d2 + "L, scale = " + scale(d2, scaleBits) + ") scaleBits = " + scaleBits));
  }

  private static void writeErrorFile(final long d1, final long d2, final byte scaleBits) {
    try {
      errorPath.toFile().getParentFile().mkdirs();
      Files.write(errorPath, (d1 + "\n" + d2 + "\n" + scaleBits).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String c(final Ansi.Color color, final String str) {
    return Ansi.apply(str, color);
  }

  static BigDecimal D(final String str) {
    return BigDecimals.intern(str);
  }

  static long randomValue(final byte maxValuePower) {
    final long maxValue = pow2[maxValuePower];
    return (long)((Math.random() < 0.5 ? -1 : 1) * random.nextDouble() * maxValue);
  }

  static void test(final DecimalOperation<?,?> operation) {
    final long[] time = new long[2];
    final boolean[] failures = new boolean[16];
    final BigDecimal[] errors = new BigDecimal[16];
    int count = test(operation, true, time, errors, failures);
    count += test(operation, false, time, errors, failures);
    operation.print(count, time, errors, failures);
  }

  private static String format(final String d) {
    final int i = d.lastIndexOf("E+");
    return i > -1 ? d.substring(0, i + 1) + d.substring(i + 2) : d;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static int test(final DecimalOperation<?,?> operation, final boolean bounded, final long[] time, final BigDecimal[] errors, final boolean[] failures) {
    int count = 0;
    byte fromBits = 0;
    byte toBits = maxBits;
    if (restrictScaleBits != -1) {
      fromBits = restrictScaleBits;
      toBits = restrictScaleBits;
    }

    for (int i = 0; i < numTests; ++i) {
      for (byte b = fromBits; b < toBits; ++b) {
        final long defaultValue = random.nextLong();
        final long d1;
        final long d2;
        final byte scaleBits;
        if (testD1 != -1) {
          scaleBits = testBits;
          d1 = testD1;
          d2 = testD2;
        }
        else if (bounded) {
          scaleBits = boundedBits;
          final short scale = (short)(random.nextDouble() * boundedScale * scaleBits % boundedScale);
          d1 = operation.randomBounded(boundedMin, boundedMax, scale, scaleBits);
          d2 = operation.randomBounded(boundedMin, boundedMax, scale, scaleBits);
        }
        else {
          scaleBits = b;
          if (i < preliminary.length) {
            long v1 = preliminary[i];
            long v2 = preliminary[i];
            final short s1, s2;
            if (v1 == Long.MAX_VALUE || v1 == Long.MIN_VALUE) {
              s1 = 0;
              s2 = 0;
            }
            else {
              s1 = (short)(Math.random() * Decimal.maxScale(scaleBits));
              v1 *= FastMath.E10[(int)Math.min(random.nextDouble() * s1, FastMath.E10.length - 1)];
              s2 = (short)(Math.random() * Decimal.maxScale(scaleBits));
              v2 *= FastMath.E10[(int)Math.min(random.nextDouble() * s2, FastMath.E10.length - 1)];
            }

            d1 = operation.randomBounded(v1, v1, s1, scaleBits);
            d2 = operation.randomBounded(v2, v2, s2, scaleBits);
          }
          else {
            d1 = operation.randomEncoded(scaleBits);
            d2 = operation.randomEncoded(scaleBits);
          }
        }

        final BigDecimal bd1 = toBigDecimal(d1, scaleBits);
        final BigDecimal bd2 = toBigDecimal(d2, scaleBits);
        final long v1 = value(d1, scaleBits);
        final short s1 = scale(d1, scaleBits);
        final long v2 = value(d2, scaleBits);
        final short s2 = scale(d2, scaleBits);
        Object actual = defaultValue;
        Object expected = defaultValue;
        BigDecimal error;
        try {
          actual = operation.test(d1, d2, bd1, bd2, scaleBits, defaultValue, time);
          expected = operation.control(bd1, bd2, time);
          error = ((DecimalOperation)operation).run(bd1, bd2, expected, actual, testBits != -1 ? testBits : scaleBits, defaultValue, errors, failures);

          // FIXME: This is done to focus on the biggest errors first
          if (errorThreshold != null && error != null && errors[scaleBits] != null && errors[scaleBits].compareTo(errorThreshold) < 0)
            error = null;
        }
        catch (final Exception e) {
          e.printStackTrace();
          error = EXCEPTION;
        }

        if (error == DEFAULT) {
          clearErrorFile();
          continue;
        }

        ++count;
        if (error == null) {
          clearErrorFile();
        }
        else if (TestAide.isInDebug()) {
          for (int j = 0; j < 100; ++j) {
            if (j == 0)
              writeErrorFile(d1, d2, scaleBits);

            logVariables(d1, d2, scaleBits);
            final String actu;
            if (actual instanceof Long) {
//              final short s = decodeScale(((Long)actual).longValue(), scaleBits);
//              expected = ((BigDecimal)expected).setScale(s, RoundingMode.HALF_UP);
              actu = String.valueOf(((Long)actual).longValue() == defaultValue ? "err" : Decimal.toString((Long)actual, scaleBits));
            }
            else {
              actu = String.valueOf(actual);
            }

            final String function = operation.toString(v1, s1, v2, s2);
            final String expe = format(String.valueOf(expected));
            final StringBuilder errorBuilder = new StringBuilder("{ε = ");
            if (error == EXCEPTION) {
              errorBuilder.append("EXCEPTION");
            }
            else if (error == EXPECTED_DEFAULT) {
              errorBuilder.append("EXPECTED DEFAULT");
            }
            else if (error == UNEXPECTED_DEFAULT) {
              errorBuilder.append("UNEXPECTED DEFAULT");
            }
            else {
              final String actualError = c(Color.RED, epsilonFormatter.format(errors[scaleBits]));
              final String expectedError = c(Color.GREEN, epsilonFormatter.format(error));
              errorBuilder.append(actualError).append(" > ").append(expectedError);
            }
            errorBuilder.append('}');

            logger.info(function + " = " + expe + "\n" + Strings.pad("= " + actu, LEFT, 3 + actu.length() + function.length()) + Strings.pad(errorBuilder.toString(), LEFT, 1 + errorBuilder.length() + Math.abs(expe.length() - actu.length())));
            try {
              actual = operation.test(d1, d2, bd1, bd2, scaleBits, defaultValue, time);
            }
            catch (final Throwable t) {
              TestAide.printStackTrace(System.err, t);
            }
            ((DecimalOperation)operation).run(bd1, bd2, expected, actual, testBits != -1 ? testBits : scaleBits, defaultValue, errors, failures);
          }
        }
      }
    }

    return count;
  }
}