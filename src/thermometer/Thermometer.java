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
package thermometer;

import com.fazecast.jSerialComm.*; //https://fazecast.github.io/jSerialComm/
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pmmowen
 */
public class Thermometer {

    public static void main(String[] args) throws UnsupportedEncodingException, IOException, InterruptedException {
        String timestamp;
        System.out.println("Getting list of comm devices...");
        SerialPort[] portList = SerialPort.getCommPorts();
        System.out.println("Got List of comm devices!");
        
        SerialPort arduino = null;
        // On Windows, the arduino shows up as the following
        String desiredCommPort = "Arduino Uno";
        // This variable could be updated if one needs to find a different board
        
        // Iterate through list of serial ports and find the arduino
        System.out.printf("Checking for %s ...\n", desiredCommPort);
        for ( SerialPort port: portList ) {
            String portDescription = port.toString();
            String portDescLower = portDescription.toLowerCase();
            if (portDescLower.matches(desiredCommPort.toLowerCase())){
                System.out.printf("Found %s!\n", desiredCommPort);
                arduino = port;
            }
        }
        
        // check if desired comm device was found
        if (arduino == null){
            System.out.printf("Could not find %s\n", desiredCommPort);
            System.out.println("Quiting Program");
            System.exit(0);
        }
        SerialPort comPort = arduino;
        comPort.setComPortParameters(9600, 8, 1, 0); // default connection settings for Arduino
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0); // block until bytes can be written

        System.out.printf("Opening serial port for %s...\n", desiredCommPort);
        if (comPort.openPort()){
            System.out.println("Port Open!");
        } else {
            System.out.println("Port did not open");
            return;
        }
        
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        InputStream in = comPort.getInputStream();
        
        // Update what keyboard interrupt does so program shuts down gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread() 
        {@Override
            public void run(){
                try {
                    System.out.println("Closing input stream....");
                    in.close();
                    System.out.println("Input stream closed!");
                    System.out.println("Closing serial port...");
                    if (comPort.closePort()){
                        System.out.println("Serial Port Closed!");
                    } else {
                        System.out.println("Failed to close port!");
                    }
                    System.exit(0);
                } catch (IOException ex) {
                    System.out.println("In IOException");
                    Logger.getLogger(Thermometer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        // Listen to input stream
        System.out.println("Establishing data connection..");
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
                    System.out.println("Connection Established! Waiting for incoming data...");
                    startPhraseFound = true;
                    int startPhraseIndex = buffer.indexOf(startPhrase);
                    //System.out.print(buffer);
                    // print startPhrase
                    System.out.println(buffer.substring(startPhraseIndex));
                    buffer = "";        // re-initialize buffer for next run
                } else if ((startPhraseFound)&&(buffer.trim().startsWith(beginDataFlag))&&(buffer.endsWith(endDataFlag))){
                    //System.out.println("sketch started");
                    if (buffer.equals("\n"))
                        buffer = "";    // re-initialize buffer to save space.
                    System.out.println(buffer.trim());
                    buffer = "";        // re-initialize buffer for next run
                }
            }       
        } catch (Exception e) { 
            System.out.println("While loop exception");
            e.printStackTrace(); 
        } 
    }
}