package main;
//This main class start the JavaFX GUI, and calls to check for available ports
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import view.ChargingOverviewController;

public class Main extends Application {
    
//    public static void main(String[] args) { //Redundant
//        launch(args);
//    }
       
    public Stage primaryStage;
    private BorderPane rootLayout;
    
    private ChargingOverviewController controller;
    private UART uart;

    @Override //This is automatically called when charger window is closed..
    public void stop() throws Exception {
        System.out.println("shutting down charger application..");
        uart.disconnect();    
    }
    
    @Override
    public void start(Stage primaryStage) {
        
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date date = new Date();
        
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("FS Team Tallinn Charging Overview");
        this.primaryStage.getIcons().add(new Image("/Images/FS_ikoon2.png"));
        this.primaryStage.setMaximized(true);
        
        setUserAgentStylesheet(STYLESHEET_MODENA);
        //initRootLayout();
        showCharging();
        
        controller.appendLogWindow("Date: "+dateFormat.format(date)); //datestamp
        controller.appendLogWindow("Console opened... Select port and Connect.");
        
        uart = new UART(controller);
        controller.setUart(uart);
        uart.searchForPorts();
        
//        for debugging only
//        int[][] volts = new int[144][2];
//        controller.setVHint(volts);
                           
//        //close ports on exit //Java FX calls stop()
//        Runtime.getRuntime().addShutdownHook(new Thread(){
//            @Override
//            public void run(){
//                System.out.println("shutting down..");
//                uart.disconnect();
//            }
//        });
    }
    
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("/view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();
            
            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    public void showCharging() {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("/view/ChargingOverview.fxml"));
            AnchorPane chargerOverview = (AnchorPane) loader.load();
            
            // Show the scene containing the root layout.
            Scene scene = new Scene(chargerOverview);
            primaryStage.setScene(scene);
            primaryStage.show();
            
            // Set charger overview into the center of root layout.
            //rootLayout.setCenter(chargerOverview);
            
            // Give the controller access to the main app.
            controller = loader.getController();
            //controller.setMain(this);

        } catch (IOException e) {
            e.printStackTrace();
        }    
    }
   
}
    
    
    
    
    

    
    
    