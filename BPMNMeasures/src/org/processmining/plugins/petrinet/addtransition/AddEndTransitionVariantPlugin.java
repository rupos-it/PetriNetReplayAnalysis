

package org.processmining.plugins.petrinet.addtransition;

import java.util.Collection;
import java.util.LinkedList;


import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;

import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;

import org.processmining.plugins.bpmn.BPMNtoPNConnection;






public class AddEndTransitionVariantPlugin extends AddEndTransitionPlugin {


	//@PluginVariant(requiredParameterLabels = { 0 },variantLabel="variant")
	@Plugin(name = "Add Artificial End Transition Variant", parameterLabels = { "PetriNet" }, returnLabels = { "PetriNet" }, returnTypes = { Petrinet.class })
	@UITopiaVariant(affiliation = "UNIPI", author = "GOs", email = "")
	public Petrinet addVariantTransition(PluginContext context, Petrinet oldnet){

		Petrinet net =  this.addTransition(context, oldnet);

		this.fixconnection(context, oldnet, net);

		return net;
	}

	private void fixconnection(PluginContext context, Petrinet oldnet,
			Petrinet net) {

		try {
			BPMNtoPNConnection connection = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, oldnet);

			// connection found. Create all necessary component to instantiate inactive visualization panel

			BPMNDiagram bpmn = connection.getObjectWithRole(BPMNtoPNConnection.BPMN);
			String error = connection.getObjectWithRole(BPMNtoPNConnection.ERRORLOG);
			Collection<Place> Collection = connection.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);

			Collection<Place> placeFlowCollection = fixplaceFlowCollection(Collection,net);

			context.addConnection(new BPMNtoPNConnection(bpmn, net, error, placeFlowCollection));

		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);


		}

	}

	private java.util.Collection<Place> fixplaceFlowCollection(
			Collection<Place> collection, Petrinet net) {
		Collection<Place> placeFlowCollection = new LinkedList<Place>();

		for(Place old : collection){
			for(Place p: net.getPlaces() ){
				if(old.getLabel()==p.getLabel()){
					placeFlowCollection.add(p);
					break;
				}
				
			}
		}
		return placeFlowCollection;
	}

}
