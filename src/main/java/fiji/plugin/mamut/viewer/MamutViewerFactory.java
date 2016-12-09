package fiji.plugin.mamut.viewer;

import java.util.List;

import javax.swing.ImageIcon;

import bdv.cache.CacheControl;
import bdv.tools.bookmarks.Bookmarks;
import bdv.util.BehaviourTransformEventHandlerPlanar;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import net.imglib2.RandomAccessibleInterval;

public class MamutViewerFactory implements ViewFactory
{
	private static final String INFO_TEXT = "A viewer based on Tobias Pietzsch SPIM Viewer";

	public static final String KEY = "MaMuT Viewer";

	private static final String NAME = KEY;

	private static final int DEFAULT_WIDTH = 800;

	private static final int DEFAULT_HEIGHT = 600;

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel )
	{
		final SourceSettings ss = ( SourceSettings ) settings;
		final List< SourceAndConverter< ? >> sources = ss.getSources();
		final int numTimePoints = ss.nframes;
		final CacheControl cache = ss.getCacheControl();
		final Bookmarks bookmarks = new Bookmarks();
		// Test if we have 2D images.
		boolean is2D = true;
		for ( final SourceAndConverter< ? > sac : sources )
		{
			final Source< ? > source = sac.getSpimSource();
			for ( int t = 0; t < numTimePoints; t++ )
			{
				if ( source.isPresent( t ) )
				{
					final RandomAccessibleInterval< ? > level = source.getSource( t, 0 );
					if ( level.dimension( 2 ) > 1 )
						is2D = false;

					break;
				}
			}
		}

		final ViewerOptions options = ViewerOptions.options();
		if ( is2D )
			options.transformEventHandlerFactory( BehaviourTransformEventHandlerPlanar.factory() );

		return new MamutViewer( DEFAULT_WIDTH, DEFAULT_HEIGHT,
				sources, numTimePoints, cache,
				model, selectionModel,
				options,
				bookmarks );
	}

}
