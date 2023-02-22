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
package fiji.plugin.mamut.action;

import java.awt.Frame;
import java.util.Arrays;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.SourceSettings;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.gui.GenericDialog;

@Plugin( type = MamutActionFactory.class )
public class MamutExtractStackActionFactory implements MamutActionFactory
{

	private static int targetSourceIndex = 0;

	private static double diameterFactor = 1.5d;

	private static int dimChoice = 0;

	@Override
	public String getInfoText()
	{
		return MamutExtractTrackStackAction.INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return Icons.MAGNIFIER_ICON;
	}

	@Override
	public String getKey()
	{
		return MamutExtractTrackStackAction.KEY;
	}

	@Override
	public String getName()
	{
		return MamutExtractTrackStackAction.NAME;
	}

	@Override
	public TrackMateAction create( final MaMuT mamut )
	{

		/*
		 * Create dialog
		 */

		final GenericDialog dialog = new GenericDialog( "Extract track stack", mamut.getGUI() );

		// Target source choice
		final SourceSettings settings = ( SourceSettings ) mamut.getTrackMate().getSettings();
		final int nSources = settings.getSources().size();
		final String[] sourceNames = new String[ nSources ];
		for ( int i = 0; i < sourceNames.length; i++ )
		{
			sourceNames[ i ] = settings.getSources().get( i ).getSpimSource().getName();
		}
		dialog.addChoice( "Target source:", sourceNames, sourceNames[ targetSourceIndex ] );

		// Radius factor
		dialog.addSlider( "Image size (spot\ndiameter units):", 0.1, 5.1, diameterFactor );

		// Central slice vs 3D
		final String[] dimChoices = new String[] { "Central slice ", "3D" };
		dialog.addRadioButtonGroup( "Dimensionality:", dimChoices, 2, 1, dimChoices[ dimChoice ] );

		// Show & Read user input
		dialog.showDialog();
		if ( dialog.wasCanceled() )
		{
			// Return dummy action.
			return new TrackMateAction()
			{

				@Override
				public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
				{}

				@Override
				public void setLogger( final Logger logger )
				{}
			};
		}

		targetSourceIndex = dialog.getNextChoiceIndex();
		diameterFactor = dialog.getNextNumber();
		dimChoice = Arrays.asList( dimChoices ).indexOf( dialog.getNextRadioButton() );

		final boolean do3D = dimChoice == 1;

		return new MamutExtractTrackStackAction( mamut.getSelectionModel(), targetSourceIndex, diameterFactor, do3D );
	}

}
