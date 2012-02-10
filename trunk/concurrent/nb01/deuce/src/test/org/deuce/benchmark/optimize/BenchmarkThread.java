package org.deuce.benchmark.optimize;

import org.deuce.transform.Exclude;

/**
 * @author Pascal Felber
 * @since 0.1
 */
@Exclude
public class BenchmarkThread extends org.deuce.benchmark.BenchmarkThread {
	final private TestCase test;
	private int testCount;
	private int threadID; 

	public BenchmarkThread(TestCase test) {
		this.test = test;
	}
	@Override
	public void run() {
		threadID = (int)Thread.currentThread().getId();
		super.run();
	}

	protected void step(int phase) {
		test.test(threadID);
		if (phase == Benchmark.TEST_PHASE)
			++testCount;
	}
	
	public String getStats() {
		return Integer.toString(getTestCount());
	}
	public int getTestCount() {
		return testCount;
	}
}
