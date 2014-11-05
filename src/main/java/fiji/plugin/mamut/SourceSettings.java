package fiji.plugin.mamut;

import bdv.img.cache.Cache;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.trackmate.Settings;
import ij.ImagePlus;

import java.io.File;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;

public class SourceSettings extends Settings {

	private List<SourceAndConverter<?>> sources;

	private Cache cache;

	@Override
	public void setFrom(final ImagePlus imp) {
		throw new UnsupportedOperationException("Cannot use ImagePlus with SourceSettings.");
	}

	public void setFrom( final List< SourceAndConverter< ? >> sources, final File file, final int numTimePoints, final Cache cache )
	{
		this.sources = sources;
		this.cache = cache;

		// File info
		this.imageFileName = file.getName();
		this.imageFolder = file.getParent();

		// Image size
		final SourceAndConverter<?> firstSource = sources.get(0);
		final RandomAccessibleInterval<?> firstStack = firstSource.getSpimSource().getSource(0, 0);
		this.width = (int) firstStack.dimension(0);
		this.height = (int) firstStack.dimension(1);
		this.nslices = (int) firstStack.dimension(2);
		this.nframes = numTimePoints;
		this.dx = 1f;
		this.dy = 1f;
		this.dz = 1f;
		this.dt = 1f;

		// Crop cube
		this.xstart = 0;
		this.xend = width - 1;
		this.ystart = 0;
		this.yend = height - 1;
		this.polygon = null;
	}

	public List<SourceAndConverter<?>> getSources() {
		return sources;
	}

	public Cache getCache()
	{
		return cache;
	}
}