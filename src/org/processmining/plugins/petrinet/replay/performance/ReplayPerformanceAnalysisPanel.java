
package org.processmining.plugins.petrinet.replay.performance;

import info.clearthought.layout.TableLayout;


import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;





import org.deckfour.xes.model.XLog;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;

import org.processmining.plugins.petrinet.replay.util.LogViewInteractivePanel;
import org.processmining.plugins.petrinet.replay.util.PetriNetDrawUtil;
import org.processmining.plugins.petrinet.replay.util.StringInteractivePanel;

import com.fluxicon.slickerbox.factory.SlickerFactory;



public class ReplayPerformanceAnalysisPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = -107889379484400541L;
	//private  JComponent totalresult;
	private ProMJGraphPanel netPNView;
	private LegendPerfomancePanel legendInteractionPanel;
	private TabTracePerformacePanel tabpanel;
	

	private Petrinet net_original;
	private PluginContext context;
	private LogViewInteractivePanel logInteractionPanel;
	
	public ReplayPerformanceAnalysisPanel(PluginContext c, Petrinet netx,
			XLog log, Progress progress, TotalPerformanceResult tovisualize) {
		
		context=c;
		net_original=netx;
		
		paintlayout(tovisualize,log);
		
	}
	
	private void paintlayout(TotalPerformanceResult tovisualize, XLog log) {
		Petrinet net = PetrinetFactory.clonePetrinet(net_original);
		if(!tovisualize.getListperformance().isEmpty()){
		PetriNetDrawUtil.drawperformancenet(net, tovisualize.getListperformance().get(0).getList(), tovisualize.getListperformance().get(0).getMaparc());
		
		String  visualize= PetriNetDrawUtil.toHTMLfromMPP(tovisualize.getListperformance().get(0).getList());
		
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, net);
		
		legendInteractionPanel = new LegendPerfomancePanel(netPNView, "Legend");
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);

		
		// LogViewUI logView = new LogViewUI(log);
		
		logInteractionPanel = new LogViewInteractivePanel(netPNView, log);
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);

		
		
	    // totalresult = visualizestring( visualize);
	    StringInteractivePanel stringpanel = new StringInteractivePanel(netPNView, "Data_Result", visualize);
	    netPNView.addViewInteractionPanel(stringpanel, SwingConstants.SOUTH);
		
		//JComponent tab = tabtrace(tovisualize,netx,context);
	    tabpanel = new TabTracePerformacePanel(netPNView, "Change_Trace", tovisualize,this);
		netPNView.addViewInteractionPanel(tabpanel, SwingConstants.SOUTH);
		
		//double border = 1;
		//double size[][] =
		//{{border, 1, 1, TableLayout.FILL, 1, 200, border},  // Columns
		//		{border, 300, 1, TableLayout.FILL, 1, 160, border}}; // Rows
		//setLayout(new TableLayout(size));
		
		double size[][] = { { TableLayout.FILL,1 }, { TableLayout.FILL,1  } };
		setLayout(new TableLayout(size));
		add(netPNView, "0, 0");
		
		//add(logView, "0, 1");
		
		// Add
		//add (netPNView, "1, 1, 5, 1"); // Top
		//add (logView, "1, 5, 5, 5"); // Bottom
		
		//add (tab, "5, 3      "); // Right
		//add (totalresult, "3, 3      "); // Center

		/*double size[][] = { { TableLayoutConstants.FILL,200 }, { TableLayoutConstants.FILL,  TableLayoutConstants.FILL} };
		setLayout(new TableLayout(size));
		
		add(netPNView, "0, 0");
		
		add(totalresult, "0, 1");
		add(logView, "1, 1");*/
		}else{
			
			SlickerFactory slickerFactory = SlickerFactory.instance();

			double size[][] = { { TableLayout.FILL,0.3 }, { TableLayout.FILL,0.3  } };
			setLayout(new TableLayout(size));
			JLabel label = slickerFactory.createLabel("<html><h2>   No events conform to the model</h2></html>");
			add(label,"0,0");
		}
		
	}







	public void fullrepaint(PerformanceResult performanceResult) {

		Petrinet nety = PetrinetFactory.clonePetrinet(net_original);
		
		PetriNetDrawUtil.drawperformancenet(nety,performanceResult.getList(), performanceResult.getMaparc());
		remove(netPNView);
		 netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, nety);
		
		 String  visualize= PetriNetDrawUtil.toHTMLfromMPP(performanceResult.getList());
				  StringInteractivePanel stringpanel = new StringInteractivePanel(netPNView, "Data_Result", visualize);
		
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);
		netPNView.addViewInteractionPanel(stringpanel, SwingConstants.SOUTH);
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);
		netPNView.addViewInteractionPanel(tabpanel, SwingConstants.SOUTH);
		 
		//add (netPNView, "1, 1, 5, 1");
		add(netPNView, "0, 0");
		revalidate();
		repaint();
	}
	
	


}
