/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thermometer;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author pmmowen
 */
public class Thermometer {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException, IOException, InterruptedException {
        SerialPort arduino = null;
        SerialPort[] portList = SerialPort.getCommPorts();
        System.out.println("Got List of comm devices...");
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
        
        
        try
        {
            String startPhrase = "Arduino Starting Up...";
            String buffer = "";
            int total = 0;
            int numOfIterations = 100;

            for (int i = 1; i<=numOfIterations; i++){
                System.out.printf("Opening serial port for %s...\n", desiredCommPort);
                if (comPort.openPort()){
                    System.out.println("Port Open...");
                } else {
                    System.out.println("Port did not open");
                    return;
                }

                comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
                InputStream in = comPort.getInputStream();
                
                System.out.println("Reading into buffer..");
                for (int j = 0; j < 500; ++j){
                    buffer = buffer + (char)in.read();
                }
                if (buffer.toLowerCase().contains(startPhrase.toLowerCase())){
                    int indexOfStartPhrase = buffer.indexOf(startPhrase);
                    total += indexOfStartPhrase;
                } else{
                    System.out.println("Didn't find the start phrase");
                }
                
                in.close();
                if (comPort.closePort()){
                    System.out.println("Closing Port...");
                } else {
                    System.out.println("Failed to close port");
                }
            }
            float averageIndex = total / numOfIterations;
            
            System.out.printf("The average start index was %.2f\n", averageIndex);
            //System.out.println("Printing buffer...");
            //System.out.println(buffer);
        } catch (Exception e) { e.printStackTrace(); }
        
        
        
        
    }
    
}