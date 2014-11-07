package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.feature.track.CellDivisionTimeAnalyzer;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

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
		KEYS.add( CellDivisionTimeAnalyzer.KEY );

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
