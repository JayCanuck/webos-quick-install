/*
 * WebOSQuickInstallApp.java
 */

package ca.canuckcoding.wosqi;

import ca.canuckcoding.utils.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class WebOSQuickInstallApp extends SingleFrameApplication {
    
    public static ResourceBundle bundle;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        MainView app = new MainView(this);
        app.getFrame().setResizable(false);
        app.getFrame().setSize(630, 400);
        app.getFrame().setVisible(true);
        //app.mainPanel.requestFocus();
        show(app);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of WebOSQuickInstallApp
     */
    public static WebOSQuickInstallApp getApplication() {
        return Application.getInstance(WebOSQuickInstallApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(org.jdesktop.application.SessionStorage.class.getName());
        logger.setLevel(java.util.logging.Level.OFF);

        String language = Preferences.systemRoot().get("language", null);
        if(language!=null) {
            Locale specific = new Locale(language);
            Locale.setDefault(specific);
            JOptionPane.setDefaultLocale(specific);
            JComponent.setDefaultLocale(specific);
            JFileChooser.setDefaultLocale(specific);
        }
        bundle = ResourceBundle.getBundle("ca/canuckcoding/wosqi/Bundle");
        File install = new File(FileUtils.appDirectory(), bundle.getString("INSTALL"));
        if(!install.isDirectory()) {
            install.mkdirs();
        }
        int start = 0;
        if(args.length>0) {
            if(args[0].equals("-log")) {
                try {
                    PrintStream stream = new PrintStream(new FileOutputStream(new File(
                            FileUtils.appDirectory(), "WOSQI.log"), true));
                    System.setErr(stream);
                    System.setOut(stream);
                } catch (FileNotFoundException e) {}
                start = 1;
            }
        }
        for(int i=start; i<args.length; i++) {
            if (args[i]!=null) {
                File from = new File(args[i]);
                if(from.isFile()) {
                    if(from.getName().toLowerCase().endsWith("ipk")) {
                        File to = new File(install, FileUtils.getFilename(from));
                        try {
                            FileUtils.copy(from, to);
                        } catch (IOException ex) {
                            System.err.println("Unable to copy to install directory: "
                                    + args[i]);
                        }
                    }
                }
            }
        }
        launch(WebOSQuickInstallApp.class, args);
    }
}
