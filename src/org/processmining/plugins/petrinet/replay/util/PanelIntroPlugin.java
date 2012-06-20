package org.processmining.plugins.petrinet.replay.util;

import info.clearthought.layout.TableLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;




import com.fluxicon.slickerbox.factory.SlickerFactory;



public class PanelIntroPlugin extends JPanel{
    
	
	
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8984242715253771983L;

	public PanelIntroPlugin(String stringlabel) {
		
		
		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayout.FILL,0.3 }, { TableLayout.FILL,0.3  } };
		setLayout(new TableLayout(size));

		
		JLabel label = slickerFactory.createLabel(stringlabel);
		add(label,"0,0");
		

		
		
	}

}
