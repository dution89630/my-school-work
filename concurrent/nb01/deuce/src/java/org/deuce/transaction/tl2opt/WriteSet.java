package org.deuce.transaction.tl2opt;

import org.deuce.transaction.IAdvisor;
import org.deuce.transaction.tl2.field.ReadFieldAccess;
import org.deuce.transaction.tl2.field.WriteFieldAccess;
import org.deuce.transform.Exclude;
import org.deuce.trove.THashSet;
import org.deuce.trove.TLinkedList;
import org.deuce.trove.TObjectProcedure;

/**
 * Represents the transaction write set.
 *  
 * @author Guy Korland
 * @since 0.7
 */
@Exclude
public class WriteSet{
	
	final private THashSet<WriteFieldAccess> writeSet = new THashSet<WriteFieldAccess>( 16);
	final private TLinkedList<WriteFieldAccess> writeSetWithNoLookup = 
		new TLinkedList<WriteFieldAccess>();
	
	public void clear() {
		writeSet.clear();
		writeSetWithNoLookup.clear();
	}

	public boolean isEmpty() {
		return writeSet.isEmpty() && writeSetWithNoLookup.isEmpty();
	}

	public boolean forEach(TObjectProcedure<WriteFieldAccess> procedure){
		if (!writeSet.forEach(procedure))
			return false;
		if (!writeSetWithNoLookup.forEachValue(procedure))
			return false;
		return true;
	}
	
	public void put(WriteFieldAccess write) {
		int advice = write.getAdvice();
		if ((advice & Advisor.WRITE_ONLY_IN_TRANSACTION) != 0)
		{
			// add to the linkedlist - it will never be looked up
			writeSetWithNoLookup.add(write);
		}
		else
		{
			// Add to write set
			if(!writeSet.add( write))
				writeSet.replace(write);
		}
	}
	
	public WriteFieldAccess contains(ReadFieldAccess read) {
		// Check if it is already included in the write set
		// don't look up in writeSetWithNoLookup.
		return writeSet.get( read);
	}
	
	public int size() {
		return writeSet.size() + writeSetWithNoLookup.size();
	}
	
}
