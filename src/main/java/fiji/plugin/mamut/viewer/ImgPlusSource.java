/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2023 MaMuT development team.
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
package fiji.plugin.mamut.viewer;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import fiji.plugin.mamut.util.TransformUtils;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class ImgPlusSource< T extends NumericType< T > > implements Source< T >
{

	private final ImgPlus< T > img;

	public ImgPlusSource( final ImgPlus< T > img )
	{
		this.img = img;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return t >= 0 && t < img.dimension( 3 );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return Views.hyperSlice( img, 3, t );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		InterpolatorFactory< T, RandomAccessible< T >> factory;
		switch ( method )
		{
		default:
		case NEARESTNEIGHBOR:
			factory = new NearestNeighborInterpolatorFactory<>();
			break;
		case NLINEAR:
			factory = new NLinearInterpolatorFactory<>();
			break;
		}
		return Views.interpolate( Views.extendZero( getSource( t, level ) ), factory );
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( TransformUtils.getTransformFromCalibration( img ).inverse() );
	}

	@Override
	public T getType()
	{
		return img.firstElement().copy();
	}

	@Override
	public String getName()
	{
		return img.getName();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

	@Override
	public VoxelDimensions getVoxelDimensions() {
		return null;
	}

}
