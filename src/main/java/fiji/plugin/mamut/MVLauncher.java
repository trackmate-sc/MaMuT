package fiji.plugin.mamut;

import fiji.plugin.mamut.util.TransformUtils;
import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

import loci.formats.FilePattern;
import loci.formats.FormatException;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import loci.plugins.in.ImporterOptions;
import net.imglib2.realtransform.AffineTransform3D;

public class MVLauncher implements PlugIn {

	private static final String ANGLE_STRING = "Angle";
	private static final String TIMEPOINT_STRING = "TL";
	private static final String REGISTRATION_SUBFOLDER = "registration";
	private static final String REGISTRATION_FILE_SUFFIX = ".registration";


	public MVLauncher() {}

	@Override
	public void run(String arg) {

		File folder = null;
		if (null != arg ) {
			folder = new File(arg);
		}

		Logger logger = Logger.IJ_LOGGER;
		logger.log("Launching a new annotation with " + MaMuT_.PLUGIN_NAME + " " + MaMuT_.PLUGIN_VERSION);

		File file = askForFile(folder, null, logger, "Open a SPIM series");
		Map<ImagePlus, List<AffineTransform3D>> impMap;

		try {

			// Load all the data
			impMap = openSPIM(file, true, Logger.IJ_LOGGER);

			// Instantiate model & setup settings
			ImagePlus imp1 = impMap.keySet().iterator().next();
			Settings settings = new Settings(imp1);
			settings.imageFileName = file.getName();
			settings.imageFolder = file.getParent();

			DetectorProvider provider = new DetectorProvider();
			provider.select(ManualDetectorFactory.DETECTOR_KEY);
			settings.detectorFactory = provider.getDetectorFactory();
			settings.detectorSettings = provider.getDefaultSettings();

			TrackMate_ tm = new TrackMate_(settings);
			TrackMateModel model = tm.getModel();
			model.setLogger(logger);

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
	 * STATIC METHODS
	 */

	static Map<ImagePlus, List<AffineTransform3D>> openSPIM(File path, boolean useVirtual, final Logger logger) throws IOException, FormatException {
		LociImporter plugin = new LociImporter();
		Importer importer = new Importer(plugin);
		ImporterOptions  options = importer.parseOptions("");

		options.setId(path.getAbsolutePath());
		options.setGroupFiles(true);
		options.setVirtual(useVirtual);
		options.setSplitChannels(true);
		options.setQuiet(true);

		ImportProcess process = new ImportProcess(options);
		ImagePlusReader reader = new ImagePlusReader(process);
		DisplayHandler displayHandler = new DisplayHandler(process);

		process.execute();

		// Get the ImagePluses - 1 per channel (or angle)
		ImagePlus[] imps = importer.readPixels(reader, options, displayHandler);
		int nImps = imps.length;
		// Get the file names used to build these imps
		String[] fileNames = process.getVirtualReader().getUsedFiles();
		int nFileNames = fileNames.length;

		logger.log("Found "+nFileNames+" files grouped in "+nImps+" views.\n");

		final FilePattern fp 		= process.getFileStitcher().getFilePattern();
		final String[][] elements 	= fp.getElements();
		final String[] blocks 		= fp.getBlocks();
		final String[] prefixes 	= fp.getPrefixes();
		final String suffix 		= fp.getSuffix();
		logger.log("File pattern used is "+fp.getPattern()+".\n");

		importer.finish(process);

		// Try to identify the angle block
		int angleBlockIndex = -1;
		int timepointBlockIndex = -1;
		for (int i = 0; i < blocks.length; i++) {
			if (prefixes[i].contains(ANGLE_STRING)) {
				angleBlockIndex = i;
			} else if (prefixes[i].contains(TIMEPOINT_STRING)) {
				timepointBlockIndex = i;
			}
		}
		if (angleBlockIndex < 0) {
			logger.error("Cound not identify the angle string in filenames.\n");
			return null;
		}
		if (timepointBlockIndex < 0) {
			logger.error("Cound not identify the time-point string in filenames.\n");
			return null;
		}

		int nAngles = elements[angleBlockIndex].length;
		logger.log("Found " + nAngles + " views from the filenames.\n");
		if (nAngles == nImps) {
			logger.log("Have " + nImps + " matching ImagePlus, so everything is fine.\n");
		} else {
			logger.error("But have " + nImps + " ImagePlus. Something is wrong.\n");
			imps[0].show();
			return null;
		}

		int nTimepoints = elements[timepointBlockIndex].length;
		logger.log("Found " + nTimepoints + " timepoints from the filenames.\n");

		logger.log("Owing to the fact that the file pattern is " + fp.getPattern() + ", the views will be split according to:\n");
		for (int i = 0; i < imps.length; i++) {
			logger.log(" - view " + i + ": Angle "+elements[angleBlockIndex][i]+"\n");
		}

		/*
		 *  Retrieve registration file
		 */

		// Build the first file names for every view.
		logger.log("Identified the following files as part of the pattern:\n");
		String[][] registrationFiles = new String[nAngles][nTimepoints];

		for (int i = 0; i < nAngles; i++) {

			for (int j = 0; j < nTimepoints; j++) {

				StringBuilder str = new StringBuilder();
				for (int k = 0; k < prefixes.length; k++) {
					str.append(prefixes[k]);
					if (k == angleBlockIndex) {
						str.append(elements[angleBlockIndex][i]);
					} else if (k == timepointBlockIndex) {
						str.append(elements[timepointBlockIndex][j]);
					} else {
						str.append(elements[j][0]);
					}
				}
				str.append(suffix);
				registrationFiles[i][j] = str.toString();
				logger.log(" - "+str.toString());
			}
		}

		// Load transforms
		AffineTransform3D[][] viewsTransforms = new AffineTransform3D[nAngles][nTimepoints];
		logger.log("Locating registration files:\n");
		File registrationFolder = new File(path.getParent(), REGISTRATION_SUBFOLDER);
		int identityTransformIndex = - 1;
		
		for (int i = 0; i < registrationFiles.length; i++) {
			
			for (int j = 0; j < registrationFiles[i].length; j++) {

				File registrationFile = new File(registrationFolder, registrationFiles[i][j] + REGISTRATION_FILE_SUFFIX);
				String str = " - for view " + i + ", registration file is: " + registrationFile.getName();
				AffineTransform3D transform = TransformUtils.getTransformFromFile(registrationFile);
				// Try to identify the identity transform
				if (TransformUtils.isIdentity(transform)) {
					str += " - is the identity transform.\n";
					identityTransformIndex= i;
				} else {
					str += ".\n";
				}
				logger.log(str);
				AffineTransform3D zscaling = TransformUtils.getZScalingFromFile(registrationFile);
				transform.concatenate(zscaling);
				viewsTransforms[i][j] = transform;
				
			}
			
		}

		// Modify transforms to match views between themselves
		/* We will bring back everything to pixel coords in the main imp.
		 * Then we need to take these pixel coords to physical coords. So we need
		 * the calibration transform of the main imp.    */ 
		AffineTransform3D calib1 = TransformUtils.getTransformFromCalibration(imps[identityTransformIndex]);
		/* But the SPIM transforms already have a scaling for the Z-factor. So we do not
		 * need to apply it twice. Since the SPIM transform deals with isotropic pixel 
		 * calibration, we need our physical coords transform be isotropic too. */
		calib1.set(calib1.get(0, 0), 2, 2);

		for (int i = 0; i < viewsTransforms.length; i++) {
			
			for (int j = 0; j < viewsTransforms[i].length; j++) {
				
				AffineTransform3D transform = viewsTransforms[i][j];
				transform .preConcatenate(calib1.inverse()); // To have real physical coordinates in 1st referential
				transform = transform.inverse();
				viewsTransforms[i][j] = transform;
				
			}
		}

		// Store in a map
		Map<ImagePlus, List<AffineTransform3D>> impMap = new HashMap<ImagePlus, List<AffineTransform3D>>(nAngles);
		for (int i = 0; i < nAngles; i++) {
			List<AffineTransform3D> transformList = new ArrayList<AffineTransform3D>(nTimepoints);
			for (int j = 0; j < nTimepoints; j++) {
				transformList.add(viewsTransforms[i][j]);
			}
			impMap.put(imps[i], transformList);
		}
		return impMap;
	}

	static final File askForFile(File file, Frame parent, Logger logger, String string) {

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, string, FileDialog.LOAD);
			if (null != file) {
				dialog.setDirectory(file.getParent());
				dialog.setFile(file.getName());
			}
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Load data aborted.\n");
				return null;
			}
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			JFileChooser fileChooser;
			if (null != file) {
				fileChooser = new JFileChooser(file.getParent());
				fileChooser.setSelectedFile(file);
			} else {
				fileChooser = new JFileChooser();
			}

			int returnVal = fileChooser.showOpenDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Load data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}


	/*
	 * MAIN
	 */

	public static void main(String[] args) {
		ImageJ.main(args);
		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		new MVLauncher().run(rootFolder);
	}
}
