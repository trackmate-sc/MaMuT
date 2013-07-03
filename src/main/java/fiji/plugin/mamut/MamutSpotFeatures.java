package fiji.plugin.mamut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;

/**
 * Definition of additional spot features used in MaMuT.
 *  
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MamutSpotFeatures {

	/** The name of the spot source id feature. */
	public static final String SOURCE_ID = "SOURCE_ID";

	/** Additional spot features used in MaMuT */
	public final static Collection<String> FEATURES = new ArrayList<String>();
	public final static Map<String, String> FEATURE_NAMES = new HashMap<String, String>();
	public final static Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>();
	public final static Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>();

	static {
		FEATURES.add(SOURCE_ID);
		FEATURE_NAMES.put(SOURCE_ID, "Source ID");
		FEATURE_SHORT_NAMES.put(SOURCE_ID, "Source");
		FEATURE_DIMENSIONS.put(SOURCE_ID, Dimension.NONE);
	}

	public static void declareFeatures( FeatureModel model )
	{
		model.declareSpotFeatures(FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS);
	}
}
