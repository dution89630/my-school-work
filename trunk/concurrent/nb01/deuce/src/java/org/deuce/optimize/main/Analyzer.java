package org.deuce.optimize.main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.jar.Manifest;

import org.deuce.optimize.analyses.alwaysatomic.AlwaysAtomicSceneTransformer;
import org.deuce.optimize.analyses.alwaysatomic.NotAlwaysAtomicDatabase;
import org.deuce.optimize.analyses.atomicstarter.AtomicStarterSceneTransformer;
import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.fieldactivity.FieldActivityDatabase;
import org.deuce.optimize.analyses.fieldactivity.FieldActivitySceneTransformer;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.SootMethodDatabase;
import org.deuce.optimize.analyses.general.StmtsDatabase;
import org.deuce.optimize.analyses.immutables.FieldsMutabilityDatabase;
import org.deuce.optimize.analyses.immutables.ImmutableFieldsAccessesDatabase;
import org.deuce.optimize.analyses.immutables.ImmutableFieldsSceneTransformer;
import org.deuce.optimize.analyses.inline.StaticInliner;
import org.deuce.optimize.analyses.lastfieldactivity.LastFieldActivityDatabase;
import org.deuce.optimize.analyses.lastfieldactivity.LastFieldActivitySceneTransformer;
import org.deuce.optimize.analyses.leakyconstructor.LeakyConstructorSceneTransformer;
import org.deuce.optimize.analyses.leakyconstructor.LeakyConstructorsDatabase;
import org.deuce.optimize.analyses.leakyconstructor.NewlyAllocatedInCtorDatabase;
import org.deuce.optimize.analyses.newlocals.NewLocalsDatabase;
import org.deuce.optimize.analyses.newlocals.NewLocalsSceneTransformer;
import org.deuce.optimize.analyses.newlyallocated.NewlyAllocatedDatabase;
import org.deuce.optimize.analyses.newlyallocated.NewlyAllocatedSceneTransformer;
import org.deuce.optimize.analyses.pre.lcm.LazyCodeMotion;
import org.deuce.optimize.analyses.pre.lcm.LazyCodeMotionSceneTransformer;
import org.deuce.optimize.analyses.puremethod.PureMethodSceneTransformer;
import org.deuce.optimize.analyses.puremethod.PureMethodsDatabase;
import org.deuce.optimize.analyses.readonlymethod.ReadonlyActivityDatabase;
import org.deuce.optimize.analyses.readonlymethod.ReadonlyMethodDatabase;
import org.deuce.optimize.analyses.rescoping.TransactionRescopingSceneTransformer;
import org.deuce.optimize.analyses.rescoping.lastfieldactivity.CommitPointDatabase;
import org.deuce.optimize.analyses.stable.StableReadsDatabase;
import org.deuce.optimize.analyses.stable.StableReadsSceneTransformer;
import org.deuce.optimize.analyses.threadescape.ThreadEscapeDatabase;
import org.deuce.optimize.analyses.threadescape.ThreadEscapeSceneTransformer;
import org.deuce.optimize.main.settings.AnalyzerSettings;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.optimize.utils.FileUtils;
import org.deuce.optimize.utils.JarUtils;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.OptimizerException;

import soot.G;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.Transformer;

public class Analyzer {

	private static Analyzer instance = new Analyzer();
	private String tempDir;
	private String unjarDir;
	private String inlineDir;
	private String preDir;
	private String finalOutputDir;

	public static Analyzer getInstance() {
		return instance;
	}

	private Analyzer() {
		reset();
	}

	public void reset() {
		FileUtils.generateNewTempDirName();
		tempDir = FileUtils.getTempDir();
		unjarDir = FileUtils.getUnjarDir();
		inlineDir = FileUtils.getInlineDir();
		preDir = FileUtils.getPreDir();
		finalOutputDir = FileUtils.getFinalOutputDir();
		FileUtils.deleteFolder(tempDir);
	}

	private void addAllAnalyses() {
		addAlwaysAtomicAnalysis();
		addThreadEscapeAnalysis();
		addLeakyConstructorAnalysis();
		addImmutableFieldsAnalysis();
		addAtomicStartersAnalysis();
		addFieldsActivityAnalysis();
		addNewLocalsAnalysis();
		addPureMethodsAnalysis();
		addLastFieldActivityAnalysis();
		addFirstFieldActivityAnalysis();
		addStableReadsAnalysis();
	}

	private void addLastFieldActivityAnalysis() {
		addToJimpleAndShimple("lfaat", LastFieldActivitySceneTransformer.v());
	}

	private void addFirstFieldActivityAnalysis() {
		addToJimpleAndShimple("ffaat", TransactionRescopingSceneTransformer.v());
	}

	private void addImmutableFieldsAnalysis() {
		addToJimpleAndShimple("ifat", ImmutableFieldsSceneTransformer.v());
	}

