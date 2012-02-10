package org.deuce.optimize.analyses.general;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.HashChain;

public abstract class BackwardInterproceduralAnalysis<E> {

	private final HashChain<SootMethod> worklist;
	private final LinkedHashMap<SootMethod, E> methodToParameterValues;
	private final LinkedHashMap<SootMethod, E> methodToReturnValue;
	private final CallGraph callGraph;
	private final Set<SootMethod> reachables;

	public BackwardInterproceduralAnalysis() {
		// start with this
		List<SootMethod> entryPoints = getEntryPoints();

		reachables = MethodUtils.findTransitiveCalleesOf(entryPoints);
		reachables.addAll(entryPoints);
		ArrayList<SootMethod> reachablesList = new ArrayList<SootMethod>(reachables);
		Collections.reverse(reachablesList);
		worklist = new HashChain<SootMethod>();
		worklist.addAll(reachablesList);

		methodToParameterValues = new LinkedHashMap<SootMethod, E>();
		methodToReturnValue = new LinkedHashMap<SootMethod, E>();

		callGraph = Scene.v().getCallGraph();

		// then, start processing the worklist one by one
		// for each discovered method, initialize its arguments

		// remember: each method call should (as needed) mark the called methods
		// as changed and add them to the worklist.
		// remember: the return value is stored with each method. so are the parameters.

		// when a method flows forwards different parameters, all callees are marked.
		// when a method flows backwards different return value, all callers are marked.

		// need to flow both NAO and non-NAO objects.
	}

	protected List<SootMethod> getEntryPoints() {
		return MethodUtils.findApplicationEntryPoints();
	}

	protected void analyze() {
		while (!worklist.isEmpty()) {
			SootMethod method = worklist.getFirst();
			worklist.removeFirst();
			if (!shouldAnalyzeMethod(method))
				continue;			
			Body activeBody = method.getActiveBody();

			E parameterValues = getParameterValues(method);
			E oldReturnValue = getReturnValue(method);
			E newReturnValue = analyzeMethod(activeBody, parameterValues);
			if (!oldReturnValue.equals(newReturnValue)) {
				methodToReturnValue.put(method, newReturnValue);
				addCallersToWorklist(method);
			}
		}
	}

	private void addCallersToWorklist(SootMethod method) {
		for (SootMethod callerMethod : MethodUtils.getCallersOf(method)) {
			// only pay attention to reachable callers: those methods that are
			// reachable from the application classes.
			// unreachable ones should not be analyzed.			
			if (reachables.contains(callerMethod)) {
				if (!worklist.contains(callerMethod)) {
					worklist.addLast(callerMethod);
				}
			}
		}
	}

	private E getReturnValue(SootMethod method) {
		E returnValue = methodToReturnValue.get(method);
		if (returnValue == null) {
			returnValue = defaultReturnValue(method);
			methodToReturnValue.put(method, returnValue);
		}
		return returnValue;
	}

	public E handleMethodInvocation(SootMethod callerMethod, E out,
			Stmt invocationStmt) {
		// which methods are called by the stmt?
		List<SootMethod> invokedMethods = MethodUtils.getCalleesOf(
				callerMethod, invocationStmt);

		InvokeExpr invokeExpr = invocationStmt.getInvokeExpr();

		//merge all their return values
		ArrayList<E> mergees = new ArrayList<E>();
		for (SootMethod invokedMethod : invokedMethods) {
			E returnValue;
			if (shouldAnalyzeMethod(invokedMethod)) {
				returnValue = handleCompleteMethodInvocation(callerMethod, out,
						invokeExpr, invokedMethod);
			} else {
				returnValue = handleSkippedMethodInvocation(callerMethod, out,
						invokeExpr, invokedMethod);
			}
			mergees.add(returnValue);
		}

		E merged = newInitialFlow();

		if (mergees.size() == 0)
			return merged;

		merge(mergees, merged);

		return merged;
	}

	protected abstract E handleSkippedMethodInvocation(SootMethod callerMethod,
			E out, InvokeExpr invokeExpr, SootMethod invokedMethod);

	private E handleCompleteMethodInvocation(SootMethod callerMethod, E out,
			InvokeExpr invokeExpr, SootMethod invokedMethod) {
		boolean shouldUpdate = false;
		E parameter = methodToParameterValues.get(invokedMethod);
		if (parameter == null) {
			shouldUpdate = true;
			parameter = getParameterValues(invokedMethod);
		}
		E prevParameter = newInitialFlow();
		copy(parameter, prevParameter);

		// propagate parameters to the called method.
		populateParameters(callerMethod, invokedMethod, invokeExpr, out,
				parameter);

		// the parameters are different. we must recalculate this method.
		if (!prevParameter.equals(parameter) || shouldUpdate) {
			methodToParameterValues.put(invokedMethod, parameter);
			if (!worklist.contains(invokedMethod)) {
				worklist.addLast(invokedMethod);
			}
		}

		// gather and merge all the return values.
		E returnValue = getReturnValue(invokedMethod);
		return returnValue;
	}

	private E getParameterValues(SootMethod method) {
		E parameter = methodToParameterValues.get(method);
		if (parameter == null) {
			parameter = defaultParameterValues(method);
			methodToParameterValues.put(method, parameter);
		}
		return parameter;
	}

	protected void merge(List<E> inList, E out) {

		while (inList.size() > 1) {
			ArrayList<E> newList = new ArrayList<E>();
			int i = 0;
			while (i < inList.size()) {
				E left = inList.get(i++);
				if (i == inList.size()) {
					newList.add(left);
				} else {
					E right = inList.get(i++);
					E merged = newInitialFlow();
					merge(left, right, merged);
					newList.add(merged);
				}
			}
			inList = newList;
		}
		copy(inList.get(0), out);
	}

	protected abstract E analyzeMethod(Body activeBody, E parameterValues);

	protected abstract E newInitialFlow();

	protected abstract void merge(E in1, E in2, E out);

	protected abstract E defaultReturnValue(SootMethod method);

	protected abstract E defaultParameterValues(SootMethod method);

	protected abstract void copy(E source, E dest);

	protected abstract void populateParameters(SootMethod callerMethod,
			SootMethod calleeMethod, InvokeExpr invokeExpr, E out, E parameter);

	protected abstract boolean shouldAnalyzeMethod(SootMethod method);

}