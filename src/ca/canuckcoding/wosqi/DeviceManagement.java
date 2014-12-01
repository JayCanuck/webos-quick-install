/*
 * WebOSQuickInstallAboutBox.java
 */

package ca.canuckcoding.wosqi;

import ca.canuckcoding.webos.InstalledEntry;
import ca.canuckcoding.webos.DeviceInfo;
import ca.canuckcoding.webos.WebOSConnection;
import java.awt.Color;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import org.jdesktop.application.Action;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import java.util.TimerTask;
import java.util.Timer;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.json.JSONArray;
import org.json.JSONObject;

public class DeviceManagement extends javax.swing.JDialog {
    private ResourceBundle bundle;
    private WebOSConnection webOS;
    private DefaultTableModel modTable;
    private JSONArray apps;
    private ArrayList<InstalledEntry> pkgs;
    private ArrayList<InstalledEntry> filtered;
    private Timer t;

    public DeviceManagement(java.awt.Frame parent, WebOSConnection connection) {
        super(parent);
        bundle = WebOSQuickInstallApp.bundle;
        initComponents();
        webOS = connection;
        apps = null;
        pkgs = null;
        filtered = new ArrayList<InstalledEntry>();
        jTable1.setAutoCreateColumnsFromModel(false);
        modTable = (DefaultTableModel) jTable1.getModel();
        modTable.addRow(new Object[] {bundle.getString("LOADING_LIST..."),Boolean.FALSE});
        this.getContentPane().requestFocus();
        t = new Timer();
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
            t.schedule(new DoLoadDetails(), 100);
            webOS.configPkgMgr();
        }
    }


    private void loadDeviceDetails() {
        DeviceInfo info = webOS.getDeviceInfo();
        URL imgURL;
        if(info.model().equals(DeviceInfo.Model.Emulator.toString())) {
            imgURL = getClass().getResource("resources/Emulator 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.Palm_Pixi.toString()) ||
                info.model().equals(DeviceInfo.Model.Palm_Pixi_Plus.toString())) {
            imgURL = getClass().getResource("resources/Palm Pixi 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.Palm_Pre.toString()) ||
                info.model().equals(DeviceInfo.Model.Palm_Pre_Plus.toString())) {
            imgURL = getClass().getResource("resources/Palm Pre 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.Palm_Pre_2.toString())) {
            imgURL = getClass().getResource("resources/Palm Pre 2 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.HP_Veer.toString())) {
            imgURL = getClass().getResource("resources/HP Veer 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.HP_TouchPad.toString())) {
            imgURL = getClass().getResource("resources/HP TouchPad 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.HP_Pre_3.toString())) {
            imgURL = getClass().getResource("resources/HP Pre 3 64x64.png");
        } else if(info.model().equals(DeviceInfo.Model.Samsung_Galaxy_Nexus.toString())
                || info.model().equals(DeviceInfo.Model.LG_Nexus_4.toString())
                || info.model().equals(DeviceInfo.Model.Asus_Nexus_7.toString())) {
            imgURL = getClass().getResource("resources/LuneOS 64x64.png");
        } else { //unknown/Open webOS generic
            imgURL = getClass().getResource("resources/Open webOS 64x64.png");
        }
        jLabel7.setIcon(new ImageIcon(imgURL));
        jLabel2.setText(bundle.getString("DEVICE:") + "    " + info.name());
        jLabel1.setText(bundle.getString("OS:") + "    " + info.os());
        jLabel5.setText(bundle.getString("ARCHITECTURE:") + "    " + info.arch());
        jLabel10.setText(bundle.getString("BUILD_NAME:") + "    " + info.buildName());
        jLabel4.setText(bundle.getString("BUILD_TIME:") + "    " + info.buildTime());

        if(!info.model().equals(DeviceInfo.Model.Emulator.toString())) {
            try {
                String out = webOS.runProgram("/bin/grep", new String[] {"-e", "Processor",
                        "-e", "BogoMIPS", "-e", "Hardware", "/proc/cpuinfo"});
                String[] tokens = out.split("\n");
                for(int i=0; i<tokens.length; i++) {
                    if(tokens[i].startsWith("Processor")) {
                        jLabel3.setText(bundle.getString("CPU_TYPE:") + "    " +
                                tokens[i].substring(tokens[i].indexOf(":")+2));
                    } else if(tokens[i].startsWith("BogoMIPS")) {
                        jLabel9.setText(bundle.getString("CPU_SPEED:") + "    " +
                                tokens[i].substring(tokens[i].indexOf(":")+2) + " MHz");
                    } else if(tokens[i].startsWith("Hardware")) {
                        jLabel8.setText(bundle.getString("CPU_HARDWARE:") + "    " +
                                tokens[i].substring(tokens[i].indexOf(":")+2));
                    }
                }
            } catch(Exception e) {
                System.err.println("Unable to get CPU info");
            }

            try {
                String out = webOS.runProgram("/usr/bin/PmModemInfo", new String[] {"-e"});
                String[] tokens = out.split("\n");
                jLabel12.setText(bundle.getString("MODEM_TYPE:") + "    " +
                        tokens[0].substring(tokens[0].indexOf("=")+1));
                jLabel13.setText(bundle.getString("MODEM_FIRMWARE:") + "    " +
                        tokens[1].substring(tokens[1].indexOf("=")+1));
                jLabel11.setText(tokens[2].substring(1,5) + ":    " + tokens[2].substring(6));
            } catch(Exception e) {
                System.err.println("Unable to get modem info");
            }
        }
        t.schedule(new DoLoadPackages(), 100);
    }

    private void loadPackages() {
        if(pkgs==null) {
            try {
                pkgs = webOS.listInstalled();
                apps = getApps();
                for(int i=0; i<pkgs.size(); i++) {
                    InstalledEntry curr = pkgs.get(i);
                    String name = curr.getName();
                    if(name.equals("This is a webOS application.") || name.equals("Unknown")) {
                        if(apps!=null) {
                            JSONObject json = getAppInfoFromId(curr.getId());
                            if(json!=null) {
                                try {
                                    curr.setName(json.getString("title"));
                                    curr.setDeveloper(json.getString("vendor"));
                                } catch(Exception e) {}
                            } else {
                                curr.setName(curr.getName(webOS));
                            }
                        } else {
                            curr.setName(curr.getName(webOS));
                        }
                    }
                }
            } catch(StackOverflowError e) {
                System.err.println(e.getMessage());
            }
        }
        filtered.clear();
        while(modTable.getRowCount()>0) {
            modTable.removeRow(0);
        }
        for(int i=0; i<pkgs.size(); i++) {
            InstalledEntry curr = pkgs.get(i);
            if((jComboBox1.getSelectedIndex()==1)==
                    (curr.getId().startsWith("ca.canuckcoding.patches.") ||
                    curr.getId().startsWith("ca.canucksoftware.patches.") ||
                    curr.getId().startsWith("org.webosinternals.patches."))) {
                filtered.add(curr);
                modTable.addRow(new Object[] {curr.getName(), Boolean.FALSE});
            }
        }
    }

    private JSONArray getApps() {
        JSONArray results = null;
        try {
            String out = webOS.runProgram("/usr/bin/luna-send", new String[] {"-n", "1",
                    "palm://com.palm.applicationManager/listApps", "{}"});
            int leftBrace = out.indexOf("{");
            int rightBrace = out.lastIndexOf("}");
            if(leftBrace>-1 && rightBrace>-1) {
                out = out.substring(leftBrace, rightBrace+1);
                results = new JSONObject(out).getJSONArray("apps");
            }
        } catch(Exception e) {
            System.err.println("Unable to get apps list from luna-send: " +
                    e.getMessage());
        }
        return results;
    }

    private JSONObject getAppInfoFromId(String appid) {
        JSONObject result = null;
        for(int i=0; i<apps.length(); i++) {
            try {
                JSONObject curr = apps.getJSONObject(i);
                if(curr.getString("id").equals(appid)) {
                    result = curr;
                    break;
                }
            } catch(Exception e) {}
        }
        return result;
    }

    public void uninstall() {
        webOS.remountPartitionReadWrite();
        for(int i=0; i<modTable.getRowCount(); i++) {
            if(modTable.getValueAt(i, 1)==Boolean.TRUE) {
                InstalledEntry curr = filtered.get(i);
                ArrayList<String> warn = webOS.whatDependsRecursive(curr.getId());
                if(warn.size()>0) {
                    String warnMsg = "<html><body width=\"300px\">" +
                            MessageFormat.format(bundle
                            .getString("DEPENDANT_PACKAGES_EXIST_WARNING"),
                            new Object[] {modTable.getValueAt(i, 0)}) + "\n";
                    for(int j=0; j<warn.size(); j++) {
                        warnMsg += "\n" + pkgs.get(indexOfPackage(warn.get(j))).getName();
                    }
                    if(JOptionPane.showConfirmDialog(rootPane, warnMsg,
                            bundle.getString("WARNING"), JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION) {
                        uninstallPackage(curr);
                        uninstall();
                        break;
                    } else {
                        break;
                    }
                } else {
                    if(uninstallPackage(curr)) {
                        i--;
                    }
                }
            }
        }
        webOS.executeRestartFlags();
    }

    public int indexOfPackage(String appid) {
        int result = -1;
        for(int i=0; i<pkgs.size(); i++) {
              if(pkgs.get(i).getId().equals(appid)) {
                result = i;
                break;
            }
        }
        return result;
    }

    public int indexOfFiltered(String appid) {
        int result = -1;
        for(int i=0; i<filtered.size(); i++) {
              if(filtered.get(i).getId().equals(appid)) {
                result = i;
                break;
            }
        }
        return result;
    }

    public boolean uninstallPackage(InstalledEntry entry) {
        InstalledEntry curr = entry;
        boolean continueOn = true;
        ArrayList<String> depends = webOS.whatDepends(entry.getId());
        if(depends.size()>0) {
            for(int i=0; i<depends.size(); i++) {
                int index = indexOfPackage(depends.get(i));
                if(index>-1) {
                    continueOn &= uninstallPackage(pkgs.get(index));
                    if(!continueOn) {
                        break;
                    }
                }
            }
        }
        if(continueOn && webOS.uninstall(curr.getId())) {
            pkgs.remove(curr);
            int index = indexOfFiltered(curr.getId());
            if(index>-1) {
                modTable.removeRow(index);
                filtered.remove(index);
            }
            continueOn = true;
        } else {
            continueOn = false;
        }
        return continueOn;
    }


    @Action
    public void closeManagement() {
        dispose();
    }

    private void setState(boolean enable) {
        jComboBox1.setEnabled(enable);
        jButton1.setEnabled(enable);
        jTable1.setEnabled(enable);
        if(enable) {
            jTable1.setBackground(new Color(255, 255, 255));
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        } else {
            jTable1.setBackground(new Color(230, 230, 230));
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
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
        jLayeredPane3 = new javax.swing.JLayeredPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jComboBox1 = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jLabel7 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("MainView.jMenuItem5.text")); // NOI18N
        setModal(true);
        setName("aboutBox"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N
        jLayeredPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLayeredPane1MouseClicked(evt);
            }
        });
        jLayeredPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jLayeredPane1KeyTyped(evt);
            }
        });

        jLayeredPane3.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("DeviceManagement.jLayeredPane3.border.title"))); // NOI18N
        jLayeredPane3.setName("jLayeredPane3"); // NOI18N
        jLayeredPane3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLayeredPane3MouseClicked(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setFillsViewportHeight(true);
        jTable1.setFocusable(false);
        jTable1.setGridColor(new java.awt.Color(192, 192, 192));
        jTable1.setName("jTable1"); // NOI18N
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.setShowVerticalLines(false);
        jTable1.getTableHeader().setResizingAllowed(false);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTable1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTable1MouseReleased(evt);
            }
        });
        jTable1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTable1KeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);
        jTable1.getColumnModel().getColumn(0).setResizable(false);
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(250);
        jTable1.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("DeviceManagement.jTable1.columnModel.title0")); // NOI18N
        jTable1.getColumnModel().getColumn(1).setResizable(false);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(50);
        jTable1.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("DeviceManagement.jTable1.columnModel.title1")); // NOI18N

        jScrollPane1.setBounds(20, 50, 240, 260);
        jLayeredPane3.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Packages", "Patches" }));
        jComboBox1.setFocusable(false);
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jComboBox1.setBounds(154, 20, 100, 20);
        jLayeredPane3.add(jComboBox1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(ca.canuckcoding.wosqi.WebOSQuickInstallApp.class).getContext().getActionMap(DeviceManagement.class, this);
        jButton1.setAction(actionMap.get("uninstall")); // NOI18N
        jButton1.setText(bundle.getString("DeviceManagement.jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(150, 320, 110, 23);
        jLayeredPane3.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane3.setBounds(320, 10, 280, 360);
        jLayeredPane1.add(jLayeredPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane2.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("DeviceManagement.jLayeredPane2.border.title"))); // NOI18N
        jLayeredPane2.setName("jLayeredPane2"); // NOI18N
        jLayeredPane2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLayeredPane2MouseClicked(evt);
            }
        });

        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel7.setName("jLabel7"); // NOI18N
        jLabel7.setBounds(190, 20, 70, 70);
        jLayeredPane2.add(jLabel7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel3.setText(bundle.getString("DeviceManagement.jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setBounds(20, 180, 250, 20);
        jLayeredPane2.add(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel13.setText(bundle.getString("DeviceManagement.jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N
        jLabel13.setBounds(20, 300, 250, 20);
        jLayeredPane2.add(jLabel13, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel8.setText(bundle.getString("DeviceManagement.jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.setBounds(20, 240, 250, 20);
        jLayeredPane2.add(jLabel8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel12.setText(bundle.getString("DeviceManagement.jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N
        jLabel12.setBounds(20, 270, 250, 20);
        jLayeredPane2.add(jLabel12, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel11.setText(bundle.getString("DeviceManagement.jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N
        jLabel11.setBounds(20, 330, 250, 20);
        jLayeredPane2.add(jLabel11, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel9.setText(bundle.getString("DeviceManagement.jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N
        jLabel9.setBounds(20, 210, 250, 20);
        jLayeredPane2.add(jLabel9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel4.setText(bundle.getString("DeviceManagement.jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setBounds(20, 150, 250, 20);
        jLayeredPane2.add(jLabel4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setText(bundle.getString("DeviceManagement.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setBounds(20, 60, 170, 20);
        jLayeredPane2.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel10.setText(bundle.getString("DeviceManagement.jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N
        jLabel10.setBounds(20, 120, 250, 20);
        jLayeredPane2.add(jLabel10, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel2.setText(bundle.getString("DeviceManagement.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setBounds(20, 30, 170, 20);
        jLayeredPane2.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel5.setText(bundle.getString("DeviceManagement.jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setBounds(20, 90, 170, 20);
        jLayeredPane2.add(jLabel5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane2.setBounds(20, 10, 280, 360);
        jLayeredPane1.add(jLayeredPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 619, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 385, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
    }//GEN-LAST:event_formKeyPressed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTable1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTable1KeyTyped
    }//GEN-LAST:event_jTable1KeyTyped

    private void jLayeredPane1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jLayeredPane1KeyTyped
    }//GEN-LAST:event_jLayeredPane1KeyTyped

    private void jLayeredPane2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLayeredPane2MouseClicked
    }//GEN-LAST:event_jLayeredPane2MouseClicked

    private void jLayeredPane3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLayeredPane3MouseClicked
    }//GEN-LAST:event_jLayeredPane3MouseClicked

    private void jLayeredPane1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLayeredPane1MouseClicked
    }//GEN-LAST:event_jLayeredPane1MouseClicked

    private void jTable1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MousePressed
        int i = jTable1.rowAtPoint(evt.getPoint());
        if(i>-1) {
            if(jTable1.columnAtPoint(evt.getPoint())==0) {
                JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
                if(jComboBox1.getSelectedIndex()==0) {
                    InstalledPkgInfo info = new InstalledPkgInfo(mainFrame, webOS,
                            filtered.get(i));
                    info.setLocationRelativeTo(mainFrame);
                    WebOSQuickInstallApp.getApplication().show(info);
                } else {
                    InstalledPatchInfo info = new InstalledPatchInfo(mainFrame, webOS,
                            filtered.get(i));
                    info.setLocationRelativeTo(mainFrame);
                    WebOSQuickInstallApp.getApplication().show(info);
                }
            }
        }
        int row = jTable1.getSelectedRow();
        if(row>-1) {
            jTable1.removeRowSelectionInterval(row, row);
        }
    }//GEN-LAST:event_jTable1MousePressed

    private void jTable1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseReleased
        int row = jTable1.getSelectedRow();
        if(row>-1) {
            jTable1.removeRowSelectionInterval(row, row);
        }
    }//GEN-LAST:event_jTable1MouseReleased

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    }//GEN-LAST:event_formWindowClosing

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        t.schedule(new DoLoadPackages(), 50);
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        t.schedule(new DoDeleteApps(), 50);
    }//GEN-LAST:event_jButton1ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables

    class DoLoadDetails extends TimerTask  {
        public void run() {
            loadDeviceDetails();
        }
    }

    class DoLoadPackages extends TimerTask  {
        public void run() {
            loadPackages();
        }
    }
    class DoDeleteApps extends TimerTask  {
        public void run() {
            setState(false);
            uninstall();
            setState(true);
        }
    }
    class DoDispose extends TimerTask  {
        public void run() {
            dispose();
        }
    }
}
