package org.deuce.transaction.tl2opt;

import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.transaction.BasicAdvisor;
import org.deuce.transaction.Context;
import org.deuce.transaction.IAdvisor;
import org.deuce.transaction.TransactionManager;

public class TransactionManagerImpl implements TransactionManager {

	private Advisor advisor = new Advisor();
	private BasicAdvisor basicAdvisor = new BasicAdvisor();

	@Override
	public IAdvisor createAdvisor() {
		if (Settings.getInstance().isOptEnabled(OptimizerSettings.L10Opt)) {
			return advisor;
		} else {
			return basicAdvisor;
		}
	}

	@Override
	public Context createContext() {
		return new org.deuce.transaction.tl2opt.Context();
	}

}
