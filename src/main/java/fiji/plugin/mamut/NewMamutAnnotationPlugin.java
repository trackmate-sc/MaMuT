package fiji.plugin.mamut;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;

public class NewMamutAnnotationPlugin implements PlugIn {

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

		} else {

			if (null == file) {
				final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				file = new File(folder.getPath() + File.separator + "data.xml");
			}
			file = IOUtils.askForFileForLoading(file, "Open a hdf5/xml file", IJ.getInstance(), logger);
			if (null == file) {
				return;
			}
		}

		final Model model = createModel();
		final SourceSettings settings = createSettings();
		new MaMuT( file, model, settings );
	}

	protected SourceSettings createSettings()
	{
		return new SourceSettings();
	}

	protected Model createModel()
	{
		return new Model();
	}

	public static void main(final String[] args) {
		ImageJ.main(args);

		final NewMamutAnnotationPlugin plugin = new NewMamutAnnotationPlugin();
		// plugin.run(
		// "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg.xml"
		// );
		plugin.run( "/Users/tinevez/Desktop/Celegans.xml" );
	}

}
