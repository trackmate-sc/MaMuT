package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.viewer.MamutViewerFactory;
import fiji.plugin.trackmate.providers.ViewProvider;
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

		final MamutViewerFactory mamutViewerFactory = new MamutViewerFactory();
		keys.add( mamutViewerFactory.getKey() );
		factories.put( mamutViewerFactory.getKey(), mamutViewerFactory );
	}
}
