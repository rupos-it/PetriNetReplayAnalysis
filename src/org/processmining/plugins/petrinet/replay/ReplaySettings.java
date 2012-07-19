package org.processmining.plugins.petrinet.replay;

import java.util.List;

import org.processmining.models.semantics.petrinet.Marking;

/**
 * Settings used by the cost-based Petri net trace replayer.
 * 
 * @author hverbeek
 * 
 * @param <C>
 *            The class implementing the cost structure.
 */
public interface ReplaySettings<C extends ReplayCost> {

	/**
	 * Returns the initial cost. Typically 0 or something like it.
	 * 
	 * @return The initial cost.
	 */
	public C getInitialCost();

	/**
	 * Returns the maximal cost. Solutions with cost that exceed this cost are
	 * not considered.
	 * 
	 * @return The maximal cost.
	 */
	public C getMaximalCost();

	/**
	 * Returns whether the trace has been replayed successfully.
	 * 
	 * @param marking
	 *            The current marking.
	 * @param trace
	 *            The trace still to replay.
	 * @return Whether the trace has been replayed successfully.
	 */
	public boolean isFinal(Marking marking, List<? extends Object> trace);

	/**
	 * Returns whether the given action type is allowed.
	 * 
	 * @param action
	 *            The given action.
	 * @return Whether the given action type is allowed.
	 */
	public boolean isAllowed(ReplayAction action);
}
