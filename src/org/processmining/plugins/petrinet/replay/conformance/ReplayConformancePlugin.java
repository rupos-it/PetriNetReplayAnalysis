package org.processmining.plugins.petrinet.replay.conformance;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetConnectionUI;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.Replayer;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisUI;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessCost;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;


@Plugin(name = "PN Conformace Analysis", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {"Log", "Petrinet", "ReplayFitnessSetting", "Marking"}, userAccessible = true)
public class ReplayConformancePlugin {




	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net, Marking marking, ReplayFitnessSetting setting,Map<Transition, XEventClass> map  ) {
		TotalConformanceResult totalResult = new TotalConformanceResult();
		totalResult.setTotal(new ConformanceResult("Total"));
		totalResult.setList(new Vector<ConformanceResult>());

		XEventClasses classes = getEventClasses(log);
		if(map==null){
			//Map<Transition, XEventClass> 
			map = getMapping(classes, net);
		}
		context.getConnectionManager().addConnection(new LogPetrinetConnectionImpl(log, classes, net, map));

		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);

		Replayer<ReplayFitnessCost> replayer = new Replayer<ReplayFitnessCost>(context, net, semantics, map,
				ReplayFitnessCost.addOperator);

		int replayedTraces = 0;
		int i =0;
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(log.size());
		for (XTrace trace : log) {
			List<XEventClass> list = getList(trace, classes);
			try {
				System.out.println("Replay :" + ++i);
				context.getProgress().inc();
				List<Transition> sequence = replayer.replayTrace(marking, list, setting);
				String tracename = getTraceName(trace);
				updateConformance(net, marking, sequence, semantics, totalResult,tracename);
				replayedTraces++;
				System.out.println("Replayed");

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


	private void updateConformance(Petrinet net, Marking initMarking, List<Transition> sequence, PetrinetSemantics semantics, TotalConformanceResult totalResult, String tracename) {
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

	private XEventClasses getEventClasses(XLog log) {
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
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


	private void suggestActions(ReplayFitnessSetting setting, XLog log, Petrinet net) {
		boolean hasInvisibleTransitions = false;
		Collection<String> transitionLabels = new HashSet<String>();
		for (Transition transition : net.getTransitions()) {
			transitionLabels.add((String) transition.getAttributeMap().get(AttributeMap.LABEL));
			if (transition.isInvisible()) {
				hasInvisibleTransitions = true;
			}
		}
		Collection<String> eventClassLabels = new HashSet<String>();
		for (XEventClass eventClass : getEventClasses(log).getClasses()) {
			eventClassLabels.add(eventClass.getId());
		}
		setting.setAction(ReplayAction.INSERT_ENABLED_MATCH, true);
		setting.setAction(ReplayAction.INSERT_DISABLED_MATCH, true);
		if (transitionLabels.containsAll(eventClassLabels)) {
			/*
			 * For every event class there is at least one transition. Thus,
			 * there is always a matching transition.
			 */
			setting.setAction(ReplayAction.REMOVE_HEAD, false);
			setting.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, false);
			setting.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, false);
		} else {
			setting.setAction(ReplayAction.REMOVE_HEAD, true);
			setting.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, true);
			setting.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, true);
		}
		if (hasInvisibleTransitions || !eventClassLabels.containsAll(transitionLabels)) {
			setting.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		} else {
			/*
			 * There are no explicit invisible transitions and all transitions
			 * correspond to event classes.
			 */
			setting.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, false);
		}
	}

	// Rupos public methos

	//@Plugin(name = "ConformaceDetailsUI", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@PluginVariant(requiredParameterLabels = { 0,1})
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalConformanceResult getConformanceDetails(UIPluginContext context, XLog log, Petrinet net) {
		ReplayFitnessSetting setting = new ReplayFitnessSetting();
		suggestActions(setting, log, net);
		ReplayAnalysisUI ui = new ReplayAnalysisUI(setting);
		//context.showWizard("Configure Conformance Settings", true, false, ui.initComponents());

		// list possible classifiers
		List<XEventClassifier> classList = new ArrayList<XEventClassifier>(log.getClassifiers());
		// add default classifiers
		if (!classList.contains(XLogInfoImpl.RESOURCE_CLASSIFIER)){
			classList.add(0, XLogInfoImpl.RESOURCE_CLASSIFIER);
		}
		//				if (!classList.contains(XLogInfoImpl.LIFECYCLE_TRANSITION_CLASSIFIER)){
		//					classList.add(0, XLogInfoImpl.LIFECYCLE_TRANSITION_CLASSIFIER);
		//				}
		if (!classList.contains(XLogInfoImpl.NAME_CLASSIFIER)){
			classList.add(0, XLogInfoImpl.NAME_CLASSIFIER);
		}
		if (!classList.contains(XLogInfoImpl.STANDARD_CLASSIFIER)){
			classList.add(0, XLogInfoImpl.STANDARD_CLASSIFIER);
		}

		Object[] availableEventClass = classList.toArray(new Object[classList.size()]);		


		//Build and show the UI to make the mapping
		LogPetrinetConnectionUI lpcfui = new LogPetrinetConnectionUI(log, net, availableEventClass);
		//InteractionResult result = context.showWizard("Mapping Petrinet - Log", false, true,  lpcfui.initComponents());

		//Create map or not according to the button pressed in the UI
		Map<Transition, XEventClass> map=null;
		InteractionResult result = InteractionResult.NEXT;
		/*
		 * The wizard loop.
		 */
		boolean sem=true;
		/*
		 * Show the current step.
		 */
		int currentStep=0;

		// TODO: Insert plugin description
		String label = "<html>"+
				"<h2>PetriNetReplayAnalysis: Conformance metrics <br></h2>" +
				"<p>This package implement the algorithms described on this article<sup>1</sup>.<br/>" +
				"<br/>This plugin replayed a log events on the bussiness process model. The" +
				" model is a Petri net and reproduces a bussiness process. <br/>" +
				"<br/>The result of Conformance plugin is a set Petri nets with annoted for all " +
				"place remaining, missing token and transition force enabled for all trace of log. <br> </p>" +
				"<br/><br/><p>The user guide for this plugin is <a href=\"https://svn.win.tue.nl/repos/prom/Documentation/\">" +
				"here </a>https://svn.win.tue.nl/repos/prom/Documentation/PetriNetReplayAnalysis.pdf</p>" +
				"<br/><br/><p>The source code for this plugin is <a href=\"https://github.com/rupos-it/PetriNetReplayAnalysis\">here </a>" +
				"https://github.com/rupos-it/PetriNetReplayAnalysis</p><br/><br/><p><span style=\"font-size:8px;\"><sup>1</sup>" +
				"Roberto Bruni, Andrea Corradini, Gianluigi Ferrari, Tito Flagella, Roberto Guanciale, and " +
				"Giorgio O. Spagnolo. Applying process analysis to the italian egovernment enterprise architecture. " +
				"<br/>In <i>Proceedings of WS-FM 2011, 8th International Workshop on Web Services and Formal Methods</i>" +
				"<a href=\"http://goo.gl/EmiDJ\">http://goo.gl/EmiDJ</a></span></p>"+	
				" </html>";

		JComponent configsimilarity = lpcfui.initComponentsDifferntMapping(label);
		JComponent config = ui.initComponents();
		result = context.showWizard("Select Type Mapping", true, false, configsimilarity );


		JComponent mapping = lpcfui;
		currentStep++;
		boolean d=false;
		while (sem) {

			switch (result) {
			case NEXT :
				/*
				 * Show the next step. 
				 */

				if (currentStep == 0) {
					currentStep = 1;
				}
				if(currentStep==1){
					result =context.showWizard("Mapping Petrinet - Log", false, false, mapping );
					currentStep++;
					d=true;
					break;
				}
				if(currentStep==2){
					d=false;
					result =context.showWizard("Configure Performance Settings", false, true, config);
					ui.setWeights();
				}

				break;
			case PREV :
				/*
				 * Move back. 
				 */
				if(d){
					currentStep--;
					d=false;
				}
				if(currentStep==1){
					result = context.showWizard("Select Type Mapping", true, false, configsimilarity );
					mapping = lpcfui;
				}
				if(currentStep==2){
					result =context.showWizard("Mapping Petrinet - Log", false, false, mapping );
					currentStep--;

				}


				break;
			case FINISHED :
				/*
				 * Return  final step.
				 */
				map = getmap(lpcfui.getMap());
				sem=false;
				break;
			default :
				/*
				 * Should not occur.
				 */
				context.log("press Cancel");
				context.getFutureResult(0).cancel(true);
				return null;
			}
		}
		//if (result == InteractionResult.FINISHED) {
		//	 map = lpcfui.getMap();
		//}

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

		TotalConformanceResult totalResult = getConformanceDetails(context, log, net,marking, setting,map);


		return totalResult;
	}

	private Map<Transition, XEventClass> getmap(
			Collection<Pair<Transition, XEventClass>> map) {
		Map<Transition, XEventClass> maps= new HashMap<Transition, XEventClass>();
		for(Pair<Transition, XEventClass> coppia:map){
			maps.put(coppia.getFirst(),coppia.getSecond());
		}

		return maps;
	}

	//@Plugin(name = "ConformanceDetails", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@PluginVariant(requiredParameterLabels = { 0,1 })
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net) {
		ReplayFitnessSetting setting = new ReplayFitnessSetting();
		suggestActions(setting, log, net);

		TotalConformanceResult total = getConformanceDetails(context, log, net, setting);


		return total;
	}

	//@Plugin(name = "ConformanceDetailsSettingsWithMarking", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	@PluginVariant(requiredParameterLabels = { 0,1,2,3 })
	public TotalConformanceResult getFitnessDetails(PluginContext context, XLog log, Petrinet net, ReplayFitnessSetting setting, Marking marking) {


		Map<Transition, XEventClass> map=null;
		TotalConformanceResult total = getConformanceDetails(context, log, net, marking, setting,map);

		return total;
	}

	//@Plugin(name = "ConformanceDetailsSettings", returnLabels = { "Conformance Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {}, userAccessible = true)
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	@PluginVariant(requiredParameterLabels = { 0,1,2 })
	public TotalConformanceResult getConformanceDetails(PluginContext context, XLog log, Petrinet net, ReplayFitnessSetting setting) {

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
		TotalConformanceResult total = getConformanceDetails(context, log, net, marking, setting,map);

		return total;
	}

	@Plugin(name = "FitnessSuggestSettings", returnLabels = { "Settings" }, returnTypes = { ReplayFitnessSetting.class }, parameterLabels = {}, userAccessible = true)
	// @UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "T. Yuliani and H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	public ReplayFitnessSetting suggestSettings(PluginContext context, XLog log, Petrinet net) {
		ReplayFitnessSetting settings = new ReplayFitnessSetting();
		suggestActions(settings, log, net);
		return settings;
	}



}
