package org.deuce.optimize.main.settings;

import java.util.EnumSet;

public class Settings {

	private static Settings instance = new Settings();
	private EnumSet<OptimizerSettings> optimizerSettings = EnumSet
			.noneOf(OptimizerSettings.class);
	private AnalyzerSettings analyzerSettings = null;

	public boolean isOptEnabled(OptimizerSettings optimizerSettings) {
		return this.optimizerSettings.contains(optimizerSettings);
	}

	public EnumSet<OptimizerSettings> getOptimizerSettings() {
		return optimizerSettings;
	}

	public void setOptimizerSettings(
			EnumSet<OptimizerSettings> optimizerSettings) {
		this.optimizerSettings = optimizerSettings;
	}

	public AnalyzerSettings getAnalyzerSettings() {
		return analyzerSettings;
	}

	public void setAnalyzerSettings(AnalyzerSettings analyzerSettings) {
		this.analyzerSettings = analyzerSettings;
	}

	public static Settings getInstance() {
		return instance;
	}

}