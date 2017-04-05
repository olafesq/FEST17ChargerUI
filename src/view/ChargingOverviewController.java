package view;
//This is the JavaFX GUI side of things, JavaFX runs on its own thread
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.StageStyle;
import main.CSVUtils;
import main.UART;


public class ChargingOverviewController {
    int maxT = 35; //over maxT is temp text in red
    
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
           
    private UART uart;
    public boolean bautoPoll = true;
    //ProgressBar helpers
    List<ProgressBar> pBar;
    List<Circle> pBal;
    String defaultBarStyle;
    boolean getStyle = true;   
    int oldRow = 0;
    
    public int aPollTime = 2500; //Default 5sec interval for 2 things
            
    XYChart.Series avVSeries = new XYChart.Series();
    XYChart.Series maxVSeries = new XYChart.Series();
    XYChart.Series minVSeries = new XYChart.Series();
    XYChart.Series tempSeries = new XYChart.Series();
    int xTime = 0;  //Initial chart x-axes value for time
    
    
    @FXML
    private Pane pBarPane;
    @FXML
    private Pane pBalPane;
    @FXML
    private ProgressIndicator tProgress;
    @FXML
    private Label tLabel;
    @FXML
    private ComboBox<String> portCombo;  
    @FXML 
    private TextArea infoText;
    @FXML
    private TextArea log; 
    @FXML
    private Button connect;
    @FXML
    private Button disconnect;
    @FXML
    private Button reset;
    @FXML
    private Button refresh;
    @FXML
    private ToggleButton balance;
    @FXML
    private ToggleButton vent;
    
    //Settings Tab
    @FXML
    private Button send;
    @FXML
    private TextField sCommandField;
    @FXML
    private CheckBox autoPoll;
    @FXML
    private TextField autoPollTime;
    
    //Chart Tab
    @FXML
    private LineChart graafik;
    @FXML
    private Button saveLog;
    @FXML
    private CheckBox autoRange;
    @FXML 
    private NumberAxis xTelg;

    //Handles
    @FXML
    private void handleConnect() throws Exception {
        String selectedPort = (String)portCombo.getValue();   
        uart.connect(selectedPort);        
    }
    
    @FXML
    private void handleDisconnect(){
        uart.disconnect();
    }
    
    @FXML
    private void handleReset(){
        uart.reset();
    }
    
    @FXML
    private void handleRefresh(){
        uart.searchForPorts();
    }
    
    @FXML
    private void handleSend(){
        String customCommand = (String)sCommandField.getText();
        sCommandField.clear();
        uart.sendCommand(customCommand);
    }
    
    @FXML
    private void handleBalance(){
       //boolean selected = vent.selectedProperty().get();
       //vent.setSelected(!selected);
       uart.toggleBalance();
    }
    @FXML
    private void handleVent(){
       uart.toggleVent();
    }
    @FXML
    private void toggleAutoPoll(){        
        bautoPoll = !bautoPoll;
    }
    @FXML
    private void handleAutoPollTime(){
        aPollTime = Integer.valueOf(autoPollTime.getText())/2;
    }
    
    @FXML
    private void handleAutoRange(){
        xTelg.setAutoRanging(!xTelg.isAutoRanging()); //toggle autoRange value
        if (!xTelg.isAutoRanging()) { //if autoRange is off, set back to degault bounds
            xTelg.setUpperBound(4.3);
            xTelg.setLowerBound(3.2);
            xTelg.setTickUnit(0.1);
        } 
    }
    
    @FXML
    private void closeApp(){
        Platform.exit();
    }
    @FXML
    private void about(){   
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(" ");
        alert.setHeaderText("FS Team Tallinn Battery Charging Controller");
        alert.setContentText("      ------Coded by Olavi------\n\n          nov.2016");
             
        alert.show();        
    }
    @FXML
    public void handleSaveLog(){
        DateFormat dateFormat2 = new SimpleDateFormat("dd_MM_yyyy");
        Date date = new Date();
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Voltage Log As..");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("ChargerLog_"+ dateFormat2.format(date));
        File selectedFile = fileChooser.showSaveDialog(saveLog.getScene().getWindow());
        if (selectedFile != null) {
            appendLogWindow(selectedFile.getName());
            saveLog(selectedFile);            
        }
    }    
    
