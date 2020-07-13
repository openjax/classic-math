package org.libj.math.survey;

import java.lang.reflect.Executable;

import org.libj.lang.Classes;

public class Rule {
  private final String className;
  private final String methodSignature;
  private final Class<?>[] trackClasses;
  private final boolean isClone;

  public Rule(final Executable executable, final Class<?>[] trackClasses) {
    this.className = executable.getDeclaringClass().getName();
    final StringBuilder builder = new StringBuilder();
    if (className.equals(executable.getName())) {
      builder.append("<init>");
      isClone = false;
    }
    else {
      builder.append(executable.getName());
      isClone = "clone".equals(executable.getName()) && executable.getParameterCount() == 0;
    }

    if (executable.getParameterCount() != 0) {
      builder.append('(');
      final Class<?>[] parameterTypes = executable.getParameterTypes();
      for (int i = 0; i < parameterTypes.length; ++i) {
        if (i > 0)
          builder.append(',');

        builder.append(Classes.getProperSimpleName(parameterTypes[i]));
      }

      builder.append(')');
    }

    this.methodSignature = builder.toString();
    this.trackClasses = trackClasses;
  }

  @Override
  public int hashCode() {
    return className.hashCode() ^ methodSignature.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof Rule))
      return false;

    final Rule that = (Rule)obj;
    if (!className.equals(that.className))
      return false;

    if (!methodSignature.equals(that.methodSignature))
      return false;

    return true;
  }

  private void toString(final StringBuilder builder, final Class<?> cls) {
    builder.append("RULE ").append(className).append(".").append(methodSignature).append(" [").append(cls.getName()).append("]\n");
    builder.append("CLASS ").append(className).append('\n');
    builder.append("METHOD ").append(methodSignature).append('\n');
    builder.append("HELPER org.jboss.byteman.tests.helpers.Default\n");
    builder.append("AT NEW ").append(cls.isArray() ? cls.getComponentType().getSimpleName() + " []" : cls.getSimpleName()).append(" ALL\n");
    builder.append("IF true\n");
//    builder.append("DO traceln(\"-----> \" + $CLASS + \" \" + $NEWCLASS);\n");
    builder.append("DO org.libj.math.survey.AuditReport.allocate($CLASS, $NEWCLASS)\n");
    builder.append("ENDRULE\n");
  }

  private void toStringClone(final StringBuilder builder) {
    builder.append("RULE ").append(className).append(".").append(methodSignature).append(" [CLONE]\n");
    builder.append("CLASS ").append(className).append('\n');
    builder.append("METHOD ").append(methodSignature).append('\n');
    builder.append("AT EXIT\n");
    builder.append("IF true\n");
    builder.append("DO org.libj.math.survey.AuditReport.allocate($1, $1)\n");
    builder.append("ENDRULE\n");
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (final Class<?> trackClass : trackClasses)
      if (trackClass != null)
        toString(builder, trackClass);

    if (isClone)
      toStringClone(builder);

    System.out.println(builder);
    return builder.toString();
  }
}