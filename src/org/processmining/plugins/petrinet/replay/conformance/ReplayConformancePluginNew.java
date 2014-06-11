package org.processmining.plugins.petrinet.replay.conformance;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.Vector;

import javax.swing.JComponent;




import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import org.processmining.connections.logmodel.LogPetrinetConnectionImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;

import org.processmining.framework.connections.ConnectionCannotBeObtained;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.collection.AlphanumComparator;
import org.processmining.models.connections.petrinets.PNRepResultAllRequiredParamConnection;
import org.processmining.models.connections.petrinets.PNRepResultConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;

import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetAssUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.Replayer;
import org.processmining.plugins.petrinet.replay.util.PanelIntroPlugin;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisUI;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessCost;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.petrinet.visualization.AlignmentConstants;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;


@Plugin(name = "PN Conformace Analysis New", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {"Log", "Petrinet", "PNRepResult", "Marking"}, userAccessible = true)
public class ReplayConformancePluginNew {

	private boolean getColor(StepTypes stepTypes) {
		switch (stepTypes){
		case L : 
			return true; //AlignmentConstants.MOVELOGCOLOR;
		case MINVI : 
			return true;//AlignmentConstants.MOVEMODELINVICOLOR;
		case MREAL : 
			return false; //AlignmentConstants.MOVEMODELREALCOLOR;
		case LMNOGOOD : 
			return false;//AlignmentConstants.MOVESYNCVIOLCOLOR;
		case LMGOOD : 
			return true;// AlignmentConstants.MOVESYNCCOLOR;
		case LMREPLACED:
			return false;//AlignmentConstants.MOVEREPLACEDCOLOR;
		case LMSWAPPED:
			return false;// AlignmentConstants.MOVESWAPPEDCOLOR;
		default :
				return false; // unknown
		}
	}
	
	public Transition getTranstoMap(TransEvClassMapping mapping, XEventClass event){
		for(Transition t: mapping.keySet()){
			 XEventClass e = mapping.get(t);
			if(e==event){
				
				return t;
			}
		}
		
		return null;
	}

