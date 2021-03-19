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

import fiji.plugin.mamut.feature.track.CellDivisionTimeAnalyzer;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A provider for the track analyzer factories for MaMuT only.
 */
public class MamutTrackAnalyzerProvider extends TrackAnalyzerProvider
{
	private static final List< String > KEYS;

	static
	{
		KEYS = new ArrayList<>( 4 );
		KEYS.add( TrackIndexAnalyzer.KEY );
		KEYS.add( TrackDurationAnalyzer.KEY );
		KEYS.add( TrackBranchingAnalyzer.KEY );
		KEYS.add( CellDivisionTimeAnalyzer.KEY );

	}

	public MamutTrackAnalyzerProvider()
	{
		super();
	}

	@Override
	public List< String > getKeys()
	{
		return KEYS;
	}

	@Override
	public TrackAnalyzer getFactory( final String key )
	{
		return super.getFactory( key );
	}
}
