package org.deuce.optimize.analyses.fieldactivity;

import java.util.List;

import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteFlowSet;
import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteIntraprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteLatticeElement;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadFlowSet;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadIntraprocAnalysis;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadLatticeElement;
import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.PointsToHelper;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.AllocNode;

public class FieldActivityTagger {
	private final FieldActivityDatabase database;
	private SootClass sootClass;
	private SootMethod sootMethod;

	// this analysis finds all accesses to fields:
	// o.f = v, C.f = v
	// v = o.f, v = C.f
	// o[i] = v, v = o[i]
	// and marks to the database whether the activities are RO, WO, or RW.

	public FieldActivityTagger() {
		database = FieldActivityDatabase.getInstance();
	}

	public void tag(SootMethod method, Body activeBody,
			VirginReadIntraprocAnalysis virginReadAnalysis,
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis) {

		if (!method.getDeclaringClass().isApplicationClass()) {
			return;
		}
		this.sootMethod = activeBody.getMethod();
		this.sootClass = sootMethod.getDeclaringClass();

		tagStatements(activeBody, virginReadAnalysis, deprivedWriteAnalysis);
	}

	private void tagStatements(Body activeBody,
			VirginReadIntraprocAnalysis virginReadAnalysis,
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis) {
		for (Unit unit : activeBody.getUnits()) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsFieldRef()) {
				FieldRef fieldRef = stmt.getFieldRef();
				if (fieldRef instanceof InstanceFieldRef) {
					handleInstanceFieldRef(virginReadAnalysis,
							deprivedWriteAnalysis, stmt, fieldRef);
				} else if (fieldRef instanceof StaticFieldRef) {
					handleStaticFieldRef(virginReadAnalysis,
							deprivedWriteAnalysis, stmt, fieldRef);
				}
			} else if (stmt.containsArrayRef()) {
				handleArrayRef(virginReadAnalysis, deprivedWriteAnalysis, stmt);
			}
		}
	}

	private void handleArrayRef(VirginReadIntraprocAnalysis virginReadAnalysis,
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis, Stmt stmt) {
		// o[i] = v   OR   v = o[i]
		ArrayRef arrayRef = stmt.getArrayRef();
		Local base = (Local) arrayRef.getBase(); // base is o
		List<AllocNode> nodes = PointsToHelper.getNodes(base);

		VirginReadLatticeElement virginReadMerged = VirginReadLatticeElement.TopUnknown;
		DeprivedWriteLatticeElement deprivedWriteMerged = DeprivedWriteLatticeElement.TopUnknown;
		for (AllocNode allocNode : nodes) {
			AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
					allocNode, AllocNodeAndField.ARRAY_ELEMENTS);

			virginReadMerged = getAndMergeVirginRead(virginReadAnalysis, stmt,
					virginReadMerged, allocNodeAndField);
			deprivedWriteMerged = getAndMergeDeprivedWrite(
					deprivedWriteAnalysis, stmt, deprivedWriteMerged,
					allocNodeAndField);
		}
		addToDatabase(stmt, virginReadMerged, deprivedWriteMerged);
	}

	private void handleStaticFieldRef(
			VirginReadIntraprocAnalysis virginReadAnalysis,
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis, Stmt stmt,
			FieldRef fieldRef) {
		// C.f = v   OR   v = C.f
		StaticFieldRef staticFieldRef = (StaticFieldRef) fieldRef;
		SootField field = staticFieldRef.getField();
		Type type = staticFieldRef.getType(); // type is C
		VirginReadLatticeElement virginReadMerged = VirginReadLatticeElement.TopUnknown;
		DeprivedWriteLatticeElement deprivedWriteMerged = DeprivedWriteLatticeElement.TopUnknown;
		AllocNodeAndField allocNodeAndField = new AllocNodeAndField(type,
				field.getName());

		virginReadMerged = getAndMergeVirginRead(virginReadAnalysis, stmt,
				virginReadMerged, allocNodeAndField);
		deprivedWriteMerged = getAndMergeDeprivedWrite(deprivedWriteAnalysis,
				stmt, deprivedWriteMerged, allocNodeAndField);

		addToDatabase(stmt, virginReadMerged, deprivedWriteMerged);
	}

	private void handleInstanceFieldRef(
			VirginReadIntraprocAnalysis virginReadAnalysis,
			DeprivedWriteIntraprocAnalysis deprivedWriteAnalysis, Stmt stmt,
			FieldRef fieldRef) {
		// o.f = v   OR   v = o.f
		InstanceFieldRef instanceFieldRef = (InstanceFieldRef) fieldRef;
		Local base = (Local) instanceFieldRef.getBase(); // base is o
		SootField field = instanceFieldRef.getField();
		List<AllocNode> nodes = PointsToHelper.getNodes(base);
		VirginReadLatticeElement virginReadMerged = VirginReadLatticeElement.TopUnknown;
		DeprivedWriteLatticeElement deprivedWriteMerged = DeprivedWriteLatticeElement.TopUnknown;
		for (AllocNode allocNode : nodes) {
			AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
					allocNode, field.getName());

			virginReadMerged = getAndMergeVirginRead(virginReadAnalysis, stmt,
					virginReadMerged, allocNodeAndField);
			deprivedWriteMerged = getAndMergeDeprivedWrite(
					deprivedWriteAnalysis, stmt, deprivedWriteMerged,
					allocNodeAndField);
		}
		addToDatabase(stmt, virginReadMerged, deprivedWriteMerged);
	}

	private VirginReadLatticeElement getAndMergeVirginRead(
			VirginReadIntraprocAnalysis fieldActivityAnalysis, Stmt stmt,
			VirginReadLatticeElement merged, AllocNodeAndField allocNodeAndField) {
		VirginReadFlowSet flowAfter = fieldActivityAnalysis.getFlowAfter(stmt);
		VirginReadLatticeElement element = flowAfter.get(allocNodeAndField);
		merged = VirginReadLatticeElement.merge(merged, element);
		return merged;
	}

	private DeprivedWriteLatticeElement getAndMergeDeprivedWrite(
			DeprivedWriteIntraprocAnalysis fieldActivityAnalysis, Stmt stmt,
			DeprivedWriteLatticeElement merged,
			AllocNodeAndField allocNodeAndField) {
		DeprivedWriteFlowSet flowAfter = fieldActivityAnalysis
				.getFlowBefore(stmt);
		DeprivedWriteLatticeElement element = flowAfter.get(allocNodeAndField);
		merged = DeprivedWriteLatticeElement.merge(merged, element);
		return merged;
	}

	private void addToDatabase(Stmt stmt,
			VirginReadLatticeElement virginReadElement,
			DeprivedWriteLatticeElement deprivedWrite) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			database.add(uniqueCodePoint, virginReadElement, deprivedWrite);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		if (sootClass.isApplicationClass()) {
			Logger.println("FAT: " + sootMethod + ": " + stmt
					+ " has VirginRead status: " + virginReadElement
					+ ", DeprivedWrite status: " + deprivedWrite, sootMethod);
		}
	}

}