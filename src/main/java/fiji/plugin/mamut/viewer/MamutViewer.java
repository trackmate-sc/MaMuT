/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.TransformEventHandler;
import bdv.cache.CacheControl;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.splitpanel.SplitPanel;
import bdv.util.AWTUtils;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.animate.MessageOverlayAnimator;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.util.ProgressWriterLogger;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.IJ;

/**
 * A {@link JFrame} containing a {@link MamutViewerPanel} and associated
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
@SuppressWarnings( "deprecation" )
public class MamutViewer extends JFrame implements TrackMateModelView
{
	private static final long serialVersionUID = 1L;

	public static final String KEY = "MaMuT Viewer";

	private static final long DEFAULT_TEXT_DISPLAY_DURATION = 3000;
	private static final double DEFAULT_FADEINTIME = 0;
	private static final double DEFAULT_FADEOUTTIME = 0.5;
	private static final Font DEFAULT_FONT = new Font( "SansSerif", Font.PLAIN, 14 );

	/** The logger instance that echoes message on this view. */
	private final Logger logger;

	private final Model model;

	private final SelectionModel selectionModel;

	protected final MamutViewerPanel viewerPanel;

	private final CardPanel cards;

	private final SplitPanel splitPanel;

	private final ConverterSetups setups;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final VisibilityAndGroupingDialog visibilityAndGroupingDialog;

	private final MamutRecordMovieDialog recordMovieDialog;

	private final MamutRecordMaxProjectionDialog recordMaxProjectionMovieDialog;

	private final BookmarksEditor bookmarkEditor;

	private final DisplaySettings ds;

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
	 * @param model
	 *            the model to show in this view.
	 * @param selectionModel
	 *            the selection model used to deal with selection.
	 * @param optional
	 *            optional parameters. See
	 *            {@link bdv.viewer.ViewerPanel#getOptionValues()}.
	 */
	public MamutViewer(
			final int width,
			final int height,
			final List< SourceAndConverter< ? > > sources,
			final int numTimePoints,
			final CacheControl cache,
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds,
			final ViewerOptions optional,
			final Bookmarks bookmarks )
	{
		super( "MaMut Viewer", AWTUtils.getSuitableGraphicsConfiguration( AWTUtils.RGB_COLOR_MODEL ) );
		this.ds = ds;
		final MessageOverlayAnimator msgOverlay = new MessageOverlayAnimator( DEFAULT_TEXT_DISPLAY_DURATION, DEFAULT_FADEINTIME, DEFAULT_FADEOUTTIME, DEFAULT_FONT );
		viewerPanel = new MamutViewerPanel( sources, numTimePoints, cache, optional.width( width ).height( height ).msgOverlay( msgOverlay ) );

		setups = new ConverterSetups( viewerPanel.state() );
		setups.listeners().add( s -> viewerPanel.requestRepaint() );

		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

		cards = new CardPanel();
		BdvDefaultCards.setup( cards, viewerPanel, setups );
		splitPanel = new SplitPanel( viewerPanel, cards );

		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = new MamutViewerLogger();
		this.bookmarkEditor = new BookmarksEditor( viewerPanel, keybindings, bookmarks );
		bookmarkEditor.setInputMapsToBlock( Arrays.asList( "all" ) );

		getRootPane().setDoubleBuffered( true );
		setPreferredSize( new Dimension( width, height ) );
		add( splitPanel, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
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
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), viewerPanel.getDisplay() );
		viewerPanel.getDisplay().addHandler( mouseAndKeyHandler );


		final Behaviours transformBehaviours = new Behaviours( optional.values.getInputTriggerConfig(), "bdv" );
		transformBehaviours.install( triggerbindings, "transform" );

		final TransformEventHandler tfHandler = viewerPanel.getTransformEventHandler();
		tfHandler.install( transformBehaviours );

		this.visibilityAndGroupingDialog = new VisibilityAndGroupingDialog( this, viewerPanel.getVisibilityAndGrouping() );

		this.recordMovieDialog = new MamutRecordMovieDialog( this, viewerPanel, new ProgressWriterLogger( logger ) );
		recordMovieDialog.setLocationRelativeTo( this );
		viewerPanel.getDisplay().addOverlayRenderer( recordMovieDialog );

		this.recordMaxProjectionMovieDialog = new MamutRecordMaxProjectionDialog( this, this, ds, new ProgressWriterLogger( logger ) );
		recordMaxProjectionMovieDialog.setLocationRelativeTo( this );
		viewerPanel.getDisplay().addOverlayRenderer( recordMaxProjectionMovieDialog );

		setIconImage( MaMuT.MAMUT_ICON.getImage() );
		setLocationByPlatform( true );
		setVisible( true );
	}

	public void addHandler( final Object handler )
	{
		viewerPanel.getDisplay().addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
			addKeyListener( ( KeyListener ) handler );
	}

	public MamutViewerPanel getViewerPanel()
	{
		return viewerPanel;
	}

	public ConverterSetups getConverterSetups()
	{
		return setups;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
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
		viewerPanel.overlay = new MamutOverlay( model, selectionModel, this, ds );
	}

	@Override
	public void refresh()
	{
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
	public Model getModel()
	{
		return model;
	}

	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	public VisibilityAndGroupingDialog getVisibilityAndGroupingDialog()
	{
		return visibilityAndGroupingDialog;
	}

	public MamutRecordMovieDialog getRecordMovieDialog()
	{
		return recordMovieDialog;
	}

	public MamutRecordMaxProjectionDialog getRecordMaxProjectionMovieDialog()
	{
		return recordMaxProjectionMovieDialog;
	}

	public void initSetBookmark()
	{
		bookmarkEditor.initSetBookmark();
	}

	public void initGoToBookmark()
	{
		bookmarkEditor.initGoToBookmark();
	}

	public void initGoToBookmarkRotation()
	{
		bookmarkEditor.initGoToBookmarkRotation();
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
		{
			IJ.showProgress( val );
		}

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
