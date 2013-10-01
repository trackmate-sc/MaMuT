package fiji.plugin.mamut.viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.TextOverlayAnimator.TextPosition;
import viewer.TranslationAnimator;
import viewer.render.SourceAndConverter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MamutViewer extends SpimViewer implements TrackMateModelView
{

	/*
	 * LOGGER L&F PARAMS
	 */

	private static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;

	private static final TextPosition DEFAULT_POSITION = TextPosition.BOTTOM_RIGHT;

	private static final double DEFAULT_FADEINTIME = 0;

	private static final double DEFAULT_FADEOUTTIME = 0.5;

	private static final Font DEFAULT_FONT = new Font( "SansSerif", Font.PLAIN, 14 );

	private static final String INFO_TEXT = "A viewer based on Tobias Pietzsch SPIM Viewer";

	public static final String KEY = "MaMuT Viewer";

	/** The overlay on which the {@link TrackMateModel} will be painted. */
	private MamutOverlay overlay;

	/** The animated text overlay that will be used to log MaMuT messages. */
	private TextOverlayAnimator loggerOverlay = null;

	/** The logger instance that echoes message on this view. */
	private final Logger logger;

	private final Model model;

	private final SelectionModel selectionModel;

	/** A map of String/Object that configures the look and feel of the display. */
	protected Map< String, Object > displaySettings = new HashMap< String, Object >();

	/** The mapping from spot to a color. */
	SpotColorGenerator spotColorProvider;

	TrackColorGenerator trackColorProvider;

	/*
	 * CONSTRUCTOR
	 */

	public MamutViewer( final int width, final int height, final List< SourceAndConverter< ? >> sources, final int numTimePoints, final Model model, final SelectionModel selectionModel )
	{
		super( width, height, sources, numTimePoints );
		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = new MamutViewerLogger();
	}

	/*
	 * METHODS
	 */

	/**
	 * Returns the {@link Logger} object that will echo any message to this
	 * {@link MamutViewer} window.
	 *
	 * @return this {@link MamutViewer} logger.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		super.drawOverlays( g );

		if ( null != overlay )
		{
			overlay.setViewerState( state );
			overlay.paint( ( Graphics2D ) g );
		}

		if ( loggerOverlay != null )
		{
			loggerOverlay.paint( ( Graphics2D ) g, System.currentTimeMillis() );
			if ( loggerOverlay.isComplete() )
				loggerOverlay = null;
			else
				display.repaint();
		}
	}

	/**
	 * Returns the {@link JFrame} component that is the parent to this viewer.
	 *
	 * @return the parent JFrame.
	 */
	@Override
	public JFrame getFrame()
	{
		return frame;
	}

	/**
	 * Returns the time-point currently displayed in this viewer.
	 *
	 * @return the time-point currently displayed.
	 */
	public int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public void render()
	{
		this.overlay = new MamutOverlay( model, selectionModel, this );
	}

	@Override
	public void refresh()
	{
		requestRepaint();
	}

	@Override
	public void clear()
	{
		this.overlay = null;
	}

	@Override
	public void centerViewOn( final Spot spot )
	{

		final int tp = spot.getFeature( Spot.FRAME ).intValue();
		state.setCurrentTimepoint( tp );
		sliderTime.setValue( tp );

		final AffineTransform3D t = new AffineTransform3D();
		state.getViewerTransform( t );
		final double[] spotCoords = new double[] { spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ) };

		// Translate view so that the target spot is in the middle of the
		// JFrame.
		final double dx = frame.getWidth() / 2 - ( t.get( 0, 0 ) * spotCoords[ 0 ] + t.get( 0, 1 ) * spotCoords[ 1 ] + t.get( 0, 2 ) * spotCoords[ 2 ] );
		final double dy = frame.getHeight() / 2 - ( t.get( 1, 0 ) * spotCoords[ 0 ] + t.get( 1, 1 ) * spotCoords[ 1 ] + t.get( 1, 2 ) * spotCoords[ 2 ] );
		final double dz = -( t.get( 2, 0 ) * spotCoords[ 0 ] + t.get( 2, 1 ) * spotCoords[ 1 ] + t.get( 2, 2 ) * spotCoords[ 2 ] );

		// But use an animator to do this smoothly.
		final double[] target = new double[] { dx, dy, dz };
		currentAnimator = new TranslationAnimator( t, target, 300 );
		currentAnimator.setTime( System.currentTimeMillis() );
		transformChanged( t );
	}

	@Override
	public void paint()
	{

		synchronized ( this )
		{
			if ( currentAnimator != null )
			{
				final TransformEventHandler< AffineTransform3D > handler = display.getTransformEventHandler();
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
	public void setDisplaySettings( final String key, final Object value )
	{

		if ( key.equals( KEY_SPOT_COLORING ) )
		{
			if ( null != spotColorProvider )
			{
				spotColorProvider.terminate();
			}
			spotColorProvider = ( SpotColorGenerator ) value;
		}
		else if ( key.equals( KEY_TRACK_COLORING ) )
		{
			if ( null != trackColorProvider )
			{
				trackColorProvider.terminate();
			}
			trackColorProvider = ( TrackColorGenerator ) value;
		}

		displaySettings.put( key, value );
		refresh();
	}

	@Override
	public Object getDisplaySettings( final String key )
	{
		return displaySettings.get( key );
	}

	@Override
	public Map< String, Object > getDisplaySettings()
	{
		return displaySettings;
	}

	@Override
	public Model getModel()
	{
		return model;

	}

	/*
	 * INNER CLASSRS
	 */

	private final class MamutViewerLogger extends Logger
	{

		@Override
		public void setStatus( final String status )
		{
			loggerOverlay = new TextOverlayAnimator( status, DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_POSITION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT );
		}

		@Override
		public void setProgress( final double val )
		{
			loggerOverlay = new TextOverlayAnimator( String.format( "%3d", Math.round( val ) ), DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_POSITION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT );
		}

		@Override
		public void log( final String message, final Color color )
		{
			loggerOverlay = new TextOverlayAnimator( message, DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_POSITION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT );
		}

		@Override
		public void error( final String message )
		{
			loggerOverlay = new TextOverlayAnimator( message, DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_POSITION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT.deriveFont( Font.BOLD ) );
		}

	}

	@Override
	public String getKey()
	{
		return KEY;
	}

}
