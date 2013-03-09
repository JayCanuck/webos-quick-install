/*
 * WebOSQuickInstallAboutBox.java
 */

package ca.canucksoftware.wosqi;

import ca.canucksoftware.ipkg.PackageFeed;
import ca.canucksoftware.utils.FileUtils;
import ca.canucksoftware.utils.OnlineFile;
import ca.canucksoftware.webos.DeviceInfo;
import ca.canucksoftware.webos.Patcher;
import ca.canucksoftware.webos.ScriptType;
import ca.canucksoftware.webos.WebOSConnection;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;

public class Installer extends javax.swing.JDialog {
    private ResourceBundle bundle;
    private WebOSConnection webOS;
    private Patcher patcher;
    private ArrayList items;
    public Timer t;

    public Installer(java.awt.Frame parent, WebOSConnection connection, ArrayList list) {
        super(parent);
        bundle = WebOSQuickInstallApp.bundle;
        initComponents();
        t = new Timer();
        webOS = connection;
        items = list;
        if(!webOS.isConnected()) {
            DeviceInfo info = webOS.getDeviceInfo();
            if(info!=null && !info.model().equals(DeviceInfo.Model.Unknown.toString())) {
                JOptionPane.showMessageDialog(rootPane, MessageFormat.format(bundle
                        .getString("{0}_IS_DISCONNECTED._PLEASE_RECONNECT_THEN_TRY_AGAIN."),
                        new Object[] {info.model()}));
            } else {
                JOptionPane.showMessageDialog(rootPane, bundle
                        .getString("DEVICE_IS_DISCONNECTED._PLEASE_RECONNECT_THEN_TRY_AGAIN."));
            }
            t.schedule(new DoDispose(), 200);
        } else {
            if(webOS.isSystemBusy()) {
                JOptionPane.showMessageDialog(rootPane,
                        bundle.getString("SYSTEM_IS_BUSY._TRY_AGAIN_LATER."));
                t.schedule(new DoDispose(), 200);
            } else {
                t.schedule(new DoTransfer(), 200);
            }
        }
    }

    private void processList() {
        boolean containsPatch = false;
        boolean okForPatch = false;

        jLabel5.setText(bundle.getString("CONNECTING..."));
        webOS.remountPartitionReadWrite();
        webOS.mkdir("/media/internal/.developer/");
        patcher = new Patcher(webOS);
        webOS.sendScript(ScriptType.ScanID);
        webOS.configIpkg();

        for(int i=0; i<items.size(); i++) {
            Object curr = items.get(i);
            File currFile = null;
            if(curr instanceof String) {
                jLabel5.setText(bundle.getString("DOWNLOADING:"));
                jLabel2.setText("<html><center>" + FileUtils.getFilename((String) curr) +
                        "</center></html>");
                System.out.println("Downloading: " + ((String) curr));
                OnlineFile url = new OnlineFile((String) curr);
                currFile = url.download();
                if(currFile==null) {
                    JOptionPane.showMessageDialog(rootPane, MessageFormat.format(
                        bundle.getString("ERROR:_UNABLE_TO_DOWNLOAD_{0}"),
                        new Object[] {FileUtils.getFilename((String) curr)}));
                }
            } else if(curr instanceof File) {
                currFile = (File) curr;
            }
            if(currFile!=null) {
                String name = FileUtils.getFilename(currFile);
                jLabel2.setText("<html><center>" + name + "</center></html>");
                if(isPatch(name)) {
                    if(!containsPatch) {
                        webOS.sendScript(ScriptType.Patch);
                        PackageFeed wosiFeed = PackageFeed.Download("http://ipkg.preware.org/feeds/webos-internals/all/Packages.gz");
                        int ausmtIndex = wosiFeed.indexOf("org.webosinternals.ausmt");
                        if(ausmtIndex>-1) {
                            okForPatch = patcher.meetsRequirements(wosiFeed.packages.get(ausmtIndex).getDownloadUrl());
                        } else {
                            okForPatch = false;
                        }
                        containsPatch = true;
                    }
                    if(okForPatch) {
                        jLabel5.setText(bundle.getString("TRANSFERRING_AND_APPLYING:"));
                        if(patcher.install(currFile)) {
                            webOS.lunaRestartFlag = true;
                            currFile.delete();
                        }
                    }
                } else {
                    jLabel5.setText(bundle.getString("TRANSFERRING:"));
                    if(webOS.sendFile(currFile, "/media/internal/.developer/" + name)) {
                        jLabel5.setText(bundle.getString("INSTALLING:"));
                        if(webOS.install("/media/internal/.developer/" + name)) {
                            currFile.delete();
                        } else {
                            webOS.delete("/media/internal/.developer/" + name);
                        }
                    } else {
                        JOptionPane.showMessageDialog(rootPane, bundle.getString("FILE_TRANSFER_FAILED."));
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch(Exception e) {}
        }

        webOS.executeRestartFlags();

        webOS.removeScript(ScriptType.ScanID);
        if(containsPatch) {
            webOS.removeScript(ScriptType.Patch);
        }
    }

    private boolean isPatch(String s) {
        return (s.endsWith(".patch") || s.endsWith(".diff"));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLabel5 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setBackground(new java.awt.Color(219, 219, 219));
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        setForeground(new java.awt.Color(219, 219, 219));
        setIconImage(null);
        setModal(true);
        setName("transfer"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jLayeredPane1.setBackground(new java.awt.Color(219, 219, 219));
        jLayeredPane1.setForeground(new java.awt.Color(243, 241, 233));
        jLayeredPane1.setName("jLayeredPane1"); // NOI18N
        jLayeredPane1.setOpaque(true);

        jLabel5.setFont(jLabel5.getFont());
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText(bundle.getString("Installer.jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setBounds(30, 70, 240, 20);
        jLayeredPane1.add(jLabel5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setBackground(new java.awt.Color(219, 219, 219));
        jLabel1.setForeground(new java.awt.Color(219, 219, 219));
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/busy_icon.gif"))); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setOpaque(true);
        jLabel1.setBounds(120, 10, 60, 50);
        jLayeredPane1.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/Pre-48x48.png"))); // NOI18N
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setBounds(170, 10, 60, 50);
        jLayeredPane1.add(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/Computer-48x48.png"))); // NOI18N
        jLabel4.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setBounds(70, 10, 50, 50);
        jLayeredPane1.add(jLabel4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel2.setFont(jLabel2.getFont());
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setBounds(30, 90, 240, 30);
        jLayeredPane1.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened

    }//GEN-LAST:event_formWindowOpened

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
    }//GEN-LAST:event_formWindowActivated

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
    }//GEN-LAST:event_formWindowClosed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLayeredPane jLayeredPane1;
    // End of variables declaration//GEN-END:variables

    class DoTransfer extends TimerTask  {
        public void run() {
            processList();
            dispose();
        }
    }
    class DoDispose extends TimerTask  {
        public void run() {
            dispose();
        }
    }
}
