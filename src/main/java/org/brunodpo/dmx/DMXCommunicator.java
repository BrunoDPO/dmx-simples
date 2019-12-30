package org.brunodpo.dmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

//  ______                          _____   ______   _____  
// (____  \                        (____ \ (_____ \ / ___ \ 
//  ____)  ) ____ _   _ ____   ___  _   \ \ _____) ) |   | |
// |  __  ( / ___) | | |  _ \ / _ \| |   | |  ____/| |   | |
// | |__)  ) |   | |_| | | | | |_| | |__/ /| |     | |___| |
// |______/|_|    \____|_| |_|\___/|_____/ |_|      \_____/ 
//
// 01000010 01110010 01110101 01101110 01101111 01000100 01010000 01001111

/**
 * This class implements the DMX512 Communication Protocol over a Serial Port.
 * It is recommended for this class to work that you either buy or make a RS232
 * to RS485 converter based on the FTDI chip. Such specification is needed
 * because it's the only one that can reach the high baud rate needed for the
 * protocol.
 * <p>
 * v.1.0.0 - Initial release
 * </p>
 * 
 * @author Bruno Di Prinzio de Oliveira
 * @version 1.0.0
 * @since 2018-05-01
 */
public class DMXCommunicator {

	private byte[] buffer = new byte[513];
	private boolean isActive = false;
	private Thread senderThread;
	private SerialPort serialPort;

	/**
	 * Default baud rate for the DMX512 Protocol
	 */
	public static final int DMX512_BAUD_RATE = 250000;

	/**
	 * Initialize a DMXCommunicator class
	 * 
	 * @param portName Name of the serial port as a string
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	public DMXCommunicator(String portName) throws Exception {
		this(SerialPort.getCommPort(portName));
	}

	/**
	 * Initialize a DMXCommunicator class
	 * 
	 * @param port Instance of a SerialPort
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	public DMXCommunicator(SerialPort port) throws Exception {
		buffer[0] = 0; // The first byte must be a zero
		serialPort = configureSerialPort(port);
	}

	/**
	 * Set up a connection and try to open it to see if the port is compatible with
	 * the DMX512 protocol.
	 * 
	 * @param port Serial port instance
	 * @return The referenced serial port instance
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	private static SerialPort configureSerialPort(SerialPort port) throws Exception {
		try {
			if (port.isOpen())
				port.closePort();

			// Port configuration
			port.setComPortParameters(DMX512_BAUD_RATE, 8, SerialPort.TWO_STOP_BITS, SerialPort.NO_PARITY);

			// Try to open a connection with the given settings
			boolean success = port.openPort();
			if (!success)
				throw new Exception("Could not open the serial port with the DMX512 parameters.");
			port.closePort();
		} catch (Exception e) {
			throw e;
		}

		return port;
	}

	/**
	 * Return the state of the connection
	 * 
	 * @return True if the communication is active
	 */
	public boolean isActive() {
		synchronized (this) {
			return isActive;
		}
	}

	/**
	 * Get a parameter value
	 * 
	 * @param index Parameter index (from 0 to 511)
	 * @return Parameter value in bytes
	 * @throws IndexOutOfBoundsException If the index is not between 0 and 511
	 */
	public byte getByte(int index) throws IndexOutOfBoundsException {
		if (index < 0 || index > 511)
			throw new IndexOutOfBoundsException("Index is not between 0 and 511");

		synchronized (this) {
			return buffer[index + 1];
		}
	}

	/**
	 * Get all the parameter values as a byte array
	 * 
	 * @return All 512 parameters as a byte array
	 */
	public byte[] getBytes() {
		synchronized (this) {
			return Arrays.copyOfRange(buffer, 1, 513);
		}
	}

	/**
	 * List all DMX512-compatible serial ports
	 * 
	 * @return A list of all valid serial ports
	 */
	public static List<String> getValidSerialPorts() {
		SerialPort[] ports = SerialPort.getCommPorts();
		List<String> portNames = new ArrayList<String>();
		for (SerialPort port : ports) {
			try {
				configureSerialPort(port);
				portNames.add(port.getSystemPortName());
			} catch (Exception e) {
			}
		}
		return portNames;
	}

	/**
	 * Send the parameters to all slaves in this DMX512 universe
	 */
	private Runnable sendBytes = new Runnable() {
		public void run() {
			while (isActive) {
				// Send a "zero" for 1ms (must send it for at least 100us)
				serialPort.setBreak();
				try {
					Thread.sleep(1);
				} catch (Exception e) {
				}
				serialPort.clearBreak();
				// Send all the byte parameters
				serialPort.writeBytes(buffer, buffer.length);
			}
		}
	};

	/**
	 * Update a parameter value
	 * 
	 * @param index Parameter index (from 0 to 511)
	 * @param value Parameter value
	 * @throws IndexOutOfBoundsException If the index is not between 0 and 511
	 */
	public void setByte(int index, byte value) throws IndexOutOfBoundsException {
		if (index < 0 || index > 511)
			throw new IndexOutOfBoundsException("Index is not between 0 and 511");

		synchronized (this) {
			buffer[index + 1] = value;
		}
	}

	/**
	 * Update all parameter values
	 * 
	 * @param newBuffer A byte array containing 512 elements
	 * @throws IllegalArgumentException If the byte array sent does not contain 512 elements
	 */
	public void setBytes(byte[] newBuffer) throws IllegalArgumentException {
		if (newBuffer.length != 512)
			throw new IllegalArgumentException("This byte array does not contain 512 elements");

		synchronized (this) {
			System.arraycopy(newBuffer, 0, buffer, 1, 512);
		}
	}

	/**
	 * Open the connection and start sending data
	 */
	public void start() {
		// Prevents it from being started more than once
		if (!this.isActive) {
			synchronized (this) {
				if (!this.isActive) {
					boolean isOpened = serialPort.isOpen();
					if (!isOpened)
						isOpened = serialPort.openPort();
					this.isActive = isOpened;
					if (isOpened) {
						senderThread = new Thread(sendBytes);
						senderThread.start();
					}
				}
			}
		}
	}

	/**
	 * Stop sending data and close the connection
	 */
	public void stop() {
		// Prevents it from being stopped more than once
		if (this.isActive) {
			synchronized (this) {
				if (this.isActive) {
					this.isActive = false;
					try {
						senderThread.join(1000);
					} catch (Exception e) {
					}
					if (serialPort.isOpen())
						serialPort.closePort();
				}
			}
		}
	}
}
