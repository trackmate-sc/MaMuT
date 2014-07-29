package fiji.plugin.mamut;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;

import mpicbg.spim.data.SpimDataException;

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

		final MaMuT mamut = new MaMuT();
		try {
			mamut.load(file);
		} catch (final SpimDataException e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		ImageJ.main(args);

		final LoadMamutAnnotationPlugin plugin = new LoadMamutAnnotationPlugin();
		//		plugin.run("/Users/tinevez/Desktop/Data/Mamut/parhyale-crop/parhyale-crop-2-mamut.xml");
		// plugin.run("/Users/tinevez/Desktop/Data/Mamut/combined-mamut-20.xml");
		// plugin.run( "/Users/JeanYves/Desktop/Data/Celegans-mamut.xml" );
		plugin.run(
		 "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg-mamut_JY.xml"
		 );
//		plugin.run( "/Users/tinevez/Desktop/Data/Mamut/parhyale/" );
	}

}
