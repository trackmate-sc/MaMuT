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
	private static File tgmmFile;

	private static File imageFile;

	@Override
	public void run( final String fileStr )
	{
		final Logger logger = Logger.IJ_LOGGER;

		if ( null != fileStr && fileStr.length() > 0 )
		{
			// Skip dialog
			imageFile = new File( fileStr );
			if ( imageFile.isDirectory() )
			{
				imageFile = IOUtils.askForFileForLoading( imageFile, "Open a MaMuT xml file", IJ.getInstance(), logger );
				if ( null == imageFile ) { return; }
			}
			if ( !imageFile.exists() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot find image file " + fileStr );
				return;
			}
			if ( !imageFile.canRead() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot read image file " + fileStr );
				return;
			}

		}
		else
		{

			if ( null == imageFile )
			{
				final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
				imageFile = new File( folder.getPath() + File.separator + "data.xml" );
			}
			imageFile = IOUtils.askForFileForLoading( imageFile, "Open a hdf5/xml file", IJ.getInstance(), logger );
			if ( null == imageFile ) { return; }
		}

		if ( null == tgmmFile )
		{
			final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
			tgmmFile = new File( folder.getPath() + File.separator + "data.xml" );
		}
		tgmmFile = IOUtils.askForFolder( tgmmFile, "Open a TGMM /xml file", IJ.getInstance(), logger );
		if ( null == tgmmFile ) { return; }


		final Model model = createModel( tgmmFile, imageFile );
		final SourceSettings settings = createSettings();
		new MaMuT( imageFile, model, settings );
	}

	protected Model createModel( final File tgmmFile, final File imageFile )
	{
		final Logger logger = Logger.IJ_LOGGER;

		final List< AffineTransform3D > transforms = pickTransform( imageFile );

		final TGMMImporter importer = new TGMMImporter( tgmmFile, transforms );
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

	protected List< AffineTransform3D > pickTransform( final File imageFile )
	{
		final Logger logger = Logger.IJ_LOGGER;
		SequenceViewsLoader loader;
		try
		{
			loader = new SequenceViewsLoader( imageFile.getAbsolutePath() );
		}
		catch ( final Exception e )
		{
			logger.error( "Problem reading the tranforms in image data file:\n" + e.getMessage() + "\n" );
			return null;
		}

		final SequenceDescription seq = loader.getSequenceDescription();
		final int numViewSetups = seq.numViewSetups();
		final String[] angles = new String[ numViewSetups ];
		for ( int setup = 0; setup < numViewSetups; setup++ )
		{
			angles[ setup ] = "angle " + seq.setups.get( setup ).getAngle();
		}

		final Component frame = IJ.getInstance();
		final Icon icon = MaMuT.MAMUT_ICON;
		final String s = ( String ) JOptionPane.showInputDialog( frame, "Select the view that was used by the TGMM:", "Pick raw source ", JOptionPane.PLAIN_MESSAGE, icon, angles, angles[ 0 ] );
		if ( s == null || s.length() == 0 ) { return null; }

		final int setupID = Arrays.asList( angles ).indexOf( s );
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

		final LoadTGMMAnnotationPlugIn plugin = new LoadTGMMAnnotationPlugIn();
		plugin.run( "/Users/tinevez/Desktop/Celegans.xml" );
	}
}
