package org.deuce.optimize.utils;

import soot.SootMethod;

public class Logger {

	private static boolean quiet = false;
	private static boolean appClassesOnly = true;

	public static void println(String string, SootMethod sootMethod) {
		if (quiet)
			return;
		if (appClassesOnly
				&& !sootMethod.getDeclaringClass().isApplicationClass())
			return;
		System.out.println(string);
	}

	public static void println(String string) {
		if (quiet)
			return;
		System.out.println(string);
	}

}
