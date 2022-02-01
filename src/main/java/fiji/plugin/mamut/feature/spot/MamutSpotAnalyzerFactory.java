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
package fiji.plugin.mamut.feature.spot;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for factories that can generate a {@code SpotAnalyzer} configured
 * specifically for MaMuT.
 * <p>
 * We must separate spot analyzers for TrackMate and for MaMuT, for they both
 * operate on two different kind of images. This is a hack, that we try to
 * smooth as much as we can.
 *
 * @author Jean-Yves Tinevez - 2020
 */
public interface MamutSpotAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends SpotAnalyzerFactoryBase< T >
{}
