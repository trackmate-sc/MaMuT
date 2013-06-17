package fiji.plugin.mamut;

import ij.ImagePlus;

import java.io.File;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import viewer.render.SourceAndConverter;
import fiji.plugin.trackmate.Settings;

public class SourceSettings extends Settings {
	
	private List<SourceAndConverter<?>> sources;


	@Override
	public void setFrom(ImagePlus imp) {
		throw new UnsupportedOperationException("Cannot use ImagePlus with SourceSettings.");
	}
	

	public void setFrom(List<SourceAndConverter<?>> sources, File file, int numTimePoints) {
		this.sources = sources;

		// File info
		this.imageFileName = file.getName();
		this.imageFolder = file.getParent();
		
		// Image size
		SourceAndConverter<?> firstSource = sources.get(0);
		RandomAccessibleInterval<?> firstStack = firstSource.getSpimSource().getSource(0, 0);
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
		this.xend = width-1;
		this.ystart = 0;
		this.yend = height-1;
		this.polygon = null;
	}
	
}
