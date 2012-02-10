package org.deuce.optimize.analyses.fieldactivity.backwards;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.fieldactivity.AllocNodeAndField;
import org.deuce.optimize.analyses.general.BackwardInterproceduralAnalysis;
import org.deuce.optimize.analyses.general.ForwardInterproceduralAnalysis;
import org.deuce.optimize.utils.MethodUtils;
import org.deuce.optimize.utils.PointsToHelper;
import org.deuce.optimize.utils.graph.ExceptionalUnitGraphEx;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.spark.pag.AllocNode;
import soot.util.Chain;

public class DeprivedWriteInterprocAnalysis extends
		BackwardInterproceduralAnalysis<DeprivedWriteFlowSet> {

	private final Map<SootMethod, DeprivedWriteIntraprocAnalysis> allApplicationOnlyAnalyses = new LinkedHashMap<SootMethod, DeprivedWriteIntraprocAnalysis>();

	public DeprivedWriteInterprocAnalysis() {
		analyze();
	}

	public Map<SootMethod, DeprivedWriteIntraprocAnalysis> getAllApplicationOnlyAnalyses() {
		return Collections.unmodifiableMap(allApplicationOnlyAnalyses);
	}


	@Override
	protected DeprivedWriteFlowSet analyzeMethod(Body activeBody,
			DeprivedWriteFlowSet parameterValues) {
		ExceptionalUnitGraphEx graph = new ExceptionalUnitGraphEx(activeBody);

		DeprivedWriteIntraprocAnalysis analysis = new DeprivedWriteIntraprocAnalysis(
				graph, this, parameterValues);
		SootMethod sootMethod = activeBody.getMethod();
		if (sootMethod.getDeclaringClass().isApplicationClass()) {
			allApplicationOnlyAnalyses.put(sootMethod, analysis);
		}

		DeprivedWriteFlowSet returnValue = analysis.getReturnValue();
		return returnValue;
	}

	@Override
	protected void copy(DeprivedWriteFlowSet source, DeprivedWriteFlowSet dest) {
		source.copy(dest);
	}

	@Override
	protected DeprivedWriteFlowSet defaultParameterValues(SootMethod method) {
		return new DeprivedWriteFlowSet();
	}

	@Override
	protected DeprivedWriteFlowSet defaultReturnValue(SootMethod method) {
		return new DeprivedWriteFlowSet();
	}

	@Override
	protected void merge(DeprivedWriteFlowSet in1, DeprivedWriteFlowSet in2,
			DeprivedWriteFlowSet out) {
		DeprivedWriteFlowSet merged = DeprivedWriteFlowSet.merge(in1, in2);
		merged.copy(out);
	}

	@Override
	protected DeprivedWriteFlowSet newInitialFlow() {
		return new DeprivedWriteFlowSet();
	}

	@Override
	protected void populateParameters(SootMethod callerMethod,
			SootMethod calleeMethod, InvokeExpr invokeExpr,
			DeprivedWriteFlowSet out, DeprivedWriteFlowSet parameter) {
		// caller method invoked the callee method.
		// if the callee is atomic-starter, then the parameter is the empty set.
		// otherwise, we merge the out into the parameter set.

		boolean isCalleeAtomicStarter = AtomicStartersDatabase.getInstance()
				.isMethodAtomicStarter(calleeMethod);
		if (!isCalleeAtomicStarter) {
			out.mergeInto(parameter);
		}
	}

	@Override
	protected boolean shouldAnalyzeMethod(SootMethod method) {
		// methods which should not be analyzed: because they are native,
		// or they belong to the JDK and it is too expensive to analyze the
		// entire JDK.

		if (!method.hasActiveBody())
			return false;

		//		if (!method.getDeclaringClass().isApplicationClass())
		//			return false;

		return true;
	}

	@Override
	protected DeprivedWriteFlowSet handleSkippedMethodInvocation(
			SootMethod callerMethod, DeprivedWriteFlowSet out,
			InvokeExpr invokeExpr, SootMethod invokedMethod) {
		// to be safe, we pretend any unanalyzed method mangles all  
		// direct fields of all incoming objects.
		DeprivedWriteFlowSet resultFlowSet = new DeprivedWriteFlowSet();
		out.copy(resultFlowSet);

		// commented out until we know why it blows up memory so much.

		if (false) {

			boolean isApplicationClass = invokedMethod.getDeclaringClass()
					.isApplicationClass();
			// find all parameters sent to the method
			for (int i = 0; i < invokeExpr.getArgCount(); ++i) {
				Value arg = invokeExpr.getArg(i);
				if (arg.getType() instanceof RefLikeType) {
					if (arg instanceof StringConstant)
						continue;
					Local local = (Local) arg;
					// add all direct (non-recursive) fields to the result flow set as RW
					List<AllocNode> parameterNodes = PointsToHelper
							.getNodes(local);
					for (AllocNode allocNode : parameterNodes) {
						// parameter is a reference
						if (arg.getType() instanceof RefType) {
							RefType refType = (RefType) arg.getType();
							Chain<SootField> fields = refType.getSootClass()
									.getFields();
							for (SootField sootField : fields) {
								String fieldName = sootField.getName();
								AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
										allocNode, fieldName);
								resultFlowSet
										.putAndMerge(
												allocNodeAndField,
												DeprivedWriteLatticeElement.BottomWillBeRead,
												isApplicationClass);
							}
							// parameter is an array
						} else if (arg.getType() instanceof ArrayType) {
							AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
									allocNode, AllocNodeAndField.ARRAY_ELEMENTS);
							resultFlowSet.putAndMerge(allocNodeAndField,
									DeprivedWriteLatticeElement.BottomWillBeRead,
									isApplicationClass);
						}
					}
				}
			}
		}

		return resultFlowSet;
	}

	@Override
	protected List<SootMethod> getEntryPoints() {
		return MethodUtils.findApplicationAtomicMethods();
	}
}
