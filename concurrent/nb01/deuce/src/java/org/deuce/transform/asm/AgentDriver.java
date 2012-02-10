package org.deuce.transform.asm;

import java.util.EnumSet;

import org.deuce.optimize.main.Analyzer;
import org.deuce.optimize.main.settings.AnalyzerSettings;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;

public class AgentDriver {

	public static void main(String[] args) throws Exception {

		String folder = "";
		folder = "I:\\Arie\\Dropbox\\My Dropbox\\Duece\\WS\\exported\\";
// folder =
// "C:\\Users\\Arie\\Documents\\My Dropbox\\Duece\\WS\\exported\\";
//		folder = "C:\\Documents and Settings\\zilbersa\\My Documents\\My Dropbox\\Duece\\WS\\exported\\";

		for (String jarName : new String[] {//
//		"intruder.jar",
//				"intrudera.jar", //
				"genome.jar",
//				"StmBench2.jar",//
//				"Lee.jar", //
//				"Bayes.jar", //
//		"tiny.jar",
//		"Hash.jar", //
//		"LinkedList.jar", //
//				"SkipList.jar", //
//		"Shash.jar", //
//				"Bank.jar", //
//				"KMeans.jar",//
//				"ssca2.jar",//
//				"MatrixMul.jar",//
//		"Vacation.jar",//			
// "Micro.jar"
//		 "rt1.6.0_12.jar"
//		 "rt1.6.0_17.jar"
// "tinyWithAttributes.jar",

		}) {
			drive(folder, jarName);
		}
	}

	private static void drive(String folder, String jarName) throws Exception {
		String[] args;
		args = new String[] { folder + jarName };

		String filename = args[0];
		// e.g.:"${workspace_loc}\exported\DeuceTests.jar"

		int lastIndexOfDot = filename.lastIndexOf(".");

		Analyzer.getInstance().reset();
//		anaOptNone(filename, lastIndexOfDot);
		anaOptAllCumulative(filename, lastIndexOfDot);
//		anaOptLoops(filename, lastIndexOfDot);
//		anaOptFull(filename, lastIndexOfDot);
//		anaOptPart(filename, lastIndexOfDot);
//		anaOptAllSingletons(filename, lastIndexOfDot);
//		anaOptImmSingleton(filename, lastIndexOfDot);
//		anaOptNlSingleton(filename, lastIndexOfDot);
//		anaOptNewSingleton(filename, lastIndexOfDot);		
//		anaOptRoWoSingleton(filename, lastIndexOfDot);
//		anaOptIpCpSingleton(filename, lastIndexOfDot);
	}

	private static void anaOptNone(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
	}

	private static void anaOptLoops(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustLoops);
	}

	private static void anaOptFull(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.CumImmNicNlTlRomLoopsRoWoIpCp);
	}

	private static void anaOptROMSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRom);
	}

	private static void anaOptLoopsSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustLoops);
	}

	private static void anaOptRoWoSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRoWo);
	}

	private static void anaOptIpCpSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRes);
	}

	private static void anaOptImmSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustImm);
	}

	private static void anaOptNewSingleton(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
//		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
//				OptimizerSettings.JustNic);
//		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
//				OptimizerSettings.JustNl);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNew);
	}

	private static void anaOptAllSingletons(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustImm);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNic);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNl);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustTl);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRom);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustLoops);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRo);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustWo);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustRes);
//		anaOpt(filename, lastIndexOfDot, DontAnalyze,
//				OptimizerSettings.L7PlusStables);
	}

	private static void anaOptAllCumulative(String filename, int lastIndexOfDot)
			throws Exception {
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustNone);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.JustOpt);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.JustImm);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNic);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNl);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTl);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTlRom);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.Analyze,
				OptimizerSettings.CumImmNicNlTlRomLoops);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTlRomLoopsRo);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTlRomLoopsRoWo);
		anaOpt(filename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTlRomLoopsRoWoIpCp);
//anaOpt(filename, lastIndexOfDot, DontAnalyze,
//		OptimizerSettings.L7PlusStables);
	}

	private static void anaOpt(String inputFilename, int lastIndexOfDot,
			AnalyzerSettings analyzeSettings,
			EnumSet<OptimizerSettings> optimizeSettings) throws Exception {

		Settings.getInstance().setAnalyzerSettings(analyzeSettings);
		Settings.getInstance().setOptimizerSettings(optimizeSettings);
		Analyzer analyzer = Analyzer.getInstance();

		// transform
		System.out.println("1---> Beginning Transformation: "
				+ optimizeSettings.toString());
		String transformedJar = analyzer.transformJar(inputFilename);
		System.out.println("1---> Ending Transformation: " + inputFilename
				+ " => " + transformedJar);

		// analyze
		System.out.println("2---> Beginning Analysis: "
				+ optimizeSettings.toString());
		analyzer.analyzeJar(transformedJar);
		System.out.println("2---> Ending Analysis: " + transformedJar);

		// instrument
		System.out.println("3---> Beginning Instrumentation: "
				+ optimizeSettings.toString());
		String instrumentedJar = inputFilename.substring(0, lastIndexOfDot)
				+ "" + optimizeSettings.toString() + ".jar";

		Agent.main(new String[] { transformedJar, instrumentedJar });
		System.out.println("3---> Ending Instrumentation: " + transformedJar
				+ " => " + instrumentedJar);

	}

}