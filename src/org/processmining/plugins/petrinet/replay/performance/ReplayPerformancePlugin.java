package org.processmining.plugins.petrinet.replay.performance;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.concurrent.ExecutionException;

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
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;

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
import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetAssUI;
import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetConnectionFactoryUI;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.Replayer;
import org.processmining.plugins.petrinet.replay.util.PanelIntroPlugin;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisUI;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessCost;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;

@Plugin(name = "PN Performance Analysis", parameterLabels = { "Log", "Petrinet", "ReplayFitnessSetting", "Marking" }, returnLabels = { "Result of Performance" }, returnTypes = { TotalPerformanceResult.class })
public class ReplayPerformancePlugin {

	Map<Transition, XEventClass> map = null;

	@PluginVariant(requiredParameterLabels = { 0,1,2 }, variantLabel = "PerformanceDetailsSettings")
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalPerformanceResult getPerformanceDetails(PluginContext context, XLog log, Petrinet net, ReplayFitnessSetting setting) {

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

		map = null;
		return   getPerformanceDetails(context, log, net, setting,marking,XLogInfoImpl.STANDARD_CLASSIFIER);
	}

	@PluginVariant(requiredParameterLabels = { 0,1,2,3 }, variantLabel = "PerformanceDetailsSettingsWithMarking")
	//@Plugin(name = "PerformanceDetailsSettingsWithMarking", returnLabels = { "Performance Total" }, returnTypes = { TotalPerformanceResult.class }, parameterLabels = {}, userAccessible = true)
	//@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalPerformanceResult getPerformanceDetails(PluginContext context, XLog log, Petrinet net, ReplayFitnessSetting setting,Marking marking, XEventClassifier classif ) {




		TotalPerformanceResult performance = new TotalPerformanceResult();

		XEventClasses classes = getEventClasses(log, classif );
		if(map==null){
			//Map<Transition, XEventClass> 
			map = getMapping(classes, net);
		}

