package fiji.plugin.mamut.viewer;

import bdv.img.cache.Cache;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;

import java.util.List;

import javax.swing.ImageIcon;

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
		final Cache cache = ss.getCache();
		return new MamutViewer( DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, numTimePoints, cache, model, selectionModel );
	}

}
