package fiji.plugin.mamut.viewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;

import javax.swing.JFrame;

import fiji.plugin.trackmate.TrackMateModel;

import viewer.SpimViewer;
import viewer.render.SourceAndConverter;
import viewer.render.ViewerState;

public class MamutViewer extends SpimViewer {

	private MamutOverlay overlay;
	
	/*
	 * CONSTRUCTOR
	 */
	

	public MamutViewer(int width, int height, Collection<SourceAndConverter<?>> sources, int numTimePoints, TrackMateModel model) {
		super(width, height, sources, numTimePoints);
		this.overlay = new MamutOverlay(model);
	}
	
	/*
	 * METHODS
	 */
	
	
	@Override
	public void drawOverlays(Graphics g) {
		super.drawOverlays(g);

		overlay.setViewerState(state);
		overlay.paint((Graphics2D) g);
		
		
	}
	
	public JFrame getJFrame() {
		return frame;
	}
	
	
	public ViewerState getViewerState() {
		return state;
	}
}
