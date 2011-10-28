package org.processmining.plugins.bpmn.exporting.metrics;





import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExt;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExtFactory;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Artifacts;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;
import org.processmining.models.graphbased.directed.bpmn.elements.Artifacts.ArtifactType;


import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import org.processmining.plugins.bpmn.BPMNtoPNConnection;

import org.processmining.plugins.petrinet.replay.conformance.ConformanceResult;
import org.processmining.plugins.petrinet.replay.conformance.TotalConformanceResult;
import org.processmining.plugins.petrinet.replay.performance.PerformanceResult;
import org.processmining.plugins.petrinet.replay.performance.TotalPerformanceResult;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;




public class MeasuresBPMNintoMetricsPlugin {
	
	
	@Plugin(name = "BPMNMAnalisysDetailsintoMetricsConformance", parameterLabels = {  "TotalConformanceResult" }, returnLabels = { "BPMN Metrics traslate" }, returnTypes = {
			List.class }, userAccessible = true)
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "GOS", email = "Di.unipi", pack = "BPMNMeasures")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "Exporting  Total Conformance to BPMN Metrics")
	public Object exportBPMNexportXPDL(PluginContext context, TotalConformanceResult totalconformanceresult) throws Exception {

		List<BPMNConfMetrics> metrics=null;
		try {
			ReplayAnalysisConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayAnalysisConnection.class, context, totalconformanceresult);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			// log = connection.getObjectWithRole(ReplayRuposConnection.XLOG);
			Petrinet net= connection.getObjectWithRole(ReplayAnalysisConnection.PNET);
		
			BPMNtoPNConnection connection2 = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, net);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			
		    BPMNDiagram bpmn = connection2.getObjectWithRole(BPMNtoPNConnection.BPMN);
		    Collection<Place> placeFlowCollection = connection2.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);
			
			metrics = annotateMetricsConformance(totalconformanceresult,net,placeFlowCollection,bpmn);


		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);
			
		}
		

		return metrics;

	}

	
	private List<BPMNConfMetrics> annotateMetricsConformance(
			TotalConformanceResult totalconformanceresult, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {
		
			List<BPMNConfMetrics> listaMetrics = new LinkedList<BPMNConfMetrics>();
			
			for( ConformanceResult conformance: totalconformanceresult.getList()){
				
				BPMNConfMetrics metric = annotateMetricConformance(conformance,net,placeFlowCollection,bpmn);
				listaMetrics.add(metric);
			}
		
		
		
		return listaMetrics;
	}
	
	


	private BPMNConfMetrics annotateMetricConformance(
			ConformanceResult conformance, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {
		
		BPMNConfMetrics metric = new BPMNConfMetrics();
		
		metric.setTraceName(conformance.getTracename());
		Marking remaning = conformance.getRemainingMarking();
		Marking missing = conformance.getMissingMarking();
		
		Map<Arc, Integer> attivazionearchi = conformance.getMapArc();

		Map<String, Integer> ArchiAttivatiBPMN = new HashMap<String, Integer>();
		Map<String, String> archibpmnwitherrorconformance = new HashMap<String, String>();

		// gli archi che attivo sul bpmn sono gli archi uscenti delle piazze
		// "arco"
		for (Place p : placeFlowCollection) {
			int att = 0;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> egde : p
					.getGraph().getOutEdges(p)) {
				if (attivazionearchi.containsKey(egde)) {
					att += attivazionearchi.get(egde);

				}

			}
			ArchiAttivatiBPMN.put(p.getLabel(), att);
			
		
		}

		Map<Activity,Artifacts> mapActiArtic = new HashMap<Activity, Artifacts>();
		// transizioni che nn fittano
		String ret = "<br/>";
		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				String tname = t.getLabel();
				String name = (String) tname.subSequence(0, tname.indexOf("+"));
				
				Activity activity = null;
				
				// cerco l'attività bpmn a cui collegare le metriche
				for (Activity a : bpmn.getActivities()) {
					if (a.getLabel().equals(name)) {
						activity = a;
						break;
					}
				}
				TaskConfMetrics metritask = new TaskConfMetrics();
			
				for (Place p : remaning) {
					if (p.getLabel().equals(name)&& tname.endsWith("start")) {
						metritask.addMissingCompletitions();
						
					} else if (p.getLabel().startsWith(name) && !tname.endsWith("start") ) {
						metritask.addInterruptedExecutions();
					}
				}
				for (Place p : missing) {
					if (p.getLabel().equals(name)&& tname.endsWith("start")) {
						metritask.addInternalFailure();
					}
					if(p.getLabel().endsWith(name)&& tname.endsWith("start")){
						metritask.addUnsoundExecutions();
					}
				}
				String label = activity.getLabel();
				
				if (activity != null) {
					
					
					
					if(!metric.getTaskMetrics().containsKey(label)){

						metric.addTaskMetrics(label, metritask);
						

						
					}else{
						
						TaskConfMetrics exmetric = metric.getTaskMetrics().get(label);
						exmetric.updateMetric(metritask);
						
					}
					

				}

			}

			
			
		
			// cerco la transizione del fork
			//t.getLabel().endsWith("_fork")			
			if (t.getGraph().getOutEdges(t).size()>1 && t.isInvisible()) {
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> p = t
				.getGraph().getOutEdges(t);
				
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : p) {
					Place target = (Place) e.getTarget();
					if(remaning.contains(target)){
						
						ForkConfMetrics forkmetric = new ForkConfMetrics();
						
						forkmetric.addInterruptedBranches(target.getLabel(),remaning.occurrences(target));
						metric.addForkMetrics(t.getLabel(), forkmetric);
					}
					
				}
				
			}
		}
		// metto gli attraversamenti sugli archi bpmn
		
		
		
		return metric;
	}


	@Plugin(name = "BPMNMAnalisysDetailsintoMetricsPerformance", parameterLabels = {  "TotalPerformanceResult" }, returnLabels = { "BPMN Metrics traslate" }, returnTypes = {
			List.class }, userAccessible = true)
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "GOS", email = "Di.unipi", pack = "BPMNMeasures")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "BPMN Performance traslate")
	public Object exportBPMNexportXPDL(PluginContext context,  TotalPerformanceResult totalPerformanceresult) throws Exception {

		List<BPMNPerfMetrics> metrics=null;
		
		
		
		try {
			ReplayAnalysisConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayAnalysisConnection.class, context, totalPerformanceresult);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			//XLog log = connection.getObjectWithRole(ReplayRuposConnection.XLOG);
			Petrinet net= connection.getObjectWithRole(ReplayAnalysisConnection.PNET);
		
			BPMNtoPNConnection connection2 = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, net);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			
		   BPMNDiagram bpmn = connection2.getObjectWithRole(BPMNtoPNConnection.BPMN);
		   Collection< Place>  placeFlowCollection = connection2.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);
			
		   //cambiare con total
		   return metrics;
			
			 

		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);
			return null;
		}

	}
	
	


}




