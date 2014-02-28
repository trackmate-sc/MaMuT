package fiji.plugin.mamut.feature.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * Definition of additional spot features used in MaMuT.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SpotSourceIdAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/** The name of the spot source id feature. */
	public static final String SOURCE_ID = "SOURCE_ID";

	/** Additional spot features used in MaMuT */
	public final static List< String > FEATURES = new ArrayList< String >();

	public final static Map< String, String > FEATURE_NAMES = new HashMap< String, String >();

	public final static Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >();

	public final static Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >();

	public static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >();

	public static final String KEY = "Spot Source ID";

	static
	{
		FEATURES.add( SOURCE_ID );
		FEATURE_NAMES.put( SOURCE_ID, "Source ID" );
		FEATURE_SHORT_NAMES.put( SOURCE_ID, "Source" );
		FEATURE_DIMENSIONS.put( SOURCE_ID, Dimension.NONE );
		IS_INT.put( SOURCE_ID, Boolean.TRUE );
	}

	@Override
	public String getKey()
	{
		return KEY;
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

	protected final SpotAnalyzer< T > dummyAnalyzer = new SpotAnalyzer< T >()
	{
		@Override
		public boolean checkInput()
		{
			return true;
		}

		@Override
		public boolean process()
		{
			return true;
		}

		@Override
		public String getErrorMessage()
		{
			return "";
		}

		@Override
		public long getProcessingTime()
		{
			return 0;
		}
	};

	@Override
	public SpotAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		return dummyAnalyzer;
	}

	@Override
	public String getInfoText()
	{
		return "";
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return "Spot source analyzer";
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return true;
	}
}