    //Methods//////////////////////////////////////////////////////////////
    private void saveLog(File file) {        
        FileWriter writer = null;
        try {
            List<XYChart.Series> logSeries = new ArrayList<>(graafik.getData()); //series list of avV, minV, maxV

            List<XYChart.Data> avVdata = new ArrayList<>(logSeries.get(0).getData());
            List<XYChart.Data> minVdata = new ArrayList<>(logSeries.get(1).getData());
            List<XYChart.Data> maxVdata = new ArrayList<>(logSeries.get(2).getData());

            writer = new FileWriter(file);

            CSVUtils.writeLine(writer, Arrays.asList(logSeries.get(0).getName(), logSeries.get(1).getName(), logSeries.get(2).getName()));

            for (int i=0; i< avVdata.size(); i++){
                String avV = avVdata.get(i).getYValue().toString();
                String minV= minVdata.get(i).getYValue().toString();
                String maxV= maxVdata.get(i).getYValue().toString();

                CSVUtils.writeLine(writer, Arrays.asList(avV, minV, maxV));
            }   

            writer.flush();
            writer.close();
        } catch (IOException ex) {
            appendLogWindow("Failed to save log!");
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                appendLogWindow("Can't close log!");
            }
        }
    }    
  
    public ComboBox getPortCombo() {
        return portCombo;
    }
    
    public void appendLogWindow(String text) {
        String time = dateFormat.format(new Date()); //crate timestamp
        Platform.runLater(() -> {
            log.appendText("\n"+time+"  ");//timestamp
            //log.appendText("\n");
            log.appendText(text);
        }); 
    }

    public void updatePortCombo(ObservableList<String> options) {
        portCombo.getItems().clear();
        portCombo.setItems(options);
    }

    public void setUart(UART uart) {
        this.uart = uart;
    }
    
    public void setProgressBar(double[] value){
        pBar = getNodesOfType(pBarPane, ProgressBar.class);
        Platform.runLater(() -> {
            for (int i = 0; i < value.length; i++){
                pBar.get(i).setProgress(value[i]);
            }
            pBarPane.setDisable(false); // would be enough to run only once at the beginning but whatever
        }); 
    }
    public void setBalIndicator(boolean[] value){
        pBal = getNodesOfType(pBalPane, Circle.class);
        Platform.runLater(() -> {
            for (int i = 0; i < value.length; i++){                
                if(value[i]) pBal.get(i).setVisible(true);
                else pBal.get(i).setVisible(false); 
            }  
        }); 
    }
    
    public void setTPogress(double progress){
        Platform.runLater(() -> {
        tProgress.setProgress(progress);
        });
    }
    
    public void settLabel(String string){
        Platform.runLater(() -> {
            tLabel.setText(string);
        });
    }
    
    public void addDPoint(double[] dpoint){
        Platform.runLater(() -> {
           
            avVSeries.getData().add(new XYChart.Data(xTime,dpoint[0]));
            minVSeries.getData().add(new XYChart.Data(xTime,dpoint[1]));
            maxVSeries.getData().add(new XYChart.Data(xTime,dpoint[2]));
           
            if (xTime==0) { 
                avVSeries.setName("average Cell Voltage");        
                minVSeries.setName("min Cell Voltage");
                maxVSeries.setName("max Cell Voltage");
            
                graafik.getData().addAll(avVSeries, minVSeries, maxVSeries);
            } else graafik.getData().retainAll(avVSeries, minVSeries, maxVSeries); //This is needed for not to get duplucate series added error.
            
            xTime++;            
        });        
    }
    
    public void setTemp(int[] temp){
        List<Label> pTemp = getNodesOfType(pBarPane, Label.class);
        Platform.runLater(() -> { //Only FX thread can update UI text label
            for (int i = 0; i < temp.length-3; i++){
                pTemp.get(i).setText(Integer.toString(temp[i])+" C");
                if (temp[i]>=maxT) pTemp.get(i).setStyle("-fx-text-fill: red"); //highlights all cell above maxT
                else pTemp.get(i).setStyle("-fx-text-fill: black");                
            }
        });
    }
    
    public void setMaxTempColor(boolean[] highlight){ //Highlight readings with highest temps
        List<Label> pTemp = getNodesOfType(pBarPane, Label.class);
        Platform.runLater(() -> { //Only FX thread can update UI text label
            for (int i = 0; i < highlight.length; i++){
                if (highlight[i]) pTemp.get(i).setStyle("-fx-text-fill: red");
                //else pTemp.get(i).setStyle("-fx-text-fill: black");    //setTemp already resets highlight color            
            }
        });
    }
    
    //This is a neat trick to get all elements in pane into a list
    private <T> List<T> getNodesOfType(Pane parent, Class<T> type) {
        List<T> elements = new ArrayList<>();
        for (Node node : parent.getChildren()) {
            if (node instanceof Pane) {
                elements.addAll(getNodesOfType((Pane) node, type));
            } else if (type.isAssignableFrom(node.getClass())) {
                //noinspection unchecked
                elements.add((T) node);
            }
        }
        return Collections.unmodifiableList(elements);
    }
    
    public void setInfoText(String maxV, String minV, String avgV, String maxT){
       infoText.setText("max V:\t"+maxV+"\nmin V:\t"+ minV+"\navg V:\t"+ avgV+"\nmax T:\t"+ maxT + " C");
    }
    
    public void setMinVCellColor(int row){
        Platform.runLater(() -> {
//            if(getStyle) { //to know the default color
//                defaultBarStyle = pBar.get(row).getStyle();
//                getStyle = false;
//                oldRow = row;
//            }            
            //pBar.get(oldRow).setStyle(defaultBarStyle);
            pBar.get(oldRow).setStyle(null);
            pBar.get(row).setStyle("-fx-accent: red");
            oldRow = row;
        }); 
    }
    
    public void setVHint(int [][] volts){ //show cell V when hovering over it useing Tooltip class      
        pBar = getNodesOfType(pBarPane, ProgressBar.class);
        List<Tooltip> hints = new ArrayList<>(pBar.size()); //crate list for all tooltips
        
        Platform.runLater(() -> {
            for (int i = 0; i < pBar.size(); i++){
                hints.add(new Tooltip()); //every loop adds new tooltip element to the list
                hints.get(i).setText(String.valueOf((double)volts[i][0]/10000)); //set the tooltip value
                pBar.get(i).setTooltip(hints.get(i)); //attach tooltip to the progress bar element
            }            
        }); 
    }
    
}