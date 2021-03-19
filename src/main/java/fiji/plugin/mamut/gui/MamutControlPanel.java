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

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class MamutControlPanel extends ConfigureViewsPanel
{

	private static final long serialVersionUID = 1L;

	private static final ImageIcon MAMUT_ICON = new ImageIcon( MaMuT.class.getResource( "mammouth-16x16.png" ) );

	private static final Icon SAVE_ICON = new ImageIcon( MaMuT.class.getResource( "page_save.png" ) );

	public static final ImageIcon THREEDVIEWER_ICON = new ImageIcon( MaMuT.class.getResource( "sport_8ball.png" ) );

	private final JButton jButtonMamutViewer;

	private final JButton jButtonSaveButton;

	public final ActionEvent MAMUT_VIEWER_BUTTON_PRESSED = new ActionEvent( this, 2, "MamutViewerButtonPushed" );

	public final ActionEvent MAMUT_SAVE_BUTTON_PRESSED = new ActionEvent( this, 3, "MamutSaveButtonPushed" );

	/*
	 * CONSTRUCTORS
	 */

	public MamutControlPanel()
	{
		this( new Model() );
	}

	public MamutControlPanel( final Model model )
	{
		super( model );

		// Hijack the do analysis bytton
		jButtonDoAnalysis.setText( "3D Viewer" );
		jButtonDoAnalysis.setIcon( THREEDVIEWER_ICON );
		jButtonDoAnalysis.setToolTipText( "Launch a 3D view of the annotation data only." );

		// New Mamut viewer button
		jButtonMamutViewer = new JButton( "MaMuT Viewer", MAMUT_ICON );
		jButtonMamutViewer.setFont( SMALL_FONT );
		jButtonMamutViewer.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				fireAction( MAMUT_VIEWER_BUTTON_PRESSED );
			}
		} );

		// Save button
		jButtonSaveButton = new JButton( "Save", SAVE_ICON );
		jButtonSaveButton.setFont( SMALL_FONT );
		jButtonSaveButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				fireAction( MAMUT_SAVE_BUTTON_PRESSED );
			}
		} );

		jPanelButtons.add( jButtonMamutViewer );
		jPanelButtons.add( jButtonSaveButton );
		resizeButtons();
	}
}
