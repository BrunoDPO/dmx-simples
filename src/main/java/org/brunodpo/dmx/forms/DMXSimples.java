package org.brunodpo.dmx.forms;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.brunodpo.dmx.DMXCommunicator;
import org.brunodpo.dmx.forms.events.CombinedChangeListener;

/**
 * The main entry point for this app
 */
public class DMXSimples {
	
	private static final int NUMBER_OF_CHANNELS = 32;

	private static byte[] buffer = new byte[513];
	private static DMXCommunicator dmxCommunicator = null;

	private static void createAndShowGUI() {
		// App's main window
		JFrame frame = new JFrame("DMX Simples");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(frame.getClass().getResource("/icons/icon.png")));

		JPanel mainPanel = new JPanel();
		mainPanel.setPreferredSize(new Dimension(810, 440));
		frame.add(mainPanel);

		// DMX channel controllers
		JPanel allChannelsPanel = new JPanel();
		allChannelsPanel.setPreferredSize(new Dimension(800, 440));
		mainPanel.add(allChannelsPanel);
		JPanel[] channelPanel = new JPanel[NUMBER_OF_CHANNELS];
		for (int i = 0; i < channelPanel.length; i++) {
			channelPanel[i] = new JPanel();
			channelPanel[i].setPreferredSize(new Dimension(45, 210));
			allChannelsPanel.add(channelPanel[i]);
		}
		JLabel[] label = new JLabel[NUMBER_OF_CHANNELS];
		for (int i = 0; i < label.length; i++) {
			label[i] = new JLabel(String.format("CH %02d", (i + 1)), SwingConstants.CENTER);
			label[i].setPreferredSize(new Dimension(45, 15));
			label[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
			channelPanel[i].add(label[i]);
		}
		JSlider[] slider = new JSlider[NUMBER_OF_CHANNELS];
		for (int i = 0; i < slider.length; i++) {
			slider[i] = new JSlider(SwingConstants.VERTICAL, 0, 255, 0);
			slider[i].setPreferredSize(new Dimension(45, 160));
			channelPanel[i].add(slider[i]);
		}
		JSpinner[] spinner = new JSpinner[NUMBER_OF_CHANNELS];
		for (int i = 0; i < spinner.length; i++) {
			spinner[i] = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
			spinner[i].setPreferredSize(new Dimension(45, 20));
			channelPanel[i].add(spinner[i]);
		}
		
		// Channel Actions
		for (int i = 0; i < NUMBER_OF_CHANNELS; i++) {
			CombinedChangeListener listener = new CombinedChangeListener(slider[i], spinner[i]);
			slider[i].addChangeListener(listener);
			spinner[i].addChangeListener(listener);
			final int index = (i + 1);
			spinner[i].addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					JSpinner spinner = (JSpinner)e.getSource();
					Integer value = (Integer)spinner.getValue();
					buffer[index] = value.byteValue();
					if (dmxCommunicator != null && dmxCommunicator.isActive()) {
						dmxCommunicator.setByte(index, value.byteValue());
					}
				}
			});
		}
		
		// Side Panel
		JPanel sidePanel = new JPanel();
		sidePanel.setPreferredSize(new Dimension(150, 440));
		mainPanel.add(sidePanel);
		JLabel portLabel = new JLabel("Porta");
		portLabel.setPreferredSize(new Dimension(145, 20));
		portLabel.setHorizontalAlignment(SwingConstants.CENTER);
		sidePanel.add(portLabel);
		
		JComboBox<String> serialPortCombo = new JComboBox<String>();
		serialPortCombo.setPreferredSize(new Dimension(145, 20));
		List<String> validPorts = DMXCommunicator.getValidSerialPorts();
		for (String port : validPorts)
			serialPortCombo.addItem(port);
		sidePanel.add(serialPortCombo);
		
		JButton startButton = new JButton("Iniciar");
		startButton.setPreferredSize(new Dimension(70, 20));
		sidePanel.add(startButton);
		
		JButton stopButton = new JButton("Parar");
		stopButton.setPreferredSize(new Dimension(70, 20));
		stopButton.setEnabled(false);
		sidePanel.add(stopButton);

		// Button actions
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (serialPortCombo.getItemCount() == 0)
					return;
				
				try {
					dmxCommunicator = new DMXCommunicator((String)serialPortCombo.getSelectedItem());
					dmxCommunicator.setBytes(buffer);
					dmxCommunicator.start();
					
					serialPortCombo.setEnabled(!serialPortCombo.isEnabled());
					startButton.setEnabled(!startButton.isEnabled());
					stopButton.setEnabled(!stopButton.isEnabled());
				} catch (Exception ex) { }
				
			}
		});
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (dmxCommunicator != null && dmxCommunicator.isActive())
					dmxCommunicator.stop();
				
				serialPortCombo.setEnabled(!serialPortCombo.isEnabled());
				startButton.setEnabled(!startButton.isEnabled());
				stopButton.setEnabled(!stopButton.isEnabled());
			}
		});
		
		// Show window
		frame.setSize(985, 485);
		frame.setResizable(false);
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
