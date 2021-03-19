/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2021 MaMuT development team.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.mamut;

import java.io.File;
import java.util.List;

import bdv.cache.CacheControl;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.trackmate.Settings;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;

public class SourceSettings extends Settings {

	private List<SourceAndConverter<?>> sources;

	private CacheControl cache;

	@Override
	public void setFrom(final ImagePlus imp) {
		throw new UnsupportedOperationException("Cannot use ImagePlus with SourceSettings.");
	}

	public void setFrom( final List< SourceAndConverter< ? >> sources, final File file, final int numTimePoints, final CacheControl cache )
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
		this.roi = null;
	}

	public List<SourceAndConverter<?>> getSources() {
		return sources;
	}

	public CacheControl getCacheControl()
	{
		return cache;
	}
}
