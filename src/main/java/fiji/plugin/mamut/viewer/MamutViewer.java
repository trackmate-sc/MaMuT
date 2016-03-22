package fiji.plugin.mamut.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.MouseAndKeyHandler;

import bdv.BehaviourTransformEventHandler;
import bdv.img.cache.Cache;
import bdv.viewer.InputActionBindings;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TriggerBehaviourBindings;
import bdv.viewer.ViewerOptions;
import bdv.viewer.animate.MessageOverlayAnimator;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;

/**
 * A {@link JFrame} containing a {@link MamutViewerPanel} and associated
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class MamutViewer extends JFrame implements TrackMateModelView
{
	private static final long serialVersionUID = 1L;

	private static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;

	private static final double DEFAULT_FADEINTIME = 0;

	private static final double DEFAULT_FADEOUTTIME = 0.5;

	private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 14);

	/** The logger instance that echoes message on this view. */
	private final Logger logger;

	private final Model model;

	private final SelectionModel selectionModel;

	/** A map of String/Object that configures the look and feel of the display. */
	protected Map< String, Object > displaySettings = new HashMap< String, Object >();

	/** The mapping from spot to a color. */
	FeatureColorGenerator< Spot > spotColorProvider;

	TrackColorGenerator trackColorProvider;

	protected final MamutViewerPanel viewerPanel;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	public MamutViewer( final int width, final int height, final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache, final Model model, final SelectionModel selectionModel )
	{
		this( width, height, sources, numTimePoints, cache, model, selectionModel, ViewerOptions.options() );
	}

	/**
	 *
	 * @param width
	 *            width of the display window.
	 * @param height
	 *            height of the display window.
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param cache
	 *            handle to cache. This is used to control io timing. Also, is
	 *            is used to subscribe / unsubscribe to the cache as a consumer,
	 *            so that eventually the io fetcher threads can be shut down.
	 * @param optional
	 *            optional parameters. See
	 *            {@link bdv.viewer.ViewerPanel#getOptionValues()}.
	 */
	public MamutViewer( final int width, final int height, final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Cache cache, final Model model, final SelectionModel selectionModel, final ViewerOptions optional )
	{
		super( "MaMut Viewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		final MessageOverlayAnimator msgOverlay = new MessageOverlayAnimator( DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT );
		viewerPanel = new MamutViewerPanel( sources, numTimePoints, cache, optional.width( width ).height( height ).msgOverlay( msgOverlay ) );
		keybindings = new InputActionBindings();

		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = new MamutViewerLogger();
		this.triggerbindings = new TriggerBehaviourBindings();

		getRootPane().setDoubleBuffered( true );
		setPreferredSize( new Dimension( width, height ) );
		add( viewerPanel, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				viewerPanel.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		viewerPanel.getDisplay().addHandler( mouseAndKeyHandler );

		final TransformEventHandler< ? > tfHandler = viewerPanel.getDisplay().getTransformEventHandler();
		if ( tfHandler instanceof BehaviourTransformEventHandler )
			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );

		setIconImage( MaMuT.MAMUT_ICON.getImage() );
		setLocationByPlatform( true );
		setVisible( true );
	}

	public void addHandler( final Object handler )
	{
		viewerPanel.getDisplay().addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
		{
			addKeyListener( ( KeyListener ) handler );
		}
	}

	public MamutViewerPanel getViewerPanel()
	{
		return viewerPanel;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	public TriggerBehaviourBindings getTriggerbindings()
	{
		return triggerbindings;
	}

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
	public void render()
	{
		viewerPanel.overlay = new MamutOverlay( model, selectionModel, this );
	}

	@Override
	public void refresh()
	{
		// System.out.println("refresh");
		viewerPanel.requestRepaint();
	}

	@Override
	public void clear()
	{
		viewerPanel.overlay = null;
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		viewerPanel.centerViewOn( spot );
	}

	@Override
	public Map< String, Object > getDisplaySettings()
	{
		return displaySettings;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public void setDisplaySettings( final String key, final Object value )
	{
		if ( key.equals( KEY_SPOT_COLORING ) )
		{
			if ( null != spotColorProvider )
			{
				spotColorProvider.terminate();
			}
			spotColorProvider = ( FeatureColorGenerator< Spot > ) value;
			spotColorProvider.activate();
		}
		else if ( key.equals( KEY_TRACK_COLORING ) )
		{
			if ( null != trackColorProvider )
			{
				trackColorProvider.terminate();
			}
			trackColorProvider = ( TrackColorGenerator ) value;
			trackColorProvider.activate();
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
	public Model getModel()
	{
		return model;
	}

	@Override
	public String getKey()
	{
		return MamutViewerFactory.KEY;
	}

	/*
	 * INNER CLASSES
	 */

	private final class MamutViewerLogger extends Logger
	{

		@Override
		public void setStatus( final String status )
		{
			viewerPanel.showMessage( status );
		}

		@Override
		public void setProgress( final double val )
		{}

		@Override
		public void log( final String message, final Color color )
		{
			viewerPanel.showMessage( message );
		}

		@Override
		public void error( final String message )
		{
			viewerPanel.showMessage( message );
		}
	}
}
