/*
 * WebOSQuickInstallAboutBox.java
 */

package ca.canuckcoding.wosqi;

import ca.canuckcoding.utils.FileUtils;
import ca.canuckcoding.webos.DeviceInfo;
import ca.canuckcoding.webos.WebOSConnection;
import javax.swing.JFileChooser;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Container;
import javax.swing.UIManager;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.io.*;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import java.util.*;
import net.iharder.dnd.*;
import java.util.Timer;
import java.util.ResourceBundle;

public class SendFile extends javax.swing.JDialog {
    private ResourceBundle bundle;
    public Timer t;
    private WebOSConnection webOS;
    private File src;
    private String dest;
    private boolean transferStarted;

    public SendFile(java.awt.Frame parent, WebOSConnection wc) {
        super(parent);
        bundle = WebOSQuickInstallApp.bundle;
        initComponents();
        src = null;
        transferStarted = false;
        webOS = wc;
        t = new Timer();
        new FileDrop(jLayeredPane1, new FileDrop.Listener()
        {
            public void  filesDropped(File[] files )
            {
                src = files[0];
                try {
                    jTextField1.setText(src.getCanonicalPath());
                } catch(Exception e){}
            }
        });
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
        }
    }

    public void startTransfer() {
        if(src!=null && src.exists()) {
            transferStarted = true;
            jButton1.setEnabled(false);
            jButton2.setEnabled(false);
            jButton3.setEnabled(false);
            jTextField2.setEnabled(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            jButton2.setText(bundle.getString("PLEASE_WAIT..."));
            dest = formatDest(jTextField2.getText().trim());
            t.schedule(new DoTransfer(), 200);
        }
    }

    private String formatDest(String path) {
        String result = null;
        result = path.replace("\\", "/");
        if(!result.startsWith("/"))
            result = "/" + result;
        if(!result.endsWith("/"))
            result = result + "/";
        return result;
    }

    public void closeCustTransfer() {
        dispose();
    }

    private void loadFileChooser() {
        JFileChooser fc = new JFileChooser(); //Create a file chooser
        fc.setAcceptAllFileFilterUsed(true);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogTitle("");
        disableNewFolderButton(fc);
        if (fc.showDialog(rootPane, bundle.getString("SELECT"))==
                JFileChooser.APPROVE_OPTION) {
            src = fc.getSelectedFile();
            try {
                jTextField1.setText(src.getCanonicalPath());
            } catch(Exception e){}
        }
    }

    private void disableNewFolderButton(Container c) {
        int len = c.getComponentCount();
        for(int i=0; i<len; i++) {
            Component comp = c.getComponent(i);
            if(comp instanceof JButton) {
                JButton b = (JButton)comp;
                Icon icon = b.getIcon();
                if(icon != null && (icon == UIManager.getIcon("FileChooser.newFolderIcon")
                        || icon == UIManager.getIcon("FileChooser.upFolderIcon")))
                    b.setEnabled(false);
            } else if (comp instanceof Container) {
                disableNewFolderButton((Container)comp);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jButton3 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("SendFile.title")); // NOI18N
        setIconImage(null);
        setModal(true);
        setName("transfer"); // NOI18N
        setResizable(false);

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N

        jLayeredPane2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(192, 192, 192)));
        jLayeredPane2.setName("jLayeredPane2"); // NOI18N

        jButton3.setFont(jButton3.getFont().deriveFont(jButton3.getFont().getSize()+1f));
        jButton3.setText(bundle.getString("SendFile.jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jLayeredPane2.add(jButton3);
        jButton3.setBounds(260, 100, 79, 25);

        jButton2.setFont(jButton2.getFont().deriveFont(jButton2.getFont().getSize()+1f));
        jButton2.setText(bundle.getString("SendFile.jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jLayeredPane2.add(jButton2);
        jButton2.setBounds(110, 100, 140, 25);

        jTextField1.setFont(jTextField1.getFont());
        jTextField1.setEnabled(false);
        jTextField1.setName("jTextField1"); // NOI18N
        jTextField1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField1MouseClicked(evt);
            }
        });
        jLayeredPane2.add(jTextField1);
        jTextField1.setBounds(80, 20, 297, 24);

        jButton1.setFont(jButton1.getFont());
        jButton1.setText(bundle.getString("SendFile.jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jLayeredPane2.add(jButton1);
        jButton1.setBounds(380, 20, 45, 25);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()+3f));
        jLabel1.setText(bundle.getString("SendFile.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLayeredPane2.add(jLabel1);
        jLabel1.setBounds(10, 60, 140, 20);

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()+3f));
        jLabel2.setText(bundle.getString("SendFile.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jLayeredPane2.add(jLabel2);
        jLabel2.setBounds(10, 20, 70, 20);

        jTextField2.setFont(jTextField2.getFont());
        jTextField2.setName("jTextField2"); // NOI18N
        jLayeredPane2.add(jTextField2);
        jTextField2.setBounds(130, 60, 290, 22);

        jLayeredPane1.add(jLayeredPane2);
        jLayeredPane2.setBounds(10, 10, 450, 140);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextField1MouseClicked
        if(!transferStarted)
            loadFileChooser();
    }//GEN-LAST:event_jTextField1MouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        loadFileChooser();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        startTransfer();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        closeCustTransfer();
    }//GEN-LAST:event_jButton3ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables

    class DoTransfer extends TimerTask  {
        public void run() {
            webOS.remountPartitionReadWrite();
            webOS.mkdir(dest);
            if(webOS.sendFile(src, dest + FileUtils.getFilename(src))) {
                JOptionPane.showMessageDialog(rootPane,
                        bundle.getString("FILE_TRANSFERRED_SUCCESSFULLY."));
            } else {
                JOptionPane.showMessageDialog(rootPane,
                        bundle.getString("FILE_TRANSFER_FAILED."));
            }
            jButton1.setEnabled(true);
            jButton2.setEnabled(true);
            jButton3.setEnabled(true);
            jTextField2.setEnabled(true);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dispose();
        }
    }
    class DoDispose extends TimerTask  {
        public void run() {
            dispose();
        }
    }
}
