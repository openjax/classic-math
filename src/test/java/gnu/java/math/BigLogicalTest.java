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

package gnu.java.math;

import java.math.BigInteger;

import org.junit.Test;
import org.libj.math.AbstractTest;

public class BigLogicalTest extends AbstractTest {
  @Override
  public double rangeCoverage() {
    return 0.00000000008;
  }

  @Test
  public void testAnd() {
    testRange(
      s("BigInt", BigInt::new, BigInt::new, (BigInt a, BigInt b) -> a.and(b), String::valueOf),
      s("BigInteger", BigInteger::new, BigInteger::new, (BigInteger a, BigInteger b) -> a.and(b), String::valueOf)
    );
  }

  @Test
  public void testOr() {
    testRange(
      s("BigInt", BigInt::new, BigInt::new, (BigInt a, BigInt b) -> a.or(b), String::valueOf),
      s("BigInteger", BigInteger::new, BigInteger::new, (BigInteger a, BigInteger b) -> a.or(b), String::valueOf)
    );
  }

  @Test
  public void testNot() {
    testRange(
      s("BigInt", BigInt::new, BigInt::new, (BigInt a, BigInt b) -> a.not(), String::valueOf),
      s("BigInteger", BigInteger::new, BigInteger::new, (BigInteger a, BigInteger b) -> a.not(), String::valueOf)
    );
  }

  @Test
  public void testXor() {
    testRange(
      s("BigInt", BigInt::new, BigInt::new, (BigInt a, BigInt b) -> a.xor(b), String::valueOf),
      s("BigInteger", BigInteger::new, BigInteger::new, (BigInteger a, BigInteger b) -> a.xor(b), String::valueOf)
    );
  }

  @Test
  public void testAndNot() {
    testRange(
      s("BigInt", BigInt::new, BigInt::new, (BigInt a, BigInt b) -> a.andNot(b), String::valueOf),
      s("BigInteger", BigInteger::new, BigInteger::new, (BigInteger a, BigInteger b) -> a.andNot(b), String::valueOf)
    );
  }
}