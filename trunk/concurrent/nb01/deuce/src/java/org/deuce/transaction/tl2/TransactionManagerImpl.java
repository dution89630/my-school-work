package org.deuce.transaction.tl2;

import org.deuce.transaction.BasicAdvisor;
import org.deuce.transaction.Context;
import org.deuce.transaction.IAdvisor;
import org.deuce.transaction.TransactionManager;

public class TransactionManagerImpl implements TransactionManager {

	@Override
	public IAdvisor createAdvisor() {		
		return new BasicAdvisor();
	}

	@Override
	public Context createContext() {
		return new org.deuce.transaction.tl2.Context();
	}

}