package org.deuce.optimize.analyses.pre.lcm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.utils.Logger;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transformer;
import soot.util.Chain;

public class LazyCodeMotionSceneTransformer extends SceneTransformer {

	private static Transformer instance = new LazyCodeMotionSceneTransformer();


	public static Transformer v() {
		return instance;
	}
	
	@Override
	protected void internalTransform(String phaseName, Map options) {
		options= new LinkedHashMap();
		options.put("enabled", "true");
		options.put("safety", "unsafe");
		options.put("unroll", "true");
		options.put("memory-accesses-removed", 0);
		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		for (SootClass sootClass : applicationClasses) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (sootMethod.hasActiveBody()){
					Body activeBody = sootMethod.getActiveBody();
					LazyCodeMotion.v().transform(activeBody, "wjtp.lcms", options);										
				}
			}
		}

		Integer total = (Integer) options.get("memory-accesses-removed");
		Logger.println("LCMS: " + total + " memory accesses were removed in total.");
	}


}
