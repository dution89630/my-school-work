package org.deuce.transform.asm;

import java.util.EnumSet;

import org.deuce.optimize.main.Analyzer;
import org.deuce.optimize.main.settings.AnalyzerSettings;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.transaction.tl2opt.TransactionManagerImpl;

public class AgentOpt {
	public static void main(String[] args) throws Exception {
		String inputFilename = args[0];
		String outputFilename = args[1];

		System.setProperty(
				"org.deuce.transaction.transactionManagerClass", 
				TransactionManagerImpl.class.getName());
		
		int lastIndexOfDot = inputFilename.lastIndexOf(".");

		Analyzer.getInstance().reset();
		
		anaOpt(inputFilename, lastIndexOfDot, AnalyzerSettings.DontAnalyze,
				OptimizerSettings.CumImmNicNlTlRomLoopsRoWoIpCp,
				outputFilename);
	}
	
	private static void anaOpt(String inputFilename, int lastIndexOfDot,
			AnalyzerSettings analyzeSettings,
			EnumSet<OptimizerSettings> optimizeSettings, String outputFilename) 
				throws Exception {

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
		
		Agent.main(new String[] { transformedJar, outputFilename });
		System.out.println("3---> Ending Instrumentation: " + transformedJar
				+ " => " + outputFilename);

	}
}
