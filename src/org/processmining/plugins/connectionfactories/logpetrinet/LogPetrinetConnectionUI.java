package org.processmining.plugins.connectionfactories.logpetrinet;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.ArrayUtils;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replay.ReplayAction;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class LogPetrinetConnectionUI extends LogPetrinetConnectionFactoryUI{
    
	Boolean similarity=false;
	/** Log */
	private final XLog log;
	/** PetriNet */
	private final PetrinetGraph net;
	
	public LogPetrinetConnectionUI(XLog l, PetrinetGraph pn, Object[] availableEventClass) {
		super(l, pn, availableEventClass);
		log = l;
		net = pn;
	}
	
	public Boolean getSimilarity() {
		return similarity;
	}

	public void setSimilarity(Boolean similarity) {
		this.similarity = similarity;
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
	private int preSelectOption(String transition, Object[] events) {
		if(similarity){
		//The metric to get the similarity between strings
		AbstractStringMetric metric = new Levenshtein();
		
		int index = 0;
		float simOld = metric.getSimilarity(transition, "none");
		simOld = Math.max(simOld, metric.getSimilarity(transition, "invisible"));
		simOld = Math.max(simOld, metric.getSimilarity(transition, "skip"));
		simOld = Math.max(simOld, metric.getSimilarity(transition, "tau"));

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
				

				if (transition.equals(event)) {
					return i;
				}
			}
			
			return 0;
		}
	}
	
	public JComponent initComponentsDifferntMapping(String stringlabel) {
		
		JPanel panel = new JPanel();
		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayout.FILL,0.3 }, { TableLayout.FILL,0.3  } };
		panel.setLayout(new TableLayout(size));

		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				similarity= !similarity;
				
			}
		};
		
		JLabel label = slickerFactory.createLabel(stringlabel);
		panel.add(label,"0,0");
		JCheckBox checkbox = slickerFactory.createCheckBox("Log to Transition 1:1", similarity);
		checkbox.addChangeListener(changeListener);
		panel.add(checkbox,"0,1");

	/*	for (ReplayAction action : ReplayAction.values()) {
			sliderMap.put(action, slickerFactory.createNiceIntegerSlider("", 1, 1000, setting.getWeight(action),
					Orientation.HORIZONTAL));
			sliderMap.get(action).setPreferredSize(new Dimension(220, 20));
			checkBoxMap.put(action, slickerFactory.createCheckBox("", setting.isAllowed(action)));
			checkBoxMap.get(action).addChangeListener(changeListener);
			panel.add(slickerFactory.createLabel("<html><h3>" + action.getLabel() + "</h3>"), "0, "
					+ (action.getValue() + 1));
			panel.add(checkBoxMap.get(action), "1, " + (action.getValue() + 1));
			panel.add(sliderMap.get(action), "2, " + (action.getValue() + 1));
		}*/

		changeListener.stateChanged(null);

		return panel;
		
		
	}

}
