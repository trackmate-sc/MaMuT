package fiji.plugin.mamut;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bdv.tools.InitializeViewerState;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class NewMamutAnnotationPlugin implements PlugIn
{

	private static File file;

	@Override
	public void run( final String fileStr )
	{

		final Logger logger = Logger.IJ_LOGGER;

		if ( null != fileStr && fileStr.length() > 0 )
		{
			// Skip dialog
			file = new File( fileStr );
			if ( file.isDirectory() )
			{
				file = IOUtils.askForFileForLoading( file, "Open a MaMuT xml file", IJ.getInstance(), logger );
				if ( null == file ) { return; }
			}
			if ( !file.exists() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot find image file " + fileStr );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot read image file " + fileStr );
				return;
			}

		}
		else
		{

			if ( null == file )
			{
				file = proposeBdvXmlFileToOpen();
			}
			file = IOUtils.askForFileForLoading( file, "Open a hdf5/xml file", IJ.getInstance(), logger );
			if ( null == file ) { return; }
		}

		final Model model = createModel();
		final SourceSettings settings = createSettings();
		final MaMuT mamut = new MaMuT( file, model, settings );

		/*
		 * Initialize default brightness.
		 */

		final List< SourceGroup > sourceGroups = new ArrayList<>();
		final ViewerState state = new ViewerState( settings.getSources(), sourceGroups, settings.nframes );
		final int nSources = settings.getSources().size();
		for ( int i = 0; i < nSources; i++ )
		{
			System.out.println( i ); // DEBUG
			state.setCurrentSource( i );
			InitializeViewerState.initBrightness( 0.001, 0.999, state, mamut.getSetupAssignments() );
		}

	}

	public static File proposeBdvXmlFileToOpen()
	{
		File folder = new File( System.getProperty( "user.dir" ) );
		if ( folder != null && folder.getParentFile() != null )
			folder = folder.getParentFile();
		if ( folder != null )
			return new File( folder.getPath() + File.separator + "data.xml" );
		else
			return new File( "data.xml" );
	}

	protected SourceSettings createSettings()
	{
		return new SourceSettings();
	}

	protected Model createModel()
	{
		return new Model();
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final NewMamutAnnotationPlugin plugin = new NewMamutAnnotationPlugin();
		plugin.run(
//				"/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg.xml"
				"" );
//		plugin.run( "/Users/tinevez/Desktop/Celegans.xml" );
	}

}
