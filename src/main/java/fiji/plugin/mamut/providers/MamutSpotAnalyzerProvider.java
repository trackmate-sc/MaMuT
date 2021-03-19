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
package fiji.plugin.mamut.providers;

import fiji.plugin.mamut.feature.spot.CellDivisionTimeAnalyzerSpotFactory;
import fiji.plugin.mamut.feature.spot.SpotSourceIdAnalyzerFactory;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A provider for the spot analyzer factories for MaMuT only.
 */
@SuppressWarnings( "rawtypes" )
public class MamutSpotAnalyzerProvider extends SpotAnalyzerProvider
{
	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList<>( 3 );
		KEYS.add( SpotSourceIdAnalyzerFactory.KEY );
		KEYS.add( CellDivisionTimeAnalyzerSpotFactory.KEY );
		KEYS.add( ManualSpotColorAnalyzerFactory.KEY );
	}

	private final SpotSourceIdAnalyzerFactory spotSourceIdAnalyzerFactory;

	private final CellDivisionTimeAnalyzerSpotFactory cellDivisionTimeAnalyzerSpotFactory;

	public MamutSpotAnalyzerProvider()
	{
		super();
		this.spotSourceIdAnalyzerFactory = new SpotSourceIdAnalyzerFactory();
		this.cellDivisionTimeAnalyzerSpotFactory = new CellDivisionTimeAnalyzerSpotFactory();
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public SpotAnalyzerFactory getFactory( final String key )
	{
		if ( key.equals( SpotSourceIdAnalyzerFactory.KEY ) )
		{
			return spotSourceIdAnalyzerFactory;
		}
		else if ( key.equals( CellDivisionTimeAnalyzerSpotFactory.KEY ) )
		{
			return cellDivisionTimeAnalyzerSpotFactory;
		}
		else
		{
			return super.getFactory( key );
		}
	}
}
