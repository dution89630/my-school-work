package org.deuce.optimize.utils;

import java.io.File;
import java.security.SecureRandom;

public class FileUtils {
	static final SecureRandom random = new SecureRandom();
	private static long num;

	public static void generateNewTempDirName() {
		num = Math.abs(random.nextLong()) % 1000;
	}

	private static String getCurrentDir() {
		return System.getProperty("user.dir");
	}

	//	the root of all the work we do
	public static String getTempDir() {
		return getCurrentDir() + "\\temp\\" + Long.toString(num);
	}

	// the place where the original jar is unzipped into
	public static String getUnjarDir() {
		return getTempDir() + "\\unjar";
	}

	public static String getInlineDir() {
		return getTempDir() + "\\inline";
	}

	// the place where soot outputs the bytecode after its optimization phase.
	// zipped for processing by the agent. 
	public static String getPreDir() {
		return getTempDir() + "\\pre";
	}

	// the place where soot outputs the bytecode after our deuce-optimizations phase.
	// not subsequently used.
	public static String getFinalOutputDir() {
		return getTempDir() + "\\final";
	}

	public static boolean deleteFolder(String destDir) {
		System.out.println(String.format("Deleting %s...", destDir));
		File dir = new File(destDir);
		return deleteFolder(dir);
	}

	private static boolean deleteFolder(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteFolder(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	public static boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}
}
