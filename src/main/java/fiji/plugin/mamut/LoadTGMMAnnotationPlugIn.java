package fiji.plugin.mamut;

import ij.IJ;
import ij.ImageJ;

import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TGMMImporter;

public class LoadTGMMAnnotationPlugIn extends NewMamutAnnotationPlugin
{
	private static File tgmmFile;

	@Override
	protected Model createModel()
	{
		final Logger logger = Logger.IJ_LOGGER;

		if ( null == tgmmFile )
		{
			final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
			tgmmFile = new File( folder.getPath() + File.separator + "data.xml" );
		}
		tgmmFile = IOUtils.askForFolder( tgmmFile, "Open a TGMM /xml file", IJ.getInstance(), logger );
		if ( null == tgmmFile ) { return new Model(); }

		final TGMMImporter importer = new TGMMImporter( tgmmFile );
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

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final LoadTGMMAnnotationPlugIn plugin = new LoadTGMMAnnotationPlugIn();
		plugin.run( "/Users/tinevez/Desktop/Celegans.xml" );
	}
}