	private void addAlwaysAtomicAnalysis() {
		addToJimpleAndShimple("aaat", AlwaysAtomicSceneTransformer.v());
	}

	private void addPureMethodsAnalysis() {
		addToJimpleAndShimple("pmat", PureMethodSceneTransformer.v());
	}

	private void addAtomicStartersAnalysis() {
		addToJimpleAndShimple("asat", AtomicStarterSceneTransformer.v());
	}

	private void addFieldsActivityAnalysis() {
		addToJimpleAndShimple("faat", FieldActivitySceneTransformer.v());
	}

	private void addNewlyAllocatedAnalysis() {
		addToJimpleAndShimple("naat", NewlyAllocatedSceneTransformer.v());
	}

	private void addThreadEscapeAnalysis() {
		addToJimpleAndShimple("teat", ThreadEscapeSceneTransformer.v());
	}

	private void addLeakyConstructorAnalysis() {
		addToJimpleAndShimple("lcat", LeakyConstructorSceneTransformer.v());
	}

	private void addNewLocalsAnalysis() {
		addToJimpleAndShimple("nlat", NewLocalsSceneTransformer.v());
	}

	private void addStableReadsAnalysis() {
		addToJimpleAndShimple("srat", StableReadsSceneTransformer.v());
	}

	private void addWholeProgramLCM() {
		addToJimpleAndShimple("lcms", LazyCodeMotionSceneTransformer.v());
	}

	private void addSiteInliner() {
		addToJimpleAndShimple("sis", StaticInliner.v());
	}

	private void addToJimpleAndShimple(String name, Transformer transformer) {
		addToJimple(name, transformer);
		addToShimple(name, transformer);
	}

	private void addToShimple(String name, Transformer transformer) {
		Pack packS = PackManager.v().getPack("wstp");
		Transform transformS = new Transform("wstp." + name, transformer);
		packS.add(transformS);
	}

	private void addToJimple(String name, Transformer transformer) {
		Pack packJ = PackManager.v().getPack("wjtp");
		Transform transformJ = new Transform("wjtp." + name, transformer);
		packJ.add(transformJ);
	}

	public void analyzeJar(String jarFilename) {
		AnalyzerSettings analyzerSettings = Settings.getInstance()
				.getAnalyzerSettings();
		if (analyzerSettings == AnalyzerSettings.DontAnalyze) {
			return;
		} else if (analyzerSettings == AnalyzerSettings.Analyze) {
			try {
				JarUtils jarUtils = new JarUtils();
				jarUtils.extractJar(jarFilename, preDir);
				jarUtils.extractJar(jarFilename, finalOutputDir);

				analyzeFolderWithDeuceOptimizations(preDir, finalOutputDir);
			} catch (Exception e) {
				throw new OptimizerException("Cannot optimize.", e);
			}
		} else {
			throw new OptimizerException();
		}
	}

	private void replaceSootLCM() {
		Pack pack = PackManager.v().getPack("jop");
		Iterator iterator = pack.iterator();
		while (iterator.hasNext()) {
			Object next = iterator.next();
			Transform t = (Transform) next;
			if (t.getPhaseName().equals("jop.lcm")) {
				iterator.remove();
			}
		}
		pack.insertAfter(new Transform("jop.lcm", LazyCodeMotion.v()),
				"jop.bcm");
//		pack.insertAfter(new Transform("jop.asst",
//				AtomicStarterSceneTransformer.v()), "jop.bcm");
	}

	private void analyzeFolderWithDeuceOptimizations(String inDir, String outDir) {
		String[] args = new String[] {
				"-cp",
				inDir
						+ ";c:\\Program Files\\Java\\jdk1.6.0_17\\jre\\lib\\jce.jar",
				"-process-dir", inDir, "-d", outDir,// 
				"-pp",// 
				// "-w",//
				"-ws", "-p", "cg.spark", "enabled:true",
				// "-p",//
				// "cg",//
				// "all-reachable:true",//
				"-p",//
				"cg",//
				"safe-newinstance:true,safe-forname:true,verbose:false",//
				// "-p",//
				// "cg.spark",//
				// "cs-demand:true",//
				// "verbose:true",//
				"-p", "jb", "use-original-names:true", "-print-tags", //
//				"-p", "jop", "enabled:true",
// "-p", "jop.bcm", "enabled:true",
// "-app",
// "-allow-phantom-refs",
				"-keep-bytecode-offset",
		// "-main-class", getMainClass(),
		};
		Logger.println("Running soot on folder " + inDir
				+ " (full Deuce optimizations)");
		G.reset();
		clearDatabases();
		G.v().out = System.out;
		addAllAnalyses();
		soot.Main.main(args);
	}

