package org.deuce.optimize.analyses.fieldactivity;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteInterprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteIntraprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadFlowSet;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadInterprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadIntraprocAnalysis;
import org.deuce.optimize.analyses.readonlymethod.ReadonlyMethodFinder;
import org.deuce.optimize.analyses.readonlymethod.ReadonlyTagger;

import soot.Body;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transformer;

public class FieldActivitySceneTransformer extends SceneTransformer {

	private static Transformer instance = new FieldActivitySceneTransformer();

	public static Transformer v() {
		return instance;
	}

	@Override
	protected void internalTransform(String phaseName, Map options) {
		VirginReadInterprocAnalysis virginReadInterprocAnalysis = new VirginReadInterprocAnalysis();
		DeprivedWriteInterprocAnalysis deprivedWriteInterprocAnalysis = new DeprivedWriteInterprocAnalysis();

		processAndTagFieldActivities(virginReadInterprocAnalysis,
				deprivedWriteInterprocAnalysis);
		processAndTagReadonlyMethods(virginReadInterprocAnalysis);
	}

	private void processAndTagFieldActivities(
			VirginReadInterprocAnalysis virginReadInterprocAnalysis,
			DeprivedWriteInterprocAnalysis deprivedWriteInterprocAnalysis) {
		FieldActivityTagger fieldActivityTagger = new FieldActivityTagger();

		Map<SootMethod, VirginReadIntraprocAnalysis> virginReadAnalyses = virginReadInterprocAnalysis
				.getAllApplicationOnlyAnalyses();
		Map<SootMethod, DeprivedWriteIntraprocAnalysis> deprivedWriteAnalyses = deprivedWriteInterprocAnalysis
				.getAllApplicationOnlyAnalyses();

		Set<SootMethod> methods = new LinkedHashSet<SootMethod>(
				virginReadAnalyses.keySet());
		methods.addAll(deprivedWriteAnalyses.keySet());

		for (SootMethod sootMethod : methods) {
			Body body = sootMethod.getActiveBody();
			VirginReadIntraprocAnalysis virginReadAnalysis = virginReadAnalyses
					.get(sootMethod);
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis = deprivedWriteAnalyses
					.get(sootMethod);
			fieldActivityTagger.tag(sootMethod, body, virginReadAnalysis,
					deprivedWriteAnalysis);
		}
	}

	private void processAndTagReadonlyMethods(
			VirginReadInterprocAnalysis activityInterprocAnalysis) {
		ReadonlyMethodFinder readonlyFinder = new ReadonlyMethodFinder();

		Set<Entry<SootMethod, VirginReadFlowSet>> entrySet = activityInterprocAnalysis
				.getAllFlowAtEnds().entrySet();
		for (Entry<SootMethod, VirginReadFlowSet> entry : entrySet) {
			SootMethod method = entry.getKey();
			Body body = method.getActiveBody();
			VirginReadFlowSet flowAtEnd = entry.getValue();
			readonlyFinder.tag(method, body, flowAtEnd);
		}

		readonlyFinder.propagate();

		ReadonlyTagger tagger = new ReadonlyTagger();
		tagger.tag();
	}

}