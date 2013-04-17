package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;

import javax.swing.JFrame;

import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.render.SourceAndConverter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;

public class MamutViewer extends SpimViewer {

	protected static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;
	private MamutOverlay overlay;
	private TextOverlayAnimator animatedOverlay = null;
	private Logger logger;
	
	/*
	 * CONSTRUCTOR
	 */
	

	public MamutViewer(int width, int height, Collection<SourceAndConverter<?>> sources, int numTimePoints, TrackMateModel model) {
		super(width, height, sources, numTimePoints);
		this.overlay = new MamutOverlay(model);
		this.logger = new Logger() {
			
			@Override
			public void setStatus(String status) {
				animatedOverlay = new TextOverlayAnimator(status, DEFAULT_TEXT_DISPLAY_DURATION);
			}
			
			@Override
			public void setProgress(double val) {
				animatedOverlay = new TextOverlayAnimator(String.format("%3d", Math.round(val)), DEFAULT_TEXT_DISPLAY_DURATION);
			}
			
			@Override
			public void log(String message, Color color) {
				animatedOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
			}
			
			@Override
			public void error(String message) {
				animatedOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
			}
		};
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Returns the {@link Logger} object that will echo any message to this {@link MamutViewer}
	 * window.
	 * @return this {@link MamutViewer} logger.
	 */
	public Logger getLogger() {
		return logger;
	}
	
	
	@Override
	public void drawOverlays(Graphics g) {
		super.drawOverlays(g);

		overlay.setViewerState(state);
		overlay.paint((Graphics2D) g);
		
		if ( animatedOverlay  != null ) {
			animatedOverlay.paint( ( Graphics2D ) g, System.currentTimeMillis() );
			if ( animatedOverlay.isComplete() )
				animatedOverlay = null;
			else
				display.repaint();
		}
		
		
	}
	
	/**
	 * Returns the {@link JFrame} component that is the parent to this viewer. 
	 * @return  the parent JFrame.
	 */
	public JFrame getFrame() {
		return frame;
	}
	
	/**
	 * Returns the time-point currently displayed in this viewer.
	 * @return the time-point currently displayed.
	 */
	public int getCurrentTimepoint() {
		return state.getCurrentTimepoint();
	}
	
}
