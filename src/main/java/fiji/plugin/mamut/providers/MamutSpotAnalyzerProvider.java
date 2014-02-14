package fiji.plugin.mamut.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;

/**
 * A provider for the spot analyzer factories for MaMuT only.
 */
@SuppressWarnings( "rawtypes" )
public class MamutSpotAnalyzerProvider extends SpotAnalyzerProvider
{
	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList< String >( 1 );
		KEYS.add( SpotSourceIdAnalyzerFactory.KEY );
	}

	private final SpotSourceIdAnalyzerFactory spotSourceIdAnalyzerFactory;

	public MamutSpotAnalyzerProvider()
	{
		super();
		this.spotSourceIdAnalyzerFactory = new SpotSourceIdAnalyzerFactory();
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public SpotAnalyzerFactory getFactory( final String key )
	{
		if ( key.equals( SpotSourceIdAnalyzerFactory.KEY ) )
		{
			return spotSourceIdAnalyzerFactory;
		}
		else
		{
			return super.getFactory( key );
		}

	}
}
