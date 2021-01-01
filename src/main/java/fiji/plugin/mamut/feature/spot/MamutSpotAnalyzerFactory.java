package fiji.plugin.mamut.feature.spot;

import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for factories that can generate a {@link SpotAnalyzer} configured
 * specifically for MaMuT.
 * <p>
 * We must separate spot analyzers for TrackMate and for MaMuT, for they both
 * operate on two different kind of images. This is a hack, that we try to
 * smooth as much as we can.
 *
 * @author Jean-Yves Tinevez - 2020
 */
public interface MamutSpotAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends SpotAnalyzerFactoryBase< T >
{}
