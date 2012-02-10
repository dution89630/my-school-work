package org.deuce.optimize.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.HashChain;
import soot.util.queue.QueueReader;

public class MethodUtils {

	private static final String ATOMIC_ANNOTATION_SIGNATURE = "Lorg/deuce/Atomic;";
	private static final String MAIN_SIGNATURE = "void main(java.lang.String[])";
	private static final String RUN_SIGNATURE = "void run()";

	public static boolean methodIsCtor(SootMethod method) {
		return methodIsInstanceCtor(method) || methodIsStaticCtor(method);
	}

	public static boolean methodIsStaticCtor(SootMethod method) {
		return method.getName().equals(SootMethod.staticInitializerName);
	}

	public static boolean methodIsInstanceCtor(SootMethod method) {
		return method.getName().equals(SootMethod.constructorName);
	}

	public static boolean methodIsEntryPoint(SootMethod method) {
		return methodIsMain(method) || methodIsRun(method);
	}

	public static boolean methodIsRun(SootMethod method) {
		String sig = method.getSubSignature();
		return !method.isStatic() && sig.equals(RUN_SIGNATURE);
	}

	public static boolean methodIsMain(SootMethod method) {
		String sig = method.getSubSignature();
		return method.isStatic() && sig.equals(MAIN_SIGNATURE);
	}

	public static boolean methodIsAtomic(SootMethod method) {

		Tag tag = method.getTag("VisibilityAnnotationTag");
		if (tag == null)
			return false;
		VisibilityAnnotationTag visibilityAnnotationTag = (VisibilityAnnotationTag) tag;
		ArrayList<AnnotationTag> annotations = visibilityAnnotationTag
				.getAnnotations();
		for (AnnotationTag annotationTag : annotations) {
			if (annotationTag.getType().equals(ATOMIC_ANNOTATION_SIGNATURE))
				return true;
		}
		return false;
	}

	public static List<SootMethod> findNonAtomicEntryPoints() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods
				.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (methodIsEntryPoint(method) && !methodIsAtomic(method)) {
				col.add(method);
			}
		}

		return col;
	}

	public static List<SootMethod> findApplicationEntryPoints() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods
				.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (methodIsEntryPoint(method)) {
				if (method.getDeclaringClass().isApplicationClass()) {
					col.add(method);
				}
			}
		}

		return col;
	}

	public static List<SootMethod> findApplicationAtomicMethods() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods
				.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (method.getDeclaringClass().isApplicationClass()) {
				if (methodIsAtomic(method)) {
					col.add(method);
				}
			}
		}

		return col;
	}

	public static List<SootMethod> getCallersOf(SootMethod method) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Iterator<Edge> edgesInto = callGraph.edgesInto(method);
		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesInto.hasNext()) {
			Edge edge = edgesInto.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getSrc().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static List<SootMethod> getCalleesOf(SootMethod method) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Iterator<Edge> edgesInto = callGraph.edgesOutOf(method);

		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesInto.hasNext()) {
			Edge edge = edgesInto.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getTgt().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static List<SootMethod> getCalleesOf(SootMethod callerMethod,
			Stmt invocation) {
		CallGraph callGraph = Scene.v().getCallGraph();

		Iterator<Edge> edgesOut = callGraph.edgesOutOf(invocation);
		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesOut.hasNext()) {
			Edge edge = edgesOut.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getTgt().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static Set<SootMethod> findTransitiveCalleesOf(SootMethod sootMethod) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Set<SootMethod> transitiveTargets = new LinkedHashSet<SootMethod>();
		HashChain<SootMethod> unprocessedTargets = new HashChain<SootMethod>();
		unprocessedTargets.add(sootMethod);
		while (!unprocessedTargets.isEmpty()) {
			sootMethod = unprocessedTargets.getFirst();
			unprocessedTargets.removeFirst();
			Iterator<Edge> edgesOutOf = callGraph.edgesOutOf(sootMethod);
			while (edgesOutOf.hasNext()) {
				Edge edge = edgesOutOf.next();
				if (edge.isExplicit()) {
					SootMethod target = edge.getTgt().method();
					if (!transitiveTargets.contains(target)) {
						transitiveTargets.add(target);
						unprocessedTargets.add(target);
					}
				}
			}
		}
		return transitiveTargets;
	}

	public static Set<SootMethod> findTransitiveCalleesOf(
			List<SootMethod> sootMethods) {
		Set<SootMethod> transitiveTargets = new LinkedHashSet<SootMethod>();
		for (SootMethod sootMethod : sootMethods) {
			Set<SootMethod> transitiveCallees = findTransitiveCalleesOf(sootMethod);
			transitiveTargets.addAll(transitiveCallees);
		}
		return transitiveTargets;
	}

}