	public List<Transition> getListTransition(PNRepResult logReplayResult,
			String tracename, XLog log,TransEvClassMapping mapping) {
		List<Transition> result = new LinkedList<Transition>();

		for (SyncReplayResult res : logReplayResult) {

			SortedSet<Integer> traceIndex = res.getTraceIndex();
			XConceptExtension ce = XConceptExtension.instance();
			for (int index : traceIndex) {
				String name = ce.extractName(log.get(index));
				if (name == tracename) {
					int x = 0;
					List<Object> lobj = res.getNodeInstance();
					for (int i=0;i<lobj.size();i++) {
						Object obj = lobj.get(i);
						if (obj instanceof Transition) {
							Transition t = ((Transition) obj);
							if (getColor(res.getStepTypes().get(x))) {
								result.add(t);
							} else {
								if (res.getNodeInstance().size() - x + 2 >= 0) {
									StepTypes tt = res.getStepTypes()
											.get(x + 1);
									StepTypes tt1 = res.getStepTypes().get(
											x + 2);
									if (tt == StepTypes.MINVI
											& tt1 == StepTypes.MINVI) {
										i=i+2;
										x=x+2;
									}
								}

							}
						}else{
							if(obj instanceof XEventClass){
								XEventClass e = ((XEventClass) obj);
								Transition t = getTranstoMap(mapping,e);
								if (getColor(res.getStepTypes().get(x))) {
									result.add(t);
								} 
							}
							
							
						}
						
						x++;
					}

				}

			}
			// reformat node instance list
			// List<Object> result = new LinkedList<Object>();

			// create combobox
			/*
			 * SortedSet<String> caseIDSets = new TreeSet<String>(new
			 * AlphanumComparator()); XConceptExtension ce =
			 * XConceptExtension.instance(); for (int index :
			 * res.getTraceIndex()) { String name =
			 * ce.extractName(log.get(index)); if (name == null) { name =
			 * String.valueOf(index); } caseIDSets.add(name); } int caseIDSize =
			 * caseIDSets.size();
			 */
		}
		System.out.println(result);
		return result;
	}

	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net, Marking marking, PNRepResult resultpn  ) {
		TotalConformanceResult totalResult = new TotalConformanceResult();
		totalResult.setTotal(new ConformanceResult("Total"));
		totalResult.setList(new Vector<ConformanceResult>());

		//= XLogInfoImpl.STANDARD_CLASSIFIER;
	
		

		int replayedTraces = 0;
		int i =0;
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(log.size());
		TransEvClassMapping mapping=null;
		try {
			PNRepResultAllRequiredParamConnection conn = context.getConnectionManager().getFirstConnection(
					PNRepResultAllRequiredParamConnection.class, context, resultpn);

			//net = conn.getObjectWithRole(PNRepResultConnection.PN);
			//log = conn.getObjectWithRole(PNRepResultConnection.LOG);
			mapping = conn.getObjectWithRole(PNRepResultAllRequiredParamConnection.TRANS2EVCLASSMAPPING);
		} catch (Exception exc) {
			context.log("No mapping can be found for this log replay result");
		}
		for (XTrace trace : log) {
			
			try {
				System.out.println("Replay :" + ++i);
				context.getProgress().inc();
				String tracename = getTraceName(trace);
				System.out.println("Replayed "+ tracename);
				List<Transition> sequence = getListTransition(resultpn,tracename,log,mapping);//replayer.replayTrace(marking, list, setting);
				
				updateConformance(net, marking, sequence, totalResult,tracename);
				replayedTraces++;
				System.out.println("Replayed Ok!");

			} catch (Exception ex) {
				System.out.println("Failed");
				context.log("Replay of trace " + trace + " failed: " + ex.getMessage());
			}
		}

		context.log("(based on a successful replay of " + replayedTraces + " out of " + log.size() + " traces)");

		totalResult.getTotal().updateConformance();
		ReplayAnalysisConnection connection = new ReplayAnalysisConnection(totalResult, log, net);
		context.getConnectionManager().addConnection(connection);
		return totalResult;
	}

	private static String getTraceName(XTrace trace) {
		String traceName = XConceptExtension.instance().extractName(trace);
		return (traceName != null ? traceName : "<unknown>");
	}

	private void addArcUsage(Arc arc, ConformanceResult fitnessResult, ConformanceResult totalResult) {
		Integer numUsage = totalResult.getMapArc().get(arc);
		totalResult.getMapArc().put(arc, numUsage == null ? 1 : numUsage+1);
		numUsage = fitnessResult.getMapArc().get(arc);
		fitnessResult.getMapArc().put(arc, numUsage == null ? 1 : numUsage+1);
	}


	private void updateConformance(Petrinet net, Marking initMarking, List<Transition> sequence, TotalConformanceResult totalResult, String tracename) {
		Marking marking = new Marking(initMarking);
		int producedTokens = marking.size();
		int consumedTokens = 0;
		int producedTrace  = marking.size();
		int consumedTrace = 0;

		int missingTrace=0;
		ConformanceResult tempConformanceResult = new ConformanceResult(tracename);
		totalResult.getList().add(tempConformanceResult);

		for (Transition transition : sequence) {
			boolean fittingTransition = true;
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net
					.getInEdges(transition);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preset) {
				if (edge instanceof Arc) {
					Arc arc = (Arc) edge;
					addArcUsage(arc, tempConformanceResult, totalResult.getTotal());
					Place place = (Place) arc.getSource();
					int consumed = arc.getWeight();
					int missing = 0;
					//
					if (arc.getWeight() > marking.occurrences(place)) {
						missing = arc.getWeight() - marking.occurrences(place);
					}
					for (int i = missing; i < consumed; i++) {
						marking.remove(place);
					}
					// Rupos patches
					for (int i = 0; i < missing; i++) {
						totalResult.getTotal().getMissingMarking().add(place);
						tempConformanceResult.getMissingMarking().add(place);
					}
					consumedTokens += consumed;
					consumedTrace += consumed;
					missingTrace +=missing;
					//se sono mancati token, questa transizione non ha fittato per questa traccia
					if (missing>0) fittingTransition = false;
				}
			}
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net
					.getOutEdges(transition);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postset) {
				if (edge instanceof Arc) {
					Arc arc = (Arc) edge;
					addArcUsage(arc, tempConformanceResult, totalResult.getTotal());
					Place place = (Place) arc.getTarget();
					int produced = arc.getWeight();
					for (int i = 0; i < produced; i++) {
						marking.add(place);
					}
					producedTokens += produced;
					producedTrace +=produced;
				}
			}
			if (!fittingTransition) {
				Integer numTraceNotFittingForTransaction = totalResult.getTotal().getMapTransition().get(transition);
				totalResult.getTotal().getMapTransition().put(transition, numTraceNotFittingForTransaction == null ? 1 : numTraceNotFittingForTransaction+1);
				//sulla singola traccia, il numero di tracce che non fittano la transizione è 0 o 1
				tempConformanceResult.getMapTransition().put(transition, 1);
			}

		}
		consumedTokens += marking.size();
		consumedTrace += marking.size();
		int remainingTokens = marking.isEmpty() ? 0 : marking.size() - 1;

		// Rupos patches
		totalResult.getTotal().getRemainingMarking().addAll(marking);
		tempConformanceResult.getRemainingMarking().addAll(marking);

		//calcola la fitness del singolo trace
		tempConformanceResult.setProducedTokens(producedTokens);
		tempConformanceResult.setConsumedTokens(consumedTokens);

		// Per il totale
		totalResult.getTotal().setProducedTokens(totalResult.getTotal().getProducedTokens() + producedTokens);
		totalResult.getTotal().setConsumedTokens(totalResult.getTotal().getConsumedTokens() + consumedTokens);

		// Calcola la fitness per il singolo trace
		tempConformanceResult.updateConformance();
	}

	private List<XEventClass> getList(XTrace trace, XEventClasses classes) {
		List<XEventClass> list = new ArrayList<XEventClass>();
		for (XEvent event : trace) {
			list.add(classes.getClassOf(event));
		}
		return list;
	}

	private XEventClasses getEventClasses(XLog log, XEventClassifier classifier) {
		 
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses eventClasses = summary.getEventClasses(classifier);
		return eventClasses;
	}

	private Map<Transition, XEventClass> getMapping(XEventClasses classes, Petrinet net) {
		Map<Transition, XEventClass> map = new HashMap<Transition, XEventClass>();

		for (Transition transition : net.getTransitions()) {
			boolean visible=false;
			for (XEventClass eventClass : classes.getClasses()) {
				if (eventClass.getId().equals(transition.getAttributeMap().get(AttributeMap.LABEL))) {
					map.put(transition, eventClass);
					visible=true;
				}
			}
			if(!visible){
				transition.setInvisible(true);
			}

		}
		return map;
	}


	

	// Rupos public methos

	//@Plugin(name = "ConformaceDetailsUI", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@PluginVariant(requiredParameterLabels = { 0,1,2})
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalConformanceResult getConformanceDetails(UIPluginContext context, XLog log, Petrinet net,PNRepResult resultpn) {
		
	

		Marking marking;

		try {
			InitialMarkingConnection connection = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			marking = connection.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained ex) {
			context.log("Petri net lacks initial marking");
			context.getFutureResult(0).cancel(true);
			return null;
		}

		TotalConformanceResult totalResult = getConformanceDetails(context, log, net,marking,resultpn);


		return totalResult;
	}

	@PluginVariant(requiredParameterLabels = { 0,1})
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalConformanceResult getConformanceDetails(UIPluginContext context, XLog log, Petrinet net) {
		
		PNLogReplayer plogr = new PNLogReplayer();
		PNRepResult resultpn=null;
		try{
		 resultpn =  plogr.replayLogGUI(context, net, log);
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		
		
		Marking marking;

		try {
			InitialMarkingConnection connection = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			marking = connection.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained ex) {
			context.log("Petri net lacks initial marking");
			context.getFutureResult(0).cancel(true);
			return null;
		}

		TotalConformanceResult totalResult = getConformanceDetails(context, log, net,marking,resultpn);


		return totalResult;
	}

	

	//@Plugin(name = "ConformanceDetails", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	/*@PluginVariant(requiredParameterLabels = { 0,1 })
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net) {
		ReplayFitnessSetting setting = new ReplayFitnessSetting();
		suggestActions(setting, log, net,XLogInfoImpl.STANDARD_CLASSIFIER);

		TotalConformanceResult total = getConformanceDetails(context, log, net, setting);


		return total;
	}*/

	//@Plugin(name = "ConformanceDetailsSettingsWithMarking", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	@PluginVariant(requiredParameterLabels = { 0,1,2,3 })
	public TotalConformanceResult getFitnessDetails(PluginContext context, XLog log, Petrinet net, PNRepResult resultpn, Marking marking) {


		Map<Transition, XEventClass> map=null;
		
		TotalConformanceResult total = getConformanceDetails(context, log, net, marking,resultpn);

		return total;
	}

	//@Plugin(name = "ConformanceDetailsSettings", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	@PluginVariant(requiredParameterLabels = { 0,1,2 })
	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net, PNRepResult resultpn) {

		Marking marking;

		try {
			InitialMarkingConnection connection = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			marking = connection.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained ex) {
			context.log("Petri net lacks initial marking");
			System.out.println("**************** NO MARKING **************");
			context.getFutureResult(0).cancel(true);
			return null;
		}
		Map<Transition, XEventClass> map=null;
		TotalConformanceResult total = getConformanceDetails(context, log, net, marking,resultpn );

		return total;
	}





}
