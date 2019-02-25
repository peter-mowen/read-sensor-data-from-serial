/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ReadSensorDataFromSerial;
import com.fazecast.jSerialComm.*; //https://fazecast.github.io/jSerialComm/
import java.io.*;
import java.sql.Timestamp;
import static java.lang.Math.toIntExact;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pmmowen
 */
public class ReadSensorDataFromSerial {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
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
        logEntry = "Checking for " + desiredCommPort + "...";
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
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(ReadSensorDataFromSerial.class.getName()).log(Level.SEVERE, null, ex);
            }
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
            
            boolean startPhraseFound = false; // marked true once start phrase is found
            
            while (true){ // this loop will repeat until interrupt
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
                } else if ((startPhraseFound)&&(buffer.trim().startsWith(beginDataFlag))
                        &&(buffer.endsWith(endDataFlag))){
                    //System.out.println("sketch started");
                    
                    if (buffer.equals("\n"))
                        buffer = "";    // re-initialize buffer to save space.
                    
                    logEntry = buffer.trim().substring(1);
                    
                    String timestamp = getTimestamp();
                    String outdata = timestamp + "\t" + logEntry + "\n";
                    System.out.printf("%s\t%s\n", timestamp, logEntry);
                    
                    String outdataCSV =  extractData(outdata);
                    // extracts date and time from timesstamp and convert them to integers,
                    //  extracts data value from logEntry
                    //  formats date, time, and data as a CSV line
                    
                    toCSV(outdataCSV);
                    
                    buffer = "";        // re-initialize buffer for next run
                }
            }       
        } catch (Exception e) { 
            //System.out.println("While loop exception");
            e.printStackTrace();
            
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

    private static String extractData(String logEntry){
        String outdata = "";
        
        int date = extractDate(logEntry);
        //System.out.println(date);
        outdata = outdata + date + ", ";
        // Trim off date
        logEntry = logEntry.substring(10).trim();
        //System.out.println(logEntry);
        
        int time = extractTime(logEntry);
        //System.out.println(time);
        outdata = outdata + time + ", ";
        // Trim off time
        logEntry = logEntry.substring(12).trim();
        //System.out.println(logEntry);
        
        // Find number of colons to figure out how many data there are
        long numOfColons = logEntry.chars().filter(ch -> ch == ':').count();
        int numOfData = toIntExact(numOfColons);
        
        // Extract data
        for (int i = 0; i < numOfData; i++){
            int indexOfColon = logEntry.indexOf(":");
            int indexOfComma = logEntry.indexOf(",");
            String datum;
            try{
                datum = logEntry.substring(indexOfColon + 1, indexOfComma).trim();
            } 
            catch (Exception e){
                datum = logEntry.substring(indexOfColon + 1).trim();
            }
            
            if ( i < numOfData - 1){
                outdata = outdata + datum + ", ";
            } else {
                outdata = outdata + datum + "\n";
            }
            
            logEntry = logEntry.substring(indexOfComma + 1);
            //System.out.println("End of for loop log entry: " + logEntry);
            //System.out.println(outdata);
        }
        return outdata;
    }

    private static int extractDate(String logEntry) {
        String dateStr = logEntry.substring(0, 10);
        String dateNumStr = dateStr.replace("-", "");
        int date = Integer.parseInt(dateNumStr);
        return date;
    }

    private static int extractTime(String logEntry) {
        String timeStr = logEntry.substring(0,12);
        //timeStr = "1502165     S";
        String lastChar = timeStr.substring(timeStr.length()-1);
        if (isInt(lastChar)) {
        } else {
            timeStr = timeStr.substring(0, timeStr.length()-1);
            lastChar = timeStr.substring(timeStr.length()-1);
            if (isInt(lastChar))
                timeStr = timeStr.trim() + "0"; 
            else
                timeStr = timeStr.trim() + "00"; 
        }
        String timeNumStr = timeStr.replace(":", "");
        timeNumStr = timeNumStr.replace(".","");
        int time = Integer.parseInt(timeNumStr);
        return time;
    }
    
    private static boolean isInt(String lastChar) {
        try { 
            Integer.parseInt(lastChar); 
            boolean isInt = true;
            return isInt;
        } catch (NumberFormatException e) { 
            boolean isInt = false;
            return isInt;
        } 
    }

    private static void toCSV(String outdata) throws IOException {
        String filePath = "C:\\Users\\pmmow\\Documents\\Environment Monitor project\\EnvironmentMonitor\\data\\" ;
        File outfile = new File(filePath + "data.csv");        
        Writer output = new BufferedWriter(new FileWriter(outfile, true));
        output.append(outdata);
        output.close();
    }

}
