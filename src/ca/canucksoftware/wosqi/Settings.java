/*
 * WebOSQuickInstallAboutBox.java
 */

package ca.canucksoftware.wosqi;

import ca.canucksoftware.utils.OnlineFile;
import ca.canucksoftware.webos.DeviceInfo;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class Settings extends javax.swing.JDialog {
    public boolean feedsChanged;
    private DefaultTableModel tableDefault;
    private DefaultTableModel tableCustom;
    private ResourceBundle bundle;
    private String language;
    private ArrayList<String> customFeeds;
    private Timer t;
    private boolean loaded;

    public Settings(java.awt.Frame parent) {
        super(parent);
        loaded = false;
        bundle = WebOSQuickInstallApp.bundle;
        t = new Timer();
        feedsChanged = false;
        initComponents();
        tableDefault = (DefaultTableModel) jTable1.getModel();
        tableCustom = (DefaultTableModel) jTable2.getModel();
        loadLanguageFromPrefs();
        jCheckBox1.setSelected(DeviceInfo.isUsingDeviceName());
        loadDefaultFeedsFromPrefs();
        loadCustomFeedsFromPrefs();
        jLayeredPane2.requestFocus();
        loaded = true;
        jScrollPane2.setBorder(null);
        try {
            jButton1.setEnabled((Preferences.userNodeForPackage(DeviceInfo.class).keys().length!=0));
        } catch(Exception e) {}
    }

    private void loadLanguageFromPrefs() {
        language = Preferences.systemRoot().get("language", null);
        jComboBox1.addItem(bundle.getString("<SYSTEM_DEFAULT>"));
        jComboBox1.addItem(bundle.getString("ENGLISH"));
        jComboBox1.addItem(bundle.getString("FRENCH"));
        jComboBox1.addItem(bundle.getString("GERMAN"));
        jComboBox1.addItem(bundle.getString("SIMPLIFIED_CHINESE"));
        if(language==null) {
            jComboBox1.setSelectedIndex(0);
        } else if(language.equals("en")) {
            jComboBox1.setSelectedIndex(1);
        } else if(language.equals("fr")) {
            jComboBox1.setSelectedIndex(2);
        } else if(language.equals("de")) {
            jComboBox1.setSelectedIndex(3);
        } else if(language.equals("zh")) {
            jComboBox1.setSelectedIndex(4);
        }
    }

    private void saveLanguagePrefs() {
        int index = jComboBox1.getSelectedIndex();
        if(index==0) { //system default
            if(language!=null) {
                Preferences.systemRoot().remove("language");
                t.schedule(new DoLanguageNotice(), 200);
                language = null;
            }
        } else {
            String newLanguage = null;
            if(index==1) { //english
                newLanguage = "en";
            } else if(index==2) { //french
                newLanguage = "fr";
            } else if(index==3) { //german
                newLanguage = "de";
            } else if(index==4) { //chinese
                newLanguage = "zh";
            }
            if(newLanguage!=null) {
                if(language==null || !language.equals(newLanguage)) {
                    language = newLanguage;
                    Preferences.systemRoot().put("language", language);
                    t.schedule(new DoLanguageNotice(), 200);
                }
            }
        }


    }

    private void loadDefaultFeedsFromPrefs() {
        while(tableDefault.getRowCount()>0) {
            tableDefault.removeRow(0);
        }
        boolean currState = Preferences.systemRoot().getBoolean("defaultFeedState-0", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_PRECENTRAL"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-1", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_WEBOSINTERNALS"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-3", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_PATCHES"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-4", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_KERNELS"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-5", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_PRETHEMER"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-6", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_PCTHEMES"), new Boolean(currState)});

        currState = Preferences.systemRoot().getBoolean("defaultFeedState-7", true);
        tableDefault.addRow(new Object[] {bundle.getString("FEED_CLOCK"), new Boolean(currState)});
    }

    private void saveDefaultFeedsToPrefs() {
        Boolean currState = (Boolean) tableDefault.getValueAt(0, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-0", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(1, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-1", currState.booleanValue());
        Preferences.systemRoot().putBoolean("defaultFeedState-2", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(2, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-3", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(3, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-4", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(4, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-5", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(5, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-6", currState.booleanValue());

        currState = (Boolean) tableDefault.getValueAt(6, 1);
        Preferences.systemRoot().putBoolean("defaultFeedState-7", currState.booleanValue());
    }

    private void loadCustomFeedsFromPrefs() {
        while(tableCustom.getRowCount()>0) {
            tableCustom.removeRow(0);
        }
        customFeeds = new ArrayList();
        int num = Preferences.systemRoot().getInt("numCustomFeeds", 0);
        for(int i=0; i<num; i++) {
            String currUrl = Preferences.systemRoot().get("customfeed" + i, null);
            if(currUrl!=null) {
                boolean currState = Preferences.systemRoot().getBoolean("customFeedState-" + i, true);
                customFeeds.add(currUrl);
                tableCustom.addRow(new Object[] {currUrl, new Boolean(currState)});
            }
        }
    }

    private void clearCustomListPref() {
        for(int i=0; i<customFeeds.size(); i++) {
            Preferences.systemRoot().remove("customfeed" + i);
        }
    }

    private void saveCustomListPref() {
        Preferences.systemRoot().putInt("numCustomFeeds", customFeeds.size());
        for(int i=0; i<customFeeds.size(); i++) {
            Preferences.systemRoot().put("customfeed" + i, customFeeds.get(i));
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
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton6 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jComboBox1 = new javax.swing.JComboBox();
        jButton7 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("Settings.title")); // NOI18N
        setIconImage(null);
        setModal(true);
        setName("transfer"); // NOI18N
        setResizable(false);

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N

        jLayeredPane2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLayeredPane2.setName("jLayeredPane2"); // NOI18N

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()+1f));
        jLabel2.setText(bundle.getString("Settings.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setBounds(20, 90, 230, 20);
        jLayeredPane2.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", ""
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
        jTable2.setFillsViewportHeight(true);
        jTable2.setName("jTable2"); // NOI18N
        jTable2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable2.setTableHeader(null);
        jScrollPane3.setViewportView(jTable2);
        jTable2.getColumnModel().getColumn(0).setResizable(false);
        jTable2.getColumnModel().getColumn(0).setPreferredWidth(350);
        jTable2.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("Settings.jTable1.columnModel.title1")); // NOI18N
        jTable2.getColumnModel().getColumn(1).setResizable(false);
        jTable2.getColumnModel().getColumn(1).setPreferredWidth(10);
        jTable2.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("Settings.jTable1.columnModel.title0")); // NOI18N

        jScrollPane3.setBounds(20, 280, 380, 120);
        jLayeredPane2.add(jScrollPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jCheckBox1.setFont(jCheckBox1.getFont().deriveFont(jCheckBox1.getFont().getSize()+1f));
        jCheckBox1.setText(bundle.getString("Settings.jCheckBox1.text")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N
        jCheckBox1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jCheckBox1.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });
        jCheckBox1.setBounds(20, 50, 410, 40);
        jLayeredPane2.add(jCheckBox1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton6.setFont(jButton6.getFont().deriveFont(jButton6.getFont().getSize()+1f));
        jButton6.setText("+");
        jButton6.setFocusable(false);
        jButton6.setIconTextGap(0);
        jButton6.setMargin(new java.awt.Insets(-1, -1, 0, 0));
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jButton6.setBounds(410, 280, 30, 21);
        jLayeredPane2.add(jButton6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton1.setText(bundle.getString("Settings.jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(30, 420, 360, 40);
        jLayeredPane2.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTable1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(128, 128, 128), 1, true));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", ""
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
        jTable1.setName("jTable1"); // NOI18N
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.setTableHeader(null);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTable1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTable1MouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(jTable1);
        jTable1.getColumnModel().getColumn(0).setResizable(false);
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(350);
        jTable1.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("Settings.jTable1.columnModel.title1")); // NOI18N
        jTable1.getColumnModel().getColumn(1).setResizable(false);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(10);
        jTable1.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("Settings.jTable1.columnModel.title0")); // NOI18N

        jScrollPane2.setBounds(20, 120, 410, 130);
        jLayeredPane2.add(jScrollPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox1.setFocusable(false);
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jComboBox1.setBounds(130, 10, 170, 22);
        jLayeredPane2.add(jComboBox1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton7.setFont(jButton7.getFont().deriveFont(jButton7.getFont().getSize()+1f));
        jButton7.setText("‒");
        jButton7.setFocusable(false);
        jButton7.setIconTextGap(0);
        jButton7.setMargin(new java.awt.Insets(-1, -1, 0, 0));
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jButton7.setBounds(410, 310, 30, 21);
        jLayeredPane2.add(jButton7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()+1f));
        jLabel1.setText(bundle.getString("Settings.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setBounds(20, 10, 110, 20);
        jLayeredPane2.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getSize()+1f));
        jLabel3.setText(bundle.getString("Settings.jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setBounds(20, 250, 230, 20);
        jLayeredPane2.add(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLayeredPane2.setBounds(10, 10, 450, 480);
        jLayeredPane1.add(jLayeredPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        String input = JOptionPane.showInputDialog(rootPane,
                bundle.getString("CUSTOM_FEED_URL:"), "",
                JOptionPane.INFORMATION_MESSAGE);
        if(input!=null && input.length()>0) {
            if(!input.startsWith("http://") && !input.startsWith("https://")) {
                input = "http://" + input;
            }
            if(!input.endsWith("Packages") && !input.endsWith("Packages.gz")) {
                if(!input.endsWith("/")) {
                    input += "/";
                }
                if(new OnlineFile(input + "Packages.gz").exists()) {
                    input += "Packages.gz";
                } else if(new OnlineFile(input + "Packages").exists()) {
                    input += "Packages";
                }
            }
            if(new OnlineFile(input).exists()) {
                clearCustomListPref();
                customFeeds.add(input);
                tableCustom.addRow(new Object[] {input, Boolean.TRUE});
                saveCustomListPref();
                feedsChanged = true;
            } else {
                JOptionPane.showMessageDialog(rootPane, bundle.getString("ERROR:_INVALID_FEED_URL"));
            }
        }
}//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        int index = jTable2.getSelectedRow();
        if(index>-1) {
            clearCustomListPref();
            customFeeds.remove(index);
            tableCustom.removeRow(index);
            saveCustomListPref();
            feedsChanged = true;
        }
}//GEN-LAST:event_jButton7ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        if(loaded) {
            saveLanguagePrefs();
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jTable1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MousePressed
        int i = jTable1.rowAtPoint(evt.getPoint());
        if(i>-1) {
            if(jTable1.columnAtPoint(evt.getPoint())==0) {
                boolean val = ((Boolean)tableDefault.getValueAt(i, 1)).booleanValue();
                val = !val;
                tableDefault.setValueAt(new Boolean(val), i, 1);
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
        saveDefaultFeedsToPrefs();
        feedsChanged = true;
        jLayeredPane2.requestFocus();
    }//GEN-LAST:event_jTable1MouseReleased

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            Preferences.userNodeForPackage(DeviceInfo.class).clear();
            jButton1.setEnabled((Preferences.userNodeForPackage(DeviceInfo.class).keys().length!=0));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        DeviceInfo.useDeviceName(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    // End of variables declaration//GEN-END:variables

    class DoLanguageNotice extends TimerTask  {
        public void run() {
            JOptionPane.showMessageDialog(rootPane,
                    bundle.getString("THE_NEW_LANGUAGE_WILL_TAKE_EFFECT_THE_NEXT_TIME_WEBOS_QUICK_INSTALL_IS_LAUNCHED."),
                    bundle.getString("LANGUAGE_CHANGED."), JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
