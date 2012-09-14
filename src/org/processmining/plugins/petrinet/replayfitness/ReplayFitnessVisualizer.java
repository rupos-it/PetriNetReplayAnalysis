package org.processmining.plugins.petrinet.replayfitness;

import javax.swing.JComponent;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

@Plugin(name = "Fitness Visualizer", returnLabels = { "Visualized Fitness" }, returnTypes = { JComponent.class }, parameterLabels = { "Fitness" }, userAccessible = false)
@Visualizer
public class ReplayFitnessVisualizer {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, ReplayFitness fitness) {
		try {
			ReplayFitnessConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayFitnessConnection.class, context, fitness);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			XLog log = connection.getObjectWithRole(ReplayFitnessConnection.XLOG);
			Petrinet net = connection.getObjectWithRole(ReplayFitnessConnection.PNET);
			return getVisualizationPanel(context, net, log, fitness);
		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);
			return null;
		}
	}

	private JComponent getVisualizationPanel(PluginContext context, Petrinet net, XLog log, ReplayFitness fitness) {
		Progress progress = context.getProgress();
		ReplayFitnessPanel panel = new ReplayFitnessPanel(context, net, log, progress, fitness);
		return panel;
	}
}
