 package main;

/**
 *
 * @author Olavi
 */

import de.ixxat.vci3.*;
import de.ixxat.vci3.bal.BalFeatures;
import de.ixxat.vci3.bal.BalSocketInfo;
import de.ixxat.vci3.bal.IBalObject;
import de.ixxat.vci3.bal.IBalResource;
import de.ixxat.vci3.bal.can.CanBitrate;
import de.ixxat.vci3.bal.can.CanCapabilities;
import de.ixxat.vci3.bal.can.CanChannelStatus;
import de.ixxat.vci3.bal.can.CanLineStatus;
import de.ixxat.vci3.bal.can.CanMessage;
import de.ixxat.vci3.bal.can.ICanChannel;
import de.ixxat.vci3.bal.can.ICanControl;
import de.ixxat.vci3.bal.can.ICanMessageReader;
import de.ixxat.vci3.bal.can.ICanMessageWriter;
import de.ixxat.vci3.bal.can.ICanScheduler;
import de.ixxat.vci3.bal.can.ICanSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
 
public class IxxatCANbus {
    
    CanParser canParser = new CanParser();
    
    int balancing = 0x00;
    boolean bbalancing = false;
    int reset = 0x00;
    
    
     // Create VCI Server Object
    VciServer         oVciServer      = null;
    IVciDeviceManager oDeviceManager  = null;
    IVciEnumDevice    oVciEnumDevice  = null;
    IVciDevice        oVciDevice      = null;
    IBalObject        oBalObject      = null;
    VciDeviceInfo     aoVciDeviceInfo[] = null;
    
    ICanControl       oCanControl     = null;
    ICanSocket        oCanSocket      = null;
    ICanScheduler     oCanScheduler   = null;
    ICanChannel       oCanChannel     = null;
    ICanMessageReader oCanMsgReader   = null;
    ICanMessageWriter oCanMsgWriter   = null;

    public IxxatCANbus(){
        // Initialize VCI Server Object
        try    {
            // Create VCI Server Object
            oVciServer = new VciServer();

            // Print Version Number
            System.out.println(oVciServer.GetVersion());

            // Open VCI Device Manager
            oDeviceManager = oVciServer.GetDeviceManager();

            // Open VCI Device Enumerator
            oVciEnumDevice = oDeviceManager.EnumDevices();

            System.out.print("Wait for VCI Device Enum Event: ...");
            try
            {
              oVciEnumDevice.WaitFor(3000);
              System.out.println("... change detected!");
            }
            catch(Throwable oException)
            {
              System.out.println("... NO change detected!");
            }

            // Show Device list and count devices
            if(oVciEnumDevice != null)
            {
              boolean       fEndFlag          = false;
              boolean       fCounting         = true;
              int           dwVciDeviceCount  = 0;
              int           dwVciDeviceIndex  = 0;
              VciDeviceInfo oVciDeviceInfo    = null;

              do
              {
                try
                {
                  // Try to get next device
                  oVciDeviceInfo = oVciEnumDevice.Next();
                }
                catch(Throwable oException)
                {
                  // Last device reached?
                  oVciDeviceInfo = null;
                }

                // Device available
                if(oVciDeviceInfo != null)
                {
                  // Do counting only?
                  if(fCounting)
                  {
                    // Print Device Info
                    dwVciDeviceCount++;
                    System.out.println("\nVCI Device: " + dwVciDeviceCount + "\n" + oVciDeviceInfo);
                  }
                  else
                  {
                    if(dwVciDeviceIndex < aoVciDeviceInfo.length)
                      aoVciDeviceInfo[dwVciDeviceIndex++] = oVciDeviceInfo;
                    else
                      throw new IndexOutOfBoundsException("VCI Device list has changed during scan -> ABORT");
                  }
                }
                else
                {
                  // Do counting only?
                  if(fCounting)
                  {
                    //Switch of counting and build device list
                    fCounting = false;

                    // Reset Enum Device Index
                    oVciEnumDevice.Reset();

                    // Build device info list
                    aoVciDeviceInfo = new VciDeviceInfo[dwVciDeviceCount];
                  }
                  else
                  {
                    fEndFlag = true;

                    // Check if device list has changed
                    if(dwVciDeviceIndex != aoVciDeviceInfo.length)
                      throw new IndexOutOfBoundsException("VCI Device list has changed during scan -> ABORT");
                  }
                }
              }while(!fEndFlag);
            }

            // If more than one device ask user
            long lVciId = 0;
            if(aoVciDeviceInfo.length > 1){
              try
              {
                lVciId = oDeviceManager.SelectDeviceDialog();
              }
              catch( Throwable oException)
              {
                System.err.println("IVciDeviceManager.SelectDeviceDialog() failed with error: " + oException);
                System.out.println("Opening first found VCI 3 board instead\n");

                // In case of error try to open the first board
                lVciId = aoVciDeviceInfo[0].m_qwVciObjectId;
              }
            }
            else if (aoVciDeviceInfo.length == 1) {
                lVciId = aoVciDeviceInfo[0].m_qwVciObjectId;
                // Open VCI Device
                oVciDevice = oDeviceManager.OpenDevice(lVciId);
            }

            

            // Get Device Info and Capabilities
            if(oVciDevice != null){
              VciDeviceCapabilities oVciDeviceCaps = null;
              VciDeviceInfo         oVciDeviceInfo = null;

              oVciDeviceCaps = oVciDevice.GetDeviceCaps();
              System.out.println("VCI Device Capabilities: " + oVciDeviceCaps);

              oVciDeviceInfo = oVciDevice.GetDeviceInfo();
              System.out.println("VCI Device Info: " + oVciDeviceInfo);
           
              // Open BAL Object
              oBalObject = oVciDevice.OpenBusAccessLayer();
            }
            // Free VciEnumDevice, DeviceManager and VCI Server which are not longer needed
            oVciEnumDevice.Dispose();
            oVciEnumDevice = null;
            oDeviceManager.Dispose();
            oDeviceManager = null;
            oVciServer.Dispose();
            oVciServer     = null;
   /*
            // Perform Tests
            System.out.println("\n\n Now testing CAN Controller 1");
            TestCan(oBalObject, (short)0);

            /*
            System.out.println("\n\n Now testing CAN Controller 2");
            TestCan(oBalObject, (short)1);
   */
   /*
            System.out.println("\n\n Now testing first LIN Controller");
            TestLin(oBalObject)
   */
        }
        catch(Throwable oException)
        {
        if (oException instanceof VciException)
        {
            VciException oVciException = (VciException) oException;
            System.err.println("VciException: " + oVciException + " => " + oVciException.VciFormatError());
        } 
        else
            System.err.println("Exception: " + oException);
        }
        if(oVciDevice != null) initCan(oBalObject,(short)0);
    }
    
