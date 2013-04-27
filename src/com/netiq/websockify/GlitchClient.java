package com.netiq.websockify;

import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GlitchClient {

	private JFrame frame;

	public static JTextField keyField;
	public static JLabel lblStatusDetail = new JLabel("");

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GlitchClient window = new GlitchClient();
					window.frame.setVisible(true);
						Websockify.main(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GlitchClient() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// TODO close websocket connection
			}
		});

		frame.setBounds(100, 100, 463, 91);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLabel lblKey = new JLabel("Key:");
		lblKey.setBounds(12, 10, 31, 15);
		frame.getContentPane().add(lblKey);

		keyField = new JTextField();
		keyField.setBounds(61, 8, 296, 19);
		frame.getContentPane().add(keyField);
		keyField.setColumns(32);

		JButton btnCopy = new JButton("Copy");
		btnCopy.setBounds(369, 5, 69, 25);
		frame.getContentPane().add(btnCopy);

		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setBounds(12, 37, 70, 15);
		frame.getContentPane().add(lblStatus);

		lblStatusDetail.setBounds(94, 37, 344, 15);
		frame.getContentPane().add(lblStatusDetail);

	}
}
