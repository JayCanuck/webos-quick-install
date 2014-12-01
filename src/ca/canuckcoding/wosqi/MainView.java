
package ca.canuckcoding.wosqi;

import ca.canuckcoding.ipkg.PackageManager;
import ca.canuckcoding.novacom.Novacom;
import ca.canuckcoding.novacom.NovacomDrivers;
import ca.canuckcoding.utils.FileUtils;
import ca.canuckcoding.webos.DeviceInfo;
import ca.canuckcoding.webos.WebOS;
import ca.canuckcoding.webos.WebOSConnection;
import ca.canuckcoding.webos.WebOSDevice;
import ca.canuckcoding.webos.WebOSException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.iharder.dnd.FileDrop;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.util.ResourceBundle;

/**
 * The application's main frame.
 */
public class MainView extends FrameView {
    private ResourceBundle bundle;
    private Image bgImg;
    private String chooserPath;
    private WebOSDevice[] devices;
    private WebOSConnection[] webOS;
    private PackageManager[] pkgMgr;
    private ArrayList<File> items;
    private DefaultTableModel modTable;
    private File installDir;
    private Timer timer;

    public MainView(SingleFrameApplication app) {
        super(app);
        bundle = WebOSQuickInstallApp.bundle;
        URL bgURL = getClass().getResource("resources/background.jpg");
        bgImg = new ImageIcon(bgURL).getImage();
        URL iconURL = getClass().getResource("resources/icon.png");
        getFrame().setIconImage(new ImageIcon(iconURL).getImage());
        items = new ArrayList<File>();
        initComponents();
        jButton2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jTable1.setGridColor(Color.CYAN);
        modTable = (DefaultTableModel) jTable1.getModel();
        installDir = new File(FileUtils.appDirectory(), bundle.getString("INSTALL"));
        chooserPath = Preferences.userRoot().get("chooserPath", null);
        new FileDrop(jTable1, new FileDrop.Listener() {
            public void  filesDropped(File[] files ) {
                for(int i=0; i<files.length; i++) {
                    String name = files[i].getName().toLowerCase();
                    if(name.endsWith(".ipk") || name.endsWith(".patch") ||
                            name.endsWith(".diff")) {
                        addFile(files[i]);
                    }
                }
            }
        });
        devices = new WebOSDevice[0];
        webOS = new WebOSConnection[0];
        loadList();
        timer = new Timer();
        reloadDeviceCache();
        if(jComboBox1.getItemCount()==0) {
            if(Novacom.isInstalled()) {
                timer.schedule(new FirstCheckConnection(), 100);
            } else {
                timer.schedule(new DoInstallDrivers(), 100);
            }
        }
    }

    private void loadList() {
        if(!installDir.isDirectory()) {
            installDir.mkdirs();
        }
        items.clear();
        while(modTable.getRowCount()>0) {
            modTable.removeRow(0);
        }
        File[] files = installDir.listFiles(new WebOSFileFilter());
        for(int i=0; i<files.length; i++) {
            items.add(files[i]);
            addRow(files[i]);
        }
    }

    private void addRow(File current){
        String type;
        if (current.getName().toLowerCase().endsWith(".ipk")) {
            type = " " + bundle.getString("WEBOS_PACKAGE_FILE");
        } else if (current.getName().toLowerCase().endsWith(".patch")) {
            type = " " + bundle.getString("WEBOS_PATCH_FILE");
        } else if (current.getName().toLowerCase().endsWith(".diff")) {
            type = " " + bundle.getString("WEBOS_DIFF_FILE");
        } else {
            type = " " + bundle.getString("UNKNOWN_FILE");
        }
        modTable.addRow(new Object[]{" " + FileUtils.getFilename(current),
                FileUtils.formatFilesize(current), type});
    }

    private void removeRow(){
        int index = jTable1.getSelectedRow();
        items.get(index).delete();
        items.remove(index);
        modTable.removeRow(index);
    }


