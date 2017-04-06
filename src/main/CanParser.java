package main;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author Olavi
 */
public class CanParser {
    
    void parseMsg(int id, String msg){ // "Timestamp: 875467195 Flags:      ID: 0x00000294 Data: 0xF8 0x00 0xC0 0x00 0x00 0x00 0xC0 0x00"
        //int idP = msg.indexOf("ID")+3;
        int dataP = msg.indexOf("Data")+5;
        //String idString = msg.substring(idP, dataP-1);
        //byte id[] = idString.getBytes(StandardCharsets.UTF_8);
        String dataString = msg.substring(dataP);
        byte data[] = dataString.getBytes(StandardCharsets.UTF_8);
        
        Main.controller.appendLogWindow(String.valueOf(id)+" & "+ String.valueOf(data));
    }
}
