package main;
//This class takes care of setting up serial connection, and defines serial commands.
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import view.ChargingOverviewController;

public class UART {
    
    SerialPort serialPort = null;
    
    static ChargingOverviewController controller;    
    SerialProtocol serialProtocol;    
 
    public UART (ChargingOverviewController controller) {
        UART.controller = controller; 
        serialProtocol = new SerialProtocol();
    }
        
    public void searchForPorts(){
        
        Enumeration ports = CommPortIdentifier.getPortIdentifiers();        
        List<String> list = new ArrayList<>();
        ObservableList<String> options = FXCollections.observableList(list); 
        
        while (ports.hasMoreElements()){           
            CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();

            //get only serial ports
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL){
                options.add(curPort.getName());
            }
        } 
        controller.updatePortCombo(options);
    }
    
    public void connect(String selectedPort) {
        
        if (serialPort!=null) {
                controller.appendLogWindow("Controller connected already..");
        } else {  

            CommPortIdentifier portIdentifier;  

            serialProtocol = new SerialProtocol();
            
            try {
                portIdentifier = CommPortIdentifier.getPortIdentifier(selectedPort);
                    // points who owns the port and connection timeout  
                    serialPort = (SerialPort) portIdentifier.open("FSChargerController", 2000);  

                    // setup connection parameters  
                    serialPort.setSerialPortParams(  
                        9600, 
                        SerialPort.DATABITS_8, 
                        SerialPort.STOPBITS_1, 
                        SerialPort.PARITY_NONE);
                                        
                    // setup serial port writer  
                    serialProtocol.commPortSender.setWriterStream(serialPort.getOutputStream());
                    // setup serial port reader  
                    serialProtocol.commPortReceiver.setInputStream(serialPort.getInputStream());
                    
                    serialPort.addEventListener(serialProtocol.commPortReceiver);
                    serialPort.notifyOnDataAvailable(true);
                    
                    serialProtocol.connected = true;
                    controller.appendLogWindow("Connected to "+ selectedPort);
                    
                    Thread tl = new Thread(new Looper());
                    tl.start();
                    if (controller.bautoPoll) controller.appendLogWindow("Started polling for V and Temp..");

            } catch (NoSuchPortException ex) {
                controller.appendLogWindow("Cant't find port");  
            } catch (PortInUseException ex) {
                controller.appendLogWindow(selectedPort+" in use!");            
            } catch (UnsupportedCommOperationException | IOException | TooManyListenersException ex) {
                controller.appendLogWindow("Failed to connect!");  
            }
        }
    }
    
    class Looper implements Runnable{//Class for auto polling stuff
        @Override
        public void run() {
            while(serialProtocol.connected){
                
                    try {
                        if (controller.bautoPoll==true){
                            askVoltage();
                            Thread.sleep(controller.aPollTime);
                            if (controller.bautoPoll==true) askTemp();
                        }
                        
                        Thread.sleep(controller.aPollTime);

                    } catch (InterruptedException ex) {
                        controller.appendLogWindow("Err..Try again! "+ ex.toString());
                    } 
            }    
        }
        
    }
    
    public void disconnect(){
        if (serialPort!=null){
            try {
                //serialProtocol.commPortReceiver.terminate();
                //serialProtocol.commPortReceiver.join();

                serialProtocol.commPortReceiver.in.close();
                serialProtocol.commPortSender.out.close();
                serialPort.removeEventListener();
                serialPort.close();

                serialPort = null;
                serialProtocol.connected = false;
                int errors = serialProtocol.dataParser.error; //number of connection errors
                controller.appendLogWindow("Previous session had "+String.valueOf(errors)+" connection errors.");
                controller.appendLogWindow("Disconnected!");
            } catch (IOException ex) {
                controller.appendLogWindow("Err..Try again!"+ ex.toString());
            } 
        } else controller.appendLogWindow("Nothing to disconnect.");
    }

    public void askVoltage(){
        serialProtocol.commPortSender.send("-out");   
    }  
    public void askTemp(){
        serialProtocol.commPortSender.send("-te");   
    }    
    public void reset(){         
         serialProtocol.commPortSender.send("-rst");    
    }        
    public void sendCommand(String sCommand){
        //CommPortSender.send(serialProtocol.getMessage(sCommand)); 
        serialProtocol.commPortSender.send(sCommand); 
    }   
    public void toggleBalance(){
        serialProtocol.commPortSender.send("-cfgw"); 
        //serialProtocol.commPortSender.send("-stat"); //get status if balancing        
    }
      public void toggleVent(){
        serialProtocol.commPortSender.send("-cal");               
    }
}