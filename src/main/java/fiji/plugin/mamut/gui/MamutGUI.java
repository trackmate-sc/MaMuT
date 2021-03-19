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

import static fiji.plugin.mamut.MaMuT.PLUGIN_NAME;
import static fiji.plugin.mamut.MaMuT.PLUGIN_VERSION;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.mamut.action.MamutActionFactory;
import fiji.plugin.mamut.providers.MamutActionProvider;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;

public class MamutGUI extends JFrame
{

	private static final long serialVersionUID = 1L;

	private final static ImageIcon ANNOTATION_ICON_ORIG = new ImageIcon( MaMuT.class.getResource( "Logo50x50-color-nofont-72p.png" ) );

	private final static ImageIcon ANNOTATION_ICON;

	private static final ImageIcon MAMUT_ICON_ORIG = new ImageIcon( MaMuT.class.getResource( "mammouth-256x256.png" ) );

	private static final ImageIcon MAMUT_ICON;

	private static final ImageIcon ACTION_ICON_ORIG = new ImageIcon( MaMuT.class.getResource( "cog.png" ) );

	private static final ImageIcon ACTION_ICON;

	static
	{
		final Image image1 = ANNOTATION_ICON_ORIG.getImage();
		final Image newimg1 = image1.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		ANNOTATION_ICON = new ImageIcon( newimg1 );

		final Image image2 = MAMUT_ICON_ORIG.getImage();
		final Image newimg2 = image2.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		MAMUT_ICON = new ImageIcon( newimg2 );

		final Image image3 = ACTION_ICON_ORIG.getImage();
		final Image newimg3 = image3.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		ACTION_ICON = new ImageIcon( newimg3 );
	}


	private static final Icon EXECUTE_ICON = new ImageIcon( MaMuT.class.getResource( "control_play_blue.png" ) );

	private final MamutControlPanel viewPanel;

	private final AnnotationPanel annotationPanel;

	private final MamutActionPanel actionPanel;

	public MamutGUI( final TrackMate trackmate, final MaMuT mamut )
	{
		setTitle( PLUGIN_NAME + " v" + PLUGIN_VERSION );
		setIconImage( MAMUT_ICON.getImage() );
		setSize( 340, 580 );

		final JTabbedPane tabbedPane = new JTabbedPane( SwingConstants.TOP );
		getContentPane().add( tabbedPane, BorderLayout.CENTER );
		setLocationByPlatform( true );
		setVisible( true );

		viewPanel = new MamutControlPanel( trackmate.getModel() );
		tabbedPane.addTab( "Views", MAMUT_ICON, viewPanel, "The control panel for views" );

		annotationPanel = new AnnotationPanel( mamut.getGuimodel() );
		tabbedPane.addTab( "Annotation", ANNOTATION_ICON, annotationPanel, "Annotation tools" );

		final MamutActionProvider actionProvider = new MamutActionProvider();
		final List< String > actionKeys = actionProvider.getVisibleKeys();
		final List< String > names = new ArrayList<>( actionKeys.size() );
		final List< String > infoTexts = new ArrayList<>( actionKeys.size() );
		final List< ImageIcon > icons = new ArrayList<>( actionKeys.size() );
		for ( final String key : actionKeys )
		{
			infoTexts.add( actionProvider.getFactory( key ).getInfoText() );
			icons.add( actionProvider.getFactory( key ).getIcon() );
			names.add( actionProvider.getFactory( key ).getName() );
		}

		final Action action = new AbstractAction( "Execute", EXECUTE_ICON )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final int choice = actionPanel.getChoice();
				final String key = actionKeys.get( choice );
				final MamutActionFactory factory = actionProvider.getFactory( key );
				final TrackMateAction mamutAction = factory.create( mamut );
				mamutAction.setLogger( actionPanel.getLogPanel().getLogger() );
				mamutAction.execute( trackmate );
			}
		};
		actionPanel = new MamutActionPanel( names, infoTexts, icons, "action", action );
		tabbedPane.addTab( "Actions", ACTION_ICON, actionPanel, "Actions" );
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
