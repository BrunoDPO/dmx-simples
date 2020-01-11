# dmx-simples
*Author: Bruno Di Prinzio de Oliveira*

[![Build Status](https://dev.azure.com/brunodpo/dmx-simples/_apis/build/status/dmx-simples-CI?branchName=master)](https://dev.azure.com/brunodpo/dmx-simples/_build/latest?definitionId=7&branchName=master)

### Description
Java implementation of the DMX-512 protocol (with a simple GUI)

### External Packages
This version of dmx-simples is using [jSerialComm](https://github.com/Fazecast/jSerialComm) for communicating through Serial with the USB Device.

### RS232 to RS485 Dongle
For this software to work, you must either buy or make a RS232 to RS485 dongle. There are many on sell today, both based on the Prolific IC or FTDI. As the DMX-512 Protocol uses a high baud rate (250kb/s), it's recommended that you choose the FTDI one as the Prolific-based can't reach such high values.

### TODOs
- Translate the UI to english
