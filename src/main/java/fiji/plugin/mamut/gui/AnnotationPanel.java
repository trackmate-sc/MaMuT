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

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.mamut.MaMuT;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;

public class AnnotationPanel extends ActionListenablePanel
{

	/*
	 * PUBLIC EVENTS
	 */

	public ActionEvent SEMI_AUTO_TRACKING_BUTTON_PRESSED = new ActionEvent( this, 0, "SemiAutoTrackingButtonPushed" );

	public ActionEvent SELECT_TRACK_BUTTON_PRESSED = new ActionEvent( this, 1, "SelectTrackButtonPushed" );

	public ActionEvent SELECT_TRACK_UPWARD_BUTTON_PRESSED = new ActionEvent( this, 2, "SelectTrackUpwardButtonPushed" );

	public ActionEvent SELECT_TRACK_DOWNWARD_BUTTON_PRESSED = new ActionEvent( this, 3, "SelectTrackDownwardButtonPushed" );

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;

	private final static ImageIcon SELECT_TRACK_ICON = new ImageIcon( MaMuT.class.getResource( "arrow_updown.png" ) );

	private final static ImageIcon SELECT_TRACK_ICON_UPWARDS = new ImageIcon( MaMuT.class.getResource( "arrow_up.png" ) );

	private final static ImageIcon SELECT_TRACK_ICON_DOWNWARDS = new ImageIcon( MaMuT.class.getResource( "arrow_down.png" ) );

	private final static ImageIcon SEMIAUTO_TRACKING_ICON;
	static
	{
		final Image image = new ImageIcon( MaMuT.class.getResource( "Icon1_print_transparency.png" ) ).getImage();
		final Image newimg = image.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		SEMIAUTO_TRACKING_ICON = new ImageIcon( newimg );
	}

	@SuppressWarnings( "unused" )
	private final static ImageIcon LINK_SPOTS_ICON;
	static
	{
		final Image image = new ImageIcon( MaMuT.class.getResource( "Icon2_print_transparency.png" ) ).getImage();
		final Image newimg = image.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		LINK_SPOTS_ICON = new ImageIcon( newimg );
	}

	private final Logger logger;

	private final MamutGUIModel guiModel;

	private final JNumericTextField jNFDistanceTolerance;

	private final JNumericTextField jNFQualityThreshold;

	private final JNumericTextField jTimeStep;

	private JPanel stepWisePanel;

	private JLabel lblJumpToEvery;

	private JLabel lblFrames;

	private final JNumericTextField jNFNFrames;

	public AnnotationPanel()
	{
		this( new MamutGUIModel() );
	}

