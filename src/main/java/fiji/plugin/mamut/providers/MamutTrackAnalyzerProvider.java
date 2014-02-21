package fiji.plugin.mamut.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.mamut.feature.track.CellDivisionRateAnalyzer;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

/**
 * A provider for the track analyzer factories for MaMuT only.
 */
public class MamutTrackAnalyzerProvider extends TrackAnalyzerProvider
{
	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList< String >( 4 );
		KEYS.add( TrackIndexAnalyzer.KEY );
		KEYS.add( TrackDurationAnalyzer.KEY );
		KEYS.add( TrackBranchingAnalyzer.KEY );
		KEYS.add( CellDivisionRateAnalyzer.KEY );

	}

	public MamutTrackAnalyzerProvider()
	{
		super();
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public TrackAnalyzer getFactory( final String key )
	{
		return super.getFactory( key );
	}
}
