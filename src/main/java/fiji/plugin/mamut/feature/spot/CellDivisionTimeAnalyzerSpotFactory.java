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
package fiji.plugin.mamut.feature.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A dummy {@link SpotAnalyzerFactory}, made to simply register the spot
 * features that will be assigned by
 * {@link fiji.plugin.mamut.feature.track.CellDivisionTimeAnalyzer}.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Mar 5, 2014
 *
 * @param <T>
 *            the type of the pixels in the image. Must extends {@link RealType}
 *            and {@link NativeType}.
 */
@Plugin( type = SpotAnalyzerFactory.class )
public class CellDivisionTimeAnalyzerSpotFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	public static final String CELL_DIVISION_TIME = "CELL_DIVISION_TIME";

	private static final List< String > FEATURES = new ArrayList<>( 1 );

	private static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 1 );

	private static final Map< String, String > FEATURE_NAMES = new HashMap<>( 1 );

	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 1 );

	private static final Map< String, Boolean > IS_INT = new HashMap<>( 1 );

	public static final String KEY = "CELL_DIVISION_TIME_ON_SPOTS";

	private static final String NAME = "Cell division time on spots";

	static
	{
		FEATURES.add( CELL_DIVISION_TIME );
		FEATURE_SHORT_NAMES.put( CELL_DIVISION_TIME, "Cell div. time" );
		FEATURE_NAMES.put( CELL_DIVISION_TIME, "Cell division time" );
		FEATURE_DIMENSIONS.put( CELL_DIVISION_TIME, Dimension.TIME );
		IS_INT.put( CELL_DIVISION_TIME, Boolean.FALSE );
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

	@Override
	public String getInfoText()
	{
		return "";
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
	public SpotAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		return new SpotAnalyzer< T >()
		{

			@Override
			public boolean checkInput()
			{
				return true;
			}

			@Override
			public boolean process()
			{
				return true;
			}

			@Override
			public String getErrorMessage()
			{
				return null;
			}

			@Override
			public long getProcessingTime()
			{
				return 0;
			}
		};
	}

}
