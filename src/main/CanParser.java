package main;
//Gets a CAN message with ID to parse.
/**
 *
 * @author Olavi
 */

public class CanParser {
    int nCells = 6*24;//
    int[] temps = new int[72]; //array of temps, 8*6 + 24
    int minV = 4480000;
    int maxV = 5880000;
    int minVcell = 32000;
    int minVcellAct = 32000;
    int maxVcell = 42000;
         
    int[] voltages = new int[nCells]; //array to hold voltages
    double[] vProgress = new double[nCells]; //array to hold V progress bar values 
   
    void parseMsg(int id, byte[] data){ // "Timestamp: 875467195 Flags:      ID: 0x00000294 Data: 0xF8 0x00 0xC0 0x00 0x00 0x00 0xC0 0x00"
     
        //Main.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data));
                
        //Sort message by ID and put voltages into voltage table, temps to temps table
        if (id>=0x61a & id<=0x66f) setVoltages(id, data);
        
        else if (id>=0x6a1 & id<=0x6ac) setTemps(id, data);
        
        else if (id == 0x6b0) BMSerror(id, data);
        
        else if (id == 0x600){ //Info text
            float[] dpoint = new float[5];
                dpoint[0] = concatByte(data, 0)/10000f;                    
                dpoint[1] = concatByte(data, 2)/10000f;
                    minVcellAct = concatByte(data, 2);
                dpoint[2] = concatByte(data, 4)/10000f;
                dpoint[3] = data[6];
                dpoint[4] = data[7];   
           
            //System.out.print("Debug this: "+ dpoint[0] + " "+ dpoint[1]+" "+ dpoint[2]);
            
            String maxV = String.format("%.2f",dpoint[0]);//two decimal points
            String minV = String.format("%.2f",dpoint[1]);  
            String avgV = String.format("%.2f",dpoint[2]);  
            String maxT = String.format("%.0f",dpoint[3]);
            String minT = String.format("%.0f",dpoint[4]);
            Main.controller.setInfoText(maxV, minV, avgV, maxT);
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
        
        voltages[(id-0x61a)*4]   = vBuffer1;
        voltages[(id-0x61a)*4+1] = vBuffer2;
        voltages[(id-0x61a)*4+2] = vBuffer3;
        voltages[(id-0x61a)*4+3] = vBuffer4;            
        
        calcVProgress(); 
    }
    
    void calcVProgress(){ //re-calculate progress bar values
        int minVcll = minVcell; 
        if (Main.controller.brescalePBar) minVcll = minVcellAct;
        
        for (int i=0; i<nCells; i++){
            vProgress[i] = (double)(voltages[i] - minVcll) / (maxVcell - minVcll); //casting it to double                    
        }
        
        Main.controller.setProgressBar(vProgress); //Send new progressbar values to UI    
    }
    
    void setTemps(int id, byte[] data){ //parse temps table
        for (int i=0; i<8; i++){ //each id holds 8 temp values
            temps[(id-0x6a1)*8 + i] = data[i];
        }
        
        Main.controller.setTemp(temps); //Send new temps to UI
    }
    
    void calcTProgress(int voltBat){ //the round total progress circle
        double tProgress =  (voltBat-minV)/(double)(maxV-minV);
        Main.controller.setTPogress(tProgress);
        //toConsole(Integer.toString(voltBalan[nCells-1-2][0]));
        
        int tLabel = voltBat/10000;
        Main.controller.settLabel(Integer.toString(tLabel)+"/"+ Integer.toString(maxV/10000));
        //toConsole(Integer.toString(tLabel));
    }
    
    void BMSerror(int id, byte[] data) {
        Main.controller.appendLogWindow("BMS error recieved!");
        Main.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data[0]));
                
    }
    
    int concatByte(byte[] partB, int pos){ //helper to concatenate Bytes
        int buffer = partB[pos];        
        buffer = buffer << 8;
        int buffer2 =  partB[pos+1] & 0x000000ff; //bcs java has no unsigned int!
        buffer = buffer | buffer2; //bitshift left + bitwise inclusive OR        
        buffer &= 0x0000ffff;
    return buffer; //biwise and bitmask, otherwise FF infront        
    }
}
