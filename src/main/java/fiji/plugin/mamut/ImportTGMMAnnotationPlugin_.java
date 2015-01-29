package fiji.plugin.mamut;

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;
import fiji.plugin.mamut.io.MamutXmlWriter;
import fiji.plugin.mamut.providers.MamutEdgeAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutSpotAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutTrackAnalyzerProvider;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.io.TGMMImporter;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.util.gui.GenericDialogPlus;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class ImportTGMMAnnotationPlugin_ implements PlugIn
{

	private static final Font BIG_FONT = new Font( "Arial", Font.BOLD, 16 );
	
	private static final URL AMAT_PAPER_LINK;
	static
	{
		URL temp;
		try
		{
			temp = new URL( "http://www.nature.com/nmeth/journal/v11/n9/full/nmeth.3036.html" );
		}
		catch ( final java.net.MalformedURLException e )
		{
			temp = null;
		}
		AMAT_PAPER_LINK = temp;
	}

	private static final String HELP_MESSAGE = "<html>"
			+ "This plugin creates a MaMuT file from a BigDataViewer "
			+ "XML/HDF5 image and a folder containing the file generated "
			+ "by the TGMM algorithm. "
			+ "<p>"
			+ "See the paper from Fernando Amat and colleagues to generate "
			+ "these annotations: <br>"
			+ "<a href=\"" + AMAT_PAPER_LINK.toString() + "\">"
			+ "Fast, accurate reconstruction of cell lineages from large-scale fluorescence microscopy data</a>."
			+ "<p>"
			+ "This plugin requires two inputs and one file input:"
			+ "<ul>"
			+ "	<li> First you need to specify where is the image data BLABLA."
			+ "</html>";

	private static final String ANGLE_HELP_MESSAGE = "<html>Patati, patata.</html>";

	private String defaultXMLHDF5Path;

	private String defaultTGMMPath;

	private String defaultOutputPath;

	private Logger logger = Logger.DEFAULT_LOGGER;

	@Override
	public void run( final String arg )
	{
		showDialog();
	}

	private void showDialog()
	{
		logger = Logger.IJ_LOGGER;
		final GenericDialogPlus dialog = new GenericDialogPlus( PLUGIN_NAME + " v" + PLUGIN_VERSION );

		dialog.addMessage( "Import TGMM annotations", BIG_FONT );
		dialog.addImage( MaMuT.MAMUT_ICON );

		if ( null == defaultXMLHDF5Path )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultXMLHDF5Path = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Select the image data (XML of the xml/hdf5 couple)." );
		dialog.addFileField( "Image data", defaultXMLHDF5Path, 30 );

		if ( null == defaultTGMMPath )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultTGMMPath = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Select the TGMM annotation folder." );
		dialog.addDirectoryField( "TGMM folder", defaultTGMMPath, 30 );

		if ( null == defaultOutputPath )
		{
			final File folder = new File( System.getProperty( "user.dir" ) );
			final File parent = folder.getParentFile();
			defaultOutputPath = parent == null ? null : parent.getParentFile().getAbsolutePath();
		}
		dialog.addMessage( "Output to file:" );
		dialog.addFileField( "MaMuT file", defaultOutputPath, 30 );

		dialog.addHelp( HELP_MESSAGE );

		dialog.showDialog();

		/*
		 * Process inputs
		 */

		if ( dialog.wasCanceled() ) { return; }

		defaultXMLHDF5Path = dialog.getNextString();
		defaultTGMMPath = dialog.getNextString();
		defaultOutputPath = dialog.getNextString();

		/*
		 * Ask for a view setup
		 */

		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( defaultXMLHDF5Path );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}

		/*
		 * Read view setup
		 */

		final List< ? extends BasicViewSetup > viewSetupsOrdered = spimData.getSequenceDescription().getViewSetupsOrdered();
		final int numViewSetups = viewSetupsOrdered.size();
		final String[] angles = new String[ numViewSetups ];
		for ( int setup = 0; setup < numViewSetups; setup++ )
		{
			final Angle angle = viewSetupsOrdered.get( setup ).getAttribute( Angle.class );
			angles[ setup ] = "angle " + ( angle == null ? setup : angle.getName() );
		}

		/*
		 * Dialog to select target angle.
		 */

		final GenericDialogPlus dialogAngles = new GenericDialogPlus( PLUGIN_NAME + " v" + PLUGIN_VERSION );
		dialogAngles.addMessage( "Select the view that was used to run TGMM.", BIG_FONT );
		dialogAngles.addImage( MaMuT.MAMUT_ICON );

		dialogAngles.addChoice( "View:", angles, angles[ 0 ] );

		dialogAngles.addHelp( ANGLE_HELP_MESSAGE );
		dialogAngles.showDialog();

		/*
		 * Load corresponding angle
		 */

		if ( dialogAngles.wasCanceled() ) { return; }

		final int angleIndex = dialogAngles.getNextChoiceIndex();
		final int setupID = spimData.getSequenceDescription().getViewSetupsOrdered().get( angleIndex ).getId();
		exec( defaultXMLHDF5Path, setupID, defaultTGMMPath, defaultOutputPath );
	}

	public void exec( final String xmlHDF5Path, final int setupID, final String tgmmPath, final String outputPath )
	{
		SpimDataMinimal spimData;
		try
		{
			spimData = new XmlIoSpimDataMinimal().load( xmlHDF5Path );
		}
		catch ( final SpimDataException e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return;
		}
		final Model model = createModel( new File( tgmmPath ), spimData, setupID );
		model.setLogger( logger );
		final Settings settings = createSettings( new File( xmlHDF5Path ) );

		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );
		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		save( outputPath, model, settings );
	}

	private void save( final String outputPath, final Model model, final Settings settings )
	{

		final File mamutFile = new File( outputPath );
		MamutXmlWriter writer = null;
		try
		{
			logger.log( "Saving to " + mamutFile + '\n' );
			writer = new MamutXmlWriter( mamutFile, logger );
			writer.appendModel( model );
			writer.appendSettings( settings );
			writer.writeToFile();
			logger.log( "Done.\n" );
		}
		catch ( final FileNotFoundException e )
		{
			logger.error( "Could not find file " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			logger.error( "Could not write to " + mamutFile + ";\n" + e.getMessage() );
			somethingWrongHappenedWhileSaving( writer );
			e.printStackTrace();
		}
		catch ( final Exception e )
		{
			logger.error( "Something wrong happened while saving to " + mamutFile + ";\n" + e.getMessage() );
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

	protected SourceSettings createSettings( final File file )
	{
		final SourceSettings settings = new SourceSettings();
		settings.imageFileName = file.getName();
		settings.imageFolder = file.getParent();

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

		return settings;
	}

	protected Model createModel( final File tgmmFolder, final SpimDataMinimal spimData, final int setupID )
	{

		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final ViewRegistrations regs = spimData.getViewRegistrations();
		final List< AffineTransform3D > transforms = new ArrayList< AffineTransform3D >( seq.getTimePoints().size() );
		for ( final TimePoint t : seq.getTimePoints().getTimePointsOrdered() )
		{
			transforms.add( regs.getViewRegistration( t.getId(), setupID ).getModel() );
		}

		final TGMMImporter importer = new TGMMImporter( tgmmFolder, transforms, logger);
		if ( !importer.checkInput() || !importer.process() )
		{
			logger.error( importer.getErrorMessage() );
		}
		final Model model = importer.getResult();

		/*
		 * Hack to set the POSITION_T feature of imported spots.
		 */
		final Settings settings = new Settings();
		settings.dt = 1;
		final TrackMate trackmate = new TrackMate( model, settings );
		final ResetSpotTimeFeatureAction action = new ResetSpotTimeFeatureAction();
		action.execute( trackmate );

		return model;
	}

	public static void main( final String[] args )
	{
		final String xmlHDF5Path = "/Users/tinevez/Desktop/iconas/Data/Mamut/parhyale/BDV130418A325_NoTempReg.xml";
		final int setupID = 1;
		final String tgmmPath = "/Users/tinevez/Development/Fernando2";
		final String outputPath = "/Users/tinevez/Desktop/MaMuT_Importer_test.xml";

		final ImportTGMMAnnotationPlugin_ importer = new ImportTGMMAnnotationPlugin_();
		importer.run( null );

//		importer.defaultOutputPath = outputPath;
//		importer.defaultTGMMPath = tgmmPath;
//		importer.defaultXMLHDF5Path = xmlHDF5Path;
//		ImageJ.main( args );
//		importer.run( "" );

//		importer.exec( xmlHDF5Path, setupID, tgmmPath, outputPath );
	}
}
