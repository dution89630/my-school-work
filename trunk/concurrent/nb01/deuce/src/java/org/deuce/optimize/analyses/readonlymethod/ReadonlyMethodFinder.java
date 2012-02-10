package org.deuce.optimize.analyses.readonlymethod;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.fieldactivity.AllocNodeAndField;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadFlowSet;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadIntraprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadLatticeElement;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.SootMethod;
import soot.util.HashChain;

public class ReadonlyMethodFinder {
	private SootMethod sootMethod;
	private ReadonlyMethodDatabase database;

	public ReadonlyMethodFinder() {
		this.database = ReadonlyMethodDatabase.getInstance();
	}
	
	public void tag(SootMethod method, Body activeBody,
			VirginReadFlowSet flowAtEnd) {
		this.sootMethod = method;
		tagMethod(activeBody, flowAtEnd);
	}

	private void tagMethod(Body activeBody,
			VirginReadFlowSet flowAtEnd) {
		if (!AtomicStartersDatabase.getInstance().isMethodInScope(sootMethod)) {
			// not atomic, nor reachable from atomic
			return;
		}

		boolean locallyReadOnly = isMethodLocallyReadOnly(flowAtEnd);
		if (locallyReadOnly)
			database.put(sootMethod, ReadonlyMethodLatticeElement.ReadOnly);
		else
			database.put(sootMethod, ReadonlyMethodLatticeElement.NotReadOnly);
		//		Logger.println("FAT: " + sootMethod + ": is completely read-only.",
		//				sootMethod);
	}

	private boolean isMethodLocallyReadOnly(
			VirginReadFlowSet flowAtEnd) {
		// check if the method is 100% read-only, i.e. all field and array accesses are for read,
		// and so are all accesses in invoked methods.		
		Map<AllocNodeAndField, VirginReadLatticeElement> map = flowAtEnd
				.getMapCopy();
		Collection<VirginReadLatticeElement> values = map.values();
		for (VirginReadLatticeElement fieldActivityLatticeElement : values) {
			if (fieldActivityLatticeElement != VirginReadLatticeElement.ReadButNotYetWrittenTo)
				// aha! found something not read-only
				return false;
		}
		// got here, so all field accesses in this method (and all methods it invokes) are read-only.
		return true;
	}

	public void propagate() {
		// a method is not RO if:
		// a. it locally writes, OR
		// b. it calls, or is called, from a method which locally writes.

		Map<SootMethod, ReadonlyMethodLatticeElement> map = database.getMap();

		// initialize set to all NRO methods
		HashChain<SootMethod> notReadOnlyMethods = new HashChain<SootMethod>();
		for (Entry<SootMethod, ReadonlyMethodLatticeElement> entry : map
				.entrySet()) {
			if (entry.getValue() == ReadonlyMethodLatticeElement.NotReadOnly)
				notReadOnlyMethods.add(entry.getKey());
		}

		// propagate until the set empties
		while (!notReadOnlyMethods.isEmpty()) {
			SootMethod methodFromWorklist = notReadOnlyMethods.getFirst();
			notReadOnlyMethods.removeFirst();

			List<SootMethod> calleesOf = MethodUtils
					.getCalleesOf(methodFromWorklist);
			List<SootMethod> callersOf = MethodUtils
					.getCallersOf(methodFromWorklist);

			addToWorklistAsNeeded(map, notReadOnlyMethods, calleesOf);
			addToWorklistAsNeeded(map, notReadOnlyMethods, callersOf);
		}
		Logger.println(database.toString());
	}

	private void addToWorklistAsNeeded(
			Map<SootMethod, ReadonlyMethodLatticeElement> map,
			HashChain<SootMethod> notReadOnlyMethods,
			List<SootMethod> neighorMethods) {
		for (SootMethod neighborMethod : neighorMethods) {
			ReadonlyMethodLatticeElement element = map.get(neighborMethod);
			if (element == ReadonlyMethodLatticeElement.ReadOnly) {
				notReadOnlyMethods.add(neighborMethod);
				database.put(neighborMethod,
						ReadonlyMethodLatticeElement.NotReadOnly);
			}
		}
	}
}
