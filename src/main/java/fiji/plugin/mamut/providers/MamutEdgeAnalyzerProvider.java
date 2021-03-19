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

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

public class MamutEdgeAnalyzerProvider extends EdgeAnalyzerProvider
{

	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList<>( 3 );
		KEYS.add( EdgeTargetAnalyzer.KEY );
		KEYS.add( EdgeVelocityAnalyzer.KEY );
		KEYS.add( ManualEdgeColorAnalyzer.KEY );
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public EdgeAnalyzer getFactory( final String key )
	{
		return super.getFactory( key );
	}

}
