package org.processmining.plugins.petrinet.replay.conformance;




import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import com.fluxicon.slickerbox.components.AutoFocusButton;
import com.fluxicon.slickerbox.factory.SlickerDecorator;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class TabTraceConformancePanel extends JPanel implements MouseListener, MouseMotionListener, ViewInteractionPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	
	
	
	protected SlickerFactory factory = SlickerFactory.instance();
	protected SlickerDecorator decorator = SlickerDecorator.instance();
	private JComponent component;
	private JTable tab;
	private String panelName;
	private TotalConformanceResult tovisualize;
	
	private ReplayConformanceAnalysisPanel replayRuposPanel;
	
	
	public TabTraceConformancePanel(ScalableViewPanel panel, String panelName,
			TotalConformanceResult tpr , ReplayConformanceAnalysisPanel replayPRP){
		super(new BorderLayout());

		this.setBorder(BorderFactory.createEmptyBorder());
		this.setOpaque(true);
		this.setSize(new Dimension(160, 260));

		this.addMouseMotionListener(this);
		this.addMouseListener(this);

		panel.getViewport();
		this.panelName = panelName;
		this.tovisualize=tpr;
		
		replayRuposPanel=replayPRP;
		painttabtrace();
	}
	

	private void painttabtrace() {

		this.setBackground(new Color(30, 30, 30));

		JPanel legendPanel = new JPanel();
		legendPanel.setBorder(BorderFactory.createEmptyBorder());
		legendPanel.setBackground(new Color(30, 30, 30));
		
		
		JPanel jp1 = new JPanel();
		 tab = new JTable(new AbstractTableModel() {
		
			private static final long serialVersionUID = -2176731961693608635L;

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return tovisualize.getList().get(rowIndex).getTracename();//rowIndex;
			}
			
			@Override
			public int getRowCount() {
			
				return tovisualize.getList().size();
			}
			
			@Override
			public int getColumnCount() {
				
				return 1;
			}
			
			public String getColumnName(int col) { 
				
				return "List of trace"; 
			}

			public boolean isCellEditable(int row, int col) 
			{ 
				
				return false; 
			}
		});
		 tab.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
				public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
				{
					Component cell = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
					if (tovisualize.getList().get(row).getMissingMarking().isEmpty() &&
							tovisualize.getList().get(row).getMapTransition().isEmpty() &&
							tovisualize.getList().get(row).getConformance()>0.92){
						cell.setBackground( Color.gray );
					}else{
					
						cell.setBackground( Color.red );
					}
						
					return cell;

				}});
		 legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
		 jp1.setLayout(new BoxLayout(jp1, BoxLayout.X_AXIS));
		 
		JScrollPane scrollpane = new JScrollPane(tab); 
		scrollpane.setOpaque(false);
		scrollpane.getViewport().setOpaque(false);
		scrollpane.setBorder(BorderFactory.createEmptyBorder());
		scrollpane.setViewportBorder(BorderFactory.createLineBorder(new Color(10, 10, 10), 2));
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		SlickerDecorator.instance().decorate(scrollpane.getVerticalScrollBar(), new Color(0, 0, 0, 0),
				new Color (140, 140, 140), new Color(80, 80, 80));
		scrollpane.getVerticalScrollBar().setOpaque(false);
		SlickerDecorator.instance().decorate(scrollpane.getHorizontalScrollBar(), new Color(0, 0, 0, 0),
				new Color (140, 140, 140), new Color(80, 80, 80));
		scrollpane.getHorizontalScrollBar().setOpaque(false);
		
		

		JButton button  = new AutoFocusButton("Update");
		JButton	button2  = new AutoFocusButton("UpdateAll");

		button.setOpaque(false);
		button2.setOpaque(false);
		
		button.setFont(new Font("Monospaced", Font.PLAIN, 12));
		button2.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		jp1.add(button,BorderLayout.NORTH);
		jp1.add(button2,BorderLayout.WEST);
		
		
		button2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				replayRuposPanel.fullrepaint();
				
			}
			
		});
		
		
		
		
		legendPanel.add(jp1);
		legendPanel.add(scrollpane,BorderLayout.SOUTH);
		
		
		
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
		
				
				int i=tab.getSelectedRow();
				if(i>=0){
					replayRuposPanel.onerepaint(i);
				}
				
			}
		});


		legendPanel.setOpaque(false);
		this.add(legendPanel, BorderLayout.WEST);
		this.setOpaque(false);

	
	}


	public double getVisWidth() {
		return component.getSize().getWidth();
	}

	public double getVisHeight() {
		return component.getSize().getHeight();
	}

	@Override
	public void paint(Graphics g) {

		super.paint(g);
	}

	public synchronized void mouseDragged(MouseEvent evt) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public synchronized void mousePressed(MouseEvent e) {

	}

	public synchronized void mouseReleased(MouseEvent e) {

	}

	public void setScalableComponent(ScalableComponent scalable) {
		this.component = scalable.getComponent();
	}

	public void setParent(ScalableViewPanel parent) {

	}

	public JComponent getComponent() {
		return this;
	}

	public int getPosition() {
		return SwingConstants.SOUTH;
	}

	public String getPanelName() {
		return panelName;
	}

	public void setPanelName(String name) {
		this.panelName = name;
	}

	public void updated() {
		
	}

	public double getHeightInView() {
		return 160;
	}

	public double getWidthInView() {
		return tab.getWidth();
	}

	public void willChangeVisibility(boolean to) {
		
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
	}
}
