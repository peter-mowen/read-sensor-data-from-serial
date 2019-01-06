/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thermometer;

import com.fazecast.jSerialComm.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 *
 * @author pmmowen
 */
public class Thermometer {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException, IOException, InterruptedException {
        System.out.println("Getting list of comm devices...");
        SerialPort[] portList = SerialPort.getCommPorts();
        System.out.println("Got List of comm devices!");
        
        SerialPort arduino = null;
        String desiredCommPort = "Arduino Uno";       
        
        System.out.printf("Checking for %s ...\n", desiredCommPort);
        for ( SerialPort port: portList ) {
            String portDescription = port.toString();
            String portDescLower = portDescription.toLowerCase();
            if (portDescLower.matches(desiredCommPort.toLowerCase())){
                System.out.printf("Found %s!\n", desiredCommPort);
                arduino = port;
            }
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
        System.out.println("Establishing data connection..");
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
                } catch (IOException ex) {
                    System.out.println("In IOException");
                    Logger.getLogger(Thermometer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        // Listen to input stream
        try
        {
            String buffer = "";
            String bufferLwr = "";
            String startPhrase = "Arduino Starting Up...";
            String startPhraseLwr = startPhrase.toLowerCase();
            boolean sketchStarted = false;
            
            while (true){
                //System.out.println("Reading into buffer..");
                char data = (char)in.read();
                buffer = buffer + data;
                //System.out.println(buffer);
                bufferLwr = buffer.toLowerCase();

                if ((!sketchStarted)&&(bufferLwr.contains(startPhraseLwr))){
                    //System.out.println("Buffer contained start phrase!");
                    System.out.println("Connection Established! Waiting for incoming data...");
                    sketchStarted = true;
                    int startPhraseIndex = buffer.indexOf(startPhrase);
                    //System.out.print(buffer);
                    System.out.println(buffer.substring(startPhraseIndex));
                    buffer = "";
                    
                } else if ((sketchStarted)&&(buffer.trim().startsWith("@"))&&(buffer.endsWith("\n"))){
                    //System.out.println("sketch started");
                    if (buffer.equals("\n"))
                        buffer = "";
                    System.out.println(buffer.trim());
                    buffer = "";
                }
                //buffer = ""; // initialize buffer for new run
            }       
        } catch (Exception e) { 
            //System.out.println("While loop exception");
            //e.printStackTrace(); 
        } 
    }
}