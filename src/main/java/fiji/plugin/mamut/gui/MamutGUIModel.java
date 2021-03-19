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
package fiji.plugin.mamut.gui;

import fiji.plugin.trackmate.gui.TrackMateGUIModel;

public class MamutGUIModel extends TrackMateGUIModel {

	/**
	 * How close must be the new spot found to be accepted, in radius units.
	 */
	public double distanceTolerance = 1.5;

	/**
	 * The fraction of the initial quality above which we keep new spots. The
	 * highest, the more intolerant.
	 */
	public double qualityThreshold = 0d;

	/**
	 * In semi auto-tracking, how many frames to process at max. Make it 0 or
	 * negative to remove this limit.
	 */
	public int maxNFrames = 5;

	/**
	 * By how much we move in time when calling the step in time action.
	 */
	public int timeStep = 5;


}
