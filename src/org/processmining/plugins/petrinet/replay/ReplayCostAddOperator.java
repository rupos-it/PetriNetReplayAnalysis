package org.processmining.plugins.petrinet.replay;

import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

/**
 * Adding the cost for an action to a certain cost.
 * 
 * @author hverbeek
 * 
 * @param <C>
 *            The class implementing the cost structure.
 */
public interface ReplayCostAddOperator<C extends ReplayCost> {
	/**
	 * Returns a new cost where the costs of the given action have been added to
	 * the current costs.
	 * 
	 * @param cost
	 *            The current costs.
	 * @param action
	 *            The given action.
	 * @param transition
	 *            The transition involved in the action, null if action is
	 *            REMOVE_HEAD.
	 * @param object
	 *            The head of the trace involved in the action, null if action
	 *            did not involve a MATCH.
	 * @return The new costs.
	 */
	public C add(C cost, ReplayAction action, Transition transition, Object object);
}