    private void initCan(IBalObject oBalObject, short wSocketNumber){
               
        // Open CAN Control and CAN Channel
        try{
            IBalResource  oBalResource  = null;
            BalFeatures   oBalFeatures  = null;

            System.out.println("Using Socket Number: " + wSocketNumber);

            oBalFeatures = oBalObject.GetFeatures();
            System.out.println("BAL Features: " + oBalFeatures);

            // Socket available?
            if(oBalFeatures.m_wBusSocketCount > wSocketNumber)
            {
              // Ensure CAN Controller Type
              if(oBalFeatures.m_awBusType[wSocketNumber] == VciDeviceCapabilities.VCI_BUS_CAN)
              {
                // Get CAN Control
                try
                {
                  oBalResource  = oBalObject.OpenSocket(wSocketNumber, IBalResource.IID_ICanControl);
                  oCanControl   = (ICanControl)oBalResource;
                  oBalResource  = null;
                }
                catch(Throwable oException)
                {
                  if (oException instanceof VciException)
                  {
                    VciException oVciException = (VciException) oException;
                    System.err.println("Open Socket(IID_ICanControl), VciException: " + oVciException + " => " + oVciException.VciFormatError());
                  } 
                  else
                    System.err.println("Open Socket(IID_ICanControl), exception: " + oException);
                }

                // Get CAN Socket
                try
                {
                  oBalResource  = oBalObject.OpenSocket(wSocketNumber, IBalResource.IID_ICanSocket);
                  oCanSocket    = (ICanSocket)oBalResource;
                  oBalResource  = null;
                }
                catch(Throwable oException)
                {
                  if (oException instanceof VciException)
                  {
                    VciException oVciException = (VciException) oException;
                    System.err.println("Open Socket(IID_ICanSocket), VciException: " + oVciException + " => " + oVciException.VciFormatError());
                  } 
                  else
                    System.err.println("Open Socket(IID_ICanSockets), exception: " + oException);
                }

                // Get CAN Scheduler
                try
                {
                  oBalResource  = oBalObject.OpenSocket(wSocketNumber, IBalResource.IID_ICanScheduler);
                  oCanScheduler = (ICanScheduler)oBalResource;
                  oBalResource  = null;
                }
                catch(Throwable oException)
                {
                  if (oException instanceof VciException)
                  {
                    VciException oVciException = (VciException) oException;
                    System.err.println("Open Socket(IID_ICanScheduler), VciException: " + oVciException + " => " + oVciException.VciFormatError());
                  } 
                  else
                    System.err.println("Open Socket(IID_ICanScheduler), exception: " + oException);
                }
              }
              else
                System.err.println("Socket No. " + wSocketNumber + " is not a \"VCI_BUS_CAN\"");
            }
            else
              System.err.println("Socket No. " + wSocketNumber + " is not a available!");
        }
        catch(Throwable oException)
        {
            if (oException instanceof VciException)
            {
                VciException oVciException = (VciException) oException;
                System.err.println("VciException: " + oVciException + " => " + oVciException.VciFormatError());
            } 
            else
                System.err.println("Exception: " + oException);
        }
        
        try
        {
            // Create CAN Channel
            if(oCanSocket != null)
            {
              BalSocketInfo   oBalSocketInfo    = null;
              CanCapabilities oCanCapabilities  = null;
              CanLineStatus   oCanLineStatus    = null;

              oBalSocketInfo = oCanSocket.GetSocketInfo();
              System.out.println("BAL Socket Info: " + oBalSocketInfo);

              oCanCapabilities = oCanSocket.GetCapabilities();
              System.out.println("CAN Capabilities: " + oCanCapabilities);

              oCanLineStatus = oCanSocket.GetLineStatus();
              System.out.println("CAN Line Status: " + oCanLineStatus);

              System.out.println("Creating CAN Channel");
              oCanChannel = oCanSocket.CreateChannel(false);
            }

            // Configure, start Channel and Query Reader and Writer
            if(oCanChannel != null)
            {
              System.out.println("Initializing CAN Message Channel");
              oCanChannel.Initialize(Short.MAX_VALUE, Short.MAX_VALUE);
              oCanChannel.Activate();

              System.out.println("Query Message Reader");
              oCanMsgReader = oCanChannel.GetMessageReader();

              System.out.println("Query Message Writer");
              oCanMsgWriter = oCanChannel.GetMessageWriter();
            }

            // Configure and start CAN Controller
            if(oCanControl != null)
            {
              CanBitrate        oCanBitrate       = new CanBitrate(CanBitrate.Cia1000KBit);
              CanChannelStatus  oChanStatus       = null;
              CanLineStatus     oLineStatus       = null;

              // Get Line Status
              oLineStatus = oCanControl.GetLineStatus();
              System.out.println("CAN Line Status: " + oLineStatus);

              // Try to detect BaudRate
              try
              {
                CanBitrate[] oaCanBitrateList  = null;
                CanBitrate   oBitrateDetected  = null;
                oaCanBitrateList  = new CanBitrate[]{ 
                    new CanBitrate(CanBitrate.Cia10KBit),
                    new CanBitrate(CanBitrate.Cia20KBit),
                    new CanBitrate(CanBitrate.Cia50KBit),
                    new CanBitrate(CanBitrate.Cia125KBit),
                    new CanBitrate(CanBitrate.Cia250KBit),
                    new CanBitrate(CanBitrate.Cia500KBit),
                    new CanBitrate(CanBitrate.Cia800KBit),
                    new CanBitrate(CanBitrate.Cia1000KBit)};

                // Detect BaudRate, wait 100ms between two messages
                oBitrateDetected = oCanControl.DetectBaud(100, oaCanBitrateList);
                System.out.println("Detected Baudrate: " + oBitrateDetected);

              }
              catch(Throwable oException)
              {
                if (oException instanceof VciException)
                {
                  VciException oVciException = (VciException) oException;
                  System.err.println("DetectBaudrate failed with VciException: " + oVciException + " => " + oVciException.VciFormatError());
                } 
                else
                  System.err.println("DetectBaudrate failed with Exception: " + oException);
              }

              System.out.println("Starting CAN Controller with 1000 kBAUD");
              oCanControl.InitLine(ICanControl.CAN_OPMODE_STANDARD |
                                   ICanControl.CAN_OPMODE_EXTENDED, 
                                   oCanBitrate);

//              // Filter closed completely
//              oCanControl.SetAccFilter(ICanControl.CAN_FILTER_STD, 0xFFFF, 0xFFFF);
//              // Filter opened completely
                oCanControl.SetAccFilter(ICanControl.CAN_FILTER_STD, 0, 0); //data and mask
//                oCanControl.SetAccFilter(ICanControl.CAN_FILTER_STD, 0x00000600, 0x00000000);

//              // Add ID 1
//              oCanControl.AddFilterIds(ICanControl.CAN_FILTER_STD, 1, 0xFFFF);
//              // Remove ID 1
//              oCanControl.RemFilterIds(ICanControl.CAN_FILTER_STD, 1, 0xFFFF);

                //Add filters ID's:0x500-0x5ff
//                oCanControl.AddFilterIds(ICanControl.CAN_FILTER_STD, 0x0500, 0xFF00);


              // Start
              oCanControl.StartLine();

              // Wait for controller
              Thread.sleep(250);

              // Get CAN Channel Status
              oChanStatus = oCanChannel.GetStatus();
              System.out.println("CAN Channel Status: " + oChanStatus);
              System.out.println("CAN Line Status: " + oChanStatus.m_oCanLineStatus);
              System.out.println("");
            }

        }
        catch(Throwable oException)
        {
            if (oException instanceof VciException)
            {
              VciException oVciException = (VciException) oException;
              System.err.println("VciException: " + oVciException + " => " + oVciException.VciFormatError());
            } 
            else
              System.err.println("Exception: " + oException);
        }      
    }
    
