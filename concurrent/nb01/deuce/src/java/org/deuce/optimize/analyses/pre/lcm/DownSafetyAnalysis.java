/* Soot - a J*va Optimization Framework
 * Copyright (C) 2002 Florian Loitsch
 *       based on FastAvailableExpressionsAnalysis from Patrick Lam.
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

/*
 * Modified by the Sable Research Group and others 1997-2002.
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */


package org.deuce.optimize.analyses.pre.lcm;
import soot.*;
import soot.toolkits.scalar.*;
import soot.toolkits.graph.*;
import soot.jimple.*;

import java.util.*;

/** 
 * Performs an DownSafe-analysis on the given graph.
 * An expression is downsafe, if the computation will occur on every path from
 * the current point down to the END.
 */
// an expression is downsafe in a unit, if it sure to be computed later.
public class DownSafetyAnalysis extends BackwardFlowAnalysis<Unit, FlowSet> {
  private SideEffectTester sideEffect = null;

  private Map unitToGenerateMap;

  private BoundedFlowSet set;

  /**
   * this constructor should not be used, and will throw a runtime-exception!
   */
  public DownSafetyAnalysis(DirectedGraph<Unit> dg) {
    /* we have to add super(dg). otherwise Javac complains. */
    super(dg);
    throw new RuntimeException("Don't use this Constructor!");
  }

  /**
   * this constructor automaticly performs the DownSafety-analysis.<br>
   * the result of the analysis is as usual in FlowBefore (getFlowBefore())
   * and FlowAfter (getFlowAfter()).<br>
   *
   * @param dg a ExceptionalUnitGraph.
   * @param unitToGen the equivalentValue of each unit.
   * @param sideEffect the SideEffectTester that performs kills.
   */
  public DownSafetyAnalysis(DirectedGraph<Unit> dg, Map unitToGen, SideEffectTester
			    sideEffect) {
    this(dg, unitToGen, sideEffect, new
      ArrayPackedSet(new CollectionFlowUniverse(unitToGen.values())));
  }

  /**
   * this constructor automaticly performs the DownSafety-analysis.<br>
   * the result of the analysis is as usual in FlowBefore (getFlowBefore())
   * and FlowAfter (getFlowAfter()).<br>
   * as sets-operations are usually more efficient, if the original set comes
   * from the same source, this allows to share sets.
   *
   * @param dg a ExceptionalUnitGraph.
   * @param unitToGen the equivalentValue of each unit.
   * @param sideEffect the SideEffectTester that performs kills.
   * @param BoundedFlowSet the shared set.
   */
  public DownSafetyAnalysis(DirectedGraph<Unit> dg, Map unitToGen, SideEffectTester
			    sideEffect, BoundedFlowSet set) {
    super(dg);
    this.sideEffect = sideEffect;
    this.set = set;
    unitToGenerateMap = unitToGen;
    doAnalysis();
  }

  protected FlowSet newInitialFlow() {
    return (FlowSet) set.topSet();
  }

  protected FlowSet entryInitialFlow() {
    return (FlowSet) set.emptySet();
  }

  protected void flowThrough(FlowSet inValue, Unit unit, FlowSet outValue) {
    FlowSet in = (FlowSet) inValue, out = (FlowSet) outValue;

    in.copy(out);

    { /* Perform kill */
      Unit u = (Unit)unit;

      Iterator outIt = (out).iterator();
      // iterate over things (avail) in out set.
      while (outIt.hasNext()) {
        EquivalentValue equiVal = (EquivalentValue)outIt.next();
        Value avail = equiVal.getValue();
        if (avail instanceof FieldRef || avail instanceof ArrayRef) {
          if (sideEffect.unitCanWriteTo(u, avail))
            out.remove(equiVal);
        } else {
          Iterator usesIt = avail.getUseBoxes().iterator();

          // iterate over uses in each avail.
          while (usesIt.hasNext()) {
            Value use = ((ValueBox)usesIt.next()).getValue();
            if (sideEffect.unitCanWriteTo(u, use)) {
              out.remove(equiVal);
              break;
            }
          }
        }
      }
    }

    // Perform generation
    Value add = (Value)unitToGenerateMap.get(unit);
    if (add != null)
      out.add(add, out);
  }

  protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
    FlowSet inSet1 = (FlowSet) in1;
    FlowSet inSet2 = (FlowSet) in2;

    FlowSet outSet = (FlowSet) out;

    inSet1.intersection(inSet2, outSet);
  }

  protected void copy(FlowSet source, FlowSet dest) {
    FlowSet sourceSet = (FlowSet) source;
    FlowSet destSet = (FlowSet) dest;

    sourceSet.copy(destSet);
  }
}