		context.getConnectionManager().addConnection(new LogPetrinetConnectionImpl(log, classes, net, map));

		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);

		Replayer<ReplayFitnessCost> replayer = new Replayer<ReplayFitnessCost>(context, net, semantics, map,
				ReplayFitnessCost.addOperator);

		int replayedTraces = 0;
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(log.size());
		for (XTrace trace : log) {
			List<XEventClass> list = getList(trace, classes);
			try {
				System.out.println("Replay :"+replayedTraces);
				List<Transition> sequence;

				sequence = replayer.replayTrace(marking, list, setting);
				if(sequence!=null){
					sequence = sortHiddenTransection(net, sequence, map);
					String tracename = getTraceName(trace);
					updatePerformance(net, marking, sequence, semantics, trace, performance, map, tracename);
					replayedTraces++;
					context.getProgress().inc();
					System.out.println("Replayed");
				}

			} catch (Exception ex) {
				System.out.println("Failed");
				context.log("Replay of trace " + trace + " failed: " + ex.getMessage());
			}
		}

		context.log("(based on a successful replay of " + replayedTraces + " out of " + log.size() + " traces)");

		ReplayAnalysisConnection connection = new ReplayAnalysisConnection(performance, log, net);
		context.getConnectionManager().addConnection(connection);

		return performance;
	}


	private static String getTraceName(XTrace trace) {
		String traceName = XConceptExtension.instance().extractName(trace);
		return (traceName != null ? traceName : "<unknown>");
	}


	private List<Transition> sortHiddenTransection(Petrinet net, List<Transition> sequence,
			Map<Transition, XEventClass> map) {
		for (int i=1; i<sequence.size(); i++) {
			Transition current = sequence.get(i);
			// Do not move visible transitions
			if (map.containsKey(current)) {
				continue;
			}
			Set<Place> presetCurrent = new HashSet<Place>();
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(current)) {
				if (! (edge instanceof Arc))
					continue;
				Arc arc = (Arc) edge;
				Place place = (Place)arc.getSource();
				presetCurrent.add(place);
			}

			int k = i-1;
			while (k >= 0) {
				Transition prev = sequence.get(k);
				Set<Place> postsetPrev = new HashSet<Place>();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(prev)) {
					if (! (edge instanceof Arc))
						continue;
					Arc arc = (Arc) edge;
					Place place = (Place)arc.getTarget();
					postsetPrev.add(place);
				}

				// Intersection
				Set<Place> intersection = new HashSet<Place>();
				for (Place place : postsetPrev) {
					if (presetCurrent.contains(place))
						intersection.add(place);
				}
				if (intersection.size() > 0)
					break;

				// Swap Transitions
				sequence.remove(k);
				sequence.add(k+1, prev);

				k-=1;
			}
		}
		return sequence;
	}






	private void updatePerformance(Petrinet net, Marking initMarking, List<Transition> sequence, PetrinetSemantics semantics, XTrace trace, TotalPerformanceResult totalResult, Map<Transition, XEventClass> map, String tracename) {
		// if (trace.size() != sequence.size())
		//     System.exit(1);

		XAttributeTimestampImpl date  = (XAttributeTimestampImpl)(trace.get(0).getAttributes().get("time:timestamp"));
		long d1 = date.getValue().getTime();

		Map<Place, PerformanceData> performance = new HashMap<Place, PerformanceData>();

		Map<Arc, Integer> maparc = new HashMap<Arc, Integer>();

		Marking marking = new Marking(initMarking);

		for (Place place : marking) {
			PerformanceData result = null;
			if (performance.containsKey(place))
				result = performance.get(place);
			else
				result = new PerformanceData();

			result.addToken();

			performance.put(place, result);
		}


		int iTrace = -1;
		for (int iTrans=0; iTrans<sequence.size(); iTrans++) {
			Transition transition = sequence.get(iTrans);
			long d2=d1;
			if (map.containsKey(transition)) {
				iTrace+=1;
			}
			if(iTrace>=0){
				XEvent event = trace.get(iTrace);
				XAttributeTimestampImpl date1  = (XAttributeTimestampImpl)(event.getAttributes().get("time:timestamp"));
				d2 = date1.getValue().getTime();
			}
			float deltaTime = d2-d1;
			d1 = d2;


			//boolean fittingTransition = true;
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net
					.getInEdges(transition);

			Set<Place> places = new HashSet<Place>();
			places.addAll(marking);
			List<Transition> futureTrans = sequence.subList(iTrans, sequence.size());
			for (Place place : places) {
				PerformanceData result = null;
				if (performance.containsKey(place))
					result = performance.get(place);
				else
					result = new PerformanceData();

				int placeMarking = marking.occurrences(place);
				if (placeMarking == 0)
					continue;

				// Transitions denending on the current place
				int maxMarking = 0;
				int minTransitionDistanceInFuture = futureTrans.size();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(place)) {
					if (! (edge instanceof Arc))
						continue;
					Arc arc = (Arc) edge;

					Transition trs = (Transition)arc.getTarget();
					int trsPos = futureTrans.indexOf(trs);
					if (trsPos < 0)
						continue;
					if (trsPos > minTransitionDistanceInFuture)
						continue;
					minTransitionDistanceInFuture = trsPos;

					// Transition preset
					int minMarking = placeMarking;
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge1 : net.getInEdges(trs)) {
						if (! (edge1 instanceof Arc))
							continue;
						Arc arc1 = (Arc) edge1;

						Place p1 = (Place)arc1.getSource();
						int tokens = marking.occurrences(p1);
						minMarking = Math.min(minMarking, tokens);
					}
					//maxMarking = Math.max(maxMarking, minMarking);
					maxMarking = minMarking;
				}
				// maxMarking < placeMarking
				// maxMarking is the consumable tokens
				// synchTime = (placeMarking - maxMarking) *  deltaTime;
				result.addTime(placeMarking * deltaTime, maxMarking * deltaTime);
				performance.put(place, result);
			}

			// Updates marking according with enabled transition
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preset) {
				if (edge instanceof Arc) {
					Arc arc = (Arc) edge;
					//add arc usage 
					addArcUsage( arc, maparc);
					Place place = (Place) arc.getSource();
					int consumed = arc.getWeight();
					int missing = 0;
					if (arc.getWeight() > marking.occurrences(place)) {
						missing = arc.getWeight() - marking.occurrences(place);
					}
					for (int i = missing; i < consumed; i++) {
						marking.remove(place);
					}
				}
			}
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net
					.getOutEdges(transition);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postset) {
				if (edge instanceof Arc) {
					Arc arc = (Arc) edge;
					//add arc usage
					addArcUsage( arc, maparc);
					Place place = (Place) arc.getTarget();
					int produced = arc.getWeight();
					for (int i = 0; i < produced; i++) {
						marking.add(place);

						PerformanceData result = null;
						if (performance.containsKey(place))
							result = performance.get(place);
						else
							result = new PerformanceData();
						result.addToken();
						performance.put(place, result);
					}
				}
			}
		}
		PerformanceResult pr = new PerformanceResult(tracename);
		pr.setList(performance);
		pr.setMaparc(maparc);
		totalResult.getListperformance().add(pr);

	}

	private void addArcUsage(Arc arc, Map<Arc, Integer> maparc) {
		Integer numUsage = maparc.get(arc);
		maparc.put(arc, numUsage == null ? 1 : numUsage+1);
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

	private void suggestActions(ReplayFitnessSetting setting, XLog log, Petrinet net) {
		setting.setAction(ReplayAction.INSERT_ENABLED_MATCH, true);
		setting.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		setting.setAction(ReplayAction.REMOVE_HEAD, false);
		setting.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, false);
		setting.setAction(ReplayAction.INSERT_DISABLED_MATCH, false);
		setting.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, false);
	}

	//List<Map<Place,PerformanceResult>> listResult;
	//PerformanceResult totalResult;


	// Rupos public methos
	@PluginVariant(requiredParameterLabels = { 0,1}, variantLabel = "PerformanceDetailsUI")
	//@Plugin(name = "PerformanceDetailsUI", returnLabels = { "Performance Total" }, returnTypes = { TotalPerformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalPerformanceResult getPerformanceDetails(UIPluginContext context, XLog log, Petrinet net) {


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



		ReplayFitnessSetting setting = new ReplayFitnessSetting();
		suggestActions(setting, log, net);
		ReplayAnalysisUI ui = new ReplayAnalysisUI(setting);

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
		//Build and show the UI to make the mapping
		LogPetrinetAssUI mapping = new LogPetrinetAssUI(log, net, availableEventClass);
		//Create map or not according to the button pressed in the UI

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
		String label = "<html>" +
				"<h2>PetriNetReplayAnalysis: Performance metrics <br/></h2><p>" +
				"This package implement the algorithms described on this article<sup>1</sup>. <br/><br/>"+
				"This plugin replayed a log events on the bussiness process model. " +
				"The model is a Petri net and reproduces a bussiness process. " +
				"<br/>The result of performance plugin is a set Petri nets with annoted for all place sojourn,wait " +
				"and synchronization time for all trace log and all Petri net place,<br/> and annoted them on the Petri net. <br></p>" +
				"<br/><p>The user guide for this plugin is <a href=\"https://svn.win.tue.nl/repos/prom/Documentation/\">here " +
				"</a>https://svn.win.tue.nl/repos/prom/Documentation/PetriNetReplayAnalysis.pdf</p>" +
				"<br/><p>The code for this plugin is <a href=\"https://github.com/rupos-it/PetriNetReplayAnalysis\">" +
				"here </a>https://github.com/rupos-it/PetriNetReplayAnalysis</p>" +
				"<p><span style=\"font-size:8px;\"><sup>1</sup>Roberto Bruni, AndreaCorradini, Gianluigi Ferrari, " +
				"Tito Flagella, Roberto Guanciale, and Giorgio O. Spagnolo. Applying process analysis to the italian " +
				"egovernment enterprise architecture. <br/>In <i>Proceedings of WS-FM 2011,8th International Workshop on " +
				"Web Services and Formal Methods</i> <ahref=\"http://goo.gl/EmiDJ\">http://goo.gl/EmiDJ</a></span></p>"
				+" </html>";



		JComponent intro = new PanelIntroPlugin(label);
		JComponent config = ui.initComponents();
		result = context.showWizard("Select Type Mapping", true, false, intro );



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
					result = context.showWizard("Select Type Mapping", true, false, intro );
					//mapping = lpcfui=null;//.initComponents();
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
				map = getmap(mapping.getMap());
				sem=false;
				break;
			default :
				/*
				 * Should not occur.
				 */
				context.log("press Cancel");
				context.log("replay is not performed because not enough parameter is submitted");
				context.getFutureResult(0).cancel(true);
				return null;
			}
		}

		TotalPerformanceResult total = getPerformanceDetails(context, log, net, setting,marking,mapping.getSelectedClassifier());

		return total;
	}
	//@Plugin(name = "PerformanceDetails", returnLabels = { "Performance Total" }, returnTypes = { TotalPerformanceResult.class }, parameterLabels = {}, userAccessible = true)
	@PluginVariant(requiredParameterLabels = { 0,1 }, variantLabel = "PerformanceDetails")
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalPerformanceResult getPerformanceDetails(PluginContext context, XLog log, Petrinet net) {
		ReplayFitnessSetting setting = new ReplayFitnessSetting();
		suggestActions(setting, log, net);
		TotalPerformanceResult total = getPerformanceDetails(context, log, net, setting);

		return total;
	}

	private Map<Transition, XEventClass> getmap(
			Collection<Pair<Transition, XEventClass>> map) {
		Map<Transition, XEventClass> maps= new HashMap<Transition, XEventClass>();
		for(Pair<Transition, XEventClass> coppia:map){
			XEventClass sec = coppia.getSecond();
			if(!sec.toString().equals("DUMMY")){
				maps.put(coppia.getFirst(),coppia.getSecond());
			}
		}

		return maps;
	}

	/*@Visualizer
	@Plugin(name = "Performance Result Visualizer", parameterLabels = "String", returnLabels = "Label of String", returnTypes = JComponent.class)
	public static JComponent visualize(PluginContext context, TotalPerformanceResult tovisualize) {
		return StringVisualizer.visualize(context, tovisualize.toString());
	}*/


}
