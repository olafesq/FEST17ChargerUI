package main;
//This class tales care of sending and receiving serial messages 
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialProtocol {  
   
    CommPortSender commPortSender = new CommPortSender();
    CommPortReceiver commPortReceiver = new CommPortReceiver();
    DataParser dataParser = new DataParser();
    
    public boolean connected = false;
  
   
    class CommPortSender {  
        OutputStream out;  
                       
        void setWriterStream(OutputStream out) {  
            this.out = out;  
        }  

        void send(String message){
            //return (message+"\n"+"\r").getBytes();  
            send((message+"\n").getBytes());             
        }

        void send(byte[] bytes) {  
            if (connected==false){
                dataParser.toConsole("Connect first..");
            } else {                     
                //Runnable runnable = () -> { //lambda expression
                    try {
                        // sending through serial port is simply writing into OutputStream
                        out.write(bytes);
                        out.flush();
                    } catch (IOException e) {  
                        dataParser.toConsole(e.toString());
                    }
                //};
                //Thread t = new Thread(runnable);
                //t.start();
                
                //try {
                //    t.join(); //and join thread when sent, otherwise every time new thread is created
                //} catch (InterruptedException ex) {
                //    Logger.getLogger(SerialProtocol.class.getName()).log(Level.SEVERE, null, ex);
                //}
            }           
        }         
    }  

    class CommPortReceiver implements SerialPortEventListener {  
        InputStream in;  
        byte[] buffer = new byte[1024];  
                
        public void setInputStream(InputStream in){  
            this.in = in;  
        }  

        @Override //When there is serial message coming in..
        public void serialEvent(SerialPortEvent spe) {
            
            if (spe.getEventType()== SerialPortEvent.DATA_AVAILABLE){
                                
                //Runnable runnable = () -> {
                    int tail = 0;
                    int b;
                    try {                        
                        // if stream is not bound in.read() method returns -1
                        while((b = in.read()) != -1) {
                            if (b == '\n') break; //end of message
                            buffer[tail] = (byte) b;
                            tail++;   
                        }                        
                    }
                    catch (IOException e) {
                        UART.controller.appendLogWindow(e.toString());
                    }
                    dataParser.parseMsg(new String (buffer,0,tail-1)); //Send received message to data parser //tail-1 to get rid of \r char 
//                };
//                Thread t = new Thread(runnable);
//                t.start();  
                
            }
        }
    }
    
}  