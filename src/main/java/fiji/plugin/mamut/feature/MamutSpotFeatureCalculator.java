package fiji.plugin.mamut.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.img.ImgView;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of spots, through {@link SpotAnalyzer}s, tuned for Mamut.
 * 
 * @author Jean-Yves Tinevez - 2020
 * 
 */
public class MamutSpotFeatureCalculator extends MultiThreadedBenchmarkAlgorithm
{

	private static final String BASE_ERROR_MSG = "[MamutSpotFeatureCalculator] ";

	private final SourceSettings settings;

	private final Model model;

	public MamutSpotFeatureCalculator( final Model model, final SourceSettings settings )
	{
		this.settings = settings;
		this.model = model;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == model )
		{
			errorMessage = BASE_ERROR_MSG + "Model object is null.";
			return false;
		}
		if ( null == settings )
		{
			errorMessage = BASE_ERROR_MSG + "Settings object is null.";
			return false;
		}
		return true;
	}

	/**
	 * Calculates the spot features configured in the {@link Settings} for all
	 * the spots of this model,
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Since a {@link SpotAnalyzer} can compute more than a feature at
	 * once, spots might received more data than required.
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		// Declare what you do.
		for ( final SpotAnalyzerFactoryBase< ? > factory : settings.getSpotAnalyzerFactories() )
		{
			final Collection< String > features = factory.getFeatures();
			final Map< String, String > featureNames = factory.getFeatureNames();
			final Map< String, String > featureShortNames = factory.getFeatureShortNames();
			final Map< String, Dimension > featureDimensions = factory.getFeatureDimensions();
			final Map< String, Boolean > isIntFeature = factory.getIsIntFeature();
			model.getFeatureModel().declareSpotFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		}

		// Do it.
		@SuppressWarnings( "rawtypes" )
		final List saf = settings.getSpotAnalyzerFactories();
		computeSpotFeaturesAgent( model.getSpots(), saf, true );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Calculates all the spot features configured in the {@link Settings}
	 * object for the specified spot collection. Features are calculated for
	 * each spot, using their location, and the raw image.
	 */
	@SuppressWarnings( "unchecked" )
	public void computeSpotFeatures( final SpotCollection toCompute, final boolean doLogIt )
	{
		@SuppressWarnings( "rawtypes" )
		final List saf = settings.getSpotAnalyzerFactories();
		computeSpotFeaturesAgent( toCompute, saf, doLogIt );
	}

	/**
	 * The method in charge of computing spot features with the given
	 * {@link SpotAnalyzer}s, for the given {@link SpotCollection}.
	 * 
	 * @param toCompute
	 */
	private < T extends RealType< T > & NativeType< T > > void computeSpotFeaturesAgent( final SpotCollection toCompute, final List< SpotAnalyzerFactoryBase< T > > analyzerFactories, final boolean doLogIt )
	{
		final Logger logger = doLogIt ? model.getLogger() : Logger.VOID_LOGGER;
		logger.setStatus( "Calculating " + toCompute.getNSpots( false ) + " spots features..." );
		logger.setProgress( 0 );

		// Do it.
		final List< Integer > frameSet = new ArrayList<>( toCompute.keySet() );
		final int numFrames = frameSet.size();

		final AtomicInteger progress = new AtomicInteger( 0 );

		@SuppressWarnings( "rawtypes" )
		final List s = settings.getSources();
		@SuppressWarnings( "unchecked" )
		final List< SourceAndConverter< T > > sources = s;

		// We always operate on the lowest resolution level.
		final int level = 0;

		/*
		 * Careful with multithreading: We do not want to have one frame per
		 * thread, like this is the case for TrackMate, because it would force
		 * loading several time-points at once.
		 */

		for ( int iframe = 0; iframe < numFrames; iframe++ )
		{
			final int frame = frameSet.get( iframe ).intValue();

			// Spots to process in this frame.
			final Iterable< Spot > spots = toCompute.iterable( frame, false );

			// Loop over each setup in this frame.
			for ( int channel = 0; channel < sources.size(); channel++ )
			{
				// The image for this setup, this frame (so 3D at max).
				final SourceAndConverter< T > sourceAnConverter = sources.get( channel );
				final Source< T > source = sourceAnConverter.getSpimSource();
				final RandomAccessibleInterval< T > rai = source.getSource( frame, level );
				final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
				final double[] cal = new double[] {
						source.getVoxelDimensions().dimension( 0 ),
						source.getVoxelDimensions().dimension( 1 ),
						source.getVoxelDimensions().dimension( 2 )
				};
				final String[] units = new String[] {
						source.getVoxelDimensions().unit(),
						source.getVoxelDimensions().unit(),
						source.getVoxelDimensions().unit() };
				final ImgPlus< T > imgPlus = new ImgPlus<>(
						ImgView.wrap( rai ),
						source.getName() + "C" + channel + "_T" + frame,
						axes,
						cal,
						units );

				// The transform for this setup, this frame.
				final AffineTransform3D sourceToGlobal = new AffineTransform3D();
				source.getSourceTransform( frame, level, sourceToGlobal );

				// Transform spot coordinates.
				final List< Spot > transformedSpots = new ArrayList<>();
				for ( final Spot spot : spots )
					transformedSpots.add( TransformedSpot.wrap( spot, sourceToGlobal, cal ) );

				// Process all analyzers.
				for ( final SpotAnalyzerFactoryBase< T > factory : analyzerFactories )
				{
					final SpotAnalyzer< T > analyzer = factory.getAnalyzer( imgPlus, frame, channel );
					// Multithread if we can.
					if ( analyzer instanceof MultiThreaded )
						( ( MultiThreaded ) analyzer ).setNumThreads( numThreads );

					analyzer.process( transformedSpots );
				}
				logger.setProgress( progress.incrementAndGet() / ( float ) numFrames );
			}
		}
		logger.setProgress( 1 );
		logger.setStatus( "" );
	}
}
