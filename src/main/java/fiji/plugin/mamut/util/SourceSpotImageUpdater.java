/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2022 MaMuT development team.
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
package fiji.plugin.mamut.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Max;
import net.imglib2.algorithm.stats.Min;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class SourceSpotImageUpdater< T extends RealType< T > > extends SpotImageUpdater
{

	/** How much extra we capture around spot radius. */
	private static final double RADIUS_FACTOR = 1.1;

	private final List< SourceAndConverter< T > > sources;
	private final ThreadGroup threadGroup;

	@SuppressWarnings( "unchecked" )
	public SourceSpotImageUpdater( final SourceSettings settings )
	{
		super(settings);
		@SuppressWarnings( "rawtypes" )
		final List s = settings.getSources();
		this.sources = s;
		this.threadGroup = new ThreadGroup("Source spot image grabber threads");
	}

	/**
	 * Returns the image string of the given spot, based on the raw images
	 * contained in the given model. For performance, the image at target frame
	 * is stored for subsequent calls of this method. So it is a good idea to
	 * group calls to this method for spots that belong to the same frame.
	 */
	@Override
	public String getImageString( final Spot spot, final double radiusFactor )
	{
		final StringBuffer str = new StringBuffer();

		final Thread th = new Thread(threadGroup, "Spot Image grabber for " + spot) {

			@Override
			public void run() {

				// Retrieve frame
				final int frame = spot.getFeature(Spot.FRAME).intValue();
				// Retrieve source ID
				final Double si = spot.getFeature(SpotSourceIdAnalyzerFactory.SOURCE_ID);
				if (null == si) {
					return;
				}

				final int sourceID = si.intValue();
				final Source<T> source = sources.get(sourceID).getSpimSource();
				final RandomAccessibleInterval<T> img = source.getSource(frame, 0);

				// Get spot coords
				final AffineTransform3D sourceToGlobal = new AffineTransform3D();
				source.getSourceTransform( frame, 0, sourceToGlobal );
				final Point roundedSourcePos = new Point(3);
				sourceToGlobal.applyInverse(new Round<>(roundedSourcePos), spot);
				final long x = roundedSourcePos.getLongPosition(0);
				final long y = roundedSourcePos.getLongPosition(1);
				final long z = Math.max(img.min(2), Math.min(img.max(2), roundedSourcePos.getLongPosition(2)));
				final long r = ( long ) Math.ceil( radiusFactor * RADIUS_FACTOR * spot.getFeature( Spot.RADIUS ).doubleValue() / Affine3DHelpers.extractScale( sourceToGlobal, 0 ) );

				// Extract central slice
				final IntervalView<T> slice = Views.hyperSlice(img, 2, z);

				// Crop
				final Interval cropInterval = Intervals.intersect(slice, Intervals.createMinMax(x - r, y - r, x + r, y + r));

				final BufferedImage image;
				if (isEmpty(cropInterval))
					image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
				else {
					final IntervalView<T> crop = Views.zeroMin(Views.interval(slice, cropInterval));
					final int width = (int) crop.dimension(0);
					final int height = (int) crop.dimension(1);
					image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
					final byte[] imgData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
					final ArrayImg< UnsignedByteType, ByteArray > target = ArrayImgs.unsignedBytes( imgData, width, height );

					final double minValue = Min.findMin(Views.iterable(crop)).get().getRealDouble();
					final double maxValue = Max.findMax(Views.iterable(crop)).get().getRealDouble();
					final RealUnsignedByteConverter< T > converter = new RealUnsignedByteConverter<>( minValue, maxValue );

					new IterableIntervalProjector2D<>( 0, 1, crop, target, converter ).map();
				}

				// Convert to string
				final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				String baf;
				try {
					ImageIO.write(image, "png", bos);
					baf = Base64.encodeBytes(bos.toByteArray());
				} catch (final IOException e) {
					e.printStackTrace();
					baf = "";
				}
				str.append(baf);
			}

		};

		th.start();
		try {
			th.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return str.toString();
	}

	private static final boolean isEmpty(final Interval interval) {
		final int n = interval.numDimensions();
		for (int d = 0; d < n; ++d)
			if (interval.min(d) > interval.max(d))
				return true;
		return false;
	}

}
