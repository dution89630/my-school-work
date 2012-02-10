package org.deuce.transaction.tl2opt;

import java.util.LinkedHashMap;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePointsAndStmt;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.transaction.IAdvisor;

import soot.SootMethod;

// the goal of Bookkeeper is to count how many advice were given.
public class AdviceBookkeeper {
	private static final String IMM = "IMM";
	private static final String NIC = "NIC";
	private static final String NL = "NL";
	private static final String TL = "TL";
	private static final String RO = "RO";
	private static final String LFA = "LFA";
	private static final String ROM = "ROM";
	private static final String WO = "WO";
	private static final String IIP = "IIP";
	private static final String RIP = "RIP";
	private static final String ICP = "ICP";
	private static final String RCP = "RCP";
	private static final String SR = "SR";

	private static AdviceBookkeeper instance = new AdviceBookkeeper();
	private final LinkedHashMap<String, Integer> book = new LinkedHashMap<String, Integer>();
	private final AtomicStartersDatabase atomicStartersDatabase = AtomicStartersDatabase
			.getInstance();

	private AdviceBookkeeper() {
		init();
	}

	public void init() {
		book.clear();
		book.put(IMM, 0);
		book.put(NIC, 0);
		book.put(NL, 0);
		book.put(TL, 0);
		book.put(ROM, 0);
		book.put(RO, 0);
		book.put(WO, 0);
		book.put(LFA, 0);
		book.put(IIP, 0);
		book.put(RIP, 0);
		book.put(ICP, 0);
		book.put(RCP, 0);
		book.put(SR, 0);
	}

	public static AdviceBookkeeper getBookkeeper() {
		return instance;
	}

	public void accountFor(int advice, UniqueCodePointsAndStmt codePoint) {

		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L10Opt))
			return;

		if (!methodIsReachable(codePoint.getSootMethod()))
			return;

		if ((advice & IAdvisor.SKIP_IMMUTABLE) != 0) {
			augment(IMM);
		}
		if ((advice & IAdvisor.SKIP_NEW_IN_CTOR) != 0) {
			augment(NIC);
		}
		if ((advice & IAdvisor.SKIP_NEW_LOCAL) != 0) {
			augment(NL);
		}
		if ((advice & IAdvisor.THREAD_LOCAL) != 0) {
			augment(TL);
		}
		if ((advice & IAdvisor.CURRENTLY_READ_ONLY) != 0) {
			if ((advice & IAdvisor.SKIP_IMMUTABLE) == 0) {
				augment(RO);
			}
		}
		if ((advice & IAdvisor.LAST_FIELD_ACTIVITY) != 0) {
			augment(LFA);
		}
		if ((advice & IAdvisor.WRITE_ONLY_IN_TRANSACTION) != 0) {
			augment(WO);
		}
		if ((advice & IAdvisor.READ_ONLY_METHOD) != 0) {
			augment(ROM);
		}
		if ((advice & IAdvisor.INITIAL_INIT_POINT) != 0) {
			augment(IIP);
		}
		if ((advice & IAdvisor.RECURRING_INIT_POINT) != 0) {
			augment(RIP);
		}
		if ((advice & IAdvisor.INITIAL_COMMIT_POINT) != 0) {
			augment(ICP);
		}
		if ((advice & IAdvisor.RECURRING_COMMIT_POINT) != 0) {
			augment(RCP);
		}
		if ((advice & IAdvisor.STABLE_READ) != 0) {
			augment(SR);
		}
	}

	private boolean methodIsReachable(SootMethod sootMethod) {
		return atomicStartersDatabase.isMethodInScope(sootMethod);
	}

	private void augment(String pieceOfAdvice) {
		book.put(pieceOfAdvice, (book.get(pieceOfAdvice)) + 1);
	}

	@Override
	public String toString() {
		return "AdviceBookkeeper [book=" + book + "]";
	}

}
