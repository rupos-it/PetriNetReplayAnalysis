package org.processmining.plugins.connectionfactories.logpetrinet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.processmining.plugins.connectionfactories.logpetrinet.*;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class LogPetrinetAssUI extends LogPetrinetConnectionFactoryUI {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7885986532303653221L;
	
	private boolean similarity=false;
	
	public LogPetrinetAssUI(XLog log, PetrinetGraph net,
			Object[] availableClassifier) {
		super(log, net, availableClassifier);
		
	}
	/**
	 * Returns the Event Option Box index of the most similar event for the
	 * transition.
	 * 
	 * @param transition
	 *            Name of the transitions
	 * @param events
	 *            Array with the options for this transition
	 * @return Index of option more similar to the transition
	 */
	private int preSelectOption(String transition, Object[] events, Pattern pattern) {
		Matcher matcher = pattern.matcher(transition);
		if(matcher.find() && matcher.start()==0){
			return 0;
		}
		if(!similarity){
			//The metric to get the similarity between strings
			AbstractStringMetric metric = new Levenshtein();
			if( transition.contains("GW_"))
				return 0;
			if( transition.contains("_"))
				transition=	transition.replace("_", "#");

			int index = 0;
			float simOld = Float.MIN_VALUE;
			for (int i = 1; i < events.length; i++) {
				String event = ((XEventClass) events[i]).toString();
				float sim = metric.getSimilarity(transition, event);

				if (simOld < sim) {
					simOld = sim;
					index = i;
				}
			}

			return index;
		}else{
			for (int i = 1; i < events.length; i++) {
				String event = ((XEventClass) events[i]).toString();
				int h = event.indexOf("+");
				if(h>0)
					event=event.substring(0, h);
				if (transition.toLowerCase().equals(event.toLowerCase())) {
					return i;
				}
			}

			return 0;
		}
	}

}
