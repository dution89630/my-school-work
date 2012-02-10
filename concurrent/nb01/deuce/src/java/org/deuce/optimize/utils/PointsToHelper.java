package org.deuce.optimize.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.Type;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.LazyContextSensitivePointsToSet;
import soot.jimple.spark.ondemand.WrappedPointsToSet;
import soot.jimple.spark.pag.AllocDotField;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.EqualsSupportingPointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.pointer.FullObjectSet;

public class PointsToHelper {
	public static List<AllocNode> getNodes(Local local) {
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet nao = pta.reachingObjects(local);
		final ArrayList<AllocNode> nodes = new ArrayList<AllocNode>();

		collectNodesFromPointsToSet(nao, nodes);

		return nodes;
	}

	private static void collectNodesFromPointsToSet(PointsToSet nao,
			final ArrayList<AllocNode> nodes) {
		if (nao instanceof PointsToSetInternal) {
			PointsToSetInternal internalNao = (PointsToSetInternal) nao;
			internalNao.forall(new P2SetVisitor() {
				@Override
				public void visit(Node n) {
					AllocNode allocNode = (AllocNode) n;
					if (should(allocNode))
						nodes.add(allocNode);
				}
			});
		} else if (nao instanceof LazyContextSensitivePointsToSet) {
			LazyContextSensitivePointsToSet lazy = (LazyContextSensitivePointsToSet) nao;
			lazy.computeContextSensitiveInfo();
			EqualsSupportingPointsToSet delegate = lazy.getDelegate();

			if (delegate instanceof AllocAndContextSet) {
				AllocAndContextSet set = (AllocAndContextSet) delegate;

				for (AllocAndContext allocAndContext : set) {
					AllocNode allocNode = allocAndContext.getAlloc();
					if (should(allocNode))
						nodes.add(allocNode);
				}
			} else {
				WrappedPointsToSet wrapped = (WrappedPointsToSet) delegate;
				PointsToSetInternal pointsToSetInternal = wrapped.getWrapped();
				collectNodesFromPointsToSet(pointsToSetInternal, nodes);
			}
		} else if (nao instanceof AllocAndContextSet) {
			AllocAndContextSet set = (AllocAndContextSet) nao;

			for (AllocAndContext allocAndContext : set) {
				AllocNode allocNode = allocAndContext.getAlloc();
				if (should(allocNode))
					nodes.add(allocNode);
			}
		} else if (nao instanceof FullObjectSet) {
			throw new RuntimeException(
					"No points-to analysis is active. Please enable Spark!");
		} else
			throw new RuntimeException("Unrecognized PointsToSet object!");
	}

	private static Boolean should(AllocNode allocNode) {
		Type type = allocNode.getType();
		//		if (type instanceof RefType) {
		//			if (((RefType) type).getClassName().equals("java.lang.String"))
		//				return false;
		//		}
		return true;
	}

	public static List<AllocNode> getNodes(PointsToSetInternal p2Set) {
		final ArrayList<AllocNode> nodes = new ArrayList<AllocNode>();

		p2Set.forall(new P2SetVisitor() {
			@Override
			public void visit(Node n) {
				AllocNode allocNode = (AllocNode) n;
				nodes.add(allocNode);
			}
		});
		return nodes;
	}

	public static List<AllocNode> getAllAccessibleNodes(List<AllocNode> nodes) {
		Set<AllocNode> foundNodes = new LinkedHashSet<AllocNode>();
		getAllAccessibleNodesInternal(nodes, foundNodes);
		return new ArrayList<AllocNode>(foundNodes);
	}

	private static void getAllAccessibleNodesInternal(List<AllocNode> nodes,
			Set<AllocNode> foundNodes) {
		for (AllocNode allocNode : nodes) {
			if (!foundNodes.contains(allocNode)) {
				// add to found nodes				
				foundNodes.add(allocNode);

				// recursively add all accessible fields
				Collection<AllocDotField> allFieldRefs = allocNode
						.getAllFieldRefs();
				for (AllocDotField allocDotField : allFieldRefs) {
					PointsToSetInternal p2Set = allocDotField.getP2Set();
					List<AllocNode> accessibleNodes = PointsToHelper
							.getNodes(p2Set);
					getAllAccessibleNodesInternal(accessibleNodes, foundNodes);
				}
			}
		}
	}
}