    public void canStartUp(){ //starts CAN reader thread and CAN writer thread to ask data        
        
        Thread readCan = new Thread(new canReader());
            readCan.start();
            Main.controller.appendLogWindow("Started listening to CAN..");

        Thread writeCan = new Thread(new canWriter());
            writeCan.start();
            if (Main.controller.bautoPoll) Main.controller.appendLogWindow("Started polling for V and Temp..");
        
        Main.controller.blinkDiod(Main.controller.canning);
    }
    
    class canWriter implements Runnable{     
        int askID = 0x6b1;
        
        @Override
        public void run() {       
            while(Main.controller.canning){
                try {
                    byte[] askData = new byte[]{(byte)balancing, (byte)reset, (byte)0xff, (byte)0xff, (byte)0xff, 0x00, 0x00, 0x00};
                    if(Main.controller.bautoPoll) canWriter(askID, askData);//loop happens here
                    reset = 0x00;
                    
                    updateUI();                    
                    
                    Thread.sleep(Main.controller.aPollTime);                    
                } catch (InterruptedException ex) {
                    Main.controller.appendLogWindow("Err..Try again! "+ ex.toString());
                }            
            }
        }
        
        void updateUI(){ //Ask to update UI
            Main.controller.setProgressBar(canParser.vProgress); //Send new progressbar values to UI    
            Main.controller.setVHint(canParser.voltages); 
            Main.controller.setTemp(canParser.temps); //Send new temps to UI
            Main.controller.setBalIndicator(canParser.bBalance);   
            Main.controller.addDPoint(canParser.dgraph);
        }
    }    
    
