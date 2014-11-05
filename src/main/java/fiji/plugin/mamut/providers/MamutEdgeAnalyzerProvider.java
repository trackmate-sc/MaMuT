package fiji.plugin.mamut.providers;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

public class MamutEdgeAnalyzerProvider extends EdgeAnalyzerProvider
{

	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList< String >( 3 );
		KEYS.add( EdgeTargetAnalyzer.KEY );
		KEYS.add( EdgeVelocityAnalyzer.KEY );
		KEYS.add( ManualEdgeColorAnalyzer.KEY );
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public EdgeAnalyzer getFactory( final String key )
	{
		return super.getFactory( key );
	}

}
