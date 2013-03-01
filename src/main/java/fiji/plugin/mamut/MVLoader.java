package fiji.plugin.mamut;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import loci.formats.FormatException;
import net.imglib2.realtransform.AffineTransform3D;

public class MVLoader implements PlugIn {

	public MVLoader() {}

	@Override
	public void run(String arg) {
		
		File folder = null;
		if (null != arg ) {
			folder = new File(arg);
		}
		
		Logger logger = Logger.IJ_LOGGER;
		logger.log("Loading a saved annotation file with " + MaMuT_.PLUGIN_NAME + " " + MaMuT_.PLUGIN_VERSION);
		
		File file = MVLauncher.askForFile(folder, null, logger, "Locate a MVT xml file.");
		if (null == file) {
			return;
		}
		
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			logger.error("Problem while reading file "+file+".\n" + reader.getErrorMessage());
			return;
		}
		
		// Build model from xml file
		TrackMateModel model = plugin.getModel();
				
		// Logger
		model.setLogger(logger);
		
		// Load image dataset
		Settings settings = model.getSettings();
		File imageFile = new File(settings.imageFolder, settings.imageFileName);
		Map<ImagePlus, List<AffineTransform3D>> impMap;

		try {
			
			// Load all the data
			impMap = MVLauncher.openSPIM(imageFile, true, Logger.IJ_LOGGER);
			
			// Configure model & setup settings
			settings.imp = impMap.keySet().iterator().next();
			settings.imageFileName = imageFile.getName();
			settings.imageFolder = imageFile.getParent();

			// Strip feature model
			model.getFeatureModel().setSpotFeatureFactory(null);
			
			// Initialize viewer
			logger.log("Instantiating viewer.\n");
			MultiViewDisplayer viewer = new MultiViewDisplayer(impMap.keySet(), impMap, model);
			logger.log("Rendering viewer.\n");
			viewer.render();
			logger.log("Done.\n");
			
			// Show controller
			MultiViewTrackerConfigPanel mtvc = new MultiViewTrackerConfigPanel(model, viewer);
			mtvc.setVisible(true);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}

	}

	/*
	 * MAIN
	 */
	
	public static void main(String[] args) {
		ImageJ.main(args);
//		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		String rootFolder = "/Users/tinevez/Projects/PTomancak/Data";
		new MVLoader().run(rootFolder);
	}
}
