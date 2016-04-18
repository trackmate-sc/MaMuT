package fiji.plugin.mamut;

import java.io.File;

import fiji.plugin.mamut.io.MamutXmlReader;
import fiji.plugin.mamut.providers.MamutEdgeAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutSpotAnalyzerProvider;
import fiji.plugin.mamut.providers.MamutTrackAnalyzerProvider;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class LoadMamutAnnotationPlugin implements PlugIn
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

			if ( !file.exists() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot find the MaMuT file " + file );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( MaMuT.PLUGIN_NAME + " v" + MaMuT.PLUGIN_VERSION, "Cannot read the MaMuT file " + file );
				return;
			}

			if ( file.isDirectory() )
			{
				file = IOUtils.askForFileForLoading( file, "Open a MaMuT xml file", IJ.getInstance(), logger );
				if ( null == file ) { return; }
			}

		}
		else
		{

			if ( null == file )
			{
				file = NewMamutAnnotationPlugin.proposeBdvXmlFileToOpen();
			}
			file = IOUtils.askForFileForLoading( file, "Open a MaMuT xml file", IJ.getInstance(), logger );
			if ( null == file ) { return; }
		}

		load( file );

	}

	protected void load( final File mamutFile )
	{

		final MamutXmlReader reader = new MamutXmlReader( mamutFile );

		/*
		 * Read model
		 */

		final Model model = reader.getModel();

		/*
		 * Read settings
		 */

		final SourceSettings settings = new SourceSettings();
		reader.readSettings( settings, null, null, new MamutSpotAnalyzerProvider(), new MamutEdgeAnalyzerProvider(), new MamutTrackAnalyzerProvider() );

		/*
		 * Read image source location from settings object.
		 */

		File imageFile = new File( settings.imageFolder, settings.imageFileName );
		if ( !imageFile.exists() )
		{
			// Then try relative path
			imageFile = new File( mamutFile.getParent(), settings.imageFileName );
			if ( !imageFile.exists() )
			{
				model.getLogger().error( "Cannot find the image data file: " + settings.imageFileName
						+ " in " + settings.imageFolder + " nor in " + mamutFile.getParent() );
				return;
			}
		}

		/*
		 * Launch MaMuT
		 */

		final MaMuT mamut = new MaMuT( imageFile, model, settings );

		/*
		 * Update setup assignments
		 */

		reader.getSetupAssignments( mamut.getSetupAssignments() );

		/*
		 * Update bookmarks
		 */

		reader.readBookmarks( mamut.getBookmarks() );

	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final LoadMamutAnnotationPlugin plugin = new LoadMamutAnnotationPlugin();
		plugin.run( "D:/Users/Jean-Yves/Development/MaMuT-tutorials/MaMuT_Parhyale_demo-mamut.xml" );
//		plugin.run( "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg-mamut_JY2.xml" );
	}

}
