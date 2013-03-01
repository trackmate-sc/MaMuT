package fiji.plugin.mamut.util;

import ij.IJ;
import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;

public class TransformUtils {

	public static final boolean isIdentity(final AffineTransform3D transform) {
		for (int i = 0; i < 3; i++) {
			if (transform.get(i, i) != 1) return false;
		}
		return true;
	}

	public static final AffineTransform3D makeXZProjection(final ImagePlus imp) {
		AffineTransform3D transform = new AffineTransform3D();
		final double[] calibration = Util.getArrayFromValue(1d, 3);
		calibration[0] = imp.getCalibration().pixelWidth;
		calibration[1] = imp.getCalibration().pixelHeight;
		if (imp.getNSlices() > 1)
			calibration[2] = imp.getCalibration().pixelDepth;
		transform.set(
				1/calibration[0], 	0, 			0, 					0,
				0, 					0,			-1/calibration[1], 	imp.getHeight(), 
				0, 					1/calibration[2], 0, 			0);
		return transform;
	}

	public static final AffineTransform3D makeYZProjection(final ImagePlus imp) {
		AffineTransform3D transform = new AffineTransform3D();
		final double[] calibration = Util.getArrayFromValue(1d, 3);
		calibration[0] = imp.getCalibration().pixelWidth;
		calibration[1] = imp.getCalibration().pixelHeight;
		if (imp.getNSlices() > 1)
			calibration[2] = imp.getCalibration().pixelDepth;
		transform.set(
				0,					0,			1/calibration[0], 	0, 			
				0, 					1/calibration[1], 	0,			0, 
				-1/calibration[2], 	0, 					0, 			imp.getNSlices());
		return transform;
	}



	public static AffineTransform3D getZScalingFromFile(final File file )	{
		final AffineTransform3D model = new AffineTransform3D();

		try  {
			final BufferedReader in = TextFileAccess.openFileRead( file );
			double z = 1;

			// the default if nothing is written
			String savedModel = "AffineModel3D";

			while ( in.ready() ) {
				String entry = in.readLine().trim();

				if (entry.startsWith("z-scaling:")) {
					z = Double.parseDouble(entry.substring(11, entry.length()));
				}
			}

			in.close();

			if ( !savedModel.equals("AffineModel3D") )
				System.out.println( "Warning: Loading a '" + savedModel + "' as AffineModel3D!" );

			model.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, z, 0);

		}  catch (IOException e) {
			IJ.log( "Cannot find file: " + file.getAbsolutePath() + ": " + e );
			e.printStackTrace();
			return null;
		}

		return model;
	}	

	public static final AffineTransform3D getTransformFromCalibration(final ImagePlus imp) {
		AffineTransform3D transform = new AffineTransform3D();
		final double[] calibration = Util.getArrayFromValue(1d, 3);
		calibration[0] = imp.getCalibration().pixelWidth;
		calibration[1] = imp.getCalibration().pixelHeight;
		if (imp.getNSlices() > 1)
			calibration[2] = imp.getCalibration().pixelDepth;
		transform.set(
				1/calibration[0], 	0, 			0, 			0,
				0, 			1/calibration[1], 	0, 			0, 
				0, 			0, 			1/calibration[2], 0);
		return transform;
	}

	public static final AffineTransform3D getTransformFromFile(final File file ) throws NumberFormatException, IOException	{
		final AffineTransform3D model = new AffineTransform3D();

		final BufferedReader in = TextFileAccess.openFileRead( file );

		// get 12 entry float array
		final double m[] = new double[ 12 ];

		// the default if nothing is written
		String savedModel = "AffineModel3D";

		while ( in.ready() ) {
			String entry = in.readLine().trim();

			if (entry.startsWith("m00:"))
				m[ 0 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m01:"))
				m[ 1 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m02:"))
				m[ 2 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m03:"))
				m[ 3 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m10:"))
				m[ 4 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m11:"))
				m[ 5 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m12:"))
				m[ 6 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m13:"))
				m[ 7 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m20:"))
				m[ 8 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m21:"))
				m[ 9 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m22:"))
				m[ 10 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("m23:"))
				m[ 11 ] = Double.parseDouble(entry.substring(5, entry.length()));
			else if (entry.startsWith("model:"))
				savedModel = entry.substring(7, entry.length()).trim();
		}

		in.close();

		if ( !savedModel.equals("AffineModel3D") )
			System.out.println( "Warning: Loading a '" + savedModel + "' as AffineModel3D!" );

		model.set( m[ 0 ], m[ 1 ], m[ 2 ], m[ 3 ], m[ 4 ], m[ 5 ], m[ 6 ], m[ 7 ], m[ 8 ], m[ 9 ], m[ 10 ], m[ 11 ] );

		return model;
	}	
}
