package main;
//Gets a CAN message with ID to parse.

import java.util.Arrays;

/**
 *
 * @author Olavi
 */

public class CanParser {
    int nCells = 6*24;//
    int[] temps = new int[48]; //array of temps, 8*6 + 24?
    int minV = 46080;
    int maxV = 60000;
    int minVcell = 32000;   
    int maxVcell = 42000;
    int minVcellAct = minVcell;
    int maxVcellAct = maxVcell;    
    int maxTCell = 0;
    int minVRow = 0;
    int minCellV = maxVcell;
    
    int[] voltages = new int[nCells]; //array to hold voltages
    double[] dgraph = new double[3]; //datapoints for graph
    double[] vProgress = new double[nCells]; //array to hold V progress bar values 
    boolean[] bBalance = new boolean[nCells]; //inits to false
    boolean[] isMaxTRow = new boolean[48];
     
    void parseMsg(int id, byte[] data){ // "Timestamp: 875467195 Flags:      ID: 0x00000294 Data: 0xF8 0x00 0xC0 0x00 0x00 0x00 0xC0 0x00"
     
        //Main.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data));
                
        //Sort message by ID and put voltages into voltage table, temps to temps table
        if (id>=0x61a && id<=0x66f) setVoltages(id, data);
        
        else if (id>=0x6a1 && id<=0x6a6) setTemps(id, data);
        
        else if (id == 0x6b0) BMSerror(id, data);
        
        else if (id>=0x6b2 && id<=0x6b4) setBalancing(id, data);
        
        else if (id == 0x600){ //Info text
            double[] dpoint = new double[5];
                dpoint[0] = concatByte(data, 0)/10000f;     
                    maxVcellAct = concatByte(data, 0);
                    dgraph[2] = dpoint[0];
                dpoint[1] = concatByte(data, 2)/10000f;
                    minVcellAct = concatByte(data, 2);
                    dgraph[1] = dpoint[1];
                dpoint[2] = concatByte(data, 4)/10000f;
                    dgraph[0] = dpoint[2];
                dpoint[3] = data[6];
                dpoint[4] = data[7];   
                
//                double[] dgraph = {dpoint[2], dpoint[1], dpoint[0]}; //for graph
//                Main17.controller.addDPoint(dgraph);
            //System.out.print("Debug this: "+ dpoint[0] + " "+ dpoint[1]+" "+ dpoint[2]);
            
            String maxVs = String.format("%.2f",dpoint[0]);//two decimal points
            String minVs = String.format("%.2f",dpoint[1]);  
            String avgVs = String.format("%.2f",dpoint[2]);  
            String maxTs = String.format("%.0f",dpoint[3]);
            String minTs = String.format("%.0f",dpoint[4]);
            Main17.controller.setInfoText(maxVs, minVs, avgVs, maxTs);
        }
        else  if (id == 0x605) {//Battery V, etc
            int voltBat = concatByte(data,0);
            calcTProgress(voltBat);
        }
        
    }
    
    void setVoltages(int id, byte[] data){ //each ID has data of 4 cells
        int vBuffer1 = concatByte(data, 0);
        int vBuffer2 = concatByte(data, 2);
        int vBuffer3 = concatByte(data, 4);
        int vBuffer4 = concatByte(data, 6);
        
        int modul = ((id>>4) & 0x0f)-1;
        int row = (id & 0x0f) -10;
                
        voltages[(row*4)  + modul*6*4] = vBuffer1;
        voltages[(row*4+1)+ modul*6*4] = vBuffer2;
        voltages[(row*4+2)+ modul*6*4] = vBuffer3;
        voltages[(row*4+3)+ modul*6*4] = vBuffer4;            
               
        calcVProgress(); 
        
        if(isMinRow(voltages[(row*4)  + modul*6*4])) minVRow = (row*4)  + modul*6*4;
        if(isMinRow(voltages[(row*4+1)  + modul*6*4])) minVRow = (row*4+1)  + modul*6*4;
        if(isMinRow(voltages[(row*4+2)  + modul*6*4])) minVRow = (row*4+2)  + modul*6*4;
        if(isMinRow(voltages[(row*4+3)  + modul*6*4])) minVRow = (row*4+3)  + modul*6*4;
        
        if (id==0x66f) minCellV = maxVcell ; //reset refernce v   
    }
    
    void calcVProgress(){ //re-calculate progress bar values
        int minVcll = minVcell; 
        int maxVcll = maxVcell;
        if (Main17.controller.brescalePBar){
            minVcll = minVcellAct;
            maxVcll = maxVcellAct;
        }
        
        for (int i=0; i<nCells; i++){
            vProgress[i] = (double)(voltages[i] - minVcll) / (maxVcll - minVcll); //casting it to double                    
        }
        
//        Main17.controller.setProgressBar(vProgress); //Send new progressbar values to UI    
//        Main17.controller.setVHint(voltages); 
    }
    
    void setTemps(int id, byte[] data){ //parse temps table
        for (int i=0; i<8; i++){ //each id holds 8 temp values
            temps[(id-0x6a1)*8 + i] = data[i];
            
             //Detect maxTcell
            int row = (id-0x6a1)*8 + i;
            if (temps[row]>= maxTCell){
                maxTCell = temps[row];//to find the highest cell temp
                Arrays.fill(isMaxTRow, false); //reset max row counter
                isMaxTRow[row] = true; //maxTCell row number
            }
        }        
//        Main17.controller.setTemp(temps); //Send new temps to UI
    }
    
    void calcTProgress(int voltBat){ //the round total progress circle
        double tProgress =  (voltBat-minV)/(double)(maxV-minV);
        Main17.controller.setTPogress(tProgress);
        //toConsole(Integer.toString(voltBalan[nCells-1-2][0]));
        
        int tLabel = voltBat/100;
        Main17.controller.settLabel(Integer.toString(tLabel)+"/"+ Integer.toString(maxV/100));
        //toConsole(Integer.toString(tLabel));
    }
    
    void BMSerror(int id, byte[] data) {
        Main17.controller.appendLogWindow("BMS error recieved!");
        Main17.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data[0]));
                
    }
    
    void setBalancing(int id, byte[] data){        
        int row = (id & 0x0f) -1;
        
        for (int bte = 5; bte>=0; bte--){ //loop over 6 bytes in data msg, last 2 bytes are empty
            for (int i=0; i<8; i++){         //loop over 8 bits in one byte
                int bit = (data[bte] >> i) & 1; //takes the last bit from byte
                bBalance[row*(bte*8+8)-i-1] = (bit == 1);
            }            
        }                
//        Main17.controller.setBalIndicator(bBalance);        
    }
    
    int concatByte(byte[] partB, int pos){ //helper to concatenate Bytes
        int buffer = partB[pos];        
        buffer = buffer << 8;
        int buffer2 =  partB[pos+1] & 0x000000ff; //bcs java has no unsigned int!
        buffer = buffer | buffer2; //bitshift left + bitwise inclusive OR        
        buffer &= 0x0000ffff;
    return buffer; //biwise and bitmask, otherwise FF infront        
    }

    boolean isMinRow(int cellV){
        if (cellV<=minCellV) {
           minCellV=cellV;
           return true;
    } else return false;       
    }
}
