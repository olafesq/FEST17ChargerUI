package main;

/**
 *
 * @author Olavi
 */
public class CanParser {
    
    void parseMsg(int id, byte[] data){ // "Timestamp: 875467195 Flags:      ID: 0x00000294 Data: 0xF8 0x00 0xC0 0x00 0x00 0x00 0xC0 0x00"
        //int idP = msg.indexOf("ID")+3;
        //int dataP = msg.indexOf("Data")+6;
        //String idString = msg.substring(idP, dataP-1);
        //byte id[] = idString.getBytes(StandardCharsets.UTF_8);
        //String dataString = msg.substring(dataP);
        //byte data1[] = dataString.getBytes(StandardCharsets.UTF_8); //parse data into byte array
        //byte data2[] = DatatypeConverter.parseHexBinary(dataString); //parse data into byte array
        
        Main.controller.appendLogWindow(String.valueOf(id)+" & data: "+ String.valueOf(data));
        //Main.controller.setProgressBar(value);
    }
}
