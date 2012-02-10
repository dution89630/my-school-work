package org.deuce.benchmark.optimize;

import org.deuce.transform.Exclude;

/**
 */
@Exclude
public class Benchmark implements org.deuce.benchmark.Benchmark {

	private TestCase test;

	public void init(String[] args) {
		boolean error = false;

		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("micro"))
				test = new Micro();
			else
				error = true;
		} else
			error = true;

		if (error) {
			System.out.println("Benchmark error");
			System.exit(1);
		}
	}

	public org.deuce.benchmark.BenchmarkThread createThread(int i, int nb) {
		return new BenchmarkThread(test);
	}

	public String getStats(org.deuce.benchmark.BenchmarkThread[] threads) {
		int count = 0;
		for (int i = 0; i < threads.length; i++) {
			count += ((BenchmarkThread)threads[i]).getTestCount();
		}
		return "Count=" + count;
	}
}