	private void clearDatabases() {
		for (IDatabase database : new IDatabase[] {
				AtomicStartersDatabase.getInstance(),
				FieldActivityDatabase.getInstance(),
				NotAlwaysAtomicDatabase.getInstance(),
				FieldsMutabilityDatabase.getInstance(),
				ImmutableFieldsAccessesDatabase.getInstance(),
				LastFieldActivityDatabase.getInstance(),
				LeakyConstructorsDatabase.getInstance(),
				NewLocalsDatabase.getInstance(),
				NewlyAllocatedDatabase.getInstance(),
				NewlyAllocatedInCtorDatabase.getInstance(),
				PureMethodsDatabase.getInstance(),
				ReadonlyActivityDatabase.getInstance(),
				ReadonlyMethodDatabase.getInstance(),
				SootMethodDatabase.getInstance(), StmtsDatabase.getInstance(),
				ThreadEscapeDatabase.getInstance(),
				StableReadsDatabase.getInstance(),
				CommitPointDatabase.getInstance(), }) {
			database.clear();
		}
	}

	private String getMainClass() {
		// /return "cases.counters.CountersTester";
		// return "cases.counters.CountersTest";
		// return "cases.sharedobject.SharedObjectTester";
		// return "cases.accesses.AccessesFields";
		// return "cases.publish.Publish";
		// return "org.deuce.benchmark.Driver";
		return "stmbench7.Benchmark";
	}

	public String transformJar(String inputFilename) {
		String workFilename = inputFilename;

		EnumSet<OptimizerSettings> optimizerSettings = Settings.getInstance()
				.getOptimizerSettings();
		if (Settings.getInstance().isOptEnabled(OptimizerSettings.L10Opt)) {
			workFilename = getInlinedVersion(workFilename);
		}
		if (Settings.getInstance().isOptEnabled(OptimizerSettings.L20Loops)) {
			workFilename = getPreVersion();
		}

		return workFilename;
	}

	private String getInlinedVersion(String jarFilename) {
		String inlineFilename = tempDir + "\\inline.jar";
		if (FileUtils.fileExists(inlineFilename))
			return inlineFilename;

		try {
			JarUtils jarUtils = new JarUtils();
			Manifest manifest = jarUtils.extractJar(jarFilename, unjarDir);
			jarUtils.extractJar(jarFilename, inlineDir);

			inlineCallSites(unjarDir, inlineDir);

			jarUtils.createJar(inlineDir, inlineFilename, manifest);
			return inlineFilename;

		} catch (Exception e) {
			throw new OptimizerException("Cannot optimize.", e);
		}
	}

	private void inlineCallSites(String inDir, String outDir) {
		Logger.println("Running soot on folder " + inDir + " (inline)");
		String[] args = new String[] { "-cp", inDir, "-process-dir", inDir,
				"-d", outDir, "-pp", "-p", "jb", "use-original-names:true",
				"-keep-line-number", "-keep-bytecode-offset", //
				"-w", //
//				"-p", "wjop", "enabled:true", //
//				"-p", "wjop.si", "enabled:true",//
				"-p", "cg.spark", "enabled:true",// 
				"-p", "wjtp.sis", "enabled:true", //
// "-print-tags", //
				"-optimize", };

		G.v().out = new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {

			}
		});
		G.reset();
		addSiteInliner();
		soot.Main.main(args);
	}

	private String getPreVersion() {
		String inlineFilename = tempDir + "\\inline.jar";
		String preFilename = tempDir + "\\pre.jar";
		if (FileUtils.fileExists(preFilename))
			return preFilename;

		try {
			JarUtils jarUtils = new JarUtils();
			Manifest manifest = jarUtils.extractJar(inlineFilename, inlineDir);
			jarUtils.extractJar(inlineFilename, preDir);

			performLCM(inlineDir, preDir);

			jarUtils.createJar(preDir, preFilename, manifest);
			return preFilename;

		} catch (Exception e) {
			throw new OptimizerException("Cannot optimize.", e);
		}
	}

	private void performLCM(String inDir, String outDir) {
		Logger.println("Running soot on folder " + inDir + " (loops)");
		String[] args = new String[] { "-cp", inDir, "-process-dir", inDir,
				"-d", outDir, "-pp", "-p", "jb", "use-original-names:true",
				"-keep-line-number", "-keep-bytecode-offset", "-w",
				// "-p", "wjop", "enabled:true",
				// "-p", "wjop.si", "enabled:true",
				"-p", "cg.spark", "enabled:true",// 
				"-p", "wjtp.lcms", "enabled:true", //
				// "-print-tags", //
				"-optimize", };

		G.v().out = new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {

			}
		});
		G.reset();
//		replaceSootLCM();
		addAlwaysAtomicAnalysis();
		addWholeProgramLCM();
		soot.Main.main(args);
	}

}
