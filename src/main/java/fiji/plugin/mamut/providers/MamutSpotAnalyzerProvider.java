package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;

/**
 * A provider for the spot analyzer factories containing for default TrackMate
 * and MaMuT factories.
 */
public class MamutSpotAnalyzerProvider extends SpotAnalyzerProvider
{
	@SuppressWarnings("rawtypes")
	public MamutSpotAnalyzerProvider()
	{
		super();
		registerAnalyzer(SpotSourceIdAnalyzerFactory.KEY, new SpotSourceIdAnalyzerFactory());

	}
}
