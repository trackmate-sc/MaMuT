package fiji.plugin.mamut;

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

import java.io.File;

public class LoadMamutAnnotationPlugin implements PlugIn {

	private static File file;

	@Override
	public void run(final String fileStr) {

		final Logger logger = Logger.IJ_LOGGER;

		if (null != fileStr && fileStr.length() > 0) {
			// Skip dialog
			file = new File(fileStr);

			if ( file.isDirectory() )
			{
				file = IOUtils.askForFileForLoading( file, "Open a MaMuT xml file", IJ.getInstance(), logger );
				if ( null == file ) { return; }
			}

		} else {

			if (null == file) {
				final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				file = new File(folder.getPath() + File.separator + "data.xml");
			}
			file = IOUtils.askForFileForLoading(file, "Open a MaMuT xml file", IJ.getInstance(), logger);
			if (null == file) {
				return;
			}
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
		}

		/*
		 * Launch MaMuT
		 */

		final MaMuT mamut = new MaMuT( imageFile, model, settings );

		/*
		 * Update setup assignments
		 */

		reader.getSetupAssignments( mamut.getSetupAssignments() );

	}

	public static void main(final String[] args) {
		ImageJ.main(args);

		final LoadMamutAnnotationPlugin plugin = new LoadMamutAnnotationPlugin();
		plugin.run( "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg-mamut_JY2.xml" );
	}

}
