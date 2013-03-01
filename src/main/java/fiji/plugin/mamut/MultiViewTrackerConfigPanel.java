package fiji.plugin.mamut;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLOR_FEATURE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.TRACK_SCHEME_ICON;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.ActionListenablePanel;
import fiji.plugin.trackmate.gui.JNumericTextField;
import fiji.plugin.trackmate.gui.JPanelColorByFeatureGUI;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class MultiViewTrackerConfigPanel extends JFrame {

	
	private static final long serialVersionUID = 1L;

	public ActionEvent TRACK_SCHEME_BUTTON_PRESSED 	= new ActionEvent(this, 0, "TrackSchemeButtonPushed");
	
	private static final Icon SAVE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
	private JButton jButtonShowTrackScheme;
	private JLabel jLabelTrackDisplayMode;
	private JComboBox jComboBoxDisplayMode;
	private JLabel jLabelDisplayOptions;
	private JPanel jPanelSpotOptions;
	private JCheckBox jCheckBoxDisplaySpots;
	private JPanel jPanelTrackOptions;
	private JCheckBox jCheckBoxDisplayTracks;
	private JCheckBox jCheckBoxLimitDepth;
	private JTextField jTextFieldFrameDepth;
	private JLabel jLabelFrameDepth;
	private JPanelColorByFeatureGUI jPanelSpotColor;
	private JNumericTextField jTextFieldSpotRadius;
	private JCheckBox jCheckBoxDisplayNames;
	private JButton saveButton;
	/** The set of {@link TrackMateModelView} views controlled by this controller.	 */
	private Set<TrackMateModelView> views = new HashSet<TrackMateModelView>();

	private ActionListenablePanel mainPanel;
	private TrackMateModel model;
	private Logger logger;
	private File file;

	/*
	 * CONSTRUCTOR
	 */
	public MultiViewTrackerConfigPanel(TrackMateModel model, MultiViewDisplayer view) {
		this.model = model;
		this.logger = model.getLogger();
		register(view);
		initGUI();
	}

	/*
	 * METHODS
	 */
	
	
	private void save() {
		saveButton.setEnabled(false);
		try {

			logger.log("Saving data...\n", Logger.BLUE_COLOR);
			if (null == file ) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					String name =  model.getSettings().imp.getShortTitle();
					String newName = name.replaceAll("<[0-9:-]+>", "X");
					file = new File(folder.getPath() + File.separator + newName +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "MultiViewTrackerData.xml");
				}
			}

			File tmpFile = IOUtils.askForFile(file, this, logger);
			if (null == tmpFile) {
				saveButton.setEnabled(true);
				return;
			}
			file = tmpFile;
			TrackMate_ plugin = new TrackMate_(model);
			plugin.initModules();
			plugin.computeTrackFeatures(true);
			TmXmlWriter writer = new TmXmlWriter(plugin);
			try {
				writer.process();
				writer.writeToFile(file);
			} catch (FileNotFoundException e) {
				logger.error("Error finding file for saving:\n"+e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				logger.error("IO error when saving:\n"+e.getMessage());
				e.printStackTrace();
			} finally {
				saveButton.setEnabled(true);
			}

		}	finally {
			saveButton.setEnabled(true);
		}
	}
	
	protected void fireAction(final ActionEvent event) {
		new Thread() {
			@Override
			public void run() {
				// Intercept event coming from the JPanelSpotColorGUI, and translate it for views
				if (event == jPanelSpotColor.COLOR_FEATURE_CHANGED) {
					for (TrackMateModelView view : views) {
						view.setDisplaySettings(KEY_SPOT_COLOR_FEATURE, jPanelSpotColor.getSelectedFeature());
						view.refresh();
					}
				} else if (event == TRACK_SCHEME_BUTTON_PRESSED) {
					
					try {
						TrackScheme trackScheme = new TrackScheme(model);
						trackScheme.render();
					} finally {
						jButtonShowTrackScheme.setEnabled(true);
					}
					
				} else {
					System.out.println("Got unkown event: "+event);
				}
			}
		}.start();
	}
	
	/**
	 * Add the given {@link TrackMateModelView} to the list managed by this controller.
	 */
	public void register(final TrackMateModelView view) {
		if (!views.contains(view)) {
			views.add(view);
		}
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		try {
			
			mainPanel = new ActionListenablePanel();
			mainPanel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					fireAction(event);
				}
			});
			mainPanel.setSize(300, 500);
			mainPanel.setLayout(null);
			{
				jPanelTrackOptions = new JPanel() {
					private static final long serialVersionUID = -1805693239189343720L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				FlowLayout jPanelTrackOptionsLayout = new FlowLayout();
				jPanelTrackOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelTrackOptions.setLayout(jPanelTrackOptionsLayout);
				mainPanel.add(jPanelTrackOptions);
				jPanelTrackOptions.setBounds(10, 212, 280, 117);
				jPanelTrackOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					jLabelTrackDisplayMode = new JLabel();
					jPanelTrackOptions.add(jLabelTrackDisplayMode);
					jLabelTrackDisplayMode.setText("  Track display mode:");
					jLabelTrackDisplayMode.setBounds(10, 163, 268, 15);
					jLabelTrackDisplayMode.setFont(FONT);
					jLabelTrackDisplayMode.setPreferredSize(new java.awt.Dimension(261, 14));
				}
				{
					String[] keyNames = TrackMateModelView.TRACK_DISPLAY_MODE_DESCRIPTION;
					ComboBoxModel jComboBoxDisplayModeModel = new DefaultComboBoxModel(keyNames);
					jComboBoxDisplayMode = new JComboBox();
					jPanelTrackOptions.add(jComboBoxDisplayMode);
					jComboBoxDisplayMode.setModel(jComboBoxDisplayModeModel);
					jComboBoxDisplayMode.setSelectedIndex(0);
					jComboBoxDisplayMode.setFont(SMALL_FONT);
					jComboBoxDisplayMode.setPreferredSize(new java.awt.Dimension(265, 27));
					jComboBoxDisplayMode.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_MODE, jComboBoxDisplayMode.getSelectedIndex());
								view.refresh();
							}
						}
					});
				}
				{
					jCheckBoxLimitDepth = new JCheckBox();
					jPanelTrackOptions.add(jCheckBoxLimitDepth);
					jCheckBoxLimitDepth.setText("Limit frame depth");
					jCheckBoxLimitDepth.setBounds(6, 216, 272, 23);
					jCheckBoxLimitDepth.setFont(FONT);
					jCheckBoxLimitDepth.setSelected(true);
					jCheckBoxLimitDepth.setPreferredSize(new java.awt.Dimension(259, 23));
					jCheckBoxLimitDepth.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							int depth;
							if (jCheckBoxLimitDepth.isSelected())
								depth = Integer.parseInt(jTextFieldFrameDepth.getText());
							else
								depth = (int) 1e9;
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_DEPTH, depth);
								view.refresh();
							}
						}
					});
				}
				{
					jLabelFrameDepth = new JLabel();
					jPanelTrackOptions.add(jLabelFrameDepth);
					jLabelFrameDepth.setText("  Frame depth:");
					jLabelFrameDepth.setFont(SMALL_FONT);
					jLabelFrameDepth.setPreferredSize(new java.awt.Dimension(103, 14));
				}
				{
					jTextFieldFrameDepth = new JTextField();
					jPanelTrackOptions.add(jTextFieldFrameDepth);
					jTextFieldFrameDepth.setFont(SMALL_FONT);
					jTextFieldFrameDepth.setText(""+TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH);
					jTextFieldFrameDepth.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldFrameDepth.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							int depth = Integer.parseInt(jTextFieldFrameDepth.getText());
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_TRACK_DISPLAY_DEPTH, depth);
								view.refresh();
							}
						}
					});
				}
			}
			{
				jCheckBoxDisplayTracks = new JCheckBox();
				mainPanel.add(jCheckBoxDisplayTracks);
				jCheckBoxDisplayTracks.setText("Display tracks");
				jCheckBoxDisplayTracks.setFont(FONT);
				jCheckBoxDisplayTracks.setBounds(10, 188, 233, 23);
				jCheckBoxDisplayTracks.setSelected(true);
				jCheckBoxDisplayTracks.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						boolean isSelected = jCheckBoxDisplayTracks.isSelected();
						jPanelTrackOptions.setEnabled(isSelected);
						for(TrackMateModelView view : views) {
							view.setDisplaySettings(KEY_TRACKS_VISIBLE, isSelected);
							view.refresh();
						}
					}
				});
			}
			{
				jCheckBoxDisplaySpots = new JCheckBox();
				mainPanel.add(jCheckBoxDisplaySpots);
				jCheckBoxDisplaySpots.setText("Display spots");
				jCheckBoxDisplaySpots.setFont(FONT);
				jCheckBoxDisplaySpots.setBounds(10, 38, 280, 23);
				jCheckBoxDisplaySpots.setSelected(true);
				jCheckBoxDisplaySpots.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						boolean isSelected = jCheckBoxDisplaySpots.isSelected();
						jPanelSpotOptions.setEnabled(isSelected);
						for(TrackMateModelView view : views) {
							view.setDisplaySettings(KEY_SPOTS_VISIBLE, isSelected);
							view.refresh();
						}
					}
				});
			}
			{
				jPanelSpotOptions = new JPanel() {
					private static final long serialVersionUID = 3259314983744108471L;
					public void setEnabled(boolean enabled) {
						for(Component c : getComponents())
							c.setEnabled(enabled);
					};
				};
				FlowLayout jPanelSpotOptionsLayout = new FlowLayout();
				jPanelSpotOptionsLayout.setAlignment(FlowLayout.LEFT);
				jPanelSpotOptions.setLayout(jPanelSpotOptionsLayout);
				mainPanel.add(jPanelSpotOptions);
				jPanelSpotOptions.setBounds(10, 63, 280, 110);
				jPanelSpotOptions.setBorder(new LineBorder(new java.awt.Color(192,192,192), 1, true));
				{
					JLabel jLabelSpotRadius = new JLabel();
					jLabelSpotRadius.setText("  Spot display radius ratio:");
					jLabelSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jLabelSpotRadius);

					jTextFieldSpotRadius = new JNumericTextField("1");
					jTextFieldSpotRadius.setPreferredSize(new java.awt.Dimension(34, 20));
					jTextFieldSpotRadius.setFont(SMALL_FONT);
					jPanelSpotOptions.add(jTextFieldSpotRadius);
					jTextFieldSpotRadius.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_SPOT_RADIUS_RATIO, (float) jTextFieldSpotRadius.getValue());
								view.refresh();
							}
						}
					});
					jTextFieldSpotRadius.addFocusListener(new FocusListener() {
						@Override
						public void focusLost(FocusEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_SPOT_RADIUS_RATIO, (float) jTextFieldSpotRadius.getValue());
								view.refresh();
							}							
						}
						@Override
						public void focusGained(FocusEvent e) {}
					});
				}
				{
					jCheckBoxDisplayNames = new JCheckBox();
					jCheckBoxDisplayNames.setText("Display spot names");
					jCheckBoxDisplayNames.setFont(SMALL_FONT);
					jCheckBoxDisplayNames.setSelected(false);
					jPanelSpotOptions.add(jCheckBoxDisplayNames);
					jCheckBoxDisplayNames.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							for(TrackMateModelView view : views) {
								view.setDisplaySettings(KEY_DISPLAY_SPOT_NAMES, jCheckBoxDisplayNames.isSelected());
								view.refresh();
							}
						}
					});
				}
				{
					Map<String, double[]> featureValues = model.getFeatureModel().getSpotFeatureValues();
					List<String> features = model.getFeatureModel().getSpotFeatures();
					Map<String, String> featureNames = model.getFeatureModel().getSpotFeatureNames();

					jPanelSpotColor = new JPanelColorByFeatureGUI(features, featureNames, mainPanel);
					jPanelSpotColor.setFeatureValues(featureValues);
					jPanelSpotOptions.add(jPanelSpotColor);
				}
			}
			{
				jLabelDisplayOptions = new JLabel();
				jLabelDisplayOptions.setText("Display options");
				jLabelDisplayOptions.setFont(BIG_FONT);
				jLabelDisplayOptions.setBounds(20, 11, 280, 20);
				jLabelDisplayOptions.setHorizontalAlignment(SwingConstants.LEFT);
				mainPanel.add(jLabelDisplayOptions);
			}
			{
				jButtonShowTrackScheme = new JButton();
				jButtonShowTrackScheme.setText("Track scheme");
				jButtonShowTrackScheme.setIcon(TRACK_SCHEME_ICON);
				jButtonShowTrackScheme.setFont(FONT);
				jButtonShowTrackScheme.setBounds(10, 345, 120, 30);
				jButtonShowTrackScheme.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						fireAction(TRACK_SCHEME_BUTTON_PRESSED);
					}
				});
				jButtonShowTrackScheme.setEnabled(false); // FIXME we compute thumbnails at creation, which is not ok in MaMuT
				mainPanel.add(jButtonShowTrackScheme);
			}
			{
				saveButton = new JButton("Save", SAVE_ICON);
				saveButton.setBounds(185, 431, 99, 30);
				saveButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						save();
					}
				});
				mainPanel.add(saveButton);
				
				setSize(300, 500);
				setResizable(false);
				setTitle(MaMuT_.PLUGIN_NAME + " " + MaMuT_.PLUGIN_VERSION);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		getContentPane().add(mainPanel);
		
	}
}
