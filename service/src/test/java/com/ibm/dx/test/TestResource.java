/*******************************************************************************
 * Copyright IBM Corp. 2016
 *******************************************************************************/
package com.ibm.dx.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class TestResource {

    private static final int CALLER_STACK_FRAME_INDEX = determineCallerStackFrameIndex();

    public static String getAsString(final String name) throws IOException {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        final String className  = stack[CALLER_STACK_FRAME_INDEX].getClassName();
        final String methodName = stack[CALLER_STACK_FRAME_INDEX].getMethodName();

        final String result = getResourceAsString(className, methodName, name);

        return result;
    }

    public static URL getResourceAsURL(final String name) {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        final String className  = stack[CALLER_STACK_FRAME_INDEX].getClassName();
        final String methodName = stack[CALLER_STACK_FRAME_INDEX].getMethodName();

        return getResourceAsURL(className, methodName, name);
    }

    public static String getResourceAsString(final String className, final String methodName, final String name) throws IOException {
        final URL url = getResourceAsURL(className, methodName, name);

        final StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(url.openStream(), "utf-8")) {
            final char[] buffer = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buffer)) > 0) {
                sb.append(buffer, 0, charsRead);
            }
        }

        return sb.toString();
    }

    private static URL getResourceAsURL(final String className, final String methodName, final String name) {
        return TestResource.class.getClassLoader().getResource(className + "/" + methodName + "/" + name);
    }

    /**
     * Unfortunately the different JVMs return different stacks.
     * The IBM JVM for example has one internal call layer more then the OpenJDK.
     */
    private static int determineCallerStackFrameIndex() {
        final String ownClassName = TestResource.class.getName();

        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stack.length - 1; ++i) {
            if (ownClassName.equals(stack[i].getClassName())) {
                return i + 1;
            }
        }

        throw new IllegalStateException();
    }
}
