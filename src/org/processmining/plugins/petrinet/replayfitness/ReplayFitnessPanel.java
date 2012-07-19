package org.processmining.plugins.petrinet.replayfitness;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.plugins.log.ui.logdialog.LogViewUI;

public class ReplayFitnessPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1078379484400548912L;

	public ReplayFitnessPanel(PluginContext context, Petrinet net, XLog log, Progress progress, ReplayFitness fitness) {

		JComponent netView = ProMJGraphVisualizer.instance().visualizeGraph(context, net);
		JComponent logView = new LogViewUI(log);
		JComponent fitnessView = new JLabel("Fitness value: " + fitness.getValue());

		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL, 30, TableLayoutConstants.FILL } };
		setLayout(new TableLayout(size));

		add(netView, "0, 0");
		add(fitnessView, "0, 1");
		add(logView, "0, 2");
	}
}
