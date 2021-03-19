/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
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
package fiji.plugin.mamut;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_DRAWING_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_LIMIT_DRAWING_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_SPOT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DRAWING_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_LIMIT_DRAWING_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;

import bdv.viewer.ConverterSetups;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.listeners.Listeners;
import org.scijava.util.VersionUtils;

import bdv.BigDataViewer;
import bdv.BigDataViewerActions;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.HelpDialog;
import bdv.tools.InitializeViewerState;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.RequestRepaint;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerState;
import fiji.plugin.mamut.detection.SourceSemiAutoTracker;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.mamut.gui.AnnotationPanel;
import fiji.plugin.mamut.gui.MamutControlPanel;
import fiji.plugin.mamut.gui.MamutGUI;
import fiji.plugin.mamut.gui.MamutGUIModel;
import fiji.plugin.mamut.gui.MamutKeyboardHandler;
import fiji.plugin.mamut.io.MamutXmlWriter;
import fiji.plugin.mamut.providers.MamutEdgeAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutSpotAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutTrackAnalyzerProvider;
import fiji.plugin.mamut.util.SourceSpotImageUpdater;
import fiji.plugin.mamut.viewer.MamutOverlay;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.mamut.viewer.MamutViewerPanel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.DisplaySettingsEvent;
import fiji.plugin.trackmate.gui.DisplaySettingsListener;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.util.ModelTools;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerTrackFeature;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.text.TextWindow;
import ij3d.Image3DUniverse;
import ij3d.ImageWindow3D;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class MaMuT implements ModelChangeListener
{

	public static final ImageIcon MAMUT_ICON = new ImageIcon( MaMuT.class.getResource( "mammouth-256x256.png" ) );

	public static final String PLUGIN_NAME = "MaMuT";

	public static final String PLUGIN_VERSION = VersionUtils.getVersion( MaMuT.class );

	private static final double DEFAULT_RADIUS = 10;

	/**
	 * By how portion of the current radius we change this radius for every
	 * change request.
	 */
	public static final double RADIUS_CHANGE_FACTOR = 0.1;

	/** The default width for new image viewers. */
	public static final int DEFAULT_WIDTH = 800;

	/** The default height for new image viewers. */
	public static final int DEFAULT_HEIGHT = 600;

	private final KeyStroke moveSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 );

	private SetupAssignments setupAssignments;

	private BrightnessDialog brightnessDialog;

	private HelpDialog helpDialog;

	/** The model shown and edited by this plugin. */
	private final Model model;

	/** The next created spot will be set with this radius. */
	private double radius = DEFAULT_RADIUS;

	/** The radius below which a spot cannot go. */
	private final double minRadius = 2; // TODO change this when we have a
	// physical calibration

	/** The spot currently moved under the mouse. */
	private Spot movedSpot = null;

	/** The image data sources to be displayed in the views. */
	private List< SourceAndConverter< ? > > sources;

	private ArrayList< ConverterSetup > converterSetups;

	private CacheControl cache;

	/** The number of timepoints in the image sources. */
	private int nTimepoints;

	/**
	 * If true, the next added spot will be automatically linked to the
	 * previously created one, given that the new spot is created in a
	 * subsequent frame.
	 */
	private boolean isLinkingMode = false;

	/**
	 * The color map for painting the spots. It is centralized here and is used
	 * in the {@link MamutOverlay}s.
	 */
	private SpotColorGenerator spotColorProvider;

	private PerTrackFeatureColorGenerator trackColorProvider;

	private PerEdgeFeatureColorGenerator edgeColorProvider;

	private SpotColorGeneratorPerTrackFeature spotColorPerTrackFeatureProvider;

	private ManualSpotColorGenerator manualSpotColorGenerator;

	private ManualEdgeColorGenerator manualEdgeColorGenerator;

	private TrackMate trackmate;

	private final SourceSettings settings;

	private SelectionModel selectionModel;

	private MamutGUIModel guimodel;

	private SourceSpotImageUpdater< ? > thumbnailUpdater;

	private Logger logger = Logger.DEFAULT_LOGGER;

	/**
	 * If <code>true</code>, then each time a spot is manually created, we will
	 * first test if it is not within the radius of an existing spot. If so,
	 * then the new spot will not be added to the model.
	 */
	private boolean testWithinSpot = true;

	private MamutGUI gui;

	private ManualTransformationEditor manualTransformationEditor;

	private Bookmarks bookmarks;

	private static File mamutFile;

	public MaMuT( final File imageFile, final Model model, final SourceSettings settings )
	{
		this.model = model;
		this.settings = settings;
		setLaF();

		final String pf = imageFile.getParent();
		String lf = imageFile.getName();
		lf = lf.split( "\\." )[ 0 ] + "-mamut.xml";
		mamutFile = new File( pf, lf );

		launch( imageFile );
	}

	/*
	 * PROTECTED METHODS. They are all about configuring and preparing the MaMuT
	 * session.
	 */

	/**
	 * Loads the specified image data (hd5/xml couple), configures and launches
	 * MaMuT.
	 *
	 * @param file
	 *            the file that points to the xml file of the hd5/xml image data
	 *            couple.
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	protected void launch( final File file )
	{

		/*
		 * Prepare model & settings
		 */

		model.addModelChangeListener( this );

		/*
		 * Configure settings object with spot, edge and track analyzers as
		 * specified in the providers.
		 */

		prepareSettingsObject();

		/*
		 * Auto-update features & declare them
		 */

		new ModelFeatureUpdater( model, settings );

		trackmate = new TrackMate( model, settings );
		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		/*
		 * Selection model
		 */

		selectionModel = new SelectionModel( model );
		selectionModel.addSelectionChangeListener( new SelectionChangeListener()
		{
			@Override
			public void selectionChanged( final SelectionChangeEvent event )
			{
				refresh();
				if ( selectionModel.getSpotSelection().size() == 1 )
				{
					centerOnSpot( selectionModel.getSpotSelection().iterator().next() );
				}
			}
		} );

		/*
		 * Color providers
		 */

		prepareColorProviders();

		/*
		 * GUI model
		 */

		guimodel = new MamutGUIModel();
		guimodel.setDisplaySettings( createDisplaySettings( model ) );

		/*
		 * Load image source
		 */

		try
		{
			prepareSources( file );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem loading the image data file " + file + ".\n" + e.getMessage() + "\n" );
			return;
		}

		/*
		 * Update settings.
		 */

		settings.setFrom( sources, file, nTimepoints, cache );

		/*
		 * Thumbnail updater. We need to have the sources loaded for it.
		 */

		thumbnailUpdater = new SourceSpotImageUpdater( settings, sources );

		/*
		 * Brightness
		 */

		brightnessDialog = new BrightnessDialog( gui, setupAssignments );

		/*
		 * Help
		 */

		helpDialog = new HelpDialog( gui, MaMuT.class.getResource( "Help.html" ) );

		/*
		 * Bookmarks.
		 */

		bookmarks = new Bookmarks();

		/*
		 * Control Panel
		 */

		launchGUI();

	}

	/**
	 * Instantiates and displays the MaMuT GUI.
	 * <p>
	 * This will sets the following fields:
	 * <ul>
	 * <li>{@link #gui}
	 * <li>{@link #logger}
	 * </ul>
	 * This requires the following fields to be set:
	 * <ul>
	 * <li>{@link #trackmate}, properly instantiated with {@link #model} and
	 * {@link #settings}
	 * <li>{@link #spotColorProvider}
	 * <li>{@link #edgeColorProvider}
	 * <li>{@link #trackColorProvider}
	 * <li>{@link #spotColorPerTrackFeatureProvider}
	 * <li>{@link #manualSpotColorGenerator}
	 * <li>{@link #manualEdgeColorGenerator}
	 * </ul>
	 */
	protected void launchGUI()
	{
		gui = new MamutGUI( trackmate, this );

		final MamutControlPanel viewPanel = gui.getViewPanel();
		viewPanel.setSpotColorGenerator( spotColorProvider );
		viewPanel.setSpotColorGeneratorPerTrackFeature( spotColorPerTrackFeatureProvider );
		viewPanel.setManualSpotColorGenerator( manualSpotColorGenerator );
		viewPanel.setEdgeColorGenerator( edgeColorProvider );
		viewPanel.setManualEdgeColorGenerator( manualEdgeColorGenerator );
		viewPanel.setTrackColorGenerator( trackColorProvider );

		viewPanel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				if ( event == viewPanel.TRACK_SCHEME_BUTTON_PRESSED )
				{
					launchTrackScheme( viewPanel.getTrackSchemeButton() );
				}
				else if ( event == viewPanel.DO_ANALYSIS_BUTTON_PRESSED )
				{
					launch3DViewer( viewPanel.getDoAnalysisButton() );
				}
				else if ( event == viewPanel.MAMUT_VIEWER_BUTTON_PRESSED )
				{
					newViewer();
				}
				else if ( event == viewPanel.MAMUT_SAVE_BUTTON_PRESSED )
				{
					save();
				}
				else
				{
					System.out.println( "[MaMuT] Caught unknown event: " + event );
				}
			}

		} );
		viewPanel.addDisplaySettingsChangeListener( new DisplaySettingsListener()
		{
			@Override
			public void displaySettingsChanged( final DisplaySettingsEvent event )
			{
				guimodel.getDisplaySettings().put( event.getKey(), event.getNewValue() );
				for ( final TrackMateModelView view : guimodel.getViews() )
				{
					view.setDisplaySettings( event.getKey(), event.getNewValue() );
					view.refresh();
				}
			}
		} );

		final AnnotationPanel annotationPanel = gui.getAnnotationPanel();
		logger = annotationPanel.getLogger();
		annotationPanel.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent event )
			{
				if ( event == annotationPanel.SEMI_AUTO_TRACKING_BUTTON_PRESSED )
				{
					semiAutoDetectSpot();

				}
				else if ( event == annotationPanel.SELECT_TRACK_BUTTON_PRESSED )
				{
					ModelTools.selectTrack( selectionModel );

				}
				else if ( event == annotationPanel.SELECT_TRACK_DOWNWARD_BUTTON_PRESSED )
				{
					ModelTools.selectTrackDownward( selectionModel );

				}
				else if ( event == annotationPanel.SELECT_TRACK_UPWARD_BUTTON_PRESSED )
				{
					ModelTools.selectTrackUpward( selectionModel );
				}
				else
				{
					logger.error( "Caught unknown event: " + event );
				}
			}
		} );

		gui.pack();
	}

	/**
	 * Sets the {@link #settings} field with the analyzers configured for MaMuT.
	 * This does not require the image sources to be loaded.
	 */
	protected void prepareSettingsObject()
	{
		settings.clearSpotAnalyzerFactories();
		final SpotAnalyzerProvider spotAnalyzerProvider = new MamutSpotAnalyzerProvider();
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
		{
			final SpotAnalyzerFactory< ? > spotFeatureAnalyzer = spotAnalyzerProvider.getFactory( key );
			settings.addSpotAnalyzerFactory( spotFeatureAnalyzer );
		}

		settings.clearEdgeAnalyzers();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new MamutEdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
		{
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
			settings.addEdgeAnalyzer( edgeAnalyzer );
		}

		settings.clearTrackAnalyzers();
		final TrackAnalyzerProvider trackAnalyzerProvider = new MamutTrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
		{
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
			settings.addTrackAnalyzer( trackAnalyzer );
		}
	}

	/**
	 * Returns the starting display settings that will be passed to any new view
	 * registered within this GUI.
	 *
	 * @param lModel
	 *            the model this GUI will configure; might be required by some
	 *            display settings.
	 * @return a map of display settings mappings.
	 */
	protected Map< String, Object > createDisplaySettings( final Model lModel )
	{
		final Map< String, Object > displaySettings = new HashMap<>();
		displaySettings.put( KEY_COLOR, DEFAULT_SPOT_COLOR );
		displaySettings.put( KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR );
		displaySettings.put( KEY_SPOTS_VISIBLE, true );
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		displaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		displaySettings.put( KEY_TRACKS_VISIBLE, true );
		displaySettings.put( KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE );
		displaySettings.put( KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH );
		displaySettings.put( KEY_TRACK_COLORING, trackColorProvider );
		displaySettings.put( KEY_SPOT_COLORING, spotColorProvider );
		displaySettings.put( KEY_LIMIT_DRAWING_DEPTH, DEFAULT_LIMIT_DRAWING_DEPTH );
		displaySettings.put( KEY_DRAWING_DEPTH, DEFAULT_DRAWING_DEPTH );
		return displaySettings;
	}

	/**
	 * Instantiates the color providers of this MaMuT.
	 * <p>
	 * This needs the following fields to be set:
	 * <ul>
	 * <li>{@link #model}
	 * </ul>
	 * This will set the following fields:
	 * <ul>
	 * <li>{@link #spotColorProvider}
	 * <li>{@link #edgeColorProvider}
	 * <li>{@link #trackColorProvider}
	 * <li>{@link #spotColorPerTrackFeatureProvider}
	 * <li>{@link #manualSpotColorGenerator}
	 * <li>{@link #manualEdgeColorGenerator}
	 * </ul>
	 */
	protected void prepareColorProviders()
	{
		spotColorProvider = new SpotColorGenerator( model );
		trackColorProvider = new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_ID );
		edgeColorProvider = new PerEdgeFeatureColorGenerator( model, EdgeTargetAnalyzer.EDGE_COST );
		spotColorPerTrackFeatureProvider = new SpotColorGeneratorPerTrackFeature( model, TrackIndexAnalyzer.TRACK_ID );
		manualSpotColorGenerator = new ManualSpotColorGenerator();
		manualEdgeColorGenerator = new ManualEdgeColorGenerator( model );
	}

	/**
	 * Loads and prepares the image sources from the specified files.
	 * <p>
	 * This instantiates and sets the following fields:
	 * <ul>
	 * <li>{@link #nTimepoints}
	 * <li>{@link #sources}
	 * <li>{@link #cache}
	 * <li>{@link #setupAssignments}
	 * <ul>
	 *
	 * @param dataFile
	 *            the file that points to the xml master file of the image data.
	 *
	 * @throws SpimDataException
	 *             if the xml master file cannot be read or is incorrectly
	 *             formatted.
	 */
	private void prepareSources( final File dataFile ) throws SpimDataException
	{
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( dataFile.getAbsolutePath() );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final List< TimePoint > timepoints = seq.getTimePoints().getTimePointsOrdered();
		nTimepoints = timepoints.size();
		sources = new ArrayList<>();
		cache = ( ( ViewerImgLoader ) seq.getImgLoader() ).getCacheControl();
		converterSetups = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
		setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		refresh();
	}

	public void toggleBrightnessDialog()
	{
		brightnessDialog.setVisible( !brightnessDialog.isVisible() );
	}

	public void toggleManualTransformation( final MamutViewer viewer )
	{
		if ( null == manualTransformationEditor )
		{
			manualTransformationEditor = new ManualTransformationEditor( viewer.getViewerPanel(), viewer.getKeybindings() );
		}
		manualTransformationEditor.toggle();
		manualTransformationEditor = null;
	}

	public void toggleHelpDialog()
	{
		helpDialog.setVisible( !helpDialog.isVisible() );
	}

	/**
	 * Performs the semi-automatic detection of subsequent spots. For this to
	 * work, exactly one spot must be in the selection.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public void semiAutoDetectSpot()
	{
		final SourceSemiAutoTracker autotracker = new SourceSemiAutoTracker( model, selectionModel, sources, logger );
		autotracker.setNumThreads( 4 );
		autotracker.setParameters( guimodel.qualityThreshold, guimodel.distanceTolerance, guimodel.maxNFrames );

		new Thread( "MaMuT semi-automated tracking thread" )
		{
			@Override
			public void run()
			{
				final boolean ok = autotracker.checkInput() && autotracker.process();
				if ( !ok )
				{
					logger.error( autotracker.getErrorMessage() );
				}
			}
		}.start();
	}

	/**
	 * Adds a new spot at the mouse current location.
	 *
	 * @param viewer
	 *            the viewer in which the add spot request was made.
	 */
	public void addSpot( final MamutViewer viewer )
	{

		// Check if the mouse is not off-screen
		final Point mouseScreenLocation = MouseInfo.getPointerInfo().getLocation();
		final Point viewerPosition = viewer.getLocationOnScreen();
		final Dimension viewerSize = viewer.getSize();
		if ( mouseScreenLocation.x < viewerPosition.x
				|| mouseScreenLocation.y < viewerPosition.y
				|| mouseScreenLocation.x > viewerPosition.x + viewerSize.width
				|| mouseScreenLocation.y > viewerPosition.y + viewerSize.height )
			return;

		final ViewerState state = viewer.getViewerPanel().state();
		final int frame = state.getCurrentTimepoint();

		// Ok, then create this spot, wherever it is.
		final double[] coordinates = new double[ 3 ];
		viewer.getViewerPanel().getGlobalMouseCoordinates( RealPoint.wrap( coordinates ) );
		final Spot spot = new Spot( coordinates[ 0 ], coordinates[ 1 ], coordinates[ 2 ], radius, -1d );

		if ( testWithinSpot )
		{
			final Spot closestSpot = model.getSpots().getClosestSpot( spot, frame, true );
			if ( null != closestSpot )
			{
				final double closestRadius = closestSpot.getFeature( Spot.RADIUS ).doubleValue();
				if ( closestSpot.squareDistanceTo( spot ) < closestRadius * closestRadius )
				{
					final String message = "Cannot create spot: it is too close to spot " + closestSpot + ".\n";
					viewer.getLogger().log( message );
					return;
				}
			}
		}

		spot.putFeature( Spot.QUALITY, -1d );
		spot.putFeature( Spot.POSITION_T, Double.valueOf( frame ) );

		// Determine source id.
		final SourceAndConverter< ? > source = state.getCurrentSource();
		int sourceId = -1;
		if ( source != null )
			sourceId = state.getSources().indexOf( source );
		spot.putFeature( SpotSourceIdAnalyzerFactory.SOURCE_ID, Double.valueOf( sourceId ) );

		model.beginUpdate();
		try
		{
			model.addSpotTo( spot, frame );
		}
		finally
		{
			model.endUpdate();
		}

		String message = String.format( "Added spot " + spot + " at location X = %.1f, Y = %.1f, Z = %.1f, T = %.0f", spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.FRAME ) );

		// Then, possibly, the edge. We must do it in a subsequent update,
		// otherwise the model gets confused.
		final Set< Spot > spotSelection = selectionModel.getSpotSelection();

		if ( isLinkingMode && spotSelection.size() == 1 )
		{ // if we are in the right mode & if there is only one spot in
			// selection.
			final Spot targetSpot = spotSelection.iterator().next();
			if ( targetSpot.getFeature( Spot.FRAME ).intValue() != spot.getFeature( Spot.FRAME ).intValue() )
			{ // & if they are on different
				// frames
				model.beginUpdate();
				final DefaultWeightedEdge newedge;
				try
				{

					// Create link
					newedge = model.addEdge( targetSpot, spot, -1 );
				}
				finally
				{
					model.endUpdate();
				}
				message += ", linked to spot " + targetSpot + ".";
				selectionModel.clearEdgeSelection();
				selectionModel.addEdgeToSelection( newedge );
			}
			else
			{
				message += ".";
			}
		}
		else
		{
			message += ".";
		}
		viewer.getLogger().log( message );

		// Store new spot as the sole selection for this model
		selectionModel.clearSpotSelection();
		selectionModel.addSpotToSelection( spot );
	}

	/**
	 * Remove spot at the mouse current location (if there is one).
	 *
	 * @param viewer
	 *            the viewer in which the delete spot request was made.
	 */
	public void deleteSpot( final MamutViewer viewer )
	{
		final Spot spot = getSpotWithinRadius( viewer.getViewerPanel() );
		if ( null != spot )
		{
			// We can delete it
			model.beginUpdate();
			try
			{
				model.removeSpot( spot );
			}
			finally
			{
				model.endUpdate();
				final String str = "Removed spot " + spot + ".";
				viewer.getLogger().log( str );
			}
		}

	}

	/**
	 * Increases (or decreases) the neighbor spot radius.
	 *
	 * @param viewer
	 *            the viewer in which the change radius was made.
	 * @param factor
	 *            the factor by which to change the radius. Negative value are
	 *            used to decrease the radius.
	 */
	public void increaseSpotRadius( final MamutViewer viewer, final double factor )
	{
		final Spot spot = getSpotWithinRadius( viewer.getViewerPanel() );
		if ( null != spot )
		{
			// Change the spot radius
			double rad = spot.getFeature( Spot.RADIUS );
			rad += factor * RADIUS_CHANGE_FACTOR * rad;

			if ( rad < minRadius )
				return;

			radius = rad;
			spot.putFeature( Spot.RADIUS, rad );
			// Mark the spot for model update;
			model.beginUpdate();
			try
			{
				model.updateFeatures( spot );
			}
			finally
			{
				model.endUpdate();
				final String str = String.format( "Changed spot " + spot + " radius to R = %.1f.", spot.getFeature( Spot.RADIUS ) );
				viewer.getLogger().log( str );
			}
			refresh();
		}
	}

	/**
	 * Toggle the spot creation test on/off. If on, MaMuT will prevent a new
	 * spot to be created within another one.
	 */
	public void toggleSpotCreationTest()
	{
		testWithinSpot = !testWithinSpot;
	}

	public void toggleLinkingMode( final Logger lLogger )
	{
		this.isLinkingMode = !isLinkingMode;
		final String str = "Switched auto-linking mode " + ( isLinkingMode ? "on." : "off." );
		lLogger.log( str );
	}

	/**
	 * Toggles a link between two spots.
	 * <p>
	 * The two spots are taken from the selection, which must have exactly two
	 * spots in it
	 *
	 * @param lLogger
	 *            the {@link Logger} to echo linking messages.
	 */
	public void toggleLink( final Logger lLogger )
	{
		/*
		 * Toggle a link between two spots.
		 */
		final Set< Spot > selectedSpots = selectionModel.getSpotSelection();
		if ( selectedSpots.size() == 2 )
		{
			final Iterator< Spot > it = selectedSpots.iterator();
			final Spot source = it.next();
			final Spot target = it.next();

			if ( model.getTrackModel().containsEdge( source, target ) )
			{
				/*
				 * Remove it
				 */
				model.beginUpdate();
				try
				{
					model.removeEdge( source, target );
					lLogger.log( "Removed the link between " + source + " and " + target + ".\n" );
				}
				finally
				{
					model.endUpdate();
				}

			}
			else
			{
				/*
				 * Create a new link
				 */
				final int ts = source.getFeature( Spot.FRAME ).intValue();
				final int tt = target.getFeature( Spot.FRAME ).intValue();

				if ( tt != ts )
				{
					model.beginUpdate();
					try
					{
						model.addEdge( source, target, -1 );
						lLogger.log( "Created a link between " + source + " and " + target + ".\n" );
					}
					finally
					{
						model.endUpdate();
					}
					/*
					 * To emulate a kind of automatic linking, we put the last
					 * spot to the selection, so several spots can be tracked in
					 * a row without having to de-select one
					 */
					Spot single;
					if ( tt > ts )
					{
						single = target;
					}
					else
					{
						single = source;
					}
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection( single );

				}
				else
				{
					lLogger.error( "Cannot create a link between two spots belonging in the same frame." );
				}
			}
		}
		else
		{
			lLogger.error( "Expected selection to contain 2 spots, found " + selectedSpots.size() + ".\n" );
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	private MamutViewer newViewer()
	{
		/*
		 * Test if we have 2D images.
		 *
		 * The test is a bit loose here. We get into 2D mode if all the sources,
		 * at the first time-point where they have data, have a number of pixels
		 * in the 3rd dimension equals or lower than 1.
		 */
		boolean is2D = true;
		for ( final SourceAndConverter< ? > sac : sources )
		{
			final Source< ? > source = sac.getSpimSource();
			for ( int t = 0; t < nTimepoints; t++ )
			{
				if ( source.isPresent( t ) )
				{
					final RandomAccessibleInterval< ? > level = source.getSource( t, 0 );
					if ( level.dimension( 2 ) > 1 )
						is2D = false;

					break;
				}
			}
		}

		final ViewerOptions options = ViewerOptions.options().is2D( is2D );

		final MamutViewer viewer = new MamutViewer(
				DEFAULT_WIDTH, DEFAULT_HEIGHT,
				sources, nTimepoints, cache,
				model, selectionModel,
				options,
				bookmarks );

		final ConverterSetups setups = viewer.getConverterSetups();
		if ( converterSetups.size() != sources.size() )
			System.err.println( "WARNING! Constructing BigDataViewer, with converterSetups.size() that is not the same as sources.size()." );
		final int numSetups = Math.min( converterSetups.size(), sources.size() );
		for ( int i = 0; i < numSetups; ++i )
		{
			final SourceAndConverter< ? > source = sources.get( i );
			final ConverterSetup setup = converterSetups.get( i );
			if ( setup != null )
				setups.put( source, setup );
		}

		for ( final String key : guimodel.getDisplaySettings().keySet() )
			viewer.setDisplaySettings( key, guimodel.getDisplaySettings().get( key ) );

		installKeyBindings( viewer );
		installMouseListeners( viewer );

		viewer.addWindowListener( new DeregisterWindowListener( viewer ) );

		InitializeViewerState.initTransform( viewer.getViewerPanel() );
		if ( is2D )
		{
			final AffineTransform3D t = new AffineTransform3D();
			viewer.getViewerPanel().state().getViewerTransform( t );
			t.set( 0., 2, 3 );
			viewer.getViewerPanel().setCurrentViewerTransform( t );

			// Blocks some actions that make no sense for 2D data.
			final AbstractAction blockerAction = new AbstractAction( "Do nothing" )
			{

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( final ActionEvent e )
				{}
			};

			final ActionMap actionMap = new ActionMap();
			actionMap.put( "align ZY plane", blockerAction );
			actionMap.put( "align XZ plane", blockerAction );
			viewer.getKeybindings().addActionMap( "2d mamut", actionMap );
		}

		viewer.setJMenuBar( createMenuBar( viewer ) );

		viewer.render();
		guimodel.addView( viewer );

		viewer.refresh();
		return viewer;

	}

	private JMenuBar createMenuBar( final MamutViewer viewer )
	{
		final ActionMap actionMap = viewer.getKeybindings().getConcatenatedActionMap();

		final JMenuBar menubar = new JMenuBar();

		/*
		 * Settings.
		 */

		JMenu menu = new JMenu( "Settings" );
		menubar.add( menu );

		final JMenuItem miBrightness = new JMenuItem( actionMap.get( BigDataViewerActions.BRIGHTNESS_SETTINGS ) );
		miBrightness.setText( "Brightness & Color" );
		menu.add( miBrightness );

		final JMenuItem miVisibility = new JMenuItem( actionMap.get( BigDataViewerActions.VISIBILITY_AND_GROUPING ) );
		miVisibility.setText( "Visibility & Grouping" );
		menu.add( miVisibility );

		/*
		 * Tools.
		 */

		menu = new JMenu( "Tools" );
		menubar.add( menu );

		final JMenuItem miMovie = new JMenuItem( actionMap.get( BigDataViewerActions.RECORD_MOVIE ) );
		miMovie.setText( "Record Movie" );
		menu.add( miMovie );

		final JMenuItem miMaxProjectMovie = new JMenuItem( actionMap.get( BigDataViewerActions.RECORD_MAX_PROJECTION_MOVIE ) );
		miMaxProjectMovie.setText( "Record Max-Projection Movie" );
		menu.add( miMaxProjectMovie );

		/*
		 * Help.
		 */

		menu = new JMenu( "Help" );
		menubar.add( menu );

		final JMenuItem miHelp = new JMenuItem( actionMap.get( BigDataViewerActions.SHOW_HELP ) );
		miHelp.setText( "Show Help" );
		menu.add( miHelp );

		return menubar;
	}

	private void save()
	{
		final Logger lLogger = Logger.IJ_LOGGER;
		final File proposed = IOUtils.askForFileForSaving( mamutFile, IJ.getInstance(), lLogger );
		if ( null == proposed )
		{
			lLogger.log( "Saving canceled.\n" );
			return;
		}
		mamutFile = proposed;

		MamutXmlWriter writer = null;
		try
		{
			lLogger.log( "Saving to " + mamutFile + '\n' );
			writer = new MamutXmlWriter( mamutFile, lLogger );
			writer.appendModel( model );
			writer.appendSettings( settings );
			writer.appendMamutState( guimodel, setupAssignments, bookmarks );
			writer.writeToFile();
			lLogger.log( "Done.\n" );
		}
		catch ( final FileNotFoundException e )
		{
			lLogger.error( "Could not find file " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			lLogger.error( "Could not write to " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final Exception e )
		{
			lLogger.error( "Something wrong happened while saving to " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
	}

	private void somethingWrongHappenedWhileSaving( final MamutXmlWriter writer )
	{
		if ( null != writer )
		{
			final String text = "A problem occured when saving to a file. "
					+ "To recuperate your work, ust copy/paste the text "
					+ "below the line and save it to an XML file.\n"
					+ "__________________________\n"
					+ writer.toString();
			new TextWindow( PLUGIN_NAME + " v" + PLUGIN_VERSION + " save file dump", text, 600, 800 );
		}
	}

	private void requestRepaintAllViewers()
	{
		if ( guimodel != null )
		{
			for ( final TrackMateModelView view : guimodel.getViews() )
			{
				if ( view instanceof MamutViewer )
				{
					( ( MamutViewer ) view ).getViewerPanel().requestRepaint();
				}
			}
		}
	}

	/**
	 * Configures the specified {@link MamutViewer} with key bindings.
	 *
	 * @param viewer
	 *            the {@link MamutViewer} to configure.
	 */
	private void installKeyBindings( final MamutViewer viewer )
	{

		new MamutKeyboardHandler( this, viewer );

		/*
		 * Custom key presses
		 */

		viewer.addHandler( new KeyListener()
		{

			@Override
			public void keyTyped( final KeyEvent event )
			{}

			@Override
			public void keyReleased( final KeyEvent event )
			{
				if ( event.getKeyCode() == moveSpotKeystroke.getKeyCode() )
				{
					if ( null != movedSpot )
					{
						model.beginUpdate();
						try
						{
							model.updateFeatures( movedSpot );
						}
						finally
						{
							model.endUpdate();
							final String str = String.format( "Moved spot " + movedSpot + " to location X = %.1f, Y = %.1f, Z = %.1f.", movedSpot.getFeature( Spot.POSITION_X ), movedSpot.getFeature( Spot.POSITION_Y ), movedSpot.getFeature( Spot.POSITION_Z ) );
							viewer.getLogger().log( str );
							movedSpot = null;
						}
						refresh();
					}
				}
			}

			@Override
			public void keyPressed( final KeyEvent event )
			{
				if ( event.getKeyCode() == moveSpotKeystroke.getKeyCode() )
				{
					movedSpot = getSpotWithinRadius( viewer.getViewerPanel() );
				}

			}
		} );

	}

	/**
	 * Configures the specified {@link MamutViewer} with mouse listeners.
	 *
	 * @param viewer
	 *            the {@link MamutViewer} to configure.
	 */
	private void installMouseListeners( final MamutViewer viewer )
	{
		viewer.addHandler( new MouseMotionListener()
		{

			@Override
			public void mouseMoved( final MouseEvent arg0 )
			{
				if ( null != movedSpot )
				{
					final RealPoint gPos = new RealPoint( 3 );
					viewer.getViewerPanel().getGlobalMouseCoordinates( gPos );
					final double[] coordinates = new double[ 3 ];
					gPos.localize( coordinates );
					movedSpot.putFeature( Spot.POSITION_X, coordinates[ 0 ] );
					movedSpot.putFeature( Spot.POSITION_Y, coordinates[ 1 ] );
					movedSpot.putFeature( Spot.POSITION_Z, coordinates[ 2 ] );
				}
			}

			@Override
			public void mouseDragged( final MouseEvent arg0 )
			{}
		} );

		viewer.addHandler( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final MouseEvent event )
			{

				if ( event.getClickCount() < 2 )
				{

					final Spot spot = getSpotWithinRadius( viewer.getViewerPanel() );
					if ( null != spot )
					{
						// Center view on it
						centerOnSpot( spot );
						if ( !event.isShiftDown() )
						{
							// Replace selection
							selectionModel.clearSpotSelection();
						}
						// Toggle it to selection
						if ( selectionModel.getSpotSelection().contains( spot ) )
						{
							selectionModel.removeSpotFromSelection( spot );
						}
						else
						{
							selectionModel.addSpotToSelection( spot );
						}

					}
					else
					{
						// Clear selection if we don't hold shift.
						if ( !event.isShiftDown() )
							selectionModel.clearSelection();
					}

				}
				else
				{

					final Spot spot = getSpotWithinRadius( viewer.getViewerPanel() );
					if ( null == spot )
					{
						// Create a new spot
						addSpot( viewer );
					}

				}
				refresh();
			}
		} );

	}

	private void launchTrackScheme( final JButton button )
	{
		button.setEnabled( false );
		new Thread( "Launching TrackScheme thread" )
		{

			@Override
			public void run()
			{
				final TrackScheme trackscheme = new TrackScheme( model, selectionModel );
				trackscheme.getGUI().addWindowListener( new DeregisterWindowListener( trackscheme ) );
				trackscheme.setSpotImageUpdater( thumbnailUpdater );
				for ( final String settingKey : guimodel.getDisplaySettings().keySet() )
				{
					trackscheme.setDisplaySettings( settingKey, guimodel.getDisplaySettings().get( settingKey ) );
				}
				selectionModel.addSelectionChangeListener( trackscheme );
				trackscheme.render();
				guimodel.addView( trackscheme );
				button.setEnabled( true );
			}
		}.start();
	}

	private void launch3DViewer( final JButton button )
	{
		button.setEnabled( false );
		new Thread( "MaMuT new 3D viewer thread" )
		{
			@Override
			public void run()
			{
				final Image3DUniverse universe = new Image3DUniverse();
				final ImageWindow3D win = new ImageWindow3D( "MaMuT 3D Viewer", universe );
				win.setIconImage( MamutControlPanel.THREEDVIEWER_ICON.getImage() );
				universe.init( win );
				win.pack();
				win.setLocationByPlatform( true );
				win.setVisible( true );

				// universe.show();
				final SpotDisplayer3D newDisplayer = new SpotDisplayer3D( model, selectionModel, universe );
				for ( final String key : guimodel.getDisplaySettings().keySet() )
				{
					newDisplayer.setDisplaySettings( key, guimodel.getDisplaySettings().get( key ) );
				}
				guimodel.addView( newDisplayer );
				newDisplayer.render();
				button.setEnabled( true );
			}
		}.start();
	}

	private void refresh()
	{
		// Just ask to repaint the TrackMate overlay
		for ( final TrackMateModelView viewer : guimodel.getViews() )
		{
			viewer.refresh();
		}
	}

	private void centerOnSpot( final Spot spot )
	{
		for ( final TrackMateModelView otherView : guimodel.getViews() )
		{
			otherView.centerViewOn( spot );
		}
	}

	/**
	 * Returns the closest {@link Spot} with respect to the current mouse
	 * location, and for which the current location is within its radius, or
	 * <code>null</code> if there is no such spot. In other words: returns the
	 * spot in which the mouse pointer is.
	 *
	 * @param viewer
	 *            the viewer to inspect.
	 * @return the closest spot within radius.
	 */
	private Spot getSpotWithinRadius( final MamutViewerPanel viewer )
	{
		/*
		 * Get the closest spot
		 */
		final int frame = viewer.state().getCurrentTimepoint();
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates( gPos );
		final double[] coordinates = new double[ 3 ];
		gPos.localize( coordinates );
		final Spot location = new Spot( coordinates[ 0 ], coordinates[ 1 ], coordinates[ 2 ], radius, -1d );
		final Spot closestSpot = model.getSpots().getClosestSpot( location, frame, true );
		if ( null == closestSpot )
			return null;
		/*
		 * Determine if we are inside the spot
		 */
		final double d2 = closestSpot.squareDistanceTo( location );
		final double r = closestSpot.getFeature( Spot.RADIUS );
		if ( d2 < r * r )
			return closestSpot;

		return null;
	}

	/*
	 * GETTERS
	 */

	/**
	 * Exposes the GUI model that stores this GUI states.
	 *
	 * @return the {@link MamutGUIModel}.
	 */
	public MamutGUIModel getGuimodel()
	{
		return guimodel;
	}

	/**
	 * Exposes the {@link SelectionModel} shared amongst all views in the MaMuT
	 * session.
	 *
	 * @return the {@link SelectionModel}.
	 */
	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	/**
	 * Exposes the GUI frame that fosters user interface with this MaMuT
	 * session.
	 *
	 * @return the {@link MamutGUI}.
	 */
	public MamutGUI getGUI()
	{
		return gui;
	}

	/**
	 * Exposes the TrackMate object that controls this MaMuT session.
	 *
	 * @return the {@link TrackMate} object.
	 */
	public TrackMate getTrackMate()
	{
		return trackmate;
	}

	/**
	 * Exposes the {@link SetupAssignments} that controls the display in this
	 * MaMuT session.
	 *
	 * @return the {@link SetupAssignments}.
	 */
	public SetupAssignments getSetupAssignments()
	{
		return setupAssignments;
	}

	/**
	 * Exposes the {@link Bookmarks} of this MaMuT session.
	 *
	 * @return the {@link Bookmarks} instance.
	 */
	public Bookmarks getBookmarks()
	{
		return bookmarks;
	}

	/*
	 * PRIVATE CLASSES
	 */

	private class DeregisterWindowListener implements WindowListener
	{

		private final TrackMateModelView view;

		public DeregisterWindowListener( final TrackMateModelView view )
		{
			this.view = view;
		}

		@Override
		public void windowActivated( final WindowEvent arg0 )
		{}

		@Override
		public void windowClosed( final WindowEvent arg0 )
		{}

		@Override
		public void windowClosing( final WindowEvent arg0 )
		{
			guimodel.removeView( view );
		}

		@Override
		public void windowDeactivated( final WindowEvent arg0 )
		{}

		@Override
		public void windowDeiconified( final WindowEvent arg0 )
		{}

		@Override
		public void windowIconified( final WindowEvent arg0 )
		{}

		@Override
		public void windowOpened( final WindowEvent arg0 )
		{}

	}

	/*
	 * STATIC METHODS
	 */

	private static final void setLaF()
	{
		// I can't stand the metal look. If this is a problem, contact me
		// (jeanyves.tinevez@gmail.com)
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( final ClassNotFoundException e )
			{
				e.printStackTrace();
			}
			catch ( final InstantiationException e )
			{
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				e.printStackTrace();
			}
			catch ( final UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}
		}
	}
}
