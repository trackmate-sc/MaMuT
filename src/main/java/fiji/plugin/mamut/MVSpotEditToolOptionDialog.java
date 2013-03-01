package fiji.plugin.mamut;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class MVSpotEditToolOptionDialog  extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textFielTimeSteppingInterval;
	private final MVSpotEditTool tool;


	/**
	 * Create the dialog.
	 */
	public MVSpotEditToolOptionDialog(final MVSpotEditTool tool) {
		this.tool = tool;
		initGUI();
	}
	
	private void initGUI() {
		setTitle("MultiViewTracker tool options");
		setModal(true);
		setBounds(100, 100, 270, 130);
		
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JLabel lblTimeSteppingInterval = new JLabel("Time stepping interval:");
		lblTimeSteppingInterval.setToolTipText("<html>The stepping interval by which to move <br>\r\nin time when pressing <b>O</b> or <b>P</b>. <br>\r\nMust be an integer greater than 1. </html>");
		lblTimeSteppingInterval.setFont(new Font("Arial", Font.PLAIN, 12));
		lblTimeSteppingInterval.setBounds(10, 11, 159, 14);
		contentPanel.add(lblTimeSteppingInterval);
		
		textFielTimeSteppingInterval = new JTextField(""+tool.steppingIncrement);
		textFielTimeSteppingInterval.setFont(new Font("Arial", Font.PLAIN, 12));
		textFielTimeSteppingInterval.setBounds(179, 9, 59, 20);
		contentPanel.add(textFielTimeSteppingInterval);
		textFielTimeSteppingInterval.setColumns(10);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			try {
				int targetStep = Integer.parseInt(textFielTimeSteppingInterval.getText());
				if (targetStep > 0) {
					tool.steppingIncrement = targetStep;
				}
			} catch (NumberFormatException nfe) { }
		}
		this.dispose();
	}
	
	
}
