package main;
//Gets a CAN message with ID to parse.
/**
 *
 * @author Olavi
 */
public class CanParser {
    int nCells = 6*24;//
    int[] temps = new int[72]; //array of temps, 8*6 + 24
    int minVcell = 32000;
    int maxVcell = 42000;
    
    int[] voltages = new int[nCells]; //array to hold voltages
    double[] vProgress = new double[nCells]; //array to hold V progress bar values 
    
   
    void parseMsg(int id, byte[] data){ // "Timestamp: 875467195 Flags:      ID: 0x00000294 Data: 0xF8 0x00 0xC0 0x00 0x00 0x00 0xC0 0x00"
     
        Main.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data));
                
        //Sort message by ID and put voltages into voltage table, temps to temps table
        if (id>=0x61a && id<=0x66f) setVoltages(id, data);
        else if (id>=0x6a1 && id<=0x6ac) setTemps(id, data);
        else if (id == 0x600){ //Info text
            float[] dpoint = new float[5];
                dpoint[0] = concatByte(data, 0);
                dpoint[1] = concatByte(data, 2);
                dpoint[2] = concatByte(data, 4);
                dpoint[3] = data[5];
                dpoint[4] = data[6];   
            
            String maxV = String.format("%.2f",dpoint[0]);//two decimal points
            String minV = String.format("%.2f",dpoint[1]);  
            String avgV = String.format("%.2f",dpoint[2]);  
            String maxT = String.format("%.0f",dpoint[3]);
            String minT = String.format("%.0f",dpoint[4]);
            Main.controller.setInfoText(maxV, minV, avgV, maxT);
        }
        else  if (id == 0x605) ; //Battery V, etc
        
    }
    
    void setVoltages(int id, byte[] data){ //each ID has data of 4 cells
        int vHelper1 = Integer.parseUnsignedInt(String.valueOf(data[0]) + String.valueOf(data[1]));
        int vHelper2 = Integer.parseUnsignedInt(String.valueOf(data[2]) + String.valueOf(data[3]));
        int vHelper3 = Integer.parseUnsignedInt(String.valueOf(data[4]) + String.valueOf(data[5]));
        int vHelper4 = Integer.parseUnsignedInt(String.valueOf(data[6]) + String.valueOf(data[7]));
        
        voltages[(id-0x61a)*4]   = vHelper1;
        voltages[(id-0x61a)*4+1] = vHelper2;
        voltages[(id-0x61a)*4+2] = vHelper3;
        voltages[(id-0x61a)*4+3] = vHelper4;    
        
        calcVProgress(); 
    }
    
    void calcVProgress(){ //re-calculate progress bar values
        for (int i=0; i==nCells; i++){
         vProgress[i] = (voltages[i] - minVcell) / (double)(maxVcell - minVcell);                     
        }
        
        Main.controller.setProgressBar(vProgress); //Send new progressbar values to UI    
    }
    
    void setTemps(int id, byte[] data){ //parse temps table
        for (int i=0; i==7; i++){ //each id holds 8 temp values
            temps[(id-0x6a1)*8 + i] = data[i];
        }
        
        Main.controller.setTemp(temps); //Send new temps to UI
    }
    
    float concatByte(byte[] partB, int pos){ //helper to concatenate Bytes
        int buffer = partB[pos];
        buffer = buffer << 8 | partB[pos+1]; //bitshift left + bitwise inclusive OR
    return (buffer & 0x0000ffff)/10000f; //biwise and bitmask, otherwise FF infront        
    }
    
}
