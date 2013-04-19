package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler3D;
import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.TranslationAnimator;
import viewer.render.SourceAndConverter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MamutViewer extends SpimViewer implements TrackMateModelView {

	private static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;
	private static final String INFO_TEXT = "A viewer based on Tobias Pietsch SPIM Viewer";
	/** The overlay on which the {@link TrackMateModel} will be painted. */
	private MamutOverlay overlay;
	/** The animated text overlay that will be used to log MaMuT messages. */ 
	private TextOverlayAnimator loggerOverlay = null;
	/** The logger instance that echoes message on this view. */
	private final Logger logger;
	private final TrackMateModel model;

	/**  A map of String/Object that configures the look and feel of the display. */
	protected Map<String, Object> displaySettings = new HashMap<String, Object>();
	/** The mapping from spot to a color. */
	final Map<Spot, Color> colorProvider;

	/*
	 * CONSTRUCTOR
	 */


	public MamutViewer(int width, int height, Collection<SourceAndConverter<?>> sources, int numTimePoints, TrackMateModel model, final Map<Spot, Color> colorProvider) {
		super(width, height, sources, numTimePoints);
		this.model = model;
		this.logger = new MamutViewerLogger(); 
		this.colorProvider = colorProvider;
		initDisplaySettings(model);
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

		if (null != overlay) {
			overlay.setViewerState(state);
			overlay.paint((Graphics2D) g);
		}

		if ( loggerOverlay  != null ) {
			loggerOverlay.paint( ( Graphics2D ) g, System.currentTimeMillis() );
			if ( loggerOverlay.isComplete() )
				loggerOverlay = null;
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

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public void render() {
		this.overlay = new MamutOverlay(model, this);
	}

	@Override
	public void refresh() {
		requestRepaint();
	}

	@Override
	public void clear() {
		this.overlay = null;
	}

	@Override
	public void centerViewOn(Spot spot) {

		AffineTransform3D t = new AffineTransform3D();
		state.getViewerTransform(t);
		double[] spotCoords = new double[] {
				spot.getFeature(Spot.POSITION_X),	
				spot.getFeature(Spot.POSITION_Y),	
				spot.getFeature(Spot.POSITION_Z)	
		};

		// Translate view so that the target spot is in the middle of the JFrame. 
		double dx = frame.getWidth()/2 - ( t.get(0, 0) * spotCoords[0] + t.get(0, 1) * spotCoords[1] + t.get(0, 2) * spotCoords[2]);
		double dy = frame.getHeight()/2 - ( t.get(1, 0) * spotCoords[0] + t.get(1, 1) * spotCoords[1] + t.get(1, 2) * spotCoords[2]);
		double dz = - ( t.get(2, 0) * spotCoords[0] + t.get(2, 1) * spotCoords[1] + t.get(2, 2) * spotCoords[2]);

		// But use an animator to do this smoothly.
		double[] target = new double[] { dx, dy, dz };
		currentAnimator = new TranslationAnimator( t, target, 300 );
		currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged(t);
	}


	@Override
	public void paint() {

		synchronized( this ) {
			if ( currentAnimator != null ) {
				final TransformEventHandler3D handler = display.getTransformEventHandler();
				final AffineTransform3D transform = currentAnimator.getCurrent( System.currentTimeMillis() );
				handler.setTransform( transform );
				transformChanged( transform );
				if ( currentAnimator.isComplete() )
					currentAnimator = null;
			}
		}

		super.paint();
	}


	@Override
	public void setDisplaySettings(final String key, final Object value) {
		displaySettings.put(key, value);
	}

	@Override 
	public Object getDisplaySettings(final String key) {
		return displaySettings.get(key);
	}

	@Override 
	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}

	@Override
	public TrackMateModel getModel() {
		return model;

	}

	/*
	 * PRIVATE METHODS
	 */

	private void initDisplaySettings(TrackMateModel model) {
		displaySettings.put(KEY_COLOR, DEFAULT_COLOR);
		displaySettings.put(KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR);
		displaySettings.put(KEY_SPOTS_VISIBLE, true);
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, false);
		displaySettings.put(KEY_SPOT_COLOR_FEATURE, null);
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, 1.0f);
		displaySettings.put(KEY_TRACKS_VISIBLE, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_TRACK_COLORING, new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_INDEX));
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
	}



	/*
	 * INNER CLASSRS
	 */


	private final class MamutViewerLogger extends Logger {

		@Override
		public void setStatus(String status) {
			loggerOverlay = new TextOverlayAnimator(status, DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void setProgress(double val) {
			loggerOverlay = new TextOverlayAnimator(String.format("%3d", Math.round(val)), DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void log(String message, Color color) {
			loggerOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
		}

		@Override
		public void error(String message) {
			loggerOverlay = new TextOverlayAnimator(message, DEFAULT_TEXT_DISPLAY_DURATION);
		}

	}




}
