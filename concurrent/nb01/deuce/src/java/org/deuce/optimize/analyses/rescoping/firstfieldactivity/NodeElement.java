package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

// types of nodes:
// 1. single + must instrument
// 2. single + don't need to instrument
// 3. multiple + must instrument
// 4. multiple + don't need to instrument

// flow statuses:
// a. no init necessary yet
// b. definitely inited already
// c. possibly inited already

// node designation:
// i. recurring init point
// ii. initial init point
// iii. neither 

/* algorithm description:
 * phase 1: classify each node into a type: 1, 2, 3 or 4.
 * phase 2: for nodes of type 3, propagate backwards the "must instrument" property, to all predecessors.
 * so nodes of type 4 become 3 and nodes of type 2 become 1. 
 * stop propagating at nodes of type 1. if graph's head is of type 3 or 1, stop right 
 * there - no optimization is possible.
 * phase 3: do a forward dataflow analysis. if the current scc must be instrumented:
 * if scc was a, designate it as ii, and flow b.
 * if scc was b, flow b.
 * if scc was c, designate it as i, and flow b.
 * the merge rule is: x&x=x. x&c=c. a&b=c.
 * phase 4: for all nodes designated as i and ii, add the proper initialization before them.
 */

public enum NodeElement {
	NoInitNecessaryYet, // only seen 2 and 4 so far 
	InitialInitPoint, // node must be 1
	RecurringInitPoint, // node must be 1

	InitedOnce, InitedMoreThanOnce
}
