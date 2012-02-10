package org.deuce.optimize.main;

import soot.Pack;
import soot.PackManager;

public class Main {
	public static void main(String[] args) {
		// Inject the analysis tagger into Soot
		Pack pack = PackManager.v().getPack("jtp");
		/*
		 * .add( new Transform("jtp.myanalysistagger2", MyAnalysisTagger
		 * .instance()));
		 */
		// Invoke soot.Main with arguments given
		
		args = new String[]
		                   {
				"-f", "jimple", "cases.basic.TestHello", "-p", "jb use-original-names:true"
		                   };
		
		soot.Main.main(args);

		}
}
