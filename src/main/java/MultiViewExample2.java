import fiji.plugin.mamut.MultiViewDisplayer;
import fiji.plugin.mamut.util.TransformUtils;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loci.formats.FormatException;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import loci.plugins.in.ImporterOptions;
import net.imglib2.exception.ImgLibException;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample2 {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgLibException, IOException, FormatException {

		ImageJ.main(args);

		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		
//		String tranformation1 = "registration/spim1_TL21_Angle90.lsm.registration";
//		String image1 = "spim1_TL21_Angle90.lsm";
		String image1 = "spim2_TL21_Angle90.tif";
		String tranformation1 = "registration/" + image1 + ".registration";

//		String tranformation2 = "registration/spim1_TL21_Angle135.lsm.registration";
//		String image2 = "spim1_TL21_Angle135.lsm";
		String image2 = "spim2_TL21_Angle135.tif";
		String tranformation2 = "registration/" + image2 + ".registration";

		AffineTransform3D transform1 = TransformUtils.getTransformFromFile(new File(rootFolder, tranformation1));
		AffineTransform3D zscaling1 = TransformUtils.getZScalingFromFile(new File(rootFolder, tranformation1));
		transform1.concatenate(zscaling1);

		AffineTransform3D transform2 = TransformUtils.getTransformFromFile(new File(rootFolder, tranformation2));
		AffineTransform3D zscaling2 = TransformUtils.getZScalingFromFile(new File(rootFolder, tranformation2));
		transform2.concatenate(zscaling2);

		ImagePlus imp1 = useImporter(new File(rootFolder, image1));
		ImagePlus imp2 = useImporter(new File(rootFolder, image2));
		
		/* We will bring back everything to pixel coords in the main imp.
		 * Then we need to take these pixel coords to physical coords. So we need
		 * the calibration transform of the main imp.    */ 
		AffineTransform3D calib1 = TransformUtils.getTransformFromCalibration(imp1);
		/* But the SPIM transforms already have a scaling for the Z-factor. So we do not
		 * need to apply it twice. Since the SPIM transform deals with isotropic pixel 
		 * calibration, we need our physical coords transform be isotropic too. */
		calib1.set(calib1.get(0, 0), 2, 2);
		
		transform1.preConcatenate(calib1.inverse()); // To have real physical coordinates in 1st referential
		transform2.preConcatenate(calib1.inverse()); // To have real physical coordinates in 1st referential
		
		transform1 = transform1.inverse();
		transform2 = transform2.inverse();
		
		Map<ImagePlus, List<AffineTransform3D>> transforms = new HashMap<ImagePlus, List<AffineTransform3D>>();
		List<AffineTransform3D> transform1List = new ArrayList<AffineTransform3D>(1);
		transform1List.add(transform1);
		List<AffineTransform3D> transform2List = new ArrayList<AffineTransform3D>(1);
		transform2List.add(transform2);
		transforms.put(imp1, transform1List );
		transforms.put(imp2, transform2List);

		List<ImagePlus> imps = new ArrayList<ImagePlus>();
		imps.add(imp1);
		imps.add(imp2);

		// Instantiate model
		Settings settings = new Settings(imp1);
		TrackMate_ tm = new TrackMate_(settings);
		TrackMateModel model = tm.getModel();

		// Initialize viewer
		MultiViewDisplayer viewer = new MultiViewDisplayer(imps, transforms, model);
		viewer.render();
		
	}

	
	public static ImagePlus useImporter(File path) throws IOException, FormatException {
		LociImporter plugin = new LociImporter();
		Importer importer = new Importer(plugin);
		
		
		ImporterOptions  options = importer.parseOptions("");
		
		
		options.setId(path.getAbsolutePath());
		options.setGroupFiles(false);
		options.setVirtual(false);
		options.setSplitChannels(true);
		options.setQuiet(true);
//
//		Location file = new Location(path);
//		FilePattern fp = new FilePattern(file);
//		System.out.println(fp.getPattern());
//		options.setId(path); // fp.getPattern());
//		System.out.println(options.getId());
//
//		options.setSpecifyRanges(true);
//		
//		options.setCBegin(0, 15);
//		options.setCEnd(0, 19);
//		options.setCStep(0, 4);
//
//		options.setTBegin(0, 0);
//		options.setTEnd(0, 4);
//		options.setTStep(0, 1);
//		
		
		ImportProcess process = new ImportProcess(options);
		ImagePlusReader reader = new ImagePlusReader(process);
		DisplayHandler displayHandler = new DisplayHandler(process);

		process.execute();
		
		
		ImagePlus[] imps = importer.readPixels(reader, options, displayHandler);
		importer.finish(process);
		return imps[0];

	}
}
