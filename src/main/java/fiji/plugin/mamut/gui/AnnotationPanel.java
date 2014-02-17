package fiji.plugin.mamut.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;

public class AnnotationPanel extends ActionListenablePanel {

	/*
	 * PUBLIC EVENTS
	 */

	public ActionEvent SEMI_AUTO_TRACKING_BUTTON_PRESSED 		= new ActionEvent(this, 0, "SemiAutoTrackingButtonPushed");
	public ActionEvent SELECT_TRACK_BUTTON_PRESSED 				= new ActionEvent(this, 1, "SelectTrackButtonPushed");
	public ActionEvent SELECT_TRACK_UPWARD_BUTTON_PRESSED 		= new ActionEvent(this, 2, "SelectTrackUpwardButtonPushed");
	public ActionEvent SELECT_TRACK_DOWNWARD_BUTTON_PRESSED 	= new ActionEvent(this, 3, "SelectTrackDownwardButtonPushed");

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;
	private final static ImageIcon SELECT_TRACK_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_updown.png"));
	private final static ImageIcon SELECT_TRACK_ICON_UPWARDS = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_up.png"));
	private final static ImageIcon SELECT_TRACK_ICON_DOWNWARDS = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_down.png"));
	private final static ImageIcon SEMIAUTO_TRACKING_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/SpotIcon_supersmall.png"));
	@SuppressWarnings("unused")
	private final static ImageIcon LINK_SPOTS_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/EdgeIcon_supersmall.png"));
	private final Logger logger;
	private final MamutGUIModel guiModel;
	private final JNumericTextField jNFDistanceTolerance;
	private final JNumericTextField jNFQualityThreshold;

	private final JNumericTextField jTimeStep;

	public AnnotationPanel() {
		this(new MamutGUIModel());
	}

