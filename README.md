# ConnectToSerial
This Java program connects to an arduino uno, reads in data, prints it to the therminal, and writes it to a csv.

Is program uses the jSerialComm library to connect to the arduino's serial port. Once connected, it waits for "Arduino Starting Up..."
to appear on the serial port. (This start phrase will need to be part of the arduino sketch). After the start phrase has been received,
the program waits looks for anything between an "@" sign and a newline character. This whole data string should be formatted as follows:

@data_1: #, data_2: #, data_3: #, ... , data_n: #

Where # represents a floating point number. (This should be part of the arduino sketch)
The program prints out a time stamp, then data to the terminal. Finally, it converts the date and time into a numeric format and 
writes the date, time, and numeric data to a csv file.
