package fiji.plugin.mamut.providers;

import java.util.List;

import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFactory;

public class MamutViewProvider extends ViewProvider
{

	public MamutViewProvider()
	{
		super();
	}

	@Override
	protected void registerViews()
	{
		final TrackSchemeFactory trackSchemeFactory = new TrackSchemeFactory();
		keys.add( trackSchemeFactory.getKey() );
		factories.put( trackSchemeFactory.getKey(), trackSchemeFactory );
	}

	@Override
	public ViewFactory getFactory( final String key )
	{
		ViewFactory val = super.getFactory( key );
		if ( null == val )
		{
			if ( key.equals( MamutViewer.KEY ) )
			{
				if (!(settings instanceof SourceSettings)) {
					throw new IllegalArgumentException("Settings must be an instance of SourceSettings.");
				}
				final SourceSettings ss = (SourceSettings) settings;
				final List< SourceAndConverter< ? >> sources = ss.getSources();
				val = new MamutViewer( MaMuT.DEFAULT_WIDTH, MaMuT.DEFAULT_HEIGHT, sources, ss.nframes, model, selectionModel );
			}
		}

		return val;
	}

}
