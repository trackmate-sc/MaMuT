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

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
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
import ij.IJ;

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

	private final JFormattedTextField ftfDistanceTolerance;

	private final JFormattedTextField ftfQualityThreshold;

	private final JFormattedTextField ftfTimeStep;

	private final JFormattedTextField ftfNFrames;

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
			public void focusLost( final FocusEvent e )
			{
				updateParamsFromTextFields();
			}

			@Override
			public void focusGained( final FocusEvent e )
			{
				SwingUtilities.invokeLater( () -> {
					final JTextField source = ( JTextField ) e.getSource();
					source.selectAll();
				} );
			}
		};

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0 };
		setLayout( gridBagLayout );

		/*
		 * Step-wise panel.
		 */

		final JPanel stepWisePanel = new JPanel();
		stepWisePanel.setBorder( new LineBorder( new Color( 252, 117, 0 ) ) );
		stepWisePanel.setLayout( new BoxLayout( stepWisePanel, BoxLayout.X_AXIS ) );
		final GridBagConstraints gbcStepWisePanel = new GridBagConstraints();
		gbcStepWisePanel.fill = GridBagConstraints.BOTH;
		gbcStepWisePanel.insets = new Insets( 5, 5, 5, 5 );
		gbcStepWisePanel.gridx = 0;
		gbcStepWisePanel.gridy = 0;
		add( stepWisePanel, gbcStepWisePanel );

		final String jumpTimeToolTip = "<html>"
				+ "Determines the interval of the timepoints accessible <br>"
				+ "when stepwise browsing in time. For instance, with a <br>"
				+ "value of 5, you will be taken to timepoints 0, 5, 10, etc.."
				+ "</html>";

		final JLabel lblJumpToEvery = new JLabel( "Stepwise time browsing " );
		lblJumpToEvery.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
		lblJumpToEvery.setToolTipText( jumpTimeToolTip );
		stepWisePanel.add( lblJumpToEvery );
		stepWisePanel.add( Box.createHorizontalGlue() );

		ftfTimeStep = new JFormattedTextField( Integer.valueOf( guiModel.timeStep ) );
		ftfTimeStep.setMaximumSize( new Dimension( 160, 2147483647 ) );
		ftfTimeStep.setFont( SMALL_FONT );
		ftfTimeStep.setColumns( 3 );
		ftfTimeStep.setHorizontalAlignment( SwingConstants.CENTER );
		ftfTimeStep.addActionListener( al );
		ftfTimeStep.addFocusListener( fl );
		ftfTimeStep.setToolTipText( jumpTimeToolTip );
		stepWisePanel.add( ftfTimeStep );
		stepWisePanel.add( Box.createHorizontalGlue() );

		final JLabel lblFrames = new JLabel( "frames" );
		lblFrames.setFont( SMALL_FONT );
		lblFrames.setToolTipText( jumpTimeToolTip );
		stepWisePanel.add( lblFrames );

		/*
		 * Panel with selection buttons.
		 */

		final JPanel panelButtons = new JPanel();
		panelButtons.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );
		final GridBagLayout gblPanelButtons = new GridBagLayout();
		gblPanelButtons.columnWeights = new double[] { 0.0, 1.0 };
		panelButtons.setLayout( gblPanelButtons );

		final GridBagConstraints gbcPanelButtons = new GridBagConstraints();
		gbcPanelButtons.fill = GridBagConstraints.BOTH;
		gbcPanelButtons.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelButtons.gridx = 0;
		gbcPanelButtons.gridy = 2;
		add( panelButtons, gbcPanelButtons );

		final JLabel lblSelectionTools = new JLabel( "Selection tools" );
		lblSelectionTools.setFont( FONT.deriveFont( Font.BOLD ) );
		final GridBagConstraints gbcLblSelectionTools = new GridBagConstraints();
		gbcLblSelectionTools.fill = GridBagConstraints.BOTH;
		gbcLblSelectionTools.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSelectionTools.gridwidth = 2;
		gbcLblSelectionTools.gridx = 0;
		gbcLblSelectionTools.gridy = 0;
		panelButtons.add( lblSelectionTools, gbcLblSelectionTools );

		final JLabel lblSelectTrack = new JLabel( "Select track" );
		lblSelectTrack.setFont( SMALL_FONT );
		lblSelectTrack.setToolTipText( "Select the whole tracks selected spots belong to." );
		final GridBagConstraints gbcLblSelectTrack = new GridBagConstraints();
		gbcLblSelectTrack.fill = GridBagConstraints.BOTH;
		gbcLblSelectTrack.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSelectTrack.gridx = 1;
		gbcLblSelectTrack.gridy = 1;
		panelButtons.add( lblSelectTrack, gbcLblSelectTrack );

		final JButton buttonSelectTrack = new JButton( SELECT_TRACK_ICON );
		buttonSelectTrack.addActionListener( e -> fireAction( SELECT_TRACK_BUTTON_PRESSED ) );
		final GridBagConstraints gbcButtonSelectTrack = new GridBagConstraints();
		gbcButtonSelectTrack.fill = GridBagConstraints.BOTH;
		gbcButtonSelectTrack.insets = new Insets( 0, 5, 5, 5 );
		gbcButtonSelectTrack.gridx = 0;
		gbcButtonSelectTrack.gridy = 1;
		panelButtons.add( buttonSelectTrack, gbcButtonSelectTrack );

		final JLabel lblSelectTrackUpward = new JLabel( "Select track upward" );
		lblSelectTrackUpward.setFont( SMALL_FONT );
		lblSelectTrackUpward.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, backward in time.</html>" );
		final GridBagConstraints gbcLblSelectTrackUpward = new GridBagConstraints();
		gbcLblSelectTrackUpward.fill = GridBagConstraints.BOTH;
		gbcLblSelectTrackUpward.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSelectTrackUpward.gridx = 1;
		gbcLblSelectTrackUpward.gridy = 2;
		panelButtons.add( lblSelectTrackUpward, gbcLblSelectTrackUpward );

		final JButton buttonSelectTrackUp = new JButton( SELECT_TRACK_ICON_UPWARDS );
		buttonSelectTrackUp.addActionListener( e -> fireAction( SELECT_TRACK_UPWARD_BUTTON_PRESSED ) );
		final GridBagConstraints gbcButtonSelectTrackUp = new GridBagConstraints();
		gbcButtonSelectTrackUp.fill = GridBagConstraints.BOTH;
		gbcButtonSelectTrackUp.insets = new Insets( 0, 5, 5, 5 );
		gbcButtonSelectTrackUp.gridx = 0;
		gbcButtonSelectTrackUp.gridy = 2;
		panelButtons.add( buttonSelectTrackUp, gbcButtonSelectTrackUp );

		final JLabel lblSelectTrackDown = new JLabel( "Select track downward" );
		lblSelectTrackDown.setFont( SMALL_FONT );
		lblSelectTrackDown.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, forward in time.</html>" );
		final GridBagConstraints gbcLblSelectTrackDown = new GridBagConstraints();
		gbcLblSelectTrackDown.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSelectTrackDown.fill = GridBagConstraints.BOTH;
		gbcLblSelectTrackDown.gridx = 1;
		gbcLblSelectTrackDown.gridy = 3;
		panelButtons.add( lblSelectTrackDown, gbcLblSelectTrackDown );

		final JButton buttonSelectTrackDown = new JButton( SELECT_TRACK_ICON_DOWNWARDS );
		buttonSelectTrackDown.addActionListener( e -> fireAction( SELECT_TRACK_DOWNWARD_BUTTON_PRESSED ) );
		final GridBagConstraints gbcButtonSelectTrackDown = new GridBagConstraints();
		gbcButtonSelectTrackDown.fill = GridBagConstraints.BOTH;
		gbcButtonSelectTrackDown.insets = new Insets( 0, 5, 5, 5 );
		gbcButtonSelectTrackDown.gridx = 0;
		gbcButtonSelectTrackDown.gridy = 3;
		panelButtons.add( buttonSelectTrackDown, gbcButtonSelectTrackDown );

		/*
		 * Semi-auto-tracking panel.
		 */

		final JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );
		final GridBagLayout gblPanelSemiAutoParams = new GridBagLayout();
		gblPanelSemiAutoParams.columnWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
		gblPanelSemiAutoParams.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelSemiAutoParams.setLayout( gblPanelSemiAutoParams );

		final GridBagConstraints gbcPanelSemiAutoParams = new GridBagConstraints();
		gbcPanelSemiAutoParams.fill = GridBagConstraints.BOTH;
		gbcPanelSemiAutoParams.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelSemiAutoParams.gridx = 0;
		gbcPanelSemiAutoParams.gridy = 1;
		add( panelSemiAutoParams, gbcPanelSemiAutoParams );

		final JLabel labelSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		labelSemiAutoTracking.setToolTipText( "Launch semi-automatic tracking on selected spots." );
		labelSemiAutoTracking.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelSemiAutoTracking = new GridBagConstraints();
		gbcLabelSemiAutoTracking.fill = GridBagConstraints.BOTH;
		gbcLabelSemiAutoTracking.insets = new Insets( 0, 0, 5, 0 );
		gbcLabelSemiAutoTracking.gridwidth = 2;
		gbcLabelSemiAutoTracking.gridx = 1;
		gbcLabelSemiAutoTracking.gridy = 1;
		panelSemiAutoParams.add( labelSemiAutoTracking, gbcLabelSemiAutoTracking );

		final JLabel lblSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		lblSemiAutoTracking.setFont( FONT.deriveFont( Font.BOLD ) );
		final GridBagConstraints gbcLblSemiAutoTracking = new GridBagConstraints();
		gbcLblSemiAutoTracking.fill = GridBagConstraints.BOTH;
		gbcLblSemiAutoTracking.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSemiAutoTracking.gridwidth = 3;
		gbcLblSemiAutoTracking.gridx = 0;
		gbcLblSemiAutoTracking.gridy = 0;
		panelSemiAutoParams.add( lblSemiAutoTracking, gbcLblSemiAutoTracking );

		final JButton buttonSemiAutoTracking = new JButton( SEMIAUTO_TRACKING_ICON );
		buttonSemiAutoTracking.addActionListener( e -> fireAction( SEMI_AUTO_TRACKING_BUTTON_PRESSED ) );
		final GridBagConstraints gbcButtonSemiAutoTracking = new GridBagConstraints();
		gbcButtonSemiAutoTracking.insets = new Insets( 5, 5, 5, 5 );
		gbcButtonSemiAutoTracking.gridx = 0;
		gbcButtonSemiAutoTracking.gridy = 1;
		panelSemiAutoParams.add( buttonSemiAutoTracking, gbcButtonSemiAutoTracking );

		final JLabel lblQualityThreshold = new JLabel( "Quality threshold" );
		lblQualityThreshold.setToolTipText( "<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>" );
		lblQualityThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblQualityThreshold = new GridBagConstraints();
		gbcLblQualityThreshold.fill = GridBagConstraints.BOTH;
		gbcLblQualityThreshold.insets = new Insets( 0, 5, 5, 5 );
		gbcLblQualityThreshold.gridwidth = 2;
		gbcLblQualityThreshold.gridx = 0;
		gbcLblQualityThreshold.gridy = 2;
		panelSemiAutoParams.add( lblQualityThreshold, gbcLblQualityThreshold );

		ftfQualityThreshold = new JFormattedTextField( Double.valueOf( guiModel.qualityThreshold ) );
		ftfQualityThreshold.setMaximumSize( new Dimension( 160, 2147483647 ) );
		ftfQualityThreshold.setColumns( 8 );
		ftfQualityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		ftfQualityThreshold.setFont( SMALL_FONT );
		ftfQualityThreshold.addActionListener( al );
		ftfQualityThreshold.addFocusListener( fl );
		final GridBagConstraints gbcFtfQualityThreshold = new GridBagConstraints();
		gbcFtfQualityThreshold.insets = new Insets( 0, 0, 5, 0 );
		gbcFtfQualityThreshold.gridx = 2;
		gbcFtfQualityThreshold.gridy = 2;
		panelSemiAutoParams.add( ftfQualityThreshold, gbcFtfQualityThreshold );

		final JLabel lblDistanceTolerance = new JLabel( "Distance tolerance" );
		lblDistanceTolerance.setToolTipText( "<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>" );
		lblDistanceTolerance.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblDistanceTolerance = new GridBagConstraints();
		gbcLblDistanceTolerance.fill = GridBagConstraints.BOTH;
		gbcLblDistanceTolerance.insets = new Insets( 0, 5, 5, 5 );
		gbcLblDistanceTolerance.gridwidth = 2;
		gbcLblDistanceTolerance.gridx = 0;
		gbcLblDistanceTolerance.gridy = 3;
		panelSemiAutoParams.add( lblDistanceTolerance, gbcLblDistanceTolerance );

		ftfDistanceTolerance = new JFormattedTextField( Double.valueOf( guiModel.distanceTolerance ) );
		ftfDistanceTolerance.setMaximumSize( new Dimension( 160, 2147483647 ) );
		ftfDistanceTolerance.setColumns( 8 );
		ftfDistanceTolerance.setHorizontalAlignment( SwingConstants.CENTER );
		ftfDistanceTolerance.setFont( SMALL_FONT );
		ftfDistanceTolerance.addActionListener( al );
		ftfDistanceTolerance.addFocusListener( fl );
		final GridBagConstraints gbcFtfDistanceTolerance = new GridBagConstraints();
		gbcFtfDistanceTolerance.insets = new Insets( 0, 0, 5, 0 );
		gbcFtfDistanceTolerance.gridx = 2;
		gbcFtfDistanceTolerance.gridy = 3;
		panelSemiAutoParams.add( ftfDistanceTolerance, gbcFtfDistanceTolerance );

		final JLabel lblNFrames = new JLabel( "Max nFrames" );
		lblNFrames.setToolTipText( "<html>How many frames to process at max. <br/>Make it 0 or negative for no limit.</html>" );
		lblNFrames.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblNFrames = new GridBagConstraints();
		gbcLblNFrames.anchor = GridBagConstraints.WEST;
		gbcLblNFrames.fill = GridBagConstraints.VERTICAL;
		gbcLblNFrames.insets = new Insets( 0, 5, 5, 5 );
		gbcLblNFrames.gridwidth = 2;
		gbcLblNFrames.gridx = 0;
		gbcLblNFrames.gridy = 4;
		panelSemiAutoParams.add( lblNFrames, gbcLblNFrames );

		ftfNFrames = new JFormattedTextField( Integer.valueOf( guiModel.maxNFrames ) );
		ftfNFrames.setMaximumSize( new Dimension( 160, 2147483647 ) );
		ftfNFrames.setColumns( 8 );
		ftfNFrames.setHorizontalAlignment( SwingConstants.CENTER );
		ftfNFrames.setFont( SMALL_FONT );
		ftfNFrames.addActionListener( al );
		ftfNFrames.addFocusListener( fl );
		final GridBagConstraints gbcFtfNFrames = new GridBagConstraints();
		gbcFtfNFrames.insets = new Insets( 0, 0, 5, 0 );
		gbcFtfNFrames.gridx = 2;
		gbcFtfNFrames.gridy = 4;
		panelSemiAutoParams.add( ftfNFrames, gbcFtfNFrames );


		/*
		 * Logger panel.
		 */

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );

		final JTextPane textPane = new JTextPane();
		scrollPane.setViewportView( textPane );
		textPane.setFont( SMALL_FONT );
		textPane.setEditable( false );
		textPane.setBackground( this.getBackground() );
		final GridBagConstraints gbcScrollPane = new GridBagConstraints();
		gbcScrollPane.insets = new Insets( 5, 5, 5, 5 );
		gbcScrollPane.fill = GridBagConstraints.BOTH;
		gbcScrollPane.gridx = 0;
		gbcScrollPane.gridy = 3;
		add( scrollPane, gbcScrollPane );

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
			{
				IJ.showProgress( val );
			}
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
		guiModel.distanceTolerance = ( ( Number ) ftfDistanceTolerance.getValue() ).doubleValue();
		guiModel.qualityThreshold = ( ( Number ) ftfQualityThreshold.getValue() ).doubleValue();
		guiModel.maxNFrames = ( ( Number ) ftfNFrames.getValue() ).intValue();
		guiModel.timeStep = ( ( Number ) ftfTimeStep.getValue() ).intValue();
	}
}
