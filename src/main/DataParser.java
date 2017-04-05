package main;
//This class gets the incoming serial message, looks for keywords and takes actions accordingly.

import java.util.Arrays;

public class DataParser {
    int nCells = 14*10+4;//14*10; //How many cells in battery pack //+4 for averaga Voltage, sumVoltage, minV, maxV 
    int nTemp = 5*10+3; //5*10; Number of temp sensors //+3 for avertage temp, minT, max T
    int maxV = 5880000;
    int maxVcell = 42000;
    int minV = 4480000;
    int minVcell = 32000;
    int maxT;
    int minCellV = maxVcell; //helper to find minV cell row
    int minVRow = 0;
    boolean[] bBalance = new boolean[nCells-4]; //boolean default is FALSE
    boolean[] isMaxTRow = new boolean[nTemp-3];
    private boolean bvoltages = false;
    private boolean btemp = false;
    int [][] voltBalan = new int[nCells][2]; //default element value for int matrix is zero
    int[] temp = new int[nTemp];
    int row = 0;
    int maxTCell = 0;
    int error = 0; //count errors in messages
        
    public void parseMsg(String message){
        //toConsole(message); //mirror everything to the console //this will overflow java fx console window
               
        if (bvoltages) voltages(message);
        else if (btemp) temps(message);
                
        else {
            row = 0;
            switch(message){
                case "Voltages":
                    //UART.controller.appendLogWindow("now its V!");
                    bvoltages = true; //set flag                    
                    break;
                case "Temperatures":
                    //toConsole("now its temp");
                    btemp = true;                    
                    break;
                case "-te": //filter -te ja -out out
                    break;
                case "-out": 
                    break;
                case "-cal":
                    break;
                case "-cfgw":
                    break;
                case "-rst":
                    toConsole("Previous session had "+String.valueOf(error)+" connection errors.");
                    toConsole("Reset done!");
                    error=0; //reset error counter
                    break;
                default: toConsole(message);
            }
        }
    }
    
    public void toConsole(String string){
        UART.controller.appendLogWindow(string);
    }
    public void temps(String message){
        try {
            if (Character.isDigit(message.charAt(0))){
                temp[row] = (Integer.valueOf(message)); //collect temps
                
                if (row<nTemp-3 & temp[row]>= maxTCell) { //find highest temp cell rows
                    maxTCell = 0; //reset max cell temp 
                    Arrays.fill(isMaxTRow, false); //reset max row counter
                    
                    maxTCell = temp[row];//to find the highest cell temp
                    isMaxTRow[row] = true; //maxTCell row number
                } 
                
                row++;
                
                if (row==nTemp){  //detect last row and send data to UI thread
                    btemp=false;
                    UART.controller.setTemp(temp);
                    UART.controller.setMaxTempColor(isMaxTRow);
                    maxT = temp[nTemp-1]; //last element is maxT                   
                    //toConsole(temp.toString());
                }
            }
                        
        } catch (NumberFormatException e){
            toConsole(e.toString());
            error++;
            btemp=false; //on error drop out from temps
        }
    }        
    
    public void voltages(String message){ //method where voltage data and balancing boolean is put into matrix
        String[] splitted = message.split(" "); //Split message into array, based on _
        //UART.controller.appendLogWindow(splitted[1]);
        if(Character.isDigit(splitted[0].charAt(0))){ //Check if first character of first string is a digit
            try {
                if (row>=nCells-4) voltBalan [row][0] = (Integer.valueOf(splitted[0]));
                else {
                    voltBalan [row][0] = (Integer.valueOf(splitted[0]));
                    voltBalan [row][1] = (Integer.valueOf(splitted[1])); //not using that really..
                    bBalance[row] = false; //reset balancing boolean
                    if(Integer.valueOf(splitted[1])==1) bBalance[row] = true; //balancing or not?
                    if(isMinRow(voltBalan[row][0])) minVRow = row; //find what cell is minV
                }
                //UART.controller.appendLogWindow("this is voltage "+voltBalan[row][0]+voltBalan[row][1]);

                row++;
            } catch (NumberFormatException e){
                toConsole(e.toString());
                row++; //increase row on error
                error++; //count errors
            }
        }       
            
        if (row==nCells) {                  
            bvoltages = false; //reset flag when last row arrived   
            calcProgress(voltBalan);
            calcTProgress(voltBalan);
            UART.controller.setVHint(voltBalan); 
            UART.controller.setMinVCellColor(minVRow);
            UART.controller.setBalIndicator(bBalance);
            //UART.controller.appendLogWindow(String.valueOf(bBalance[0]));
            minCellV = maxVcell ; //reset refernce v            
        }         
    }
    
    boolean isMinRow(int cellV){
        if (cellV<=minCellV) {
           minCellV=cellV;
           return true;
        } else return false;       
    }
    
    void calcTProgress(int [][] voltBalan){ //the round total progress circle
        double tProgress =  (voltBalan[nCells-1-2][0]-minV)/(double)(maxV-minV);
        UART.controller.setTPogress(tProgress);
        //toConsole(Integer.toString(voltBalan[nCells-1-2][0]));
        
        int tLabel = voltBalan[nCells-1-2][0]/10000;
        UART.controller.settLabel(Integer.toString(tLabel)+"/"+ Integer.toString(maxV/10000));
        //toConsole(Integer.toString(tLabel));
    }
    
    void calcProgress(int [][] voltBalan){
        double[] progress = new double[nCells-4];
 
        for (int i=0; i<nCells-4; i++){               
            progress[i] = (voltBalan[i][0]-minVcell)/(double)(maxVcell-minVcell) ;     
            //toConsole(Double.toString(progress[i]));
        }
        UART.controller.setProgressBar(progress);
        
        //Datapoints for graph & info text area
        double[] dpoint = new double[3];
        dpoint[0] = (double)voltBalan[nCells-1-4][0]/10000; //avV
        dpoint[1] = (double)voltBalan[nCells-1-1][0]/10000; //minV
        dpoint[2] = (double)voltBalan[nCells-1][0]/10000;   //maxV
        
        UART.controller.addDPoint(dpoint);
        
        UART.controller.setInfoText(String.format("%.2f",dpoint[2]), String.format("%.2f",dpoint[1]),  
                String.format("%.2f",dpoint[0]),  Integer.toString(maxT)); //two decimal points
    }
    
}
