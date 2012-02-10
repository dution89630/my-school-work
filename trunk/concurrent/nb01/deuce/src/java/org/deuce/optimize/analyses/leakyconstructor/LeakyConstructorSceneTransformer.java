package org.deuce.optimize.analyses.leakyconstructor;

import java.util.Map;

import org.deuce.optimize.utils.Logger;

import soot.SceneTransformer;
import soot.Transformer;

public class LeakyConstructorSceneTransformer extends SceneTransformer {

	private static Transformer instance = new LeakyConstructorSceneTransformer();

	public static Transformer v() {
		return instance;
	}

	@Override
	protected void internalTransform(String phaseName, Map options) {
		LeakyConstructorInterprocAnalysis leakyInterprocAnalysis = new LeakyConstructorInterprocAnalysis();
		Logger.println("LCA: "
				+ LeakyConstructorsDatabase.getInstance().toString());

		NewlyAllocatedInCtorTagger tagger = new NewlyAllocatedInCtorTagger();
		tagger.tag();
	}

}
