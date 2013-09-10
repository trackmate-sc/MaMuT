package fiji.plugin.mamut.providers;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;

/**
 * A provider for the spot analyzer factories containing for default TrackMate
 * and MaMuT factories.
 */
public class MamutSpotAnalyzerProvider extends SpotAnalyzerProvider
{
	public MamutSpotAnalyzerProvider( final Model model )
	{
		super( model );
		registerSpotFeatureAnalyzer( SpotSourceIdAnalyzerFactory.KEY, new FactoryCreator()
		{
			@Override
			public < T extends RealType< T > & NativeType< T >> SpotAnalyzerFactory< T > create( final ImgPlus< T > img )
			{
				return new SpotSourceIdAnalyzerFactory< T >();
			}
		} );
	}
}
