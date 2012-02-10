package org.deuce.optimize.analyses.newlocals;

import org.deuce.optimize.analyses.leakyconstructor.LeakyConstructorsDatabase;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;

import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.BreakpointStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.NopStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThisRef;
import soot.jimple.ThrowStmt;
import soot.jimple.UnopExpr;
import soot.jimple.internal.AbstractNewExpr;
import soot.jimple.internal.JNewExpr;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class NewLocalsIntraprocAnalysis extends
		ForwardFlowAnalysis<Unit, NewLocalsFlowSet> {

	private final NewLocalsDatabase database;
	private final SootMethod sootMethod;
	private final SootClass sootClass;

	// flow 2 maps:
	// 1. from locals to value numbers; (just like LocalMustAliasAnalysis)
	// 2. from value numbers to lattice elements.
	// the lattice element is either Top=NotNew, New or Bottom=Unknown.

	public NewLocalsIntraprocAnalysis(UnitGraph graph) {
		super(graph);

		this.database = NewLocalsDatabase.getInstance();

		this.sootMethod = graph.getBody().getMethod();
		this.sootClass = sootMethod.getDeclaringClass();

		ValueNumber.reset();

		Logger.println("NLA: Analyzing method: " + sootMethod.toString(),
				sootMethod);
		doAnalysis();
	}

	@Override
	protected void flowThrough(NewLocalsFlowSet in, Unit unit,
			NewLocalsFlowSet out) {
		Stmt stmt = (Stmt) unit;

		in.copyInto(out);

		/////////////
		// InvokeStmt
		/////////////
		if (stmt instanceof InvokeStmt) {
			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			escapeAllParameters(out, invokeExpr);
		}

		/////////////
		// AssignStmt
		/////////////

		else if (stmt instanceof AssignStmt) {
			Value leftOp = ((AssignStmt) stmt).getLeftOp();
			Value rightOp = ((AssignStmt) stmt).getRightOp();

			// v = ...
			if (leftOp instanceof Local) {
				Local left = (Local) leftOp;

				// remove optional cast
				if (rightOp instanceof CastExpr)
					rightOp = ((CastExpr) rightOp).getOp();

				// ignore primitive types
				if (!(left.getType() instanceof RefLikeType)) {
				}

				// v = o
				else if (rightOp instanceof Local) {
					equateValues(out, leftOp, rightOp);
				}

				// v = o[i]
				else if (rightOp instanceof ArrayRef) {
					escapeValue(out, rightOp);
					equateValues(out, leftOp, rightOp);
				}

				// v = o.f
				else if (rightOp instanceof InstanceFieldRef) {
					escapeValue(out, rightOp);
					equateValues(out, leftOp, rightOp);
				}

				// v = C.f
				else if (rightOp instanceof StaticFieldRef) {
					escapeValue(out, rightOp);
					equateValues(out, leftOp, rightOp);
				}

				// v = cst
				else if (rightOp instanceof Constant) {
					// treat a constant as a new object, because the object is immutable					
					assignNewToValue(out, rightOp);
					equateValues(out, leftOp, rightOp);
				}

				// v = new / newarray / newmultiarray
				else if (rightOp instanceof AnyNewExpr) {
					assignNewToValue(out, rightOp);
					if (rightOp instanceof AbstractNewExpr)
					{
						RefType baseType = ((AbstractNewExpr) rightOp).getBaseType();
						SootClass newSootClass = baseType.getSootClass();
						if (LeakyConstructorsDatabase.getInstance().hasLeakyConstructor(newSootClass))
								{
							escapeValue(out, rightOp);
								}
					}
					equateValues(out, leftOp, rightOp);
				}

				
				// v = Phi(expr1, expr2, ...)
				else if (rightOp instanceof PhiExpr) {
					ValueNumber newValueNumber = out.getValueNumber(rightOp);
					NewLocalsLatticeElement mergedElement = NewLocalsLatticeElement.TopUnknown;
					for (Value value : ((PhiExpr) rightOp).getValues()) {
						ValueNumber valueNumber = out.getValueNumber(value);
						mergedElement = NewLocalsLatticeElement.merge(
								mergedElement, in
										.getLatticeElement(valueNumber));
					}
					out.putLatticeElement(newValueNumber, mergedElement);
					out.setValueNumber(leftOp, newValueNumber);
				}

				// v = invoke(p1, p2, ...)
				else if (stmt.containsInvokeExpr()) {
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					escapeAllParameters(out, invokeExpr);
					escapeValue(out, rightOp);
					equateValues(out, leftOp, rightOp);
				}

				// v = binary or unary operator
				else if (rightOp instanceof BinopExpr
						|| rightOp instanceof UnopExpr
						|| rightOp instanceof InstanceOfExpr) {
					// do nothing...
					assert false;
				}

				else
					throw new Error("AssignStmt match failure (rightOp)" + stmt);
			}

			// v[i] = ...
			else if (leftOp instanceof ArrayRef) {
				if (!(rightOp.getType() instanceof RefLikeType)) {
				}

				// v[i] = o
				else if (rightOp instanceof Local) {
					escapeValue(out, rightOp);
				}

				// v[i] = cst
				else if (rightOp instanceof Constant) {
				}

				else
					throw new Error("AssignStmt match failure (rightOp)" + stmt);
			}

			// v.f = ...
			else if (leftOp instanceof InstanceFieldRef) {
				if (!(rightOp.getType() instanceof RefLikeType)) {
				}

				// v.f = o
				else if (rightOp instanceof Local) {
					escapeValue(out, rightOp);
				}

				// v.f = cst
				else if (rightOp instanceof Constant) {
					// TODO
				}

				else
					throw new Error("AssignStmt match failure (rightOp) "
							+ stmt);
			}

			// C.f = ...
			else if (leftOp instanceof StaticFieldRef) {
				if (!(rightOp.getType() instanceof RefLikeType)) {
				}

				// C.f = v
				else if (rightOp instanceof Local) {
					escapeValue(out, rightOp);
				}

				// C.f = cst
				else if (rightOp instanceof Constant) {
				}

				else
					throw new Error("AssignStmt match failure (rightOp) "
							+ stmt);
			}

			else
				throw new Error("AssignStmt match failure (leftOp) " + stmt);

		}

		///////////////
		// IdentityStmt
		///////////////

		else if (stmt instanceof IdentityStmt) {
			Local leftOp = (Local) ((IdentityStmt) stmt).getLeftOp();
			Value rightOp = ((IdentityStmt) stmt).getRightOp();

			// v = @this
			if (rightOp instanceof ThisRef) {
				escapeValue(out, rightOp);
				equateValues(out, leftOp, rightOp);
			}

			// v = @parameter
			else if (rightOp instanceof ParameterRef) {
				escapeValue(out, rightOp);
				equateValues(out, leftOp, rightOp);
			}

			// v = @exception
			else if (rightOp instanceof CaughtExceptionRef) {
				escapeValue(out, rightOp);
				equateValues(out, leftOp, rightOp);
			}

			else
				throw new Error("IdentityStmt match failure (rightOp) " + stmt);

		}

		////////////
		// ThrowStmt
		////////////

		else if (stmt instanceof ThrowStmt) {
			Value op = ((ThrowStmt) stmt).getOp();

			if (op instanceof Local) {
				escapeValue(out, op);
			}

			else if (op instanceof Constant) {
			}

			else
				throw new Error("ThrowStmt match failure " + stmt);
		}

		/////////////
		// ReturnStmt
		/////////////

		else if (stmt instanceof ReturnVoidStmt) {
		}

		else if (stmt instanceof ReturnStmt) {
			Value v = ((ReturnStmt) stmt).getOp();

			if (v instanceof Local) {
			}

			else if (v instanceof Constant) {
			}

			else
				throw new Error("ReturnStmt match failure " + stmt);

		}

		//////////
		// ignored
		//////////

		else if (stmt instanceof IfStmt || stmt instanceof GotoStmt
				|| stmt instanceof LookupSwitchStmt
				|| stmt instanceof TableSwitchStmt
				|| stmt instanceof MonitorStmt
				|| stmt instanceof BreakpointStmt || stmt instanceof NopStmt) {
			// do nothing...
		}

		else
			throw new Error("Stmt match faliure " + stmt);

	}

	private void escapeAllParameters(NewLocalsFlowSet out, InvokeExpr invokeExpr) {
		for (int i = 0; i < invokeExpr.getArgCount(); ++i) {
			Value arg = invokeExpr.getArg(i);
			escapeValue(out, arg);
		}
		if (invokeExpr instanceof InstanceInvokeExpr) {
			SootMethod invokedMethod = invokeExpr.getMethod();
			Value receiverObject = ((InstanceInvokeExpr) invokeExpr).getBase();
			if (MethodUtils.methodIsInstanceCtor(invokedMethod)
					&& !LeakyConstructorsDatabase.getInstance()
							.isConstructorLeaky(invokedMethod)) {
				// don't escape the receiver
			} else {
				escapeValue(out, receiverObject);
			}
		}
		// TODO: maybe don't escape, if method is known to be safe.
	}

	private void assignNewToValue(NewLocalsFlowSet out, Value value) {
		ValueNumber valueNumber = out.getValueNumber(value);
		out.putLatticeElement(valueNumber,
				NewLocalsLatticeElement.DefinitelyNew);
		out.setValueNumber(value, valueNumber);
	}

	private void escapeValue(NewLocalsFlowSet out, Value value) {
		ValueNumber valueNumber = out.getValueNumber(value);
		out.putLatticeElement(valueNumber,
				NewLocalsLatticeElement.BottomSuspectedNotNew);
		out.setValueNumber(value, valueNumber);
	}

	private void equateValues(NewLocalsFlowSet out, Value leftOp, Value rightOp) {
		ValueNumber valueNumber = out.getValueNumber(rightOp);
		out.setValueNumber(leftOp, valueNumber);
	}

	@Override
	protected void copy(NewLocalsFlowSet source, NewLocalsFlowSet dest) {
		source.copyInto(dest);
	}

	@Override
	protected NewLocalsFlowSet entryInitialFlow() {
		// start the method with an empty set of new locals
		return new NewLocalsFlowSet();
	}

	@Override
	protected void merge(Unit succNode, NewLocalsFlowSet in1,
			NewLocalsFlowSet in2, NewLocalsFlowSet out) {
		NewLocalsFlowSet merged = NewLocalsFlowSet.merge(succNode, in1, in2);
		merged.copyInto(out);
	}

	@Override
	protected NewLocalsFlowSet newInitialFlow() {
		// default new locals set is the empty set
		return new NewLocalsFlowSet();
	}

	@Override
	protected void merge(NewLocalsFlowSet in1, NewLocalsFlowSet in2,
			NewLocalsFlowSet out) {
		throw new UnsupportedOperationException(
				"not implemented; use other merge method instead");
	}

}