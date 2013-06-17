package fiji.plugin.mamut;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.io.IOUtils;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import loci.formats.FormatException;
import net.imglib2.io.ImgIOException;

public class LoadMamutAnnotationPlugin implements PlugIn {

	private static File file;

	@Override
	public void run(String arg0) {
		
		Logger logger = Logger.IJ_LOGGER;
		if (null == file) {
			File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + "data.xml");
		}
		file = IOUtils.askForFileForLoading(file, "Open a MaMuT xml file", IJ.getInstance(), logger );
		if (null == file) {
			return;
		}
		
		MaMuT_ mamut = new MaMuT_();
		mamut.load(file);
	}
	
	
	public static void main(String[] args) {
		ImageJ.main(args);
		
		LoadMamutAnnotationPlugin plugin = new LoadMamutAnnotationPlugin();
		plugin.run(null);
	}

}
