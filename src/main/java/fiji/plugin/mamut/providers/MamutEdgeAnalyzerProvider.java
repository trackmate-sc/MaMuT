package fiji.plugin.mamut.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;

public class MamutEdgeAnalyzerProvider extends EdgeAnalyzerProvider
{

	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList< String >( 2 );
		KEYS.add( EdgeTargetAnalyzer.KEY );
		KEYS.add( EdgeVelocityAnalyzer.KEY );
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
