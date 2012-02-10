package org.deuce.benchmark.optimize;

import org.deuce.Atomic;

public class Micro implements TestCase {

	static int SIZE = 1000; 
	private int x;
	private int[] sum = new int[SIZE];
	
	public Micro(){
		x = 5;
	}
	
	@Atomic
	@Override
	public int test(int threadID) {
		
		Counter counter = new Counter(); // thread escape
		counter.x = x;					 //  write to thread escape + read of "final"
		counter.sum = sum[threadID%SIZE]; // write to thread escape + read with no write before + read of "final
		
		sum[threadID%SIZE] += x; // read of "final" + write with no read after
		
		int result = counter.x + counter.sum;  //  read of thread escape
		return result * 10; // re-scope
		
	}
}
