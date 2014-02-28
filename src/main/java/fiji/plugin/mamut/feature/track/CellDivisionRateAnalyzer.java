package fiji.plugin.mamut.feature.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.multithreading.SimpleMultiThreading;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;

@Plugin( type = TrackAnalyzer.class, priority = 1d )
public class CellDivisionRateAnalyzer implements TrackAnalyzer
{

	public static final String KEY = "CELL_DIVISION_RATE_ANALYZER";

	private static final List< String > FEATURES = new ArrayList< String >( 1 );

	public static final String FEATURE = "CELL_DIVISION_RATE";

	private static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 1 );

	private static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 1 );

	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );

	private static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >();

	private static final String INFO_TEXT = "<html>This analyzers measures the number of cell divisions per unit of time, from track start to track stop.</html>";

	private static final String NAME = "Cell division rate analyzer";

	static
	{
		FEATURES.add( FEATURE );
		FEATURE_SHORT_NAMES.put( FEATURE, "Div. rate" );
		FEATURE_NAMES.put( FEATURE, "Cell division rate" );
		FEATURE_DIMENSIONS.put( FEATURE, Dimension.RATE );
		IS_INT.put( FEATURE, Boolean.FALSE );
	}

	private long processingTime;

	private int numThreads;

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;

	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{
		if ( trackIDs.isEmpty() ) { return; }

		final FeatureModel fm = model.getFeatureModel();

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< Integer >( trackIDs.size(), false, trackIDs );

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "CellDivisionRateAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{
						final double trackStart = fm.getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_START ).doubleValue();
						final double trackStop = fm.getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_STOP ).doubleValue();
						final double nSplits = fm.getTrackFeature( trackID, TrackBranchingAnalyzer.NUMBER_SPLITS ).doubleValue();
						final double rate = nSplits / ( trackStop - trackStart );
						fm.putTrackFeature( trackID, FEATURE, Double.valueOf( rate ) );
					}
				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

}
