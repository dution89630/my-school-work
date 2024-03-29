/* Soot - a J*va Optimization Framework
 * Copyright (C) 2007 Patrick Lam
 * Copyright (C) 2007 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.deuce.optimize.analyses.newlyallocated;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.CORBA.UNKNOWN;

import soot.EquivalentValue;
import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.BinaryIdentitySet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * ValueNumberingAnalysis attempts to determine if two local
 * variables (at two potentially different program points) must point
 * to the same object.
 * 
 * The underlying abstraction is based on global value numbering.
 * 
 * See also {@link StrongLocalMustAliasAnalysis} for an analysis
 * that soundly treats redefinitions within loops.
 * 
 * See Sable TR 2007-8 for details.
 * 
 * @author Patrick Lam
 * @author Eric Bodden
 * @see StrongLocalMustAliasAnalysis
 * */
public class ValueNumberingAnalysis extends
		ForwardFlowAnalysis<Unit, HashMap<Value, Object>> {

	public static final String UNKNOWN_LABEL = "UNKNOWN";

	protected static final Object UNKNOWN = new Object() {
		@Override
		public String toString() {
			return UNKNOWN_LABEL;
		}
	};
	protected static final Object NOTHING = new Object() {
		@Override
		public String toString() {
			return "NOTHING";
		}
	};

	/**
	 * The set of all local variables and field references that we track.
	 * This set contains objects of type {@link Local} and also contain
	 * {@link EquivalentValue}s of {@link FieldRef}s.
	 * These field references are to be tracked on the same way as {@link Local}
	 * s are.
	 */
	protected Set<Value> localsAndFieldRefs;

	/** maps from right-hand side expressions (non-locals) to value numbers */
	protected transient Map<Value, Integer> rhsToNumber;

	/**
	 * maps from a merge point (set of containers for analysis information) to
	 * value numbers
	 */
	protected transient HashMap<BinaryIdentitySet<HashMap<Value, Object>>, Integer> mergeToNumber;

	/** the next value number */
	protected int nextNumber = 1;

	/** the containing method */
	protected SootMethod container;

	/**
	 * Creates a new {@link ValueNumberingAnalysis}. If
	 * tryTrackFieldAssignments,
	 * we run an interprocedural side-effects analysis to determine which fields
	 * are (transitively) written to by this method. All fields which that are
	 * not written
	 * to are tracked just as local variables. This semantics is sound for
	 * single-threaded programs.
	 */
	public ValueNumberingAnalysis(UnitGraph g) {
		super(g);
		this.container = g.getBody().getMethod();
		this.localsAndFieldRefs = new HashSet<Value>();

		//add all locals
		for (Local l : g.getBody().getLocals()) {
			if (l.getType() instanceof RefLikeType)
				this.localsAndFieldRefs.add(l);
		}

		this.localsAndFieldRefs.addAll(trackableFields());

		this.rhsToNumber = new HashMap<Value, Integer>();
		this.mergeToNumber = new HashMap<BinaryIdentitySet<HashMap<Value, Object>>, Integer>();

		doAnalysis();

		//not needed any more
		this.rhsToNumber = null;
		this.mergeToNumber = null;
	}

	/**
	 * Computes the set of {@link EquivalentValue}s of all field references that
	 * are used
	 * in this method but not set by the method or any method transitively
	 * called by this method.
	 */
	private Set<Value> trackableFields() {
		Set<Value> usedFieldRefs = new HashSet<Value>();
		//add all field references that are in use and def boxes
		for (Unit unit : this.graph) {
			Stmt s = (Stmt) unit;
			List<ValueBox> useBoxes = s.getUseAndDefBoxes();
			for (ValueBox useBox : useBoxes) {
				Value val = useBox.getValue();
				if (val instanceof FieldRef) {
					FieldRef fieldRef = (FieldRef) val;
					if (fieldRef.getType() instanceof RefLikeType
							&& fieldRef instanceof InstanceFieldRef)
						usedFieldRefs.add(new EquivalentValue(fieldRef));
				}
			}
		}

		return usedFieldRefs;
	}

	@Override
	protected void merge(HashMap<Value, Object> inMap1,
			HashMap<Value, Object> inMap2, HashMap<Value, Object> outMap) {
		for (Value l : localsAndFieldRefs) {
			Object i1 = inMap1.get(l), i2 = inMap2.get(l);
			if (i1.equals(i2))
				outMap.put(l, i1);
			else if (i1 == NOTHING)
				outMap.put(l, i2);
			else if (i2 == NOTHING)
				outMap.put(l, i1);
			else {
				/* Merging two different values is tricky...
				 * A naive approach would be to assign UNKNOWN. However, that would lead to imprecision in the following case:
				 * 
				 * x = null;
				 * if(p) x = new X();
				 * y = x;
				 * z = x;
				 *
				 * Even though it is obvious that after this block y and z are aliased, both would be UNKNOWN :-(
				 * Hence, when merging the numbers for the two branches (null, new X()), we assign a value number that is unique
				 * to that merge location. Consequently, both y and z is assigned that same number!
				 * In the following it is important that we use an IdentityHashSet because we want the number to be unique to the
				 * location. Using a normal HashSet would make it unique to the contents.
				 * (Eric)  
				 */
				BinaryIdentitySet<HashMap<Value, Object>> unorderedPair = new BinaryIdentitySet<HashMap<Value, Object>>(
						inMap1, inMap2);
				Integer number = mergeToNumber.get(unorderedPair);
				if (number == null) {
					number = nextNumber++;
					mergeToNumber.put(unorderedPair, number);
				} else {
					System.out.println(number);
				}
				outMap.put(l, number);
			}
		}
	}

	@Override
	protected void flowThrough(HashMap<Value, Object> in, Unit u,
			HashMap<Value, Object> out) {
		Stmt s = (Stmt) u;
		out.clear();

		out.putAll(in);

		if (s instanceof DefinitionStmt) {
			DefinitionStmt ds = (DefinitionStmt) s;
			Value lhs = ds.getLeftOp();
			Value rhs = ds.getRightOp();

			if (rhs instanceof CastExpr) {
				//un-box casted value
				CastExpr castExpr = (CastExpr) rhs;
				rhs = castExpr.getOp();
			}

			if ((lhs instanceof Local || (lhs instanceof FieldRef && this.localsAndFieldRefs
					.contains(new EquivalentValue(lhs))))
					&& lhs.getType() instanceof RefLikeType) {
				if (rhs instanceof Local) {
					//local-assignment - must be aliased...
					out.put(lhs, in.get(rhs));
				} else if (rhs instanceof ThisRef) {
					//ThisRef can never change; assign unique number
					out.put(lhs, thisRefNumber());
				} else if (rhs instanceof ParameterRef) {
					//ParameterRef can never change; assign unique number
					out.put(lhs, parameterRefNumber((ParameterRef) rhs));
				} else {
					//assign number for expression
					out.put(lhs, numberOfRhs(rhs));
				}
			}
		} else {
			//which other kind of statement has def-boxes? hopefully none...
			assert s.getDefBoxes().isEmpty();
		}
	}

	private Object numberOfRhs(Value rhs) {
		EquivalentValue equivValue = new EquivalentValue(rhs);
		if (localsAndFieldRefs.contains(equivValue)) {
			rhs = equivValue;
		}
		Integer num = rhsToNumber.get(rhs);
		if (num == null) {
			num = nextNumber++;
			if (!(rhs instanceof StaticFieldRef)) {
				rhsToNumber.put(rhs, num);
			}
		}
		return num;
	}

	private int thisRefNumber() {
		//unique number for ThisRef (must be <1)
		return 0;
	}

	private int parameterRefNumber(ParameterRef r) {
		//unique number for ParameterRef[i] (must be <0)
		return 0 - r.getIndex();
	}

	@Override
	protected void copy(HashMap<Value, Object> sourceMap,
			HashMap<Value, Object> destMap) {
		destMap.clear();
		destMap.putAll(sourceMap);
	}

	/** Initial most conservative value: has to be {@link UNKNOWN} (top). */
	@Override
	protected HashMap<Value, Object> entryInitialFlow() {
		HashMap<Value, Object> m = new HashMap<Value, Object>();
		for (Value l : localsAndFieldRefs) {
			m.put(l, UNKNOWN);
		}
		return m;
	}

	/** Initial bottom value: objects have no definitions. */
	@Override
	protected HashMap<Value, Object> newInitialFlow() {
		HashMap<Value, Object> m = new HashMap<Value, Object>();
		for (Value l : localsAndFieldRefs) {
			m.put(l, NOTHING);
		}
		return m;
	}

	/**
	 * Returns a string (natural number) representation of the instance key
	 * associated with l
	 * at statement s or <code>null</code> if there is no such key associated or
	 * <code>UNKNOWN</code> if
	 * the value of l at s is {@link #UNKNOWN}.
	 * 
	 * @param l
	 *            any local of the associated method
	 * @param s
	 *            the statement at which to check
	 */
	public String instanceKeyString(Local l, Stmt s) {
		Object ln = getFlowBefore(s).get(l);
		if (ln == null) {
			return null;
		} else if (ln == UNKNOWN) {
			return UNKNOWN.toString();
		}
		return ln.toString();
	}

	/**
	 * Returns true if this analysis has any information about local l
	 * at statement s (i.e. it is not {@link #UNKNOWN}).
	 * In particular, it is safe to pass in locals/statements that are not
	 * even part of the right method. In those cases <code>false</code> will be
	 * returned.
	 * Permits s to be <code>null</code>, in which case <code>false</code> will
	 * be returned.
	 */
	public boolean hasInfoOn(Local l, Stmt s) {
		HashMap<Value, Object> flowBefore = getFlowBefore(s);
		if (flowBefore == null) {
			return false;
		} else {
			Object info = flowBefore.get(l);
			return info != null && info != UNKNOWN;
		}
	}

	/**
	 * @return true if values of l1 (at s1) and l2 (at s2) have the
	 *         exact same object IDs, i.e. at statement s1 the variable l1 must
	 *         point to the same object
	 *         as l2 at s2.
	 */
	public boolean mustAlias(Local l1, Stmt s1, Local l2, Stmt s2) {
		Object l1n = getFlowBefore(s1).get(l1);
		Object l2n = getFlowBefore(s2).get(l2);

		if (l1n == UNKNOWN || l2n == UNKNOWN)
			return false;

		return l1n == l2n;
	}
}
