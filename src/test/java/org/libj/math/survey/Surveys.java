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

package org.libj.math.survey;

import java.util.Arrays;

import org.libj.lang.Ansi;
import org.libj.lang.Ansi.Color;
import org.libj.lang.Ansi.Intensity;
import org.libj.lang.Classes;
import org.libj.lang.Strings;
import org.libj.lang.Strings.Align;
import org.libj.math.survey.CaseTest.Case;

public abstract class Surveys {
  private final int variables;
  private final int divisions;
  private final Survey[] surveys;
  private final AuditReport report;
  private int count;

  public Surveys(final Case<?,?,?,?,?>[] cases, final int variables, final int divisions, final int warmup, final AuditReport report) {
    this.variables = variables;
    this.divisions = divisions;
    this.report = report;
    this.count = 0;
    surveys = new Survey[cases.length];
    for (int i = 0; i < cases.length; ++i) {
      surveys[i] = new Survey(cases[i], report, variables, divisions, warmup) {
        @Override
        public int getDivision(final int variable, final Object obj) {
          return Surveys.this.getDivision(variable, obj);
        }
      };
    }
  }

  public abstract int getDivision(int variable, Object obj);

  public void addSample(final int survey, final int variable, final Object obj, final long time) {
    surveys[survey].addSample(variable, obj, time, report);
  }

  public void onSuccess() {
    ++count;
  }

  public void reset() {
    for (int s = 0; s < surveys.length; ++s)
      surveys[s].reset();
  }

  public abstract String key(int variable, int division);

  public void print(final String label, final long ts, final String ... headings) {
    for (int s = 0; s < surveys.length; ++s)
      surveys[s].normalize(count);

    final long[][] min = new long[variables][divisions + 1];
    for (int v = 0; v < variables; ++v)
      Arrays.fill(min[v], 0, divisions, Long.MAX_VALUE);

    final long[][] max = new long[variables][divisions + 1];
    for (int v = 0; v < variables; ++v)
      Arrays.fill(max[v], 0, divisions, Long.MIN_VALUE);

    for (int s = 0; s < surveys.length; ++s) {
      for (int v = 0; v < variables; ++v) {
        for (int d = 0; d < divisions; ++d) {
          final long time = surveys[s].getTimes()[v][d];
          min[v][d] = Math.min(min[v][d], time);
          max[v][d] = Math.max(max[v][d], time);
        }
      }
    }

    for (int v = 0; v < variables; ++v) {
      for (int d = 0; d < divisions; ++d) {
        min[v][divisions] += min[v][d];
        max[v][divisions] += max[v][d];
      }
    }

    final String[][] columns = new String[2 + surveys.length][];
    if (report != null) {
      String[] rows = columns[0] = new String[1 + 2];
      rows[0] = "";
      rows[1] = "T";
      rows[2] = Classes.getProperSimpleName(int[].class);

      for (int s = 0; s < surveys.length; ++s) {
        final Survey survey = surveys[s];
        rows = columns[1 + s] = new String[1 + 2];
        rows[0] = headings[s];
        rows[1] = String.valueOf(survey.getAllocs(survey.getSubject()));
        rows[2] = String.valueOf(survey.getAllocs(int[].class));
      }
    }
    else {
      final int[][] counts = surveys[0].getCounts();
      String[] rows = columns[0] = new String[1 + 2 * variables + variables * divisions];
      rows[0] = "precision";
      for (int v = 0; v < variables; ++v)
        for (int d = 0; d < divisions; ++d)
          rows[1 + v + d * variables] = key(v, d);

      rows = columns[1] = new String[1 + 2 * variables + variables * divisions];
      rows[0] = "count";
      for (int v = 0; v < variables; ++v)
        for (int d = 0; d < divisions; ++d)
          rows[1 + v + d * variables] = String.valueOf(counts[v][d]);

      rows[rows.length - 1 - variables] = Ansi.apply("sum:", Intensity.BOLD, Color.CYAN);
      rows[rows.length - 1] = Ansi.apply("% fstr:", Intensity.BOLD, Color.CYAN);
      for (int s = 0, c = 0, tot = 0, lo = 0, hi = 0; s < surveys.length; ++s, tot = 0, lo = 0, hi = 0) {
        final Survey survey = surveys[s];
        rows = columns[s + 2] = new String[1 + 2 * variables + variables * divisions];
        rows[0] = headings[s];
        final long[][] times = survey.getTimes();
        for (int v = 0, sum = 0; v < variables; ++v, tot += sum, sum = 0) {
          for (int d = 0; d < divisions; ++d) {
            final long time = times[v][d];
            sum += time;
            c = 1 + v + d * variables;
            rows[c] = String.valueOf(time);
            color(rows, c, time, min[v][d], max[v][d], s, surveys.length);
          }

          // Output the "sum" row
          c = 1 + v + divisions * variables;
          rows[c] = String.valueOf(sum);
          color(rows, c, sum, min[v][divisions], max[v][divisions], s, surveys.length);
          lo += min[v][divisions];
          hi += max[v][divisions];
        }

        // Output the "% fstr" row
        final int percent = (int)(100d * hi / tot - 100);
        rows[c + variables] = String.valueOf(percent >= 0 ? "+" + percent : percent);
        color(rows, c + variables, tot, lo, hi, s, surveys.length);
      }
    }

    System.out.println(label + "\n  " + count + " in " + ts + "ms\n" + Strings.printTable(true, Align.RIGHT, variables, false, columns));
  }

  private static void color(final String[] rows, final int c, final long time, final long min, final long max, final int s, final int len) {
    if (time == min)
      rows[c] = Ansi.apply(rows[c], Intensity.BOLD, Color.GREEN);
    else if (len > 2 ? s >= len - 2 : s >= len - 1)
      rows[c] = Ansi.apply(rows[c], Intensity.BOLD, time == max ? Color.RED : Color.YELLOW);
    else
      rows[c] = Ansi.apply(rows[c], Intensity.BOLD, Color.WHITE);
  }
}