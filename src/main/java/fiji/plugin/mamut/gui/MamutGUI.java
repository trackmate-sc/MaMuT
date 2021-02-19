/*-
 * #%L
 * Fiji plugin for the annotation of massive, multi-view data.
 * %%
 * Copyright (C) 2012 - 2016 MaMuT development team.
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

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.providers.MamutActionProvider;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class MamutGUI extends JFrame
{

	private static final long serialVersionUID = 1L;

	private static final ImageIcon MAMUT_ICON_32x32;
	static
	{
		final Image image = MaMuT.MAMUT_ICON.getImage();
		final Image newimg = image.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		MAMUT_ICON_32x32 = new ImageIcon( newimg );
	}

	private final MamutControlPanel viewPanel;

	private final AnnotationPanel annotationPanel;

	private final MamutActionChooserPanel actionPanel;

	public MamutGUI(
			final TrackMate trackmate,
			final MaMuT mamut,
			final DisplaySettings ds )
	{
		setTitle( PLUGIN_NAME + " v" + PLUGIN_VERSION );
		setIconImage( MAMUT_ICON_32x32.getImage() );

		final JTabbedPane tabbedPane = new JTabbedPane( SwingConstants.TOP );
		getContentPane().add( tabbedPane, BorderLayout.CENTER );
		setLocationByPlatform( true );

		final FeatureDisplaySelector featureSelector = new FeatureDisplaySelector( trackmate.getModel(), trackmate.getSettings(), ds );
		viewPanel = new MamutControlPanel(
				ds,
				featureSelector,
				trackmate.getModel().getSpaceUnits(),
				e -> mamut.newViewer(),
				e -> mamut.newTrackScheme(),
				e -> mamut.newTrackTables(),
				e -> mamut.newSpotTable(),
				e -> mamut.save() );

		tabbedPane.addTab( "Views", MAMUT_ICON_32x32, viewPanel, "The control panel for views" );

		annotationPanel = new AnnotationPanel( mamut );
		tabbedPane.addTab( "Annotation", Icons.TRACKMATE_ICON_16x16, annotationPanel, "Annotation tools" );

		actionPanel = new MamutActionChooserPanel( new MamutActionProvider(), mamut, trackmate, mamut.getSelectionModel(), ds );
		tabbedPane.addTab( "Actions", Icons.COG_ICON, actionPanel, "Actions" );
	}

	public MamutControlPanel getViewPanel()
	{
		return viewPanel;
	}

	public AnnotationPanel getAnnotationPanel()
	{
		return annotationPanel;
	}
}
