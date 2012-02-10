package org.deuce.optimize.main;

import java.io.File;

import org.deuce.optimize.utils.FileUtils;
import org.deuce.optimize.utils.JarUtils;

import soot.PhaseOptions;

public class Decompiler {

	public static void main(String[] args) {
		Decompiler decompiler = new Decompiler();
		decompiler.decompileJar(args[0]);
	}

	public void decompileJar(String jarFilename) {
		try {
			JarUtils jarUtils = new JarUtils();
			String finalOutputDir = FileUtils.getFinalOutputDir();
			jarUtils.extractJar(jarFilename, finalOutputDir);
			decompileFolder(finalOutputDir);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void decompileFolder(String folder) {

		disablePhases();

		File file = new File(folder);
		String outputFolder = file.getParent();
		outputFolder = outputFolder + "\\sootOutputIns";

		String[] args = new String[] {
				"-f",
				"dava",
				"-cp",
				folder
				//+ ":"
				//+ "C:\\Users\\Arie\\Documents\\My Dropbox\\Duece\\WS\\Deuce\\bin\\deuceAgent.jar"
				// + ":"
				// +
				// "sootclasses-2.2.3.jar:jasminclasses-2.2.3.jar:polyglotclasses-1.3.2.jar:",
				, "-process-dir", folder, "-d", outputFolder, "-pp",
				"-allow-phantom-refs"
		//, "-w",
		//"-ws", 
		//"-p", "cg.spark", "enabled:true",
		//"enabled:true,cs-demand:true,lazy-pts:false", 
		//"-p", "jb", "use-original-names:true", "-app",
		//"-keep-bytecode-offset",
		//"-p", "wjtp.tn", "enabled:true",
		//"-main-class", getMainClass(),
		//"-v",
		// "--help"
		};

		soot.Main.main(args);
	}

	private String getMainClass() {
		// /return "cases.counters.CountersTester";
		// return "cases.counters.CountersTest";
		// return "cases.sharedobject.SharedObjectTester";
		// return "cases.accesses.AccessesFields";
		return "cases.publish.Publish";
	}

	private void disablePhases() {
		// disablePhase("jb.tt");
		// disablePhase("jb.ls");
		// disablePhase("jb.a");
		// disablePhase("jb.ule");
		// disablePhase("jb.tr");
		// disablePhase("jb.ulp");
		// disablePhase("jb.lns");
		// disablePhase("jb.cp");
	}

	private void disablePhase(String phaseName) {
		PhaseOptions.v().setPhaseOption(phaseName, "enabled:false");
	}

}
