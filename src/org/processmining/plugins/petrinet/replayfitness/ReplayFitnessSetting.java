package org.processmining.plugins.petrinet.replayfitness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.ReplaySettings;

public class ReplayFitnessSetting implements ReplaySettings<ReplayFitnessCost> {

	private final Map<ReplayAction, Integer> weights;
	private final Map<ReplayAction, Boolean> actions;

	public ReplayFitnessSetting() {
		actions = new HashMap<ReplayAction, Boolean>();
		actions.put(ReplayAction.INSERT_ENABLED_MATCH, true);
		actions.put(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		actions.put(ReplayAction.REMOVE_HEAD, true);
		actions.put(ReplayAction.INSERT_ENABLED_MISMATCH, true);
		actions.put(ReplayAction.INSERT_DISABLED_MATCH, true);
		actions.put(ReplayAction.INSERT_DISABLED_MISMATCH, false);

		weights = new HashMap<ReplayAction, Integer>();
		weights.put(ReplayAction.INSERT_ENABLED_MATCH, 1);
		weights.put(ReplayAction.INSERT_ENABLED_INVISIBLE, 10);
		weights.put(ReplayAction.REMOVE_HEAD, 100);
		weights.put(ReplayAction.INSERT_ENABLED_MISMATCH, 100);
		weights.put(ReplayAction.INSERT_DISABLED_MATCH, 100);
		weights.put(ReplayAction.INSERT_DISABLED_MISMATCH, 1000);
	}

	public Integer getWeight(ReplayAction action) {
		return weights.get(action);
	}

	public void setWeight(ReplayAction action, int weight) {
		weights.put(action, weight);
	}

	public ReplayFitnessCost getInitialCost() {
		return new ReplayFitnessCost(0, this);
	}

	public ReplayFitnessCost getMaximalCost() {
		return new ReplayFitnessCost(Integer.MAX_VALUE, this);
	}

	public void setAction(ReplayAction action, boolean isEnabled) {
		actions.put(action, isEnabled);
	}

	public boolean isAllowed(ReplayAction action) {
		return actions.get(action);
	}

	public boolean isFinal(Marking marking, List<? extends Object> trace) {
		return trace.isEmpty();
	}
}