    private void addDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new WebOSChooseFilter());
        fc.setMultiSelectionEnabled(true);
        if(chooserPath!=null)
            fc.setCurrentDirectory(new File(chooserPath));
        fc.setDialogTitle("");
        disableNewFolderButton(fc);
        if(fc.showDialog(getFrame(), bundle.getString("SELECT"))==
                JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            if(files.length>0) {
                chooserPath = files[0].getParentFile().getAbsolutePath();
                Preferences.userRoot().put("chooserPath", chooserPath);
            }
            for(int i=0; i<files.length; i++){
                addFile(files[i]);
            }
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

    private void addFile(File from) {
            File to = new File(installDir, FileUtils.getFilename(from));
            boolean exists = to.exists();
            if(exists) {
                to.delete();
            }
            try {
                FileUtils.copy(from, to);
            } catch(Exception e) {
                if(to.length()==0) {
                    to.delete();
                    exists = false;
                }
                System.err.println("ERROR: Unable to copy file to install directory: "
                        + from.getAbsolutePath());
            }
            if(exists) { //update list
                loadList();
            } else { //new file to add to list
                items.add(to);
                addRow(to);
            }
    }

    private void reloadDeviceCache() {
        try {
            String id = null;
            if(devices.length>0) {
                id = devices[getConnectionIndex()].getId();
            }
            WebOSDevice[] newDevices = WebOS.listDevices();
            WebOSConnection[] newWebOS = new WebOSConnection[newDevices.length];
            PackageManager[] newPkgMgr = new PackageManager[newDevices.length];
            jComboBox1.removeAllItems();
            for(int i=0; i<newDevices.length; i++) {
                for(int j=0; j<devices.length; j++) {
                    if(devices[j].getId().equals(newDevices[i].getId())) {
                        newWebOS[i] = webOS[j];
                        newPkgMgr[i] = pkgMgr[j];
                    }
                }
                if(newWebOS[i]==null) {
                    try {
                        newWebOS[i] = newDevices[i].connect();
                    } catch(WebOSException e) {
                        newWebOS[i] = null;
                    }
                }
                if( newWebOS[i] != null) {
                    String deviceName = newWebOS[i].getDeviceInfo().name();
                    if(deviceName.equals(DeviceInfo.Model.Unknown.toString())) {
                        deviceName = newDevices[i].getName();
                    }
                    jComboBox1.addItem(deviceName);
                    if(id!=null && newDevices[i].getId().equals(id)) {
                        jComboBox1.setSelectedIndex(i);
                    }
                }
            }
            if(jComboBox1.getSelectedIndex()==-1 && jComboBox1.getItemCount()>0) {
                jComboBox1.setSelectedIndex(0);
            }
            devices = newDevices;
            webOS = newWebOS;
            pkgMgr = newPkgMgr;
        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public int getConnectionIndex() {
        int currIndex = jComboBox1.getSelectedIndex();
        int result = 0;
        int counter = -1;
        for(result=0; result<devices.length; result++) {
            if(webOS[result]!=null) {
                counter++;
            }
            if(counter==currIndex) {
                break;
            }
        }
        return result;
    }

    private void connectionWarningIfNeeded() {
        if(jComboBox1.getItemCount()==0) {
            if(JOptionPane.showConfirmDialog(mainPanel,
                    bundle.getString("REPEATING_CONNECTION_WARNING"),
                    bundle.getString("NO_DEVICES_FOUND"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION) {
                reloadDeviceCache();
                connectionWarningIfNeeded();
            } else {
                System.exit(0);
            }
        }
    }

    private void firstConnectionWarningIfNeeded() {
        if(jComboBox1.getItemCount()==0) {
            int choice = JOptionPane.showOptionDialog(mainPanel, bundle.getString("REPEATING_CONNECTION_WARNING"),
                    bundle.getString("NO_DEVICES_FOUND"), JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE, null, new Object[] {bundle.getString("OK"),
                    bundle.getString("CANCEL"), bundle.getString("MainView.jMenuItem2.text")},
                    bundle.getString("OK"));
            if(choice==JOptionPane.YES_OPTION) {
                reloadDeviceCache();
                firstConnectionWarningIfNeeded();
            } else if(choice==JOptionPane.CANCEL_OPTION) {
                installDrivers(false);
                firstConnectionWarningIfNeeded();
            } else {
                System.exit(0);
            }
        }
    }

    private void installDrivers(boolean exitOnFail) {
        NovacomDrivers driver = new NovacomDrivers();
        if(driver.install()) {
            JOptionPane.showMessageDialog(mainPanel, bundle.getString("DRIVER_INSTALLED_SUCCESSFULLY."));
        } else {
            JOptionPane.showMessageDialog(mainPanel, bundle.getString("ERROR\\:_DRIVER_INSTALLATION_FAILED"));
        }
        reloadDeviceCache();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new ImagePanel(bgImg);
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLayeredPane3 = new javax.swing.JLayeredPane();
        jLabel1 = new javax.swing.JLabel();
        jLayeredPane7 = new javax.swing.JLayeredPane();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        toolMenu = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMenuItem9 = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();

        mainPanel.setName("mainView"); // NOI18N

        jLayeredPane3.setFocusable(false);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canuckcoding/wosqi/resources/pre_shadow.png"))); // NOI18N
        jLabel1.setFocusable(false);
        jLayeredPane3.add(jLabel1);
        jLabel1.setBounds(10, 30, 150, 160);

        jLayeredPane1.add(jLayeredPane3);
        jLayeredPane3.setBounds(460, 160, 170, 210);

        jLayeredPane7.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(128, 128, 128), 1, true), bundle.getString("MainView.jLayeredPane7.border.title"))); // NOI18N

        jButton4.setBackground(new java.awt.Color(203, 203, 203));
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canuckcoding/wosqi/resources/remove.png"))); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setIconTextGap(0);
        jButton4.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton4.setMaximumSize(new java.awt.Dimension(36, 33));
        jButton4.setMinimumSize(new java.awt.Dimension(36, 33));
        jButton4.setPreferredSize(new java.awt.Dimension(36, 33));
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jLayeredPane7.add(jButton4);
        jButton4.setBounds(520, 70, 40, 30);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(ca.canuckcoding.wosqi.WebOSQuickInstallApp.class).getContext().getActionMap(MainView.class, this);
        jButton5.setAction(actionMap.get("openRepository")); // NOI18N
        jButton5.setBackground(new java.awt.Color(203, 203, 203));
        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canuckcoding/wosqi/resources/online.png"))); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setIconTextGap(0);
        jButton5.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton5.setMaximumSize(new java.awt.Dimension(36, 33));
        jButton5.setMinimumSize(new java.awt.Dimension(36, 33));
        jButton5.setPreferredSize(new java.awt.Dimension(36, 33));
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jLayeredPane7.add(jButton5);
        jButton5.setBounds(520, 120, 40, 40);

        jScrollPane2.setAutoscrolls(true);
        jScrollPane2.setPreferredSize(new java.awt.Dimension(485, 191));

        jTable1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Size", "Type"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
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
        jTable1.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        jTable1.setMinimumSize(new java.awt.Dimension(45, 45));
        jTable1.setName("installTable"); // NOI18N
        jTable1.setRowHeight(20);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(350);
            jTable1.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("MainView.installTable.columnModel.title0")); // NOI18N
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(125);
            jTable1.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("MainView.installTable.columnModel.title1")); // NOI18N
            jTable1.getColumnModel().getColumn(2).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(250);
            jTable1.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("MainView.installTable.columnModel.title2")); // NOI18N
        }

        jLayeredPane7.add(jScrollPane2);
        jScrollPane2.setBounds(20, 30, 485, 191);

        jButton1.setBackground(new java.awt.Color(220, 220, 220));
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canuckcoding/wosqi/resources/add.png"))); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setIconTextGap(0);
        jButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton1.setMaximumSize(new java.awt.Dimension(36, 33));
        jButton1.setMinimumSize(new java.awt.Dimension(36, 33));
        jButton1.setPreferredSize(new java.awt.Dimension(36, 33));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jLayeredPane7.add(jButton1);
        jButton1.setBounds(520, 30, 40, 30);

        jLayeredPane1.add(jLayeredPane7);
        jLayeredPane7.setBounds(10, 25, 590, 240);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText(bundle.getString("MainView.jLabel2.text")); // NOI18N
        jLabel2.setFocusable(false);
        jLayeredPane1.add(jLabel2);
        jLabel2.setBounds(350, 5, 100, 20);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canuckcoding/wosqi/resources/find-apps-precentralnet.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.setFocusable(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jLayeredPane1.add(jButton2);
        jButton2.setBounds(20, 280, 130, 60);

        jButton3.setBackground(new java.awt.Color(165, 165, 165));
        jButton3.setFont(jButton3.getFont().deriveFont(jButton3.getFont().getSize()+3f));
        jButton3.setText(bundle.getString("MainView.jButton3.text")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jLayeredPane1.add(jButton3);
        jButton3.setBounds(240, 290, 140, 33);

        jComboBox1.setBackground(new java.awt.Color(220, 220, 220));
        jComboBox1.setFocusable(false);
        jComboBox1.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                jComboBox1PopupMenuWillBecomeInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                jComboBox1PopupMenuWillBecomeVisible(evt);
            }
        });
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jLayeredPane1.add(jComboBox1);
        jComboBox1.setBounds(460, 5, 140, 20);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jLayeredPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jLayeredPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
        );

        fileMenu.setText(bundle.getString("MainView.fileMenu.text")); // NOI18N

        jMenuItem1.setText(bundle.getString("MainView.jMenuItem1.text")); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem1);
        fileMenu.add(jSeparator2);

        jMenuItem8.setText(bundle.getString("MainView.jMenuItem8.text")); // NOI18N
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem8);

        jMenuItem2.setText(bundle.getString("MainView.jMenuItem2.text")); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem2);
        fileMenu.add(jSeparator1);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setText(bundle.getString("EXIT")); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        toolMenu.setText(bundle.getString("MainView.toolMenu.text")); // NOI18N

        jMenuItem3.setText(bundle.getString("MainView.jMenuItem3.text")); // NOI18N
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        toolMenu.add(jMenuItem3);

        jMenuItem4.setText(bundle.getString("MainView.jMenuItem4.text")); // NOI18N
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        toolMenu.add(jMenuItem4);

        jMenuItem5.setText(bundle.getString("MainView.jMenuItem5.text")); // NOI18N
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        toolMenu.add(jMenuItem5);
        toolMenu.add(jSeparator3);

        jMenuItem9.setText(bundle.getString("MainView.jMenuItem9.text")); // NOI18N
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        toolMenu.add(jMenuItem9);

        menuBar.add(toolMenu);

        helpMenu.setText(bundle.getString("MainView.helpMenu.text")); // NOI18N

        jMenuItem6.setText(bundle.getString("MainView.jMenuItem6.text")); // NOI18N
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        helpMenu.add(jMenuItem6);

        jMenuItem7.setText(bundle.getString("MainView.jMenuItem7.text")); // NOI18N
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        helpMenu.add(jMenuItem7);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ca/canuckcoding/wosqi/Bundle"); // NOI18N
        jMenuItem10.setText(bundle.getString("MainView.jMenuItem10.text")); // NOI18N
        jMenuItem10.setToolTipText(bundle.getString("MainView.jMenuItem10.toolTipText")); // NOI18N
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        helpMenu.add(jMenuItem10);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(bundle.getString("MainView.aboutMenuItem.text")); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        if(items.size()>0) {
            JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
            Installer installer = new Installer(mainFrame, webOS[getConnectionIndex()],
                    items);
            installer.setLocationRelativeTo(mainFrame);
            WebOSQuickInstallApp.getApplication().show(installer);
            loadList();
            reloadDeviceCache();
            timer.schedule(new CheckConnection(), 200);
        }
}//GEN-LAST:event_jButton3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        int index = getConnectionIndex();
        if(pkgMgr[index]==null) {
            FeedLoader loader = new FeedLoader(mainFrame, webOS[index]);
            loader.setLocationRelativeTo(mainFrame);
            WebOSQuickInstallApp.getApplication().show(loader);
            pkgMgr[index] = loader.pkgMgr;
        } else {
            pkgMgr[index].setInstalledAppList(webOS[index].listInstalled());
        }
        if(pkgMgr[index]!=null) {
            if(pkgMgr[index].hasPackages()) {
                FeedViewer feeds = new FeedViewer(mainFrame, webOS[index], pkgMgr[index]);
                feeds.setLocationRelativeTo(mainFrame);
                WebOSQuickInstallApp.getApplication().show(feeds);
            } else {
                JOptionPane.showMessageDialog(mainPanel, bundle.getString("ERROR:_FEED_LOADING_FAILED"));
            }
        } else {
            JOptionPane.showMessageDialog(mainPanel, bundle.getString("ERROR:_FEED_LOADING_FAILED"));
        }
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
}//GEN-LAST:event_jButton5ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        addDialog();
}//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        removeRow();
}//GEN-LAST:event_jButton4ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("http://www.webosnation.com/"));
        } catch(Exception e) {}
}//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        addDialog();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jComboBox1PopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jComboBox1PopupMenuWillBecomeVisible
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jComboBox1PopupMenuWillBecomeVisible

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        JOptionPane.showMessageDialog(mainPanel, "<html><body width=\"400px\">" +
                bundle.getString("FULL_WOSQI_DISCLAIMER") + "</body>",
                bundle.getString("DISCLAIMER"),JOptionPane.INFORMATION_MESSAGE);
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        installDrivers(false);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        SendFile transfer = new SendFile(mainFrame, webOS[getConnectionIndex()]);
        transfer.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(transfer);
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        ReceiveFile transfer = new ReceiveFile(mainFrame, webOS[getConnectionIndex()]);
        transfer.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(transfer);
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jComboBox1PopupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jComboBox1PopupMenuWillBecomeInvisible
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jComboBox1PopupMenuWillBecomeInvisible

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        Usage usage = new Usage(mainFrame);
        usage.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(usage);
        reloadDeviceCache();
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        About about = new About(mainFrame);
        about.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(about);
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        DeviceManagement manage = new DeviceManagement(mainFrame, webOS[getConnectionIndex()]);
        manage.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(manage);
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        Settings settings = new Settings(mainFrame);
        settings.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(settings);
        if(settings.feedsChanged) {
            for(int i=0; i<pkgMgr.length; i++) {
                pkgMgr[i] = null;
            }
        }
        reloadDeviceCache();
        timer.schedule(new CheckConnection(), 200);
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        int index = getConnectionIndex();
        if(webOS[index]!=null) {
            try {
                webOS[index].launchTerminal();
            } catch(UnsupportedOperationException e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        JOptionPane.showMessageDialog(mainPanel, "<html><body width=\"400px\">" +
                "WebOS Quick Install is opensource under the Apache License 2.0.<br>" +
                "Contains the Novacom driver, also under the Apache License 2.0.<br><br>" + 
                "The full license is included within the source code and this " + 
                "java archive. It can also be read at:<br>" + 
                "<br>http://www.apache.org/licenses/LICENSE-2.0.txt</body>",
                "License Info",JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JLayeredPane jLayeredPane7;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu toolMenu;
    // End of variables declaration//GEN-END:variables

    class DoInstallDrivers extends TimerTask  {
        public void run() {
            if(JOptionPane.showConfirmDialog(null, "<html><body width=\"400px\">" +
                    bundle.getString("FOR_WEBOS_QUICK_INSTALL_TO_RUN_CORRECTLY," +
                    "_YOUR_COMPUTER_NEEDS_TO_HAVE_THE_NOVACOM_DRIVER_INSTALLED._" +
                    "WOULD_YOU_LIKE_TO_INSTALL_IT_NOW?"),
                    bundle.getString("NOVACOM_DRIVER_MISSING"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION) {
                installDrivers(true);
                if(devices.length==0) {
                    timer.schedule(new CheckConnection(), 100);
                    /*JOptionPane.showMessageDialog(mainPanel,
                            bundle.getString("NO_DEVICES_ARE_CONNECTED." +
                            "_CONNECT_A_DEVICE_THEN_RELAUNCH_WEBOS_QUICK_INSTALL."),
                            bundle.getString("NO_DEVICES_FOUND"),
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(0);*/
                }
            } else {
                System.exit(0);
            }
        }
    }

    class CheckConnection extends TimerTask  {
        public void run() {
            connectionWarningIfNeeded();
        }
    }

    class FirstCheckConnection extends TimerTask  {
        public void run() {
            firstConnectionWarningIfNeeded();
            /*JOptionPane.showMessageDialog(mainPanel,
                    bundle.getString("NO_DEVICES_ARE_CONNECTED." +
                    "_CONNECT_A_DEVICE_THEN_RELAUNCH_WEBOS_QUICK_INSTALL."),
                    bundle.getString("NO_DEVICES_FOUND"), JOptionPane.ERROR_MESSAGE);
            System.exit(0);*/
        }
    }

    class ImagePanel extends JPanel {

        private Image img;

        public ImagePanel(String img) {
            this(new ImageIcon(img).getImage());
        }

        public ImagePanel(Image img) {
            this.img = img;
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }

        @Override public void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0, null);
        }
    }

    class DoctorChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".jar"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return bundle.getString("JAR_FILES_(*.JAR)");
        }
    }
    
    class WebOSChooseFilter extends javax.swing.filechooser.FileFilter {
        private final String[] okFileExtensions = new String[] {".ipk", ".diff", ".patch"};

        public boolean accept(File f) {
            for (String extension : okFileExtensions)
                if (f.getName().toLowerCase().endsWith(extension) || f.isDirectory())
                    return true;
            return false;
        }

        public String getDescription() {
            return bundle.getString("WEBOS_FILES_(*.IPK,_*.PATCH,_*.DIFF)");
        }
    }

    class WebOSFileFilter implements java.io.FileFilter{
        private final String[] okFileExtensions = new String[] {".ipk", ".diff", ".patch"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions)
                if (file.getName().toLowerCase().endsWith(extension))
                    return true;
            return false;
        }
    }
}
