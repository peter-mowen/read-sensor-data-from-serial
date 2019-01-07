/*
 * This program reads data in from the serial port.
 *  The desired comm Port is currently set to Arduino Uno but could potentially 
 *  be changed to another comm port. When reading from the comm port, this 
 *  program looks for a phrase that signals that good data is on the way. Once
 *  that phrase is found, the program looks for a symbol that indicates the
 *  the beginning of good data and a symbol that indicates the end of this data.
 *  New line characters coming in on the serial port are ignored.
 *  
 * Webpages I referenced writing this program include:
 *  https://github.com/Fazecast/jSerialComm/wiki/Modes-of-Operation
 *  https://fazecast.github.io/jSerialComm/javadoc/com/fazecast/jSerialComm/package-summary.html
 *  https://stackoverflow.com/questions/26360541/handle-a-keyboardinterrupt-in-java#
 */
package connecttoserial;

import com.fazecast.jSerialComm.*; //https://fazecast.github.io/jSerialComm/
import java.io.*;
import java.sql.Timestamp;

/**
 *
 * @author pmmowen
 */
public class ConnectToSerial {

    public static void main(String[] args) throws UnsupportedEncodingException, IOException, InterruptedException {
        String logEntry;
        
        logEntry = "Getting list of comm devices...";
        toSystemOut(logEntry);
        
        SerialPort[] portList = SerialPort.getCommPorts();
        
        logEntry = "Got List of comm devices!";
        toSystemOut(logEntry);
        
        SerialPort arduino = null;
        // On Windows, the arduino shows up as the following
        String desiredCommPort = "Arduino Uno";
        // This variable could be updated if one needs to find a different board
        
        // Iterate through list of serial ports and find the arduino
        logEntry = "Checking for %s ...";
        toSystemOut(logEntry);
        
        for ( SerialPort port: portList ) {
            String portDescription = port.toString();
            String portDescLower = portDescription.toLowerCase();
            if (portDescLower.matches(desiredCommPort.toLowerCase())){
                logEntry = "Found " + desiredCommPort + "!";
                toSystemOut(logEntry);
                arduino = port;
            }
        }
        
        // check if desired comm device was found
        if (arduino == null){
            logEntry = "Could not find " + desiredCommPort + "!";
            toSystemOut(logEntry);
            
            logEntry = "Quiting Program!";
            toSystemOut(logEntry);
            
            System.exit(0);
        }
        SerialPort comPort = arduino;
        comPort.setComPortParameters(9600, 8, 1, 0); // default connection settings for Arduino
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0); // block until bytes can be written
        
        logEntry = "Opening serial port for " + desiredCommPort + "..." ;
        toSystemOut(logEntry);
        
        System.out.printf("", desiredCommPort);
        if (comPort.openPort()){
            logEntry = "Port Open!";
            toSystemOut(logEntry);
        } else {
            logEntry = "Port did not open!";
            toSystemOut(logEntry);
            return;
        }
        
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        InputStream in = comPort.getInputStream();
        
        // Update what keyboard interrupt does so program shuts down gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread() 
        {@Override
            public void run(){
                String logEntry;

                logEntry = "Closing serial port...";
                toSystemOut(logEntry);

                if (comPort.closePort()){
                    logEntry = "Serial Port Closed!";
                    toSystemOut(logEntry);
                } else {
                    logEntry = "Failed to close port!";
                    toSystemOut(logEntry);
                }
            }
        });
        // Listen to input stream
        logEntry = "Establishing data connection...";
        toSystemOut(logEntry);

        try
        {
            String buffer = "";     // initialize to hold incoming data
            String bufferLwr;       // initialize for lowercase buffer 
            String startPhrase = "Arduino Starting Up...";
            String startPhraseLwr = startPhrase.toLowerCase();
            String beginDataFlag = "@";
            String endDataFlag = "\n";
            
            boolean startPhraseFound = false; // marked true once start
            
            while (true){ // this loop will repeat until keyboard interrupt
                //System.out.println("Reading into buffer..");
                char data = (char)in.read();    // single incoming byte
                buffer = buffer + data;         // add byte to buffer
                //System.out.println(buffer);
                bufferLwr = buffer.toLowerCase();

                if ((!startPhraseFound)&&(bufferLwr.contains(startPhraseLwr))){
                    //System.out.println("Buffer contained start phrase!");
                    logEntry = "Connection Established! Waiting for incoming data...";
                    toSystemOut(logEntry);
                    
                    startPhraseFound = true;
                    int startPhraseIndex = buffer.indexOf(startPhrase);
                    //System.out.print(buffer);
                    // print startPhrase
                    logEntry = buffer.substring(startPhraseIndex);
                    toSystemOut(logEntry);
                    buffer = "";        // re-initialize buffer for next run
                } else if ((startPhraseFound)&&(buffer.trim().startsWith(beginDataFlag))&&(buffer.endsWith(endDataFlag))){
                    //System.out.println("sketch started");
                    if (buffer.equals("\n"))
                        buffer = "";    // re-initialize buffer to save space.
                    logEntry = buffer.trim().substring(1);
                    toSystemOut(logEntry);
                    buffer = "";        // re-initialize buffer for next run
                }
            }       
        } catch (Exception e) { 
            //System.out.println("While loop exception");
            //e.printStackTrace(); 
        } 
    }
    private static String getTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestampStr = timestamp.toString();
        return timestampStr;
    }
    
    private static void toSystemOut(String logEntry){
        String timestamp = getTimestamp();
        System.out.printf("%s\t%s\n", timestamp, logEntry);
    }
}