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

public class LoadMamutAnnotationPlugin implements PlugIn {

	private static File file;

	@Override
	public void run(final String arg0) {

		final Logger logger = Logger.IJ_LOGGER;
		if (null == file) {
			final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + "data.xml");
		}
		file = IOUtils.askForFileForLoading(file, "Open a MaMuT xml file", IJ.getInstance(), logger);
		if (null == file) {
			return;
		}

		final MaMuT mamut = new MaMuT();
		try {
			mamut.load(file);
		} catch (final ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		ImageJ.main(args);

		final LoadMamutAnnotationPlugin plugin = new LoadMamutAnnotationPlugin();
		plugin.run(null);
	}

}
