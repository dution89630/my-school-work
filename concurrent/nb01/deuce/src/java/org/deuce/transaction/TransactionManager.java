package org.deuce.transaction;


public interface TransactionManager {
	Context createContext();
	IAdvisor createAdvisor();
}
