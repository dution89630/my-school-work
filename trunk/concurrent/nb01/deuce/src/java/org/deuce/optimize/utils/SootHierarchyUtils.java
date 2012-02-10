package org.deuce.optimize.utils;

import soot.SootClass;

public class SootHierarchyUtils {
	public static boolean implementsInterfaceRecursive(SootClass cl,
			String interfaceName) {
		if (cl.implementsInterface(interfaceName))
			return true;
		else {
			if (cl.hasSuperclass())
				return implementsInterfaceRecursive(cl.getSuperclass(),
						interfaceName);
			else
				return false;
		}
	}
}
