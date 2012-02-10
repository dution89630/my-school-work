package org.deuce.transaction;

import org.deuce.reflection.FieldOptimizer;

public interface IAdvisor {

	final static public int SKIP_IMMUTABLE = 0x0001; // 1
	final static public int SKIP_NEW_IN_CTOR = 0x0002; // 2
	final static public int SKIP_NEW_LOCAL = 0x0004; // 4
	final static public int THREAD_LOCAL = 0x0008; // 8
	final static public int CURRENTLY_READ_ONLY = 0x0010; // 16
	final static public int WRITE_ONLY_IN_TRANSACTION = 0x0020; // 32
	final static public int READ_ONLY_METHOD = 0x0040; // 64
	final static public int LAST_FIELD_ACTIVITY = 0x0080; // 128
	final static public int INITIAL_INIT_POINT = 0x0100; // 256
	final static public int RECURRING_INIT_POINT = 0x0200; // 512
	final static public int INITIAL_COMMIT_POINT = 0x0400; // 1024
	final static public int RECURRING_COMMIT_POINT = 0x0800; // 2048
	final static public int STABLE_READ = 0x1000; // 4096

	/**
	 * Return -1 for skip, otherwise the returned value will be provided to the
	 * Context.
	 * 
	 * @param optimizer
	 * @return
	 */
	int visitFieldInsn(FieldOptimizer optimizer);

	String adviceGiven();
}
