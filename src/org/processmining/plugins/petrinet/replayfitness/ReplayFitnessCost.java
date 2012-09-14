package org.processmining.plugins.petrinet.replayfitness;

import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.ReplayCost;
import org.processmining.plugins.petrinet.replay.ReplayCostAddOperator;

public class ReplayFitnessCost implements ReplayCost, Comparable<ReplayFitnessCost> {

	private Integer cost;
	private final ReplayFitnessSetting setting;

	public ReplayFitnessCost(Integer cost, ReplayFitnessSetting setting) {
		this.cost = cost;
		this.setting = setting;
	}

	public static ReplayCostAddOperator<ReplayFitnessCost> addOperator = new ReplayCostAddOperator<ReplayFitnessCost>() {
		public ReplayFitnessCost add(ReplayFitnessCost cost, ReplayAction action, Transition transition, Object object) {
			ReplayFitnessCost newCost = new ReplayFitnessCost(cost.cost, cost.setting);
			newCost.cost += cost.setting.getWeight(action);
			return newCost;
		}
	};

	public int compareTo(ReplayFitnessCost cost) {
		return this.cost.compareTo(cost.cost);
	}

	public boolean isAcceptable() {
		return true;
	}

	public int hashCode() {
		return cost.hashCode();
	}

	public String toString() {
		return cost.toString();
	}

	public boolean equals(Object o) {
		if (o instanceof ReplayFitnessCost) {
			return cost.equals(((ReplayFitnessCost) o).cost);
		}
		return false;
	}
}
