

import fiji.plugin.mamut.MultiViewDisplayer;
import fiji.plugin.mamut.MultiViewTrackerConfigPanel;
import fiji.plugin.mamut.util.TransformUtils;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

		// Set UI toolkit
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		// Launch ImageJ
		ImageJ.main(args);

		// Open several imp
		List<ImagePlus> imps = new ArrayList<ImagePlus>();

		ImagePlus imp1, imp2, imp3;

		if (IJ.isWindows()) {
			imp1 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans.tif");
			imp2 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_XZ.tif");
			imp3 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_YZ.tif");
		} else {
			imp1 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XY.tif");
			imp2 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XZ.tif");
			imp3 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-YZ.tif");
		}

//		imp1 = IJ.openImage("../Celegans-XY.tif");
//		imp2 = IJ.openImage("../Celegans-XZ.tif");
//		imp3 = IJ.openImage("../Celegans-YZ.tif");

		
		imp1.show();
		imps.add(imp1);

		imp2.show();
		imps.add(imp2);

		imp3.show();
		imps.add(imp3);
		
		int nFrames = imp1.getNFrames();

		// Transforms
		Map<ImagePlus, List<AffineTransform3D>> transforms = new HashMap<ImagePlus, List<AffineTransform3D>>();

		AffineTransform3D identity = TransformUtils.getTransformFromCalibration(imp1);
		List<AffineTransform3D> identityList = new ArrayList<AffineTransform3D>(nFrames);
		for (int i = 0; i < nFrames; i++) {
			identityList.add(identity);
		}
		transforms.put(imp1, identityList);

		AffineTransform3D projXZ = TransformUtils.makeXZProjection(imp2);
		List<AffineTransform3D> projXZList = new ArrayList<AffineTransform3D>(nFrames);
		for (int i = 0; i < nFrames; i++) {
			projXZList.add(projXZ);
		}
		transforms.put(imp2, projXZList );

		AffineTransform3D projYZ = TransformUtils.makeYZProjection(imp3);
		List<AffineTransform3D> projYZList = new ArrayList<AffineTransform3D>(nFrames);
		for (int i = 0; i < nFrames; i++) {
			projYZList.add(projYZ);
		}
		transforms.put(imp3, projYZList );

		// Instantiate model
		Settings settings = new Settings(imp1);
		TrackMate_ plugin = new TrackMate_(settings);
		plugin.initModules();
		TrackMateModel model = plugin.getModel();
		
		// Initialize viewer
		MultiViewDisplayer viewer = new MultiViewDisplayer(imps, transforms, model);
		viewer.render();
		
		// Control panel
		MultiViewTrackerConfigPanel mvFrame = new MultiViewTrackerConfigPanel(model, viewer);
		mvFrame.setVisible(true);

	}

}
