package fiji.plugin.mamut;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TGMMImporter;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.SequenceViewsLoader;

public class LoadTGMMAnnotationPlugIn implements PlugIn
{
	private static File staticTGMMFolder;

	private static File staticImageFile;

	protected final Logger logger = Logger.IJ_LOGGER;

	private File askForImageFile()
	{
		if ( null == staticImageFile )
		{
			final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
			staticImageFile = new File( folder.getPath() + File.separator + "data.xml" );
		}
		staticImageFile = IOUtils.askForFileForLoading( staticImageFile, "Open a hdf5/xml file", IJ.getInstance(), logger );
		if ( null == staticImageFile ) { return null; }
		return staticImageFile;
	}

	private File askForTGMMFolder()
	{
		if ( null == staticTGMMFolder )
		{
			final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
			staticTGMMFolder = new File( folder.getPath() + File.separator + "data.xml" );
		}
		staticTGMMFolder = IOUtils.askForFolder( staticTGMMFolder, "Open a TGMM /xml folder", IJ.getInstance(), logger );
		if ( null == staticTGMMFolder ) { return null; }
		return staticImageFile;
	}

	private int askForAngle( final String[] angles )
	{
		final Component frame = IJ.getInstance();
		final Icon icon = MaMuT.MAMUT_ICON;
		final String s = ( String ) JOptionPane.showInputDialog( frame, "Select the view that was used by the TGMM:", "MaMuT TGMM import", JOptionPane.PLAIN_MESSAGE, icon, angles, angles[ 0 ] );
		if ( s == null || s.length() == 0 ) { return -1; }

		final int setupID = Arrays.asList( angles ).indexOf( s );
		return setupID;
	}

	@Override
	public void run( final String ignored )
	{
		final File imageFile = askForImageFile();
		if ( null == imageFile ) { return; }

		final File tgmmFolder = askForTGMMFolder();
		if ( null == tgmmFolder ) { return; }

		final SequenceViewsLoader loader = createImageFileLoader( imageFile );
		final String[] angles = readSetupNames( loader.getSequenceDescription() );
		final int angleIndex = askForAngle( angles );
		if ( angleIndex < 0 ) { return; }

		launchMamut( imageFile, tgmmFolder, angleIndex, loader );

	}

	private SequenceViewsLoader createImageFileLoader( final File imageFile )
	{
		SequenceViewsLoader loader;
		try
		{
			loader = new SequenceViewsLoader( imageFile.getAbsolutePath() );
		}
		catch ( final Exception e )
		{
			logger.error( "Problem reading the transforms in image data file:\n" + e.getMessage() + "\n" );
			return null;
		}
		return loader;
	}

	public void launchMamut( final File imageFile, final File tgmmFile, final int setupID, final SequenceViewsLoader loader )
	{
		final Model model = createModel( tgmmFile, loader, setupID );
		final SourceSettings settings = createSettings();
		new MaMuT( imageFile, model, settings );
	}

	public void launchMamut( final File imageFile, final File tgmmFolder, final int angleIndex )
	{
		final SequenceViewsLoader loader = createImageFileLoader( imageFile );
		launchMamut( imageFile, tgmmFolder, angleIndex, loader );
	}

	protected Model createModel( final File tgmmFolder, final SequenceViewsLoader loader, final int setupID )
	{
		final List< AffineTransform3D > transforms = pickTransform( loader, setupID );

		final TGMMImporter importer = new TGMMImporter( tgmmFolder, transforms, Logger.IJ_LOGGER );
		if ( !importer.checkInput() || !importer.process() )
		{
			logger.error( importer.getErrorMessage() );
			return new Model();
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

	protected SourceSettings createSettings()
	{
		return new SourceSettings();
	}

	protected String[] readSetupNames( final SequenceDescription seq )
	{
		final int numViewSetups = seq.numViewSetups();
		final String[] angles = new String[ numViewSetups ];
		for ( int setup = 0; setup < numViewSetups; setup++ )
		{
			angles[ setup ] = "angle " + seq.setups.get( setup ).getAngle();
		}
		return angles;
	}

	protected List< AffineTransform3D > pickTransform( final SequenceViewsLoader loader, final int setupID )
	{
		final SequenceDescription seq = loader.getSequenceDescription();
		final List< AffineTransform3D > transforms = new ArrayList< AffineTransform3D >( seq.numTimepoints() );
		for ( int t = 0; t < seq.numTimepoints(); t++ )
		{
			transforms.add( loader.getView( t, setupID ).getModel() );
		}
		return transforms;
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final File imageFile = new File( "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg.xml" );
		final File tgmmFolder = new File( "/Users/tinevez/Development/Fernando/extract" );
		final int angleIndex = 0;

		final LoadTGMMAnnotationPlugIn plugin = new LoadTGMMAnnotationPlugIn();
		plugin.launchMamut( imageFile, tgmmFolder, angleIndex );
	}

}
