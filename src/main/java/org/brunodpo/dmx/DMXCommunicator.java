package org.brunodpo.dmx;

import java.util.ArrayList;
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
 * This class implements the DMX Communication Protocol over a Serial Port.
 * It is recommended for this class to work that you either buy or make a
 * RS232 to RS485 converter.
 * <p>
 * v.1.0.0 - Initial release
 * </p>
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
	 * Initialize a DMXCommunicator class
	 * @param portName Name of the serial port as a string
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	public DMXCommunicator(String portName) throws Exception {
		this(SerialPort.getCommPort(portName));
	}
	
	/**
	 * Initialize a DMXCommunicator class
	 * @param port Instance of a SerialPort
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	public DMXCommunicator(SerialPort port) throws Exception {
		buffer[0] = 0; // The first byte must be a zero
		serialPort = configureSerialPort(port);
	}

	/**
	 * Set a connection and try to open it to see if the port is
	 * compatible with the DMX512 protocol. 
	 * @param port Serial port instance
	 * @return The referenced serial port instance
	 * @throws Exception If the serial port is somehow inaccessible
	 */
	private static SerialPort configureSerialPort(SerialPort port) throws Exception {
		try {
			if (port.isOpen())
				port.closePort();

			// Port configuration
			port.setComPortParameters(250000, 8, SerialPort.TWO_STOP_BITS, SerialPort.NO_PARITY);

			// Try to open a connection with the given settings
			boolean success = port.openPort();
			if (!success)
				throw new Exception("Could not open the serial port with the DMX parameters.");
			port.closePort();
		} catch (Exception e) {
			throw e;
		}

		return port;
	}

	/**
	 * Returns the state of the connection
	 * @return True if the communication is active
	 */
	public boolean isActive() {
		synchronized (this) {
			return isActive;
		}
	}

	/**
	 * Get a parameter value
	 * @param index Parameter index (from 1 to 512)
	 * @return Parameter value in bytes
	 * @throws IndexOutOfBoundsException If the index is not between 1 and 512
	 */
	public byte getByte(int index) throws IndexOutOfBoundsException {
		if (index < 1 || index > 512)
			throw new IndexOutOfBoundsException("Index is not between 1 and 512");

		synchronized (this) {
			return buffer[index];
		}
	}

	/**
	 * Get all the parameter values
	 * @return The entire 513 byte vector
	 */
	public byte[] getBytes() {
		synchronized (this) {
			return buffer;
		}
	}

	/**
	 * List all DMX-compatible serial ports
	 * @return A list of all valid serial ports
	 */
	public static List<String> getValidSerialPorts() {
		SerialPort[] ports = SerialPort.getCommPorts();
		List<String> portNames = new ArrayList<String>();
		for (SerialPort port : ports) {
			try {
				configureSerialPort(port);
				portNames.add(port.getSystemPortName());
			} catch (Exception e) { }
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
				} catch(Exception e) { }
				serialPort.clearBreak();
				// Send all the byte parameters
				serialPort.writeBytes(buffer, buffer.length);
			}
		}
	};

	/**
	 * Update a parameter value
	 * @param index Parameter index (from 1 to 512)
	 * @param value Parameter value
	 * @throws IndexOutOfBoundsException If the index is not between 1 and 512
	 */
	public void setByte(int index, byte value) throws IndexOutOfBoundsException {
		if (index < 1 || index > 512)
			throw new IndexOutOfBoundsException("Index is not between 1 and 512");

		synchronized (this) {
			buffer[index] = value;
		}
	}

	/**
	 * Update all parameter values
	 * @param newBuffer A 513 element vector with the first one being a zero
	 * @throws IllegalArgumentException If the byte vector sent does not contain 513 elements
	 */
	public void setBytes(byte[] newBuffer) throws IllegalArgumentException {
		if (newBuffer.length != 513)
			throw new IllegalArgumentException("This byte vector does not contain 513 elements");

		newBuffer[0] = 0; // Grants that the first byte will be a zero
		synchronized (this) {
			buffer = newBuffer;
		}
	}

	/**
	 * Open the connection and start sending data
	 */
	public void start() {
		// Prevents it from being started more than once
		if (this.isActive)
			return;

		if (!serialPort.isOpen())
			serialPort.openPort();
		synchronized(this) {
			this.isActive = true;
		}
		senderThread = new Thread(sendBytes);
		senderThread.start();
	}

	/**
	 * Stop sending data and close the connection
	 */
	public void stop() {
		// Prevents it from being stopped more than once
		if (!this.isActive)
			return;

		synchronized(this) {
			this.isActive = false;
		}
		try {
			senderThread.join(1000);
		} catch(Exception e) { }
		if (serialPort.isOpen())
			serialPort.closePort();
	}
}