    void canWriter(int askID, byte[] askThis){
        CanMessage  oTxCanMsg = new CanMessage();
        long        qwMsgNo   = 0;
        boolean     fEnd      = false;
        boolean     fTimedOut = false;

        // PreSet CAN Message
        oTxCanMsg.m_abData        = askThis;
        oTxCanMsg.m_bDataLength   = 8;
        oTxCanMsg.m_dwIdentifier  = askID;
        oTxCanMsg.m_dwTimestamp   = 0; // No Delay
        oTxCanMsg.m_fExtendedFrameFormat        = false;
        oTxCanMsg.m_fRemoteTransmissionRequest  = false;
        oTxCanMsg.m_fSelfReception              = true;

        try
        {
          // Write CAN Message
          // If WriteMessage fails, it throws an exception
          oCanMsgWriter.WriteMessage(oTxCanMsg);

          qwMsgNo++;
          if(fTimedOut)
          {
            System.out.print("\n");
            fTimedOut = false;
          }
          //System.out.println("No: " + qwMsgNo + " " + oCanMsg); //Scroll Mode
          System.out.print("\rNo: " + qwMsgNo + " " + oTxCanMsg + "  "); //Overwrite Mode

          // Prepare Message ID for next Message
          oTxCanMsg.m_dwIdentifier++;
          if(oTxCanMsg.m_dwIdentifier > 0x7ff)
            oTxCanMsg.m_dwIdentifier = 0;
        }
        catch(Throwable oException)
        {
          //System.out.print("\r" + oException);
          //Wait for empty space in the FIFO
          try 
          {
            oCanMsgWriter.WaitFor(500);
          }
          catch(Throwable oThrowable)
          {
            if(!fTimedOut)
            {
              System.out.print("\n");
              fTimedOut = true;
            }
            System.out.print(".");
          }
        }
    }

    
    class canReader implements Runnable{
        @Override
        public void run() {
            while(Main.controller.canning) canReader();
        }
              
