package org.deuce.optimize.analyses.newlyallocated;

import org.deuce.optimize.analyses.alwaysatomic.NotAlwaysAtomicDatabase;

import soot.Local;
import soot.RefLikeType;
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
import soot.shimple.PhiExpr;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class NewlyAllocatedIntraprocAnalysis extends
		ForwardFlowAnalysis<Unit, NewlyAllocatedFlowSet> {

	private final NewlyAllocatedInterprocAnalysis interprocAnalysis;
	private final NewlyAllocatedDatabase database;
	private final SootMethod sootMethod;
	private final SootClass sootClass;
	private final NewlyAllocatedFlowSet paramSet;
	private final NewlyAllocatedFlowSet returnValue;

	// flow a map from value numbers to lattice elements.
	// there is a global map from Local or a InstanceFieldRef into a value number.
	// the lattice element is either Top, New or Unknown.

	public NewlyAllocatedIntraprocAnalysis(UnitGraph graph,
			NewlyAllocatedInterprocAnalysis interprocAnalysis,
			NewlyAllocatedFlowSet parameters) {
		super(graph);

		this.interprocAnalysis = interprocAnalysis;
		this.database = NewlyAllocatedDatabase.getInstance();

		this.sootMethod = graph.getBody().getMethod();
		this.sootClass = sootMethod.getDeclaringClass();

		this.paramSet = parameters;
		this.returnValue = NewlyAllocatedCommon.defaultReturnValue(sootMethod);

		ValueNumber.reset();

		System.out.println("Analyzing method: " + sootMethod.toString());
		doAnalysis();
	}

	@Override
	protected void flowThrough(NewlyAllocatedFlowSet in, Unit unit,
			NewlyAllocatedFlowSet out) {
		Stmt stmt = (Stmt) unit;

		in.copyInto(out);

		/////////////
		// InvokeStmt
		/////////////
		if (stmt instanceof InvokeStmt) {

			// flow the parameters values into the called method(s).
			// if method is not AlwaysAtomic, flow TOP for all parameters!

			InvokeExpr invokeExpr = stmt.getInvokeExpr();
			boolean methodAlwaysAtomic = NotAlwaysAtomicDatabase.getInstance()
					.isMethodAlwaysAtomic(sootMethod);
			// flow parameters in...
			NewlyAllocatedFlowSet returnValue = interprocAnalysis
					.handleMethodInvocation(sootMethod, out, stmt);

			// because parameters incoming from not-AA methods cannot be trusted to be new!
			//interprocAnalysis.analyseMethodCall(in, stmt, out);
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
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.setValueNumber(leftOp, valueNumber);
				}

				// v = o[i]
				else if (rightOp instanceof ArrayRef) {
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.putLatticeElement(valueNumber,
							NewlyAllocatedLatticeElement.SuspectedNotNew);
					out.setValueNumber(leftOp, valueNumber);
					// TODO
				}

				// v = o.f
				else if (rightOp instanceof InstanceFieldRef) {
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.putLatticeElement(valueNumber,
							NewlyAllocatedLatticeElement.SuspectedNotNew);
					out.setValueNumber(leftOp, valueNumber);
					// TODO
				}

				// v = C.f
				else if (rightOp instanceof StaticFieldRef) {
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.putLatticeElement(valueNumber,
							NewlyAllocatedLatticeElement.SuspectedNotNew);
					out.setValueNumber(leftOp, valueNumber);
				}

				// v = cst
				else if (rightOp instanceof Constant) {
					// treat a constant as a new object, because the object is immutable
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.putLatticeElement(valueNumber,
							NewlyAllocatedLatticeElement.DefinitelyNew);
					out.setValueNumber(leftOp, valueNumber);
				}

				// v = new / newarray / newmultiarray
				else if (rightOp instanceof AnyNewExpr) {
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.putLatticeElement(valueNumber,
							NewlyAllocatedLatticeElement.DefinitelyNew);
					out.setValueNumber(leftOp, valueNumber);
				}

				// v = Phi(expr1, expr2, ...)
				else if (rightOp instanceof PhiExpr) {
					ValueNumber newValueNumber = out.getValueNumber(rightOp);
					NewlyAllocatedLatticeElement element = NewlyAllocatedLatticeElement.Unknown;
					for (Value value : ((PhiExpr) rightOp).getValues()) {
						ValueNumber valueNumber = out.getValueNumber(value);
						element = NewlyAllocatedLatticeElement.takeWorse(
								element, in.getLatticeElement(valueNumber));
					}
					out.putLatticeElement(newValueNumber, element);
					out.setValueNumber(leftOp, newValueNumber);
				}

				// v = invoke(p1, p2, ...)
				else if (stmt.containsInvokeExpr()) {
					// flow the parameters values into the called method(s).
					// if method is not AlwaysAtomic, flow TOP for all parameters!
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					boolean methodAlwaysAtomic = NotAlwaysAtomicDatabase
							.getInstance().isMethodAlwaysAtomic(sootMethod);

					// flow parameters in...
					NewlyAllocatedFlowSet returnValue = interprocAnalysis
							.handleMethodInvocation(sootMethod, out, stmt);

					// flow return value out
					NewlyAllocatedLatticeElement element = returnValue
							.getLatticeElement(NewlyAllocatedCommon
									.getValueNumberOfReturnValue());
					ValueNumber valueNumber = out.getValueNumber(rightOp);
					out.setValueNumber(leftOp, valueNumber);
					out.putLatticeElement(valueNumber, element);
					// because parameters incoming from not-AA methods cannot be trusted to be new!
					//interprocAnalysis.analyseMethodCall(in, stmt, out);
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
					// TODO
				}

				// v[i] = cst
				else if (rightOp instanceof Constant) {
					// TODO
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
					// TODO
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
				if (rightOp.getType() instanceof RefLikeType) {
				}

				// C.f = v
				else if (rightOp instanceof Local) {
					// TODO
				}

				// C.f = cst
				else if (rightOp instanceof Constant) {
					// TODO
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
			Local left = (Local) ((IdentityStmt) stmt).getLeftOp();
			Value rightOp = ((IdentityStmt) stmt).getRightOp();

			//addToParamSet(left);

			// v = @this
			if (rightOp instanceof ThisRef) {
				ValueNumber valueNumber = NewlyAllocatedCommon
						.getValueNumberOfThisRef();
				out.setValueNumber(left, valueNumber);
				NewlyAllocatedLatticeElement latticeElement = paramSet
						.getLatticeElement(valueNumber);

				out.putLatticeElement(valueNumber, latticeElement);
			}

			// v = @parameter
			else if (rightOp instanceof ParameterRef) {
				ParameterRef p = (ParameterRef) rightOp;
				// ignore primitive types
				if (p.getType() instanceof RefLikeType) {
					ValueNumber valueNumber = NewlyAllocatedCommon
							.getValueNumberOfParameterRef(p);
					out.setValueNumber(left, valueNumber);

					NewlyAllocatedLatticeElement latticeElement = paramSet
							.getLatticeElement(valueNumber);

					out.putLatticeElement(valueNumber, latticeElement);
				}
			}

			// v = @exception
			else if (rightOp instanceof CaughtExceptionRef) {
				ValueNumber valueNumber = NewlyAllocatedCommon
						.getValueNumberOfCaughtException();
				out.putLatticeElement(valueNumber,
						NewlyAllocatedLatticeElement.SuspectedNotNew);
				out.setValueNumber(left, valueNumber);
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
				ValueNumber valueNumber = out.getValueNumber(op);
				out.putLatticeElement(valueNumber,
						NewlyAllocatedLatticeElement.SuspectedNotNew);
			}

			else if (op instanceof Constant) {
				ValueNumber valueNumber = out.getValueNumber(op);
				out.putLatticeElement(valueNumber,
						NewlyAllocatedLatticeElement.SuspectedNotNew);
			}

			else
				throw new Error("ThrowStmt match failure " + stmt);
		}

		/////////////
		// ReturnStmt
		/////////////

		else if (stmt instanceof ReturnVoidStmt) {
			// do nothing...
		}

		else if (stmt instanceof ReturnStmt) {
			Value v = ((ReturnStmt) stmt).getOp();

			if (v instanceof Local) {
				// ignore primitive types
				if (v.getType() instanceof RefLikeType) {
					ValueNumber valueNumber = out.getValueNumber(v);
					NewlyAllocatedLatticeElement element = out
							.getLatticeElement(valueNumber);

					NewlyAllocatedLatticeElement latticeElement = returnValue
							.getLatticeElement(NewlyAllocatedCommon
									.getValueNumberOfReturnValue());

					latticeElement = NewlyAllocatedLatticeElement.takeWorse(
							latticeElement, element);
					returnValue.putLatticeElement(NewlyAllocatedCommon
							.getValueNumberOfReturnValue(), latticeElement);
				}
			}

			else if (v instanceof Constant) {
				returnValue.putLatticeElement(NewlyAllocatedCommon
						.getValueNumberOfReturnValue(),
						NewlyAllocatedLatticeElement.DefinitelyNew);
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

	private void addToParamSet(Local local) {
		// all objects that are passed as parameters to the method
		// are stored in the paramSet.
		// such objects are considered non-NAOs.
		//		List<AllocNode> nodes = PointsToHelper.getNodes(local);
		//		List<AllocNode> allAccessibleNodes = PointsToHelper
		//				.getAllAccessibleNodes(nodes);
		//		for (AllocNode allocNode : allAccessibleNodes) {
		//			paramSet.add(allocNode);
		//		}
	}

	@Override
	protected void copy(NewlyAllocatedFlowSet source, NewlyAllocatedFlowSet dest) {
		source.copyInto(dest);
	}

	@Override
	protected NewlyAllocatedFlowSet entryInitialFlow() {
		// start the method with an empty set of NAO
		return new NewlyAllocatedFlowSet();
	}

	@Override
	protected void merge(NewlyAllocatedFlowSet in1, NewlyAllocatedFlowSet in2,
			NewlyAllocatedFlowSet out) {
		NewlyAllocatedFlowSet merged = NewlyAllocatedFlowSet.mergeAndTakeWorse(
				in1, in2);
		merged.copyInto(out);
	}

	@Override
	protected NewlyAllocatedFlowSet newInitialFlow() {
		// default NAO set is the empty set
		return new NewlyAllocatedFlowSet();
	}

	public NewlyAllocatedFlowSet getReturnValue() {
		return returnValue;
	}
}