	public AnnotationPanel(final MamutGUIModel guiModel) {

		this.guiModel = guiModel;

		/*
		 * Listeners
		 */

		final ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateParamsFromTextFields();
			}
		};
		final FocusListener fl = new FocusListener() {
			@Override
			public void focusLost(final FocusEvent arg0) {
				updateParamsFromTextFields();
			}

			@Override
			public void focusGained(final FocusEvent arg0) {
			}
		};

		/*
		 * GUI
		 */
		setLayout(null);

		final JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		panelSemiAutoParams.setBounds( 5, 47, 288, 108 );
		add(panelSemiAutoParams);
		panelSemiAutoParams.setLayout(null);

		final JLabel lblSemiAutoTracking = new JLabel("Semi-automatic tracking");
		lblSemiAutoTracking.setBounds(6, 6, 180, 16);
		lblSemiAutoTracking.setFont(FONT.deriveFont(Font.BOLD));
		panelSemiAutoParams.add(lblSemiAutoTracking);

		final JLabel lblQualityThreshold = new JLabel("Quality threshold");
		lblQualityThreshold.setToolTipText("<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>");
		lblQualityThreshold.setBounds(6, 66, 119, 16);
		lblQualityThreshold.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblQualityThreshold);

		jNFQualityThreshold = new JNumericTextField(guiModel.qualityThreshold);
		jNFQualityThreshold.setHorizontalAlignment(SwingConstants.CENTER);
		jNFQualityThreshold.setFont(SMALL_FONT);
		jNFQualityThreshold.setBounds(137, 64, 49, 18);
		jNFQualityThreshold.addActionListener(al);
		jNFQualityThreshold.addFocusListener(fl);

		panelSemiAutoParams.add(jNFQualityThreshold);

		final JLabel lblDistanceTolerance = new JLabel("Distance tolerance");
		lblDistanceTolerance.setToolTipText("<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>");
		lblDistanceTolerance.setBounds(6, 86, 119, 16);
		lblDistanceTolerance.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblDistanceTolerance);

		jNFDistanceTolerance = new JNumericTextField(guiModel.distanceTolerance);
		jNFDistanceTolerance.setHorizontalAlignment(SwingConstants.CENTER);
		jNFDistanceTolerance.setFont(SMALL_FONT);
		jNFDistanceTolerance.setBounds(137, 84, 49, 18);
		jNFDistanceTolerance.addActionListener(al);
		jNFDistanceTolerance.addFocusListener(fl);
		panelSemiAutoParams.add(jNFDistanceTolerance);

		final JButton buttonSemiAutoTracking = new JButton(SEMIAUTO_TRACKING_ICON);
		buttonSemiAutoTracking.setBounds(6, 31, 33, 23);
		panelSemiAutoParams.add(buttonSemiAutoTracking);
		buttonSemiAutoTracking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				fireAction(SEMI_AUTO_TRACKING_BUTTON_PRESSED);
			}
		});

		final JLabel labelSemiAutoTracking = new JLabel("Semi-automatic tracking");
		labelSemiAutoTracking.setToolTipText("Launch semi-automatic tracking on selected spots.");
		labelSemiAutoTracking.setFont(SMALL_FONT);
		labelSemiAutoTracking.setBounds(49, 31, 137, 23);
		panelSemiAutoParams.add(labelSemiAutoTracking);

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds( 6, 273, 288, 214 );
		add(scrollPane);

		final JTextPane textPane = new JTextPane();
		scrollPane.setViewportView(textPane);
		textPane.setFont(SMALL_FONT);
		textPane.setEditable(false);
		textPane.setBackground(this.getBackground());

		final JPanel panelButtons = new JPanel();
		panelButtons.setBounds( 6, 162, 288, 103 );
		panelButtons.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		add(panelButtons);
		panelButtons.setLayout(null);

		final JLabel lblSelectionTools = new JLabel("Selection tools");
		lblSelectionTools.setFont(FONT.deriveFont(Font.BOLD));
		lblSelectionTools.setBounds( 9, 6, 172, 14 );
		panelButtons.add(lblSelectionTools);

		final JButton buttonSelectTrack = new JButton(SELECT_TRACK_ICON);
		buttonSelectTrack.setBounds( 9, 23, 33, 23 );
		buttonSelectTrack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fireAction(SELECT_TRACK_BUTTON_PRESSED);
			}
		});
		panelButtons.add(buttonSelectTrack);

		final JLabel lblSelectTrack = new JLabel("Select track");
		lblSelectTrack.setBounds( 52, 23, 129, 23 );
		lblSelectTrack.setFont(SMALL_FONT);
		lblSelectTrack.setToolTipText("Select the whole tracks selected spots belong to.");
		panelButtons.add(lblSelectTrack);

		final JButton buttonSelectTrackUp = new JButton(SELECT_TRACK_ICON_UPWARDS);
		buttonSelectTrackUp.setBounds( 9, 48, 33, 23 );
		buttonSelectTrackUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fireAction(SELECT_TRACK_UPWARD_BUTTON_PRESSED);
			}
		});
		panelButtons.add(buttonSelectTrackUp);

		final JLabel lblSelectTrackUpward = new JLabel("Select track upward");
		lblSelectTrackUpward.setBounds( 52, 48, 129, 23 );
		lblSelectTrackUpward.setFont(SMALL_FONT);
		lblSelectTrackUpward.setToolTipText("<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, backward in time.</html>");
		panelButtons.add(lblSelectTrackUpward);

		final JButton buttonSelectTrackDown = new JButton(SELECT_TRACK_ICON_DOWNWARDS);
		buttonSelectTrackDown.setBounds( 9, 73, 33, 23 );
		buttonSelectTrackDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				fireAction(SELECT_TRACK_DOWNWARD_BUTTON_PRESSED);
			}
		});
		panelButtons.add(buttonSelectTrackDown);

		final JLabel lblSelectTrackDown = new JLabel("Select track downward");
		lblSelectTrackDown.setBounds( 52, 73, 129, 23 );
		lblSelectTrackDown.setFont(SMALL_FONT);
		lblSelectTrackDown.setToolTipText("<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, forward in time.</html>");
		panelButtons.add(lblSelectTrackDown);

		{
			final String toolTip = "<html>Determines the interval of the timepoints accessible <br>" + "when stepwise browsing in time. For instance, with a <br>" + "value of 5, you will be taken to timepoints 0, 5, 10, etc.." + "</html>";

			final JPanel stepWisePanel = new JPanel();
			final FlowLayout flowLayout = ( FlowLayout ) stepWisePanel.getLayout();
			flowLayout.setAlignment( FlowLayout.LEFT );
			stepWisePanel.setBorder( new LineBorder( new Color( 252, 117, 0 ) ) );
			stepWisePanel.setBounds( 6, 11, 287, 30 );
			add( stepWisePanel );

			final JLabel lblJumpToEvery = new JLabel( "Stepwise time browsing " );
			lblJumpToEvery.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
			lblJumpToEvery.setToolTipText( toolTip );
			stepWisePanel.add( lblJumpToEvery );

			jTimeStep = new JNumericTextField( ( double ) guiModel.timeStep );
			jTimeStep.setFont( SMALL_FONT );
			jTimeStep.setColumns( 3 );
			jTimeStep.setHorizontalAlignment( SwingConstants.CENTER );
			jTimeStep.addActionListener( al );
			jTimeStep.addFocusListener( fl );
			jTimeStep.setToolTipText( toolTip );
			stepWisePanel.add( jTimeStep );

			final JLabel lblFrames = new JLabel( "frames" );
			lblFrames.setFont( SMALL_FONT );
			lblFrames.setToolTipText( toolTip );
			stepWisePanel.add( lblFrames );
		}

		logger = new Logger() {

			@Override
			public void error(final String message) {
				log(message, Logger.ERROR_COLOR);
			}

			@Override
			public void log(final String message, final Color color) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						textPane.setEditable(true);
						final StyleContext sc = StyleContext.getDefaultStyleContext();
						final AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
						final int len = textPane.getDocument().getLength();
						textPane.setCaretPosition(len);
						textPane.setCharacterAttributes(aset, false);
						textPane.replaceSelection(message);
						textPane.setEditable(false);
					}
				});
			}

			@Override
			public void setStatus(final String status) {
				log(status, Logger.GREEN_COLOR);
			}

			@Override
			public void setProgress(final double val) {
			}
		};
	}

	/**
	 * Returns the {@link Logger} that outputs on this config panel.
	 *
	 * @return the {@link Logger} instance of this panel.
	 */
	public Logger getLogger() {
		return logger;
	}

	private void updateParamsFromTextFields() {
		guiModel.distanceTolerance = jNFDistanceTolerance.getValue();
		guiModel.qualityThreshold = jNFQualityThreshold.getValue();
		guiModel.timeStep = ( int ) jTimeStep.getValue();
	}
}