        void canReader(){ //everything is sent to can parser from here

            try{
                CanMessage  oCanMsg   = new CanMessage();
                long        qwMsgNo   = 0;
                //boolean     fEnd      = false;
                boolean     fTimedOut = false;

                //System.out.println("\nPress <Enter> to exit reception mode\n");
                // do
                {
                  try
                  {
                    // Read CAN Message
                    // If ReadMessage fails, it throws an exception
                    oCanMsg = oCanMsgReader.ReadMessage(oCanMsg);

                    qwMsgNo++;
                    if(fTimedOut)
                    {
                      System.out.print("\n");
                      fTimedOut = false;
                    }
                    System.out.println("No: " + qwMsgNo + " " + oCanMsg); // Scroll mode
                    //System.out.print("\rNo: " + qwMsgNo + " " + oCanMsg + "  "); // Overwrite mode
                    int id = oCanMsg.m_dwIdentifier;
                    byte[] data = oCanMsg.m_abData;
                    canParser.parseMsg(id, data); //Send can messager to canParser
                  }
                  catch(Throwable oException)
                  {
                    //System.out.print("\r" + oException);

                    //Wait for a new message in the queue
                    try 
                    {
                      oCanMsgReader.WaitFor(500);
                    }
                    catch(Throwable oThrowable)
                    {
                      if(!fTimedOut)
                      {
                        System.out.print("\n");
                        fTimedOut = true;
                      }
                      System.out.print(".");
                    }
                  }
//                  try
//                  {
//                    // User abort?
//                    if(System.in.available() > 0)
//                    {
//                      while(System.in.available() > 0)
//                        System.in.read();
//                      fEnd = true;
//                    }
//                  }
//                  catch(IOException ioErr)
//                  {
//                    System.err.println("An IO Error occured: " + ioErr);
//                    fEnd = true;
//                  }

                }
                //while(!fEnd);

            }
            catch(Throwable oException)
            {
              if (oException instanceof VciException)
              {
                VciException oVciException = (VciException) oException;
                System.err.println("VciException: " + oVciException + " => " + oVciException.VciFormatError());
              } 
              else
                System.err.println("Exception: " + oException);
            }

        }
    }
    
    public void IxxatClose(){
        
        System.out.println("Cleaning up CAN channels");        
        // Release CAN Message Writer
        try {
            if(oCanMsgWriter != null)
            {              
                oCanMsgWriter.Dispose();
                oCanMsgWriter = null;
            }

            // Dispose CAN Message Reader
            if(oCanMsgReader != null)
            {
                // Release CAN Message Reader
                oCanMsgReader.Dispose();
                oCanMsgReader = null;
            }
        } catch (Throwable ex) {
            Logger.getLogger(IxxatCANbus.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Stop CAN Controller
        if(oCanControl != null)
        {
          try
          { 
            System.out.println("Stopping CAN Controller");
            oCanControl.StopLine();
            oCanControl.ResetLine();
          }
          catch(Throwable oException)
          {
            if (oException instanceof VciException)
            {
              VciException oVciException = (VciException) oException;
              System.err.println("Reset CAN Controller, VciException: " + oVciException + " => " + oVciException.VciFormatError());
            } 
            else
              System.err.println("Reset CAN Controller, Exception: " + oException);
          }
        }  
        
        // release all references
        System.out.println("Cleaning up Interface references to VCI Device");
        try
        {
          oBalObject.Dispose();
        } 
        catch (Throwable oException){} finally
        {
          oBalObject = null;
        }
        try
        {
          oVciDevice.Dispose();
        } 
        catch (Throwable oException){} 
        finally
        {
          oVciDevice = null;
        }

        System.out.println("CAN disconnected");
        
        Main.controller.blinkDiod(false);
        Main.controller.appendLogWindow("CAN disconnected!");
        
    }

    public void toggleBalance(){
        bbalancing = !bbalancing;   
        if(bbalancing) balancing = 0xff;
        else balancing = 0x00;
    }
    
    public void hitReset(){
        reset = 0xff;
    }
    
    public void togglePBarScale(){
        canParser.calcVProgress();
    }
}