	public AnnotationPanel( final MamutGUIModel guiModel )
	{

		this.guiModel = guiModel;

		/*
		 * Listeners
		 */

		final ActionListener al = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				updateParamsFromTextFields();
			}
		};
		final FocusListener fl = new FocusListener()
		{
			@Override
			public void focusLost( final FocusEvent arg0 )
			{
				updateParamsFromTextFields();
			}

			@Override
			public void focusGained( final FocusEvent arg0 )
			{}
		};

		final JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );

		final JLabel lblSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		lblSemiAutoTracking.setFont( FONT.deriveFont( Font.BOLD ) );

		final JLabel lblQualityThreshold = new JLabel( "Quality threshold" );
		lblQualityThreshold.setToolTipText( "<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>" );
		lblQualityThreshold.setFont( SMALL_FONT );

		jNFQualityThreshold = new JNumericTextField( guiModel.qualityThreshold );
		jNFQualityThreshold.setFormat( "%.1f" );
		jNFQualityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		jNFQualityThreshold.setFont( SMALL_FONT );
		jNFQualityThreshold.addActionListener( al );
		jNFQualityThreshold.addFocusListener( fl );

		final JLabel lblDistanceTolerance = new JLabel( "Distance tolerance" );
		lblDistanceTolerance.setToolTipText( "<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>" );
		lblDistanceTolerance.setFont( SMALL_FONT );

		jNFDistanceTolerance = new JNumericTextField( guiModel.distanceTolerance );
		jNFDistanceTolerance.setFormat( "%.1f" );
		jNFDistanceTolerance.setHorizontalAlignment( SwingConstants.CENTER );
		jNFDistanceTolerance.setFont( SMALL_FONT );
		jNFDistanceTolerance.addActionListener( al );
		jNFDistanceTolerance.addFocusListener( fl );

		final JLabel lblNFrames = new JLabel( "Max nFrames" );
		lblNFrames.setToolTipText( "<html>How many frames to process at max. <br/>Make it 0 or negative for no limit.</html>" );
		lblNFrames.setFont( SMALL_FONT );

		jNFNFrames = new JNumericTextField( ( double ) guiModel.maxNFrames );
		jNFNFrames.setFormat( "%.0f" );
		jNFNFrames.setHorizontalAlignment( SwingConstants.CENTER );
		jNFNFrames.setFont( SMALL_FONT );
		jNFNFrames.addActionListener( al );
		jNFNFrames.addFocusListener( fl );

		final JButton buttonSemiAutoTracking = new JButton( SEMIAUTO_TRACKING_ICON );
		buttonSemiAutoTracking.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				fireAction( SEMI_AUTO_TRACKING_BUTTON_PRESSED );
			}
		} );

		final JLabel labelSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		labelSemiAutoTracking.setToolTipText( "Launch semi-automatic tracking on selected spots." );
		labelSemiAutoTracking.setFont( SMALL_FONT );

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );

		final JPanel panelButtons = new JPanel();
		panelButtons.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );
		panelButtons.setLayout( null );

		final JLabel lblSelectionTools = new JLabel( "Selection tools" );
		lblSelectionTools.setFont( FONT.deriveFont( Font.BOLD ) );
		lblSelectionTools.setBounds( 9, 6, 172, 14 );
		panelButtons.add( lblSelectionTools );

		final JButton buttonSelectTrack = new JButton( SELECT_TRACK_ICON );
		buttonSelectTrack.setBounds( 9, 23, 33, 23 );
		buttonSelectTrack.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fireAction( SELECT_TRACK_BUTTON_PRESSED );
			}
		} );
		panelButtons.add( buttonSelectTrack );

		final JLabel lblSelectTrack = new JLabel( "Select track" );
		lblSelectTrack.setBounds( 52, 23, 129, 23 );
		lblSelectTrack.setFont( SMALL_FONT );
		lblSelectTrack.setToolTipText( "Select the whole tracks selected spots belong to." );
		panelButtons.add( lblSelectTrack );

		final JButton buttonSelectTrackUp = new JButton( SELECT_TRACK_ICON_UPWARDS );
		buttonSelectTrackUp.setBounds( 9, 48, 33, 23 );
		buttonSelectTrackUp.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fireAction( SELECT_TRACK_UPWARD_BUTTON_PRESSED );
			}
		} );
		panelButtons.add( buttonSelectTrackUp );

		final JLabel lblSelectTrackUpward = new JLabel( "Select track upward" );
		lblSelectTrackUpward.setBounds( 52, 48, 129, 23 );
		lblSelectTrackUpward.setFont( SMALL_FONT );
		lblSelectTrackUpward.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, backward in time.</html>" );
		panelButtons.add( lblSelectTrackUpward );

		final JButton buttonSelectTrackDown = new JButton( SELECT_TRACK_ICON_DOWNWARDS );
		buttonSelectTrackDown.setBounds( 9, 73, 33, 23 );
		buttonSelectTrackDown.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				fireAction( SELECT_TRACK_DOWNWARD_BUTTON_PRESSED );
			}
		} );
		panelButtons.add( buttonSelectTrackDown );

		final JLabel lblSelectTrackDown = new JLabel( "Select track downward" );
		lblSelectTrackDown.setBounds( 52, 73, 129, 23 );
		lblSelectTrackDown.setFont( SMALL_FONT );
		lblSelectTrackDown.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, forward in time.</html>" );
		panelButtons.add( lblSelectTrackDown );

		{
			final String toolTip = "<html>Determines the interval of the timepoints accessible <br>" + "when stepwise browsing in time. For instance, with a <br>" + "value of 5, you will be taken to timepoints 0, 5, 10, etc.." + "</html>";

			stepWisePanel = new JPanel();
			stepWisePanel.setBorder( new LineBorder( new Color( 252, 117, 0 ) ) );

			lblJumpToEvery = new JLabel( "Stepwise time browsing " );
			lblJumpToEvery.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
			lblJumpToEvery.setToolTipText( toolTip );

			jTimeStep = new JNumericTextField( ( double ) guiModel.timeStep );
			jTimeStep.setFormat( "%.0f" );
			jTimeStep.setFont( SMALL_FONT );
			jTimeStep.setColumns( 3 );
			jTimeStep.setHorizontalAlignment( SwingConstants.CENTER );
			jTimeStep.addActionListener( al );
			jTimeStep.addFocusListener( fl );
			jTimeStep.setToolTipText( toolTip );

			lblFrames = new JLabel( "frames" );
			lblFrames.setFont( SMALL_FONT );
			lblFrames.setToolTipText( toolTip );
		}
		final GroupLayout groupLayout = new GroupLayout( this );
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addContainerGap()
								.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
										.addComponent( panelButtons, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE )
										.addComponent( scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE )
										.addComponent( stepWisePanel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE )
										.addComponent( panelSemiAutoParams, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE ) )
								.addContainerGap() )
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addGap( 11 )
								.addComponent( stepWisePanel, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( panelSemiAutoParams, GroupLayout.PREFERRED_SIZE, 121, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( panelButtons, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( scrollPane, GroupLayout.DEFAULT_SIZE, 209, Short.MAX_VALUE )
								.addContainerGap() )
				);

		final GroupLayout gl_panelSemiAutoParams = new GroupLayout( panelSemiAutoParams );
		gl_panelSemiAutoParams.setHorizontalGroup(
				gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
								.addGap( 5 )
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
										.addComponent( lblSemiAutoTracking, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE )
										.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
												.addComponent( buttonSemiAutoTracking, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE )
												.addGap( 10 )
												.addComponent( labelSemiAutoTracking, GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE ) ) )
								.addContainerGap() )
						.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
								.addContainerGap()
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.TRAILING )
										.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
												.addComponent( lblQualityThreshold, GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE )
												.addGap( 12 )
												.addComponent( jNFQualityThreshold, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE ) )
										.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
												.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.TRAILING )
														.addComponent( lblNFrames, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 119, GroupLayout.PREFERRED_SIZE )
														.addComponent( lblDistanceTolerance, GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE ) )
												.addGap( 12 )
												.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
														.addComponent( jNFNFrames, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE )
														.addComponent( jNFDistanceTolerance, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE ) ) ) )
								.addGap( 100 ) )
				);
		gl_panelSemiAutoParams.setVerticalGroup(
				gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
								.addGap( 5 )
								.addComponent( lblSemiAutoTracking, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE )
								.addGap( 9 )
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
										.addComponent( buttonSemiAutoTracking, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE )
										.addComponent( labelSemiAutoTracking, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE ) )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
										.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
												.addGap( 2 )
												.addComponent( lblQualityThreshold, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE ) )
										.addComponent( jNFQualityThreshold, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE ) )
								.addGap( 2 )
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.LEADING )
										.addGroup( gl_panelSemiAutoParams.createSequentialGroup()
												.addGap( 2 )
												.addComponent( lblDistanceTolerance, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE ) )
										.addComponent( jNFDistanceTolerance, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE ) )
								.addGap( 2 )
								.addGroup( gl_panelSemiAutoParams.createParallelGroup( Alignment.BASELINE )
										.addComponent( lblNFrames, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE )
										.addComponent( jNFNFrames, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE ) )
								.addContainerGap() )
				);
		panelSemiAutoParams.setLayout( gl_panelSemiAutoParams );
		final GroupLayout gl_stepWisePanel = new GroupLayout( stepWisePanel );
		gl_stepWisePanel.setHorizontalGroup(
				gl_stepWisePanel.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_stepWisePanel.createSequentialGroup()
								.addGap( 5 )
								.addComponent( lblJumpToEvery, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE )
								.addGap( 16 )
								.addComponent( jTimeStep, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
								.addGap( 20 )
								.addComponent( lblFrames, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
								.addGap( 68 ) )
				);
		gl_stepWisePanel.setVerticalGroup(
				gl_stepWisePanel.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_stepWisePanel.createSequentialGroup()
								.addGap( 6 )
								.addGroup( gl_stepWisePanel.createParallelGroup( Alignment.BASELINE )
										.addComponent( lblJumpToEvery )
										.addComponent( jTimeStep, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
										.addComponent( lblFrames ) )
								.addGap( 7 ) )
				);
		gl_stepWisePanel.setAutoCreateContainerGaps( true );
		gl_stepWisePanel.setAutoCreateGaps( true );
		stepWisePanel.setLayout( gl_stepWisePanel );

		final JTextPane textPane = new JTextPane();
		scrollPane.setViewportView( textPane );
		textPane.setFont( SMALL_FONT );
		textPane.setEditable( false );
		textPane.setBackground( this.getBackground() );
		setLayout( groupLayout );

		logger = new Logger()
		{

			@Override
			public void error( final String message )
			{
				log( message, Logger.ERROR_COLOR );
			}

			@Override
			public void log( final String message, final Color color )
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						textPane.setEditable( true );
						final StyleContext sc = StyleContext.getDefaultStyleContext();
						final AttributeSet aset = sc.addAttribute( SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color );
						final int len = textPane.getDocument().getLength();
						textPane.setCaretPosition( len );
						textPane.setCharacterAttributes( aset, false );
						textPane.replaceSelection( message );
						textPane.setEditable( false );
					}
				} );
			}

			@Override
			public void setStatus( final String status )
			{
				log( status, Logger.GREEN_COLOR );
			}

			@Override
			public void setProgress( final double val )
			{}
		};
	}

	/**
	 * Returns the {@link Logger} that outputs on this config panel.
	 * 
	 * @return the {@link Logger} instance of this panel.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	private void updateParamsFromTextFields()
	{
		guiModel.distanceTolerance = jNFDistanceTolerance.getValue();
		guiModel.qualityThreshold = jNFQualityThreshold.getValue();
		guiModel.maxNFrames = ( int ) jNFNFrames.getValue();
		guiModel.timeStep = ( int ) jTimeStep.getValue();
	}
}
