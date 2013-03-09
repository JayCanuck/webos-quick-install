/*
 * WebOSQuickInstallAboutBox.java
 */

package ca.canucksoftware.wosqi;

import ca.canucksoftware.ipkg.PackageEntry;
import ca.canucksoftware.ipkg.PackageFilter;
import ca.canucksoftware.ipkg.PackageManager;
import ca.canucksoftware.webos.DeviceInfo;
import ca.canucksoftware.webos.WebOSConnection;
import java.awt.Cursor;
import java.awt.Desktop;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import java.util.*;
import java.util.Timer;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import org.json.JSONArray;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;

public class FeedViewer extends javax.swing.JDialog {
    private ResourceBundle bundle;
    private PackageManager pkgMgr;
    private final String UNKNOWN_VALUE;
    private final Icon NO_SCREENSHOT;
    private final PackageFilter[] TAB_FILTER;
    private final JComboBox[] CATEGORY;
    private final JTextField[] SEARCH;
    private final JList[] LIST;
    private final JLabel[] NAME;
    private final JLabel[] VERSION;
    private final JLabel[] DEVELOPER;
    private final JLabel[] LASTUPDATED;
    private final JLabel[] SCREENSHOT;
    private final JLabel[] LICENSE;
    private final JLabel[] FEED;
    private final JLabel[] DESCRIPTION_LABEL;
    private final JLabel[] HOMEPAGE;
    private final JLabel[] LICENSE_LABEL;
    private final JLabel[] FEED_LABEL;
    private final JTextPane[] DESCRIPTION;
    private final JButton[] INSTALL_BUTTON;
    private ArrayList<PackageEntry>[] packages;
    private ArrayList<PackageEntry>[] filtered;
    private PackageEntry[] selected;
    private Timer t;
    private WebOSConnection webOS;
    private boolean loaded;
    private Image background;

    public FeedViewer(java.awt.Frame parent, WebOSConnection wc, PackageManager pm) {
        super(parent);
        bundle = WebOSQuickInstallApp.bundle;
        URL bgURL = getClass().getResource("resources/background2.jpg");
        background = new ImageIcon(bgURL).getImage();
        initComponents();
        loaded = false;
        UNKNOWN_VALUE = bundle.getString("UNKNOWN");
        NO_SCREENSHOT = jLabel1.getIcon();
        TAB_FILTER = new PackageFilter[] {PackageFilter.Applications,
                PackageFilter.Services, PackageFilter.Plugins, PackageFilter.Linux_Apps,
                PackageFilter.Linux_Daemons, PackageFilter.Kernels, PackageFilter.Patches,
                PackageFilter.Themes};
        CATEGORY = new JComboBox[] {jComboBox1, jComboBox2, jComboBox3, jComboBox4,
                jComboBox9, jComboBox5, jComboBox6, jComboBox7};
        SEARCH = new JTextField[] {jTextField1, jTextField2, jTextField3, jTextField4,
                jTextField9, jTextField5, jTextField6, jTextField7};
        LIST = new JList[] {jList1, jList2, jList3, jList4, jList9, jList5,
                jList6, jList7, jList8};
        NAME = new JLabel[] {jLabel2, jLabel13, jLabel24, jLabel35, jLabel90,
                jLabel46, jLabel57, jLabel68, jLabel79};
        VERSION = new JLabel[] {jLabel5, jLabel16, jLabel27, jLabel38, jLabel93,
                jLabel49, jLabel60, jLabel71, jLabel82};
        DEVELOPER = new JLabel[] {jLabel9, jLabel20, jLabel31, jLabel42, jLabel97,
                jLabel53, jLabel64, jLabel75, jLabel86};
        LASTUPDATED = new JLabel[] {jLabel6, jLabel17, jLabel28, jLabel39, jLabel94,
                jLabel50, jLabel61, jLabel72, jLabel83};
        SCREENSHOT = new JLabel[] {jLabel1, jLabel12, jLabel23, jLabel34, jLabel89,
                jLabel45, jLabel56, jLabel67, jLabel78};
        LICENSE = new JLabel[] {jLabel10, jLabel21, jLabel32, jLabel43, jLabel98,
                jLabel54, jLabel65, jLabel76, jLabel87};
        FEED = new JLabel[] {jLabel7, jLabel19, jLabel30, jLabel41, jLabel96,
                jLabel52, jLabel63, jLabel74, jLabel85};
        DESCRIPTION_LABEL = new JLabel[] {jLabel8, jLabel18, jLabel29, jLabel40,
                jLabel95, jLabel51, jLabel62, jLabel73, jLabel84};
        HOMEPAGE = new JLabel[] {jLabel11, jLabel22, jLabel33, jLabel44,
                jLabel99, jLabel55, jLabel66, jLabel77, jLabel88};
        LICENSE_LABEL = new JLabel[] {jLabel3, jLabel15, jLabel26, jLabel37,
                jLabel92, jLabel48, jLabel59, jLabel70, jLabel81};
        FEED_LABEL = new JLabel[] {jLabel4, jLabel14, jLabel25, jLabel36,
                jLabel91, jLabel47, jLabel58, jLabel69, jLabel80};
        DESCRIPTION = new JTextPane[] {jTextPane1, jTextPane2, jTextPane3, jTextPane4,
                jTextPane9, jTextPane5, jTextPane6, jTextPane7, jTextPane8};
        INSTALL_BUTTON = new JButton[] {jButton2, jButton4, jButton6, jButton8,
                jButton18, jButton10, jButton12, jButton14, jButton16};
        packages = new ArrayList[9];
        filtered = new ArrayList[9];
        selected = new PackageEntry[9];
        webOS = wc;
        pkgMgr = pm;
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
            for(int i=0; i<HOMEPAGE.length; i++) {
                HOMEPAGE[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            for(int i=0; i<NAME.length; i++) {
                NAME[i].setVisible(false);
            }
            t.schedule(new DoLoad(), 200);
        }
    }

    public void loadTab() {
        int tab = jTabbedPane2.getSelectedIndex();
        pkgMgr.setFilter(TAB_FILTER[tab]);
        if(packages[tab]==null) { //not previously loaded tab
            ArrayList<String> categories = pkgMgr.getCategories();
            for(int i=0; i<categories.size(); i++) {
                CATEGORY[tab].addItem((Object)categories.get(i));
            }
            CATEGORY[tab].setSelectedIndex(0);
            loadCategory(tab);
        }
    }

    private void loadCategory(int tab) {
        String currCategory = (String)CATEGORY[tab].getSelectedItem();
        packages[tab] = pkgMgr.getPackagesByCategory(currCategory);
        filtered[tab] = packages[tab];
        Object[] data = new Object[packages[tab].size()];
        for(int i=0; i<packages[tab].size(); i++) {
            data[i] = (Object) packages[tab].get(i).name;
        }
        LIST[tab].setListData(data);
        if(data.length>0) {
            LIST[tab].setSelectedIndex(0);
        } else {
            jButton15.setEnabled(false);
        }
        displayPackage(tab);
    }

    public void displayPackage(int tab) {
        int index = LIST[tab].getSelectedIndex();
        boolean flag = (index>-1);
        VERSION[tab].setVisible(flag);
        DEVELOPER[tab].setVisible(flag);
        LASTUPDATED[tab].setVisible(flag);
        SCREENSHOT[tab].setVisible(flag);
        LICENSE[tab].setVisible(flag);
        FEED[tab].setVisible(flag);
        DESCRIPTION_LABEL[tab].setVisible(flag);
        HOMEPAGE[tab].setVisible(flag);
        LICENSE_LABEL[tab].setVisible(flag);
        FEED_LABEL[tab].setVisible(flag);
        DESCRIPTION[tab].getParent().getParent().setVisible(flag);
        INSTALL_BUTTON[tab].setVisible(flag);
        jButton15.setEnabled(flag); //Update All button
        if(flag) {
            selected[tab] = filtered[tab].get(index);
            if(selected[tab].name!=null) {
                NAME[tab].setText("<html>" + selected[tab].name);
            } else {
                NAME[tab].setText("<html>" + UNKNOWN_VALUE);
            }
            if(selected[tab].version!=null) {
                VERSION[tab].setText("<html><b>" + bundle.getString("VERSION:") +
                        "</b> &nbsp;&nbsp;&nbsp;" + selected[tab].version);
            } else {
                VERSION[tab].setText("<html><b>" + bundle.getString("VERSION:") +
                        "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
            }
            if(selected[tab].developer!=null) {
                if(!selected[tab].developer.equals("N/A")) {
                    DEVELOPER[tab].setText("<html><b>" + bundle.getString("DEVELOPER:") +
                            "</b> &nbsp;&nbsp;&nbsp;" + selected[tab].developer);
                } else {
                    DEVELOPER[tab].setText("<html><b>" + bundle.getString("DEVELOPER:") +
                            "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
                }
                
            } else {
                DEVELOPER[tab].setText("<html><b>" + bundle.getString("DEVELOPER:") +
                        "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
            }
            if(selected[tab].source!=null) {
                if(selected[tab].source.has("LastUpdated")) {
                    try {
                        String date = selected[tab].getFormattedDate();
                        if(date!=null) {
                            LASTUPDATED[tab].setText("<html><b>" + bundle.getString("LAST_UPDATED:") +
                                    "</b> &nbsp;&nbsp;&nbsp;" + date);
                        } else {
                            LASTUPDATED[tab].setText("<html><b>" + bundle.getString("LAST_UPDATED:") +
                                    "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
                        }
                    } catch(Exception e) {}
                } else {
                    LASTUPDATED[tab].setText("<html><b>" + bundle.getString("LAST_UPDATED:") +
                            "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
                }
                if(selected[tab].source.has("License")) {
                    try {
                        String license = selected[tab].source.getString("License");
                        if(license.length()==0) {
                            license = UNKNOWN_VALUE;
                        }
                        LICENSE[tab].setText("<html>" + license);
                    } catch(Exception e) {}
                } else {
                    LICENSE[tab].setText("<html>" + UNKNOWN_VALUE);
                }
                if(selected[tab].source.has("Feed")) {
                    try {
                        FEED[tab].setText("<html>" + selected[tab].source
                                .getString("Feed"));
                    } catch(Exception e) {}
                } else {
                    FEED[tab].setText("<html>" + UNKNOWN_VALUE);
                }
                if(selected[tab].source.has("FullDescription")) {
                    try {
                        DESCRIPTION[tab].setText("<html>" + selected[tab].source
                                .getString("FullDescription"));
                        DESCRIPTION[tab].setCaretPosition(0);
                    } catch(Exception e) {}
                } else {
                    DESCRIPTION[tab].setText("<html>" + UNKNOWN_VALUE);
                }
                /*if(selected[tab].source.has("Icon")) {
                    try {
                        String iconUrl = selected[tab].source.getString("Icon");
                        String currName = NAME[tab].getText().replaceFirst("<html>", "");
                        NAME[tab].setText("<html><img src=\"" + iconUrl + "\" width" +
                                "=\"48\" height=\"48\">&nbsp;&nbsp;" + currName);
                    } catch(Exception e) {}
                } else {
                    NAME[tab].setIcon(null);
                }*/
                new LoadScreenshot(tab).start();
                /*if(selected[tab].source.has("Screenshots")) {
                    try {
                        JSONArray ss = selected[tab].source.getJSONArray("Screenshots");
                        if(ss.length()>0) {
                            SCREENSHOT[tab].setIcon(null);
                            SCREENSHOT[tab].setText("<html><center><img src=\"" +
                                    ss.getString(0) + "\" width=\"180\" " +
                                    "height=\"240\"></center>");
                        } else {
                            SCREENSHOT[tab].setText("");
                            SCREENSHOT[tab].setIcon(NO_SCREENSHOT);
                        }
                    } catch(Exception e) {}
                } else {
                    SCREENSHOT[tab].setText("");
                    SCREENSHOT[tab].setIcon(NO_SCREENSHOT);
                }*/
                if(!selected[tab].source.has("Homepage")) {
                    HOMEPAGE[tab].setVisible(false);
                }
            } else {
                LASTUPDATED[tab].setText("<html><b>" + bundle.getString("LAST_UPDATED:") +
                    "</b> &nbsp;&nbsp;&nbsp;" + UNKNOWN_VALUE);
                LICENSE[tab].setText("<html>" + UNKNOWN_VALUE);
                FEED[tab].setText("<html>" + UNKNOWN_VALUE);
                DESCRIPTION[tab].setText("<html>" + UNKNOWN_VALUE);
                SCREENSHOT[tab].setText("");
                SCREENSHOT[tab].setIcon(NO_SCREENSHOT);
                HOMEPAGE[tab].setVisible(false);
            }
        } else { //no packages selected,aka no packages found
            selected[tab] = null;
            if(tab==packages.length-1) {
                NAME[tab].setText("<html>" + bundle.getString("NO_UPDATES_FOUND"));
            } else {
                NAME[tab].setText("<html>" + bundle.getString("NO_PACKAGES_FOUND"));
            }
            DESCRIPTION[tab].setText("");
        }
    }

    private void query() {
        int tab = jTabbedPane2.getSelectedIndex();
        String toSearch = SEARCH[tab].getText().trim();
        filtered[tab] = new ArrayList<PackageEntry>(0);
        for(int i=0; i<packages[tab].size(); i++) {
            PackageEntry curr = packages[tab].get(i);
            if(curr.name.toLowerCase().contains(toSearch.toLowerCase())) {
                filtered[tab].add(curr);
            }
        }
        Object[] data = new Object[filtered[tab].size()];
        for(int i=0; i<filtered[tab].size(); i++) {
            data[i] = filtered[tab].get(i).name;
        }
        LIST[tab].setListData(data);
        if(data.length>0) {
            LIST[tab].setSelectedIndex(0);
        }
        displayPackage(tab);
    }

    private void install() {
        int tab = jTabbedPane2.getSelectedIndex();
        ArrayList<PackageEntry> pkgList = getInstallList(selected[tab]);
        String msg = "<html><body width=\"300px\">" +
                MessageFormat.format(bundle.getString("DEPENDANCY_INSTALL_WARNING"),
                new Object[] {selected[tab].name}) + "\n";
        ArrayList installList = new ArrayList();
        for(int i=0; i<pkgList.size(); i++) {
            PackageEntry curr = pkgList.get(i);
            if(!pkgMgr.isInstalled(curr)) {
                if(!curr.id.equals(selected[tab].id)) {
                    msg += "\n" + curr.name;
                }
                installList.add(curr.getDownloadUrl());
            }
        }
        if(installList.size()>1) {
            if(JOptionPane.showConfirmDialog(rootPane, msg,
                    bundle.getString("WARNING"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION) {
                doInstall(tab, installList);
            }
        } else {
            doInstall(tab, installList);
        }
    }

    private ArrayList<PackageEntry> getInstallList(PackageEntry pkg) {
        ArrayList<PackageEntry> result = new ArrayList(0);
        String[] depends = pkg.depends;
        if(depends!=null) {
            for(int i=0; i<depends.length; i++) {
                result.addAll(getInstallList(pkgMgr.getEntryById(depends[i])));
            }
        }
        result.add(pkg);
        return result;
    }

    private void doInstall(int tab, ArrayList installList) {
        JFrame mainFrame = WebOSQuickInstallApp.getApplication().getMainFrame();
        Installer installer = new Installer(mainFrame, webOS, installList);
        installer.setLocationRelativeTo(mainFrame);
        WebOSQuickInstallApp.getApplication().show(installer);
        pkgMgr.setInstalledAppList(webOS.listInstalled());
        if(tab!=packages.length-1) {
            loadCategory(tab);
            SEARCH[tab].setText("");
        } else {
            packages[tab] = null;
            loadUpdates();
        }
        for(int i=0; i<packages.length; i++) {
            if(i!=tab) {
                packages[i] = null;
            }
        }
    }

    private void openHomepage() {
        int tab = jTabbedPane2.getSelectedIndex();
        if(selected[tab].source!=null) {
            if(selected[tab].source.has("Homepage")) {
                try {
                    openUrl(new URL(selected[tab].source.getString("Homepage")));
                } catch(Exception e) {}
            }
        }
    }

    private void openUrl(URL path) {
        try {
            Desktop.getDesktop().browse(path.toURI());
        } catch(Exception e) {}
    }

    private void loadUpdates() {
        int tab = packages.length-1;
        if(packages[tab]==null) {
            packages[tab] = pkgMgr.getUpdates();
            filtered[tab] = packages[tab];
            Object[] data = new Object[filtered[tab].size()];
            for(int i=0; i<filtered[tab].size(); i++) {
                data[i] = filtered[tab].get(i).name;
            }
            LIST[tab].setListData(data);
            if(data.length>0) {
                LIST[tab].setSelectedIndex(0);
            }
            displayPackage(tab);
        }
    }

    private void update() {
        int tab = packages.length-1;
        ArrayList installList = new ArrayList();
        installList.add(selected[tab].getDownloadUrl());
        doInstall(tab, installList);
        packages[tab] = null;
        loadUpdates();
        if(filtered[tab].size()==0) {
            jButton15.setEnabled(false);
        }

    }

    private void updateAll() {
        int tab = packages.length-1;
        ArrayList installList = new ArrayList();
        for(int i=0; i<filtered[tab].size(); i++) {
            installList.add(filtered[tab].get(i).getDownloadUrl());
        }
        doInstall(tab, installList);
        if(filtered[tab].size()==0) {
            jButton15.setEnabled(false);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jLayeredPane2 = new ImageLayeredPane(background);
        jLabel5 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jLabel3 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jLayeredPane3 = new ImageLayeredPane(background);
        jLabel20 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jLabel17 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel19 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel18 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jLabel13 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jLayeredPane4 = new ImageLayeredPane(background);
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextPane3 = new javax.swing.JTextPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList();
        jLabel29 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jTextField3 = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLayeredPane5 = new ImageLayeredPane(background);
        jScrollPane7 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList();
        jSeparator4 = new javax.swing.JSeparator();
        jButton7 = new javax.swing.JButton();
        jLabel40 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox();
        jLabel43 = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTextPane4 = new javax.swing.JTextPane();
        jLabel39 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel41 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jLabel42 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLayeredPane9 = new ImageLayeredPane(background);
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jLabel94 = new javax.swing.JLabel();
        jLabel95 = new javax.swing.JLabel();
        jScrollPane17 = new javax.swing.JScrollPane();
        jList9 = new javax.swing.JList();
        jLabel97 = new javax.swing.JLabel();
        jLabel93 = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        jLabel92 = new javax.swing.JLabel();
        jLabel98 = new javax.swing.JLabel();
        jScrollPane18 = new javax.swing.JScrollPane();
        jTextPane9 = new javax.swing.JTextPane();
        jLabel91 = new javax.swing.JLabel();
        jLabel96 = new javax.swing.JLabel();
        jLabel90 = new javax.swing.JLabel();
        jLabel99 = new javax.swing.JLabel();
        jComboBox9 = new javax.swing.JComboBox();
        jLabel89 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jLayeredPane6 = new ImageLayeredPane(background);
        jLabel45 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel49 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jLabel52 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTextPane5 = new javax.swing.JTextPane();
        jTextField5 = new javax.swing.JTextField();
        jLabel47 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jScrollPane9 = new javax.swing.JScrollPane();
        jList5 = new javax.swing.JList();
        jLabel54 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        jComboBox5 = new javax.swing.JComboBox();
        jLayeredPane7 = new ImageLayeredPane(background);
        jButton12 = new javax.swing.JButton();
        jLabel64 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jScrollPane12 = new javax.swing.JScrollPane();
        jTextPane6 = new javax.swing.JTextPane();
        jLabel63 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        jScrollPane11 = new javax.swing.JScrollPane();
        jList6 = new javax.swing.JList();
        jLabel65 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel62 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jComboBox6 = new javax.swing.JComboBox();
        jLayeredPane8 = new ImageLayeredPane(background);
        jButton13 = new javax.swing.JButton();
        jLabel72 = new javax.swing.JLabel();
        jScrollPane13 = new javax.swing.JScrollPane();
        jList7 = new javax.swing.JList();
        jLabel73 = new javax.swing.JLabel();
        jLabel75 = new javax.swing.JLabel();
        jComboBox7 = new javax.swing.JComboBox();
        jLabel76 = new javax.swing.JLabel();
        jLabel71 = new javax.swing.JLabel();
        jButton14 = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JSeparator();
        jLabel68 = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jLabel74 = new javax.swing.JLabel();
        jLabel77 = new javax.swing.JLabel();
        jLabel70 = new javax.swing.JLabel();
        jScrollPane14 = new javax.swing.JScrollPane();
        jTextPane7 = new javax.swing.JTextPane();
        jLabel67 = new javax.swing.JLabel();
        jLayeredPane1 = new ImageLayeredPane(background);
        jLabel88 = new javax.swing.JLabel();
        jLabel84 = new javax.swing.JLabel();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jScrollPane15 = new javax.swing.JScrollPane();
        jList8 = new javax.swing.JList();
        jLabel81 = new javax.swing.JLabel();
        jLabel83 = new javax.swing.JLabel();
        jLabel85 = new javax.swing.JLabel();
        jLabel82 = new javax.swing.JLabel();
        jLabel79 = new javax.swing.JLabel();
        jLabel80 = new javax.swing.JLabel();
        jScrollPane16 = new javax.swing.JScrollPane();
        jTextPane8 = new javax.swing.JTextPane();
        jSeparator8 = new javax.swing.JSeparator();
        jLabel87 = new javax.swing.JLabel();
        jLabel78 = new javax.swing.JLabel();
        jLabel86 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(null);
        setModal(true);
        setName("feedViewer"); // NOI18N
        setResizable(false);

        jTabbedPane1.setBackground(new java.awt.Color(245, 243, 244));
        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        jTabbedPane1.setFocusable(false);
        jTabbedPane1.setFont(jTabbedPane1.getFont().deriveFont(jTabbedPane1.getFont().getSize()+3f));
        jTabbedPane1.setName("mainTabs"); // NOI18N
        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jTabbedPane2.setBackground(new java.awt.Color(245, 243, 244));
        jTabbedPane2.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane2.setFocusable(false);
        jTabbedPane2.setFont(jTabbedPane2.getFont().deriveFont(jTabbedPane2.getFont().getStyle() | java.awt.Font.BOLD));
        jTabbedPane2.setName("availablePackagesTabs"); // NOI18N
        jTabbedPane2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane2StateChanged(evt);
            }
        });

        jLayeredPane2.setName("jLayeredPane2"); // NOI18N

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getSize()+1f));
        jLabel5.setText(bundle.getString("FeedViewer.jLabel5.text")); // NOI18N
        jLabel5.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setBounds(230, 80, 280, 30);
        jLayeredPane2.add(jLabel5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()+1f));
        jLabel9.setText(bundle.getString("FeedViewer.jLabel9.text")); // NOI18N
        jLabel9.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel9.setName("jLabel9"); // NOI18N
        jLabel9.setBounds(230, 110, 280, 30);
        jLayeredPane2.add(jLabel9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton1.setText(bundle.getString("FeedViewer.jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jButton1.setBounds(150, 54, 30, 25);
        jLayeredPane2.add(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getSize()+1f));
        jLabel4.setText(bundle.getString("FeedViewer.jLabel4.text")); // NOI18N
        jLabel4.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setBounds(550, 350, 120, 20);
        jLayeredPane2.add(jLabel4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField1.setText(bundle.getString("FeedViewer.jTextField1.text")); // NOI18N
        jTextField1.setName("jTextField1"); // NOI18N
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });
        jTextField1.setBounds(10, 55, 140, 23);
        jLayeredPane2.add(jTextField1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()+13f));
        jLabel2.setText(bundle.getString("FeedViewer.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setBounds(230, 10, 280, 70);
        jLayeredPane2.add(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getSize()+1f));
        jLabel6.setText(bundle.getString("FeedViewer.jLabel6.text")); // NOI18N
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel6.setName("jLabel6"); // NOI18N
        jLabel6.setBounds(230, 140, 280, 30);
        jLayeredPane2.add(jLabel6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox1.setMaximumRowCount(10);
        jComboBox1.setFocusable(false);
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jComboBox1.setBounds(10, 20, 170, 22);
        jLayeredPane2.add(jComboBox1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator1.setName("jSeparator1"); // NOI18N
        jSeparator1.setBounds(200, 20, 10, 390);
        jLayeredPane2.add(jSeparator1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setOpaque(false);

        jTextPane1.setBorder(null);
        jTextPane1.setContentType(bundle.getString("FeedViewer.jTextPane1.contentType")); // NOI18N
        jTextPane1.setEditable(false);
        jTextPane1.setFont(jTextPane1.getFont());
        jTextPane1.setText(bundle.getString("FeedViewer.jTextPane1.text")); // NOI18N
        jTextPane1.setName("jTextPane1"); // NOI18N
        jTextPane1.setOpaque(false);
        jTextPane1.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane1HyperlinkUpdate(evt);
            }
        });
        jScrollPane2.setViewportView(jTextPane1);

        jScrollPane2.setBounds(230, 200, 280, 170);
        jLayeredPane2.add(jScrollPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getSize()+1f));
        jLabel3.setText(bundle.getString("FeedViewer.jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setBounds(550, 290, 120, 20);
        jLayeredPane2.add(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getSize()+1f));
        jLabel10.setText(bundle.getString("FeedViewer.jLabel10.text")); // NOI18N
        jLabel10.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel10.setName("jLabel10"); // NOI18N
        jLabel10.setBounds(550, 310, 120, 40);
        jLayeredPane2.add(jLabel10, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()+1f));
        jLabel8.setText(bundle.getString("FeedViewer.jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.setBounds(230, 170, 140, 20);
        jLayeredPane2.add(jLabel8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel1.setText(bundle.getString("FeedViewer.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setBounds(520, 10, 180, 240);
        jLayeredPane2.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getStyle() | java.awt.Font.BOLD, jLabel11.getFont().getSize()+1));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel11.setText(bundle.getString("FeedViewer.jLabel11.text")); // NOI18N
        jLabel11.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel11.setName("jLabel11"); // NOI18N
        jLabel11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel11MouseClicked(evt);
            }
        });
        jLabel11.setBounds(550, 260, 120, 20);
        jLayeredPane2.add(jLabel11, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()+1f));
        jLabel7.setText(bundle.getString("FeedViewer.jLabel7.text")); // NOI18N
        jLabel7.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel7.setName("jLabel7"); // NOI18N
        jLabel7.setBounds(550, 370, 120, 40);
        jLayeredPane2.add(jLabel7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton2.setBackground(new java.awt.Color(224, 218, 218));
        jButton2.setFont(jButton2.getFont().deriveFont(jButton2.getFont().getSize()+1f));
        jButton2.setText(bundle.getString("FeedViewer.jButton2.text")); // NOI18N
        jButton2.setFocusable(false);
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jButton2.setBounds(370, 380, 90, 30);
        jLayeredPane2.add(jButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setName("jList1"); // NOI18N
        jList1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jScrollPane1.setBounds(10, 90, 170, 320);
        jLayeredPane2.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane2.TabConstraints.tabTitle"), jLayeredPane2); // NOI18N

        jLayeredPane3.setName("jLayeredPane3"); // NOI18N

        jLabel20.setFont(jLabel20.getFont().deriveFont(jLabel20.getFont().getSize()+1f));
        jLabel20.setText(bundle.getString("FeedViewer.jLabel20.text")); // NOI18N
        jLabel20.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel20.setName("jLabel20"); // NOI18N
        jLabel20.setBounds(230, 110, 280, 30);
        jLayeredPane3.add(jLabel20, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getSize()+1f));
        jLabel15.setText(bundle.getString("FeedViewer.jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N
        jLabel15.setBounds(550, 290, 120, 20);
        jLayeredPane3.add(jLabel15, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jTextPane2.setBorder(null);
        jTextPane2.setContentType(bundle.getString("FeedViewer.jTextPane2.contentType")); // NOI18N
        jTextPane2.setEditable(false);
        jTextPane2.setFont(jTextPane2.getFont());
        jTextPane2.setText(bundle.getString("FeedViewer.jTextPane2.text")); // NOI18N
        jTextPane2.setName("jTextPane2"); // NOI18N
        jTextPane2.setOpaque(false);
        jTextPane2.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane2HyperlinkUpdate(evt);
            }
        });
        jScrollPane4.setViewportView(jTextPane2);

        jScrollPane4.setBounds(230, 200, 280, 170);
        jLayeredPane3.add(jScrollPane4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel17.setFont(jLabel17.getFont().deriveFont(jLabel17.getFont().getSize()+1f));
        jLabel17.setText(bundle.getString("FeedViewer.jLabel17.text")); // NOI18N
        jLabel17.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel17.setName("jLabel17"); // NOI18N
        jLabel17.setBounds(230, 140, 280, 30);
        jLayeredPane3.add(jLabel17, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel12.setText(bundle.getString("FeedViewer.jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N
        jLabel12.setBounds(520, 10, 180, 240);
        jLayeredPane3.add(jLabel12, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField2.setText(bundle.getString("FeedViewer.jTextField2.text")); // NOI18N
        jTextField2.setName("jTextField2"); // NOI18N
        jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField2KeyTyped(evt);
            }
        });
        jTextField2.setBounds(10, 55, 140, 23);
        jLayeredPane3.add(jTextField2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel16.setFont(jLabel16.getFont().deriveFont(jLabel16.getFont().getSize()+1f));
        jLabel16.setText(bundle.getString("FeedViewer.jLabel16.text")); // NOI18N
        jLabel16.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel16.setName("jLabel16"); // NOI18N
        jLabel16.setBounds(230, 80, 280, 30);
        jLayeredPane3.add(jLabel16, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel21.setFont(jLabel21.getFont().deriveFont(jLabel21.getFont().getSize()+1f));
        jLabel21.setText(bundle.getString("FeedViewer.jLabel21.text")); // NOI18N
        jLabel21.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel21.setName("jLabel21"); // NOI18N
        jLabel21.setBounds(550, 310, 120, 40);
        jLayeredPane3.add(jLabel21, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox2.setMaximumRowCount(10);
        jComboBox2.setFocusable(false);
        jComboBox2.setName("jComboBox2"); // NOI18N
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });
        jComboBox2.setBounds(10, 20, 170, 22);
        jLayeredPane3.add(jComboBox2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel19.setFont(jLabel19.getFont().deriveFont(jLabel19.getFont().getSize()+1f));
        jLabel19.setText(bundle.getString("FeedViewer.jLabel19.text")); // NOI18N
        jLabel19.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel19.setName("jLabel19"); // NOI18N
        jLabel19.setBounds(550, 370, 120, 40);
        jLayeredPane3.add(jLabel19, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getSize()+1f));
        jLabel14.setText(bundle.getString("FeedViewer.jLabel14.text")); // NOI18N
        jLabel14.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel14.setName("jLabel14"); // NOI18N
        jLabel14.setBounds(550, 350, 120, 20);
        jLayeredPane3.add(jLabel14, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setName("jSeparator2"); // NOI18N
        jSeparator2.setBounds(200, 20, 10, 390);
        jLayeredPane3.add(jSeparator2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel18.setFont(jLabel18.getFont().deriveFont(jLabel18.getFont().getSize()+1f));
        jLabel18.setText(bundle.getString("FeedViewer.jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N
        jLabel18.setBounds(230, 170, 140, 20);
        jLayeredPane3.add(jLabel18, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton3.setText(bundle.getString("FeedViewer.jButton3.text")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jButton3.setBounds(150, 54, 30, 25);
        jLayeredPane3.add(jButton3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jList2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList2.setName("jList2"); // NOI18N
        jList2.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList2ValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jList2);

        jScrollPane3.setBounds(10, 90, 170, 320);
        jLayeredPane3.add(jScrollPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel13.setFont(jLabel13.getFont().deriveFont(jLabel13.getFont().getSize()+13f));
        jLabel13.setText(bundle.getString("FeedViewer.jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N
        jLabel13.setBounds(230, 10, 280, 70);
        jLayeredPane3.add(jLabel13, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel22.setFont(jLabel22.getFont().deriveFont(jLabel22.getFont().getStyle() | java.awt.Font.BOLD, jLabel22.getFont().getSize()+1));
        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel22.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel22.setText(bundle.getString("FeedViewer.jLabel22.text")); // NOI18N
        jLabel22.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel22.setName("jLabel22"); // NOI18N
        jLabel22.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel22MouseClicked(evt);
            }
        });
        jLabel22.setBounds(550, 260, 120, 20);
        jLayeredPane3.add(jLabel22, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton4.setBackground(new java.awt.Color(224, 218, 218));
        jButton4.setFont(jButton4.getFont().deriveFont(jButton4.getFont().getSize()+1f));
        jButton4.setText(bundle.getString("FeedViewer.jButton4.text")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jButton4.setBounds(370, 380, 90, 30);
        jLayeredPane3.add(jButton4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane3.TabConstraints.tabTitle"), jLayeredPane3); // NOI18N

        jLayeredPane4.setName("jLayeredPane4"); // NOI18N

        jScrollPane6.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane6.setName("jScrollPane6"); // NOI18N

        jTextPane3.setBorder(null);
        jTextPane3.setContentType(bundle.getString("FeedViewer.jTextPane3.contentType")); // NOI18N
        jTextPane3.setEditable(false);
        jTextPane3.setFont(jTextPane3.getFont());
        jTextPane3.setText(bundle.getString("FeedViewer.jTextPane3.text")); // NOI18N
        jTextPane3.setName("jTextPane3"); // NOI18N
        jTextPane3.setOpaque(false);
        jTextPane3.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane3HyperlinkUpdate(evt);
            }
        });
        jScrollPane6.setViewportView(jTextPane3);

        jScrollPane6.setBounds(230, 200, 280, 170);
        jLayeredPane4.add(jScrollPane6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jList3.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList3.setName("jList3"); // NOI18N
        jList3.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList3ValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(jList3);

        jScrollPane5.setBounds(10, 90, 170, 320);
        jLayeredPane4.add(jScrollPane5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel29.setFont(jLabel29.getFont().deriveFont(jLabel29.getFont().getSize()+1f));
        jLabel29.setText(bundle.getString("FeedViewer.jLabel29.text")); // NOI18N
        jLabel29.setName("jLabel29"); // NOI18N
        jLabel29.setBounds(230, 170, 140, 20);
        jLayeredPane4.add(jLabel29, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel32.setFont(jLabel32.getFont().deriveFont(jLabel32.getFont().getSize()+1f));
        jLabel32.setText(bundle.getString("FeedViewer.jLabel32.text")); // NOI18N
        jLabel32.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel32.setName("jLabel32"); // NOI18N
        jLabel32.setBounds(550, 310, 120, 40);
        jLayeredPane4.add(jLabel32, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel26.setFont(jLabel26.getFont().deriveFont(jLabel26.getFont().getSize()+1f));
        jLabel26.setText(bundle.getString("FeedViewer.jLabel26.text")); // NOI18N
        jLabel26.setName("jLabel26"); // NOI18N
        jLabel26.setBounds(550, 290, 120, 20);
        jLayeredPane4.add(jLabel26, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton5.setText(bundle.getString("FeedViewer.jButton5.text")); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jButton5.setBounds(150, 54, 30, 25);
        jLayeredPane4.add(jButton5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton6.setBackground(new java.awt.Color(224, 218, 218));
        jButton6.setFont(jButton6.getFont().deriveFont(jButton6.getFont().getSize()+1f));
        jButton6.setText(bundle.getString("FeedViewer.jButton6.text")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jButton6.setBounds(370, 380, 90, 30);
        jLayeredPane4.add(jButton6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel27.setFont(jLabel27.getFont().deriveFont(jLabel27.getFont().getSize()+1f));
        jLabel27.setText(bundle.getString("FeedViewer.jLabel27.text")); // NOI18N
        jLabel27.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel27.setName("jLabel27"); // NOI18N
        jLabel27.setBounds(230, 80, 280, 30);
        jLayeredPane4.add(jLabel27, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel33.setFont(jLabel33.getFont().deriveFont(jLabel33.getFont().getStyle() | java.awt.Font.BOLD, jLabel33.getFont().getSize()+1));
        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel33.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel33.setText(bundle.getString("FeedViewer.jLabel33.text")); // NOI18N
        jLabel33.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel33.setName("jLabel33"); // NOI18N
        jLabel33.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel33MouseClicked(evt);
            }
        });
        jLabel33.setBounds(550, 260, 120, 20);
        jLayeredPane4.add(jLabel33, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel30.setFont(jLabel30.getFont().deriveFont(jLabel30.getFont().getSize()+1f));
        jLabel30.setText(bundle.getString("FeedViewer.jLabel30.text")); // NOI18N
        jLabel30.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel30.setName("jLabel30"); // NOI18N
        jLabel30.setBounds(550, 370, 120, 40);
        jLayeredPane4.add(jLabel30, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator3.setName("jSeparator3"); // NOI18N
        jSeparator3.setBounds(200, 20, 10, 390);
        jLayeredPane4.add(jSeparator3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField3.setText(bundle.getString("FeedViewer.jTextField3.text")); // NOI18N
        jTextField3.setName("jTextField3"); // NOI18N
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField3KeyTyped(evt);
            }
        });
        jTextField3.setBounds(10, 55, 140, 23);
        jLayeredPane4.add(jTextField3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel23.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel23.setText(bundle.getString("FeedViewer.jLabel23.text")); // NOI18N
        jLabel23.setName("jLabel23"); // NOI18N
        jLabel23.setBounds(520, 10, 180, 240);
        jLayeredPane4.add(jLabel23, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox3.setMaximumRowCount(10);
        jComboBox3.setFocusable(false);
        jComboBox3.setName("jComboBox3"); // NOI18N
        jComboBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox3ActionPerformed(evt);
            }
        });
        jComboBox3.setBounds(10, 20, 170, 22);
        jLayeredPane4.add(jComboBox3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel24.setFont(jLabel24.getFont().deriveFont(jLabel24.getFont().getSize()+13f));
        jLabel24.setText(bundle.getString("FeedViewer.jLabel24.text")); // NOI18N
        jLabel24.setName("jLabel24"); // NOI18N
        jLabel24.setBounds(230, 10, 280, 70);
        jLayeredPane4.add(jLabel24, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel31.setFont(jLabel31.getFont().deriveFont(jLabel31.getFont().getSize()+1f));
        jLabel31.setText(bundle.getString("FeedViewer.jLabel31.text")); // NOI18N
        jLabel31.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel31.setName("jLabel31"); // NOI18N
        jLabel31.setBounds(230, 110, 280, 30);
        jLayeredPane4.add(jLabel31, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel28.setFont(jLabel28.getFont().deriveFont(jLabel28.getFont().getSize()+1f));
        jLabel28.setText(bundle.getString("FeedViewer.jLabel28.text")); // NOI18N
        jLabel28.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel28.setName("jLabel28"); // NOI18N
        jLabel28.setBounds(230, 140, 280, 30);
        jLayeredPane4.add(jLabel28, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel25.setFont(jLabel25.getFont().deriveFont(jLabel25.getFont().getSize()+1f));
        jLabel25.setText(bundle.getString("FeedViewer.jLabel25.text")); // NOI18N
        jLabel25.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel25.setName("jLabel25"); // NOI18N
        jLabel25.setBounds(550, 350, 120, 20);
        jLayeredPane4.add(jLabel25, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane4.TabConstraints.tabTitle"), jLayeredPane4); // NOI18N

        jLayeredPane5.setName("jLayeredPane5"); // NOI18N

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        jList4.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList4.setName("jList4"); // NOI18N
        jList4.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList4ValueChanged(evt);
            }
        });
        jScrollPane7.setViewportView(jList4);

        jScrollPane7.setBounds(10, 90, 170, 320);
        jLayeredPane5.add(jScrollPane7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator4.setName("jSeparator4"); // NOI18N
        jSeparator4.setBounds(200, 20, 10, 390);
        jLayeredPane5.add(jSeparator4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton7.setText(bundle.getString("FeedViewer.jButton7.text")); // NOI18N
        jButton7.setFocusable(false);
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jButton7.setBounds(150, 54, 30, 25);
        jLayeredPane5.add(jButton7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel40.setFont(jLabel40.getFont().deriveFont(jLabel40.getFont().getSize()+1f));
        jLabel40.setText(bundle.getString("FeedViewer.jLabel40.text")); // NOI18N
        jLabel40.setName("jLabel40"); // NOI18N
        jLabel40.setBounds(230, 170, 140, 20);
        jLayeredPane5.add(jLabel40, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel38.setFont(jLabel38.getFont().deriveFont(jLabel38.getFont().getSize()+1f));
        jLabel38.setText(bundle.getString("FeedViewer.jLabel38.text")); // NOI18N
        jLabel38.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel38.setName("jLabel38"); // NOI18N
        jLabel38.setBounds(230, 80, 280, 30);
        jLayeredPane5.add(jLabel38, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel37.setFont(jLabel37.getFont().deriveFont(jLabel37.getFont().getSize()+1f));
        jLabel37.setText(bundle.getString("FeedViewer.jLabel37.text")); // NOI18N
        jLabel37.setName("jLabel37"); // NOI18N
        jLabel37.setBounds(550, 290, 120, 20);
        jLayeredPane5.add(jLabel37, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel44.setFont(jLabel44.getFont().deriveFont(jLabel44.getFont().getStyle() | java.awt.Font.BOLD, jLabel44.getFont().getSize()+1));
        jLabel44.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel44.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel44.setText(bundle.getString("FeedViewer.jLabel44.text")); // NOI18N
        jLabel44.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel44.setName("jLabel44"); // NOI18N
        jLabel44.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel44MouseClicked(evt);
            }
        });
        jLabel44.setBounds(550, 260, 120, 20);
        jLayeredPane5.add(jLabel44, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel35.setFont(jLabel35.getFont().deriveFont(jLabel35.getFont().getSize()+13f));
        jLabel35.setText(bundle.getString("FeedViewer.jLabel35.text")); // NOI18N
        jLabel35.setName("jLabel35"); // NOI18N
        jLabel35.setBounds(230, 10, 280, 70);
        jLayeredPane5.add(jLabel35, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox4.setMaximumRowCount(10);
        jComboBox4.setFocusable(false);
        jComboBox4.setName("jComboBox4"); // NOI18N
        jComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox4ActionPerformed(evt);
            }
        });
        jComboBox4.setBounds(10, 20, 170, 22);
        jLayeredPane5.add(jComboBox4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel43.setFont(jLabel43.getFont().deriveFont(jLabel43.getFont().getSize()+1f));
        jLabel43.setText(bundle.getString("FeedViewer.jLabel43.text")); // NOI18N
        jLabel43.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel43.setName("jLabel43"); // NOI18N
        jLabel43.setBounds(550, 310, 120, 40);
        jLayeredPane5.add(jLabel43, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane8.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane8.setName("jScrollPane8"); // NOI18N

        jTextPane4.setBorder(null);
        jTextPane4.setContentType(bundle.getString("FeedViewer.jTextPane4.contentType")); // NOI18N
        jTextPane4.setEditable(false);
        jTextPane4.setFont(jTextPane4.getFont());
        jTextPane4.setText(bundle.getString("FeedViewer.jTextPane4.text")); // NOI18N
        jTextPane4.setName("jTextPane4"); // NOI18N
        jTextPane4.setOpaque(false);
        jTextPane4.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane4HyperlinkUpdate(evt);
            }
        });
        jScrollPane8.setViewportView(jTextPane4);

        jScrollPane8.setBounds(230, 200, 280, 170);
        jLayeredPane5.add(jScrollPane8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel39.setFont(jLabel39.getFont().deriveFont(jLabel39.getFont().getSize()+1f));
        jLabel39.setText(bundle.getString("FeedViewer.jLabel39.text")); // NOI18N
        jLabel39.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel39.setName("jLabel39"); // NOI18N
        jLabel39.setBounds(230, 140, 280, 30);
        jLayeredPane5.add(jLabel39, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField4.setText(bundle.getString("FeedViewer.jTextField4.text")); // NOI18N
        jTextField4.setName("jTextField4"); // NOI18N
        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField4KeyTyped(evt);
            }
        });
        jTextField4.setBounds(10, 55, 140, 23);
        jLayeredPane5.add(jTextField4, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel41.setFont(jLabel41.getFont().deriveFont(jLabel41.getFont().getSize()+1f));
        jLabel41.setText(bundle.getString("FeedViewer.jLabel41.text")); // NOI18N
        jLabel41.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel41.setName("jLabel41"); // NOI18N
        jLabel41.setBounds(550, 370, 130, 40);
        jLayeredPane5.add(jLabel41, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel34.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel34.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel34.setText(bundle.getString("FeedViewer.jLabel34.text")); // NOI18N
        jLabel34.setName("jLabel34"); // NOI18N
        jLabel34.setBounds(520, 10, 180, 240);
        jLayeredPane5.add(jLabel34, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton8.setBackground(new java.awt.Color(224, 218, 218));
        jButton8.setFont(jButton8.getFont().deriveFont(jButton8.getFont().getSize()+1f));
        jButton8.setText(bundle.getString("FeedViewer.jButton8.text")); // NOI18N
        jButton8.setFocusable(false);
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jButton8.setBounds(370, 380, 90, 30);
        jLayeredPane5.add(jButton8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel42.setFont(jLabel42.getFont().deriveFont(jLabel42.getFont().getSize()+1f));
        jLabel42.setText(bundle.getString("FeedViewer.jLabel42.text")); // NOI18N
        jLabel42.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel42.setName("jLabel42"); // NOI18N
        jLabel42.setBounds(230, 110, 280, 30);
        jLayeredPane5.add(jLabel42, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel36.setFont(jLabel36.getFont().deriveFont(jLabel36.getFont().getSize()+1f));
        jLabel36.setText(bundle.getString("FeedViewer.jLabel36.text")); // NOI18N
        jLabel36.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel36.setName("jLabel36"); // NOI18N
        jLabel36.setBounds(550, 350, 120, 20);
        jLayeredPane5.add(jLabel36, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane5.TabConstraints.tabTitle"), jLayeredPane5); // NOI18N

        jLayeredPane9.setName("jLayeredPane9"); // NOI18N

        jButton17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton17.setText(bundle.getString("FeedViewer.jButton17.text")); // NOI18N
        jButton17.setFocusable(false);
        jButton17.setName("jButton17"); // NOI18N
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });
        jButton17.setBounds(150, 54, 30, 25);
        jLayeredPane9.add(jButton17, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton18.setBackground(new java.awt.Color(224, 218, 218));
        jButton18.setFont(jButton18.getFont().deriveFont(jButton18.getFont().getSize()+1f));
        jButton18.setText(bundle.getString("FeedViewer.jButton18.text")); // NOI18N
        jButton18.setFocusable(false);
        jButton18.setName("jButton18"); // NOI18N
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });
        jButton18.setBounds(370, 380, 90, 30);
        jLayeredPane9.add(jButton18, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel94.setFont(jLabel94.getFont().deriveFont(jLabel94.getFont().getSize()+1f));
        jLabel94.setText(bundle.getString("FeedViewer.jLabel94.text")); // NOI18N
        jLabel94.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel94.setName("jLabel94"); // NOI18N
        jLabel94.setBounds(230, 140, 280, 30);
        jLayeredPane9.add(jLabel94, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel95.setFont(jLabel95.getFont().deriveFont(jLabel95.getFont().getSize()+1f));
        jLabel95.setText(bundle.getString("FeedViewer.jLabel95.text")); // NOI18N
        jLabel95.setName("jLabel95"); // NOI18N
        jLabel95.setBounds(230, 170, 140, 20);
        jLayeredPane9.add(jLabel95, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane17.setName("jScrollPane17"); // NOI18N

        jList9.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList9.setName("jList9"); // NOI18N
        jList9.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList9ValueChanged(evt);
            }
        });
        jScrollPane17.setViewportView(jList9);

        jScrollPane17.setBounds(10, 90, 170, 320);
        jLayeredPane9.add(jScrollPane17, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel97.setFont(jLabel97.getFont().deriveFont(jLabel97.getFont().getSize()+1f));
        jLabel97.setText(bundle.getString("FeedViewer.jLabel97.text")); // NOI18N
        jLabel97.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel97.setName("jLabel97"); // NOI18N
        jLabel97.setBounds(230, 110, 280, 30);
        jLayeredPane9.add(jLabel97, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel93.setFont(jLabel93.getFont().deriveFont(jLabel93.getFont().getSize()+1f));
        jLabel93.setText(bundle.getString("FeedViewer.jLabel93.text")); // NOI18N
        jLabel93.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel93.setName("jLabel93"); // NOI18N
        jLabel93.setBounds(230, 80, 280, 30);
        jLayeredPane9.add(jLabel93, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator9.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator9.setName("jSeparator9"); // NOI18N
        jSeparator9.setBounds(200, 20, 10, 390);
        jLayeredPane9.add(jSeparator9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel92.setFont(jLabel92.getFont().deriveFont(jLabel92.getFont().getSize()+1f));
        jLabel92.setText(bundle.getString("FeedViewer.jLabel92.text")); // NOI18N
        jLabel92.setName("jLabel92"); // NOI18N
        jLabel92.setBounds(550, 290, 120, 20);
        jLayeredPane9.add(jLabel92, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel98.setFont(jLabel98.getFont().deriveFont(jLabel98.getFont().getSize()+1f));
        jLabel98.setText(bundle.getString("FeedViewer.jLabel98.text")); // NOI18N
        jLabel98.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel98.setName("jLabel98"); // NOI18N
        jLabel98.setBounds(550, 310, 120, 40);
        jLayeredPane9.add(jLabel98, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane18.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane18.setName("jScrollPane18"); // NOI18N

        jTextPane9.setBorder(null);
        jTextPane9.setContentType(bundle.getString("FeedViewer.jTextPane9.contentType")); // NOI18N
        jTextPane9.setEditable(false);
        jTextPane9.setFont(jTextPane9.getFont());
        jTextPane9.setText(bundle.getString("FeedViewer.jTextPane9.text")); // NOI18N
        jTextPane9.setName("jTextPane9"); // NOI18N
        jTextPane9.setOpaque(false);
        jTextPane9.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane9HyperlinkUpdate(evt);
            }
        });
        jScrollPane18.setViewportView(jTextPane9);

        jScrollPane18.setBounds(230, 200, 280, 170);
        jLayeredPane9.add(jScrollPane18, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel91.setFont(jLabel91.getFont().deriveFont(jLabel91.getFont().getSize()+1f));
        jLabel91.setText(bundle.getString("FeedViewer.jLabel91.text")); // NOI18N
        jLabel91.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel91.setName("jLabel91"); // NOI18N
        jLabel91.setBounds(550, 350, 120, 20);
        jLayeredPane9.add(jLabel91, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel96.setFont(jLabel96.getFont().deriveFont(jLabel96.getFont().getSize()+1f));
        jLabel96.setText(bundle.getString("FeedViewer.jLabel96.text")); // NOI18N
        jLabel96.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel96.setName("jLabel96"); // NOI18N
        jLabel96.setBounds(550, 370, 120, 40);
        jLayeredPane9.add(jLabel96, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel90.setFont(jLabel90.getFont().deriveFont(jLabel90.getFont().getSize()+13f));
        jLabel90.setText(bundle.getString("FeedViewer.jLabel90.text")); // NOI18N
        jLabel90.setName("jLabel90"); // NOI18N
        jLabel90.setBounds(230, 10, 280, 70);
        jLayeredPane9.add(jLabel90, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel99.setFont(jLabel99.getFont().deriveFont(jLabel99.getFont().getStyle() | java.awt.Font.BOLD, jLabel99.getFont().getSize()+1));
        jLabel99.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel99.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel99.setText(bundle.getString("FeedViewer.jLabel99.text")); // NOI18N
        jLabel99.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel99.setName("jLabel99"); // NOI18N
        jLabel99.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel99MouseClicked(evt);
            }
        });
        jLabel99.setBounds(550, 260, 120, 20);
        jLayeredPane9.add(jLabel99, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox9.setMaximumRowCount(10);
        jComboBox9.setFocusable(false);
        jComboBox9.setName("jComboBox9"); // NOI18N
        jComboBox9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox9ActionPerformed(evt);
            }
        });
        jComboBox9.setBounds(10, 20, 170, 22);
        jLayeredPane9.add(jComboBox9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel89.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel89.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel89.setText(bundle.getString("FeedViewer.jLabel89.text")); // NOI18N
        jLabel89.setName("jLabel89"); // NOI18N
        jLabel89.setBounds(520, 10, 180, 240);
        jLayeredPane9.add(jLabel89, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField9.setText(bundle.getString("FeedViewer.jTextField9.text")); // NOI18N
        jTextField9.setName("jTextField9"); // NOI18N
        jTextField9.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField9KeyTyped(evt);
            }
        });
        jTextField9.setBounds(10, 55, 140, 23);
        jLayeredPane9.add(jTextField9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane9.TabConstraints.tabTitle"), jLayeredPane9); // NOI18N

        jLayeredPane6.setName("jLayeredPane6"); // NOI18N

        jLabel45.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel45.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel45.setText(bundle.getString("FeedViewer.jLabel45.text")); // NOI18N
        jLabel45.setName("jLabel45"); // NOI18N
        jLabel45.setBounds(520, 10, 180, 240);
        jLayeredPane6.add(jLabel45, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel51.setFont(jLabel51.getFont().deriveFont(jLabel51.getFont().getSize()+1f));
        jLabel51.setText(bundle.getString("FeedViewer.jLabel51.text")); // NOI18N
        jLabel51.setName("jLabel51"); // NOI18N
        jLabel51.setBounds(230, 170, 140, 20);
        jLayeredPane6.add(jLabel51, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel48.setFont(jLabel48.getFont().deriveFont(jLabel48.getFont().getSize()+1f));
        jLabel48.setText(bundle.getString("FeedViewer.jLabel48.text")); // NOI18N
        jLabel48.setName("jLabel48"); // NOI18N
        jLabel48.setBounds(550, 290, 120, 20);
        jLayeredPane6.add(jLabel48, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator5.setName("jSeparator5"); // NOI18N
        jSeparator5.setBounds(200, 20, 10, 390);
        jLayeredPane6.add(jSeparator5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel49.setFont(jLabel49.getFont().deriveFont(jLabel49.getFont().getSize()+1f));
        jLabel49.setText(bundle.getString("FeedViewer.jLabel49.text")); // NOI18N
        jLabel49.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel49.setName("jLabel49"); // NOI18N
        jLabel49.setBounds(230, 80, 280, 30);
        jLayeredPane6.add(jLabel49, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton9.setText(bundle.getString("FeedViewer.jButton9.text")); // NOI18N
        jButton9.setFocusable(false);
        jButton9.setName("jButton9"); // NOI18N
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jButton9.setBounds(150, 54, 30, 25);
        jLayeredPane6.add(jButton9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel52.setFont(jLabel52.getFont().deriveFont(jLabel52.getFont().getSize()+1f));
        jLabel52.setText(bundle.getString("FeedViewer.jLabel52.text")); // NOI18N
        jLabel52.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel52.setName("jLabel52"); // NOI18N
        jLabel52.setBounds(550, 370, 120, 40);
        jLayeredPane6.add(jLabel52, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane10.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane10.setName("jScrollPane10"); // NOI18N

        jTextPane5.setBorder(null);
        jTextPane5.setContentType(bundle.getString("FeedViewer.jTextPane5.contentType")); // NOI18N
        jTextPane5.setEditable(false);
        jTextPane5.setFont(jTextPane5.getFont());
        jTextPane5.setText(bundle.getString("FeedViewer.jTextPane5.text")); // NOI18N
        jTextPane5.setName("jTextPane5"); // NOI18N
        jTextPane5.setOpaque(false);
        jTextPane5.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane5HyperlinkUpdate(evt);
            }
        });
        jScrollPane10.setViewportView(jTextPane5);

        jScrollPane10.setBounds(230, 200, 280, 170);
        jLayeredPane6.add(jScrollPane10, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField5.setText(bundle.getString("FeedViewer.jTextField5.text")); // NOI18N
        jTextField5.setName("jTextField5"); // NOI18N
        jTextField5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField5KeyTyped(evt);
            }
        });
        jTextField5.setBounds(10, 55, 140, 23);
        jLayeredPane6.add(jTextField5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel47.setFont(jLabel47.getFont().deriveFont(jLabel47.getFont().getSize()+1f));
        jLabel47.setText(bundle.getString("FeedViewer.jLabel47.text")); // NOI18N
        jLabel47.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel47.setName("jLabel47"); // NOI18N
        jLabel47.setBounds(550, 350, 120, 20);
        jLayeredPane6.add(jLabel47, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel50.setFont(jLabel50.getFont().deriveFont(jLabel50.getFont().getSize()+1f));
        jLabel50.setText(bundle.getString("FeedViewer.jLabel50.text")); // NOI18N
        jLabel50.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel50.setName("jLabel50"); // NOI18N
        jLabel50.setBounds(230, 140, 280, 30);
        jLayeredPane6.add(jLabel50, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel53.setFont(jLabel53.getFont().deriveFont(jLabel53.getFont().getSize()+1f));
        jLabel53.setText(bundle.getString("FeedViewer.jLabel53.text")); // NOI18N
        jLabel53.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel53.setName("jLabel53"); // NOI18N
        jLabel53.setBounds(230, 110, 280, 30);
        jLayeredPane6.add(jLabel53, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel55.setFont(jLabel55.getFont().deriveFont(jLabel55.getFont().getStyle() | java.awt.Font.BOLD, jLabel55.getFont().getSize()+1));
        jLabel55.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel55.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel55.setText(bundle.getString("FeedViewer.jLabel55.text")); // NOI18N
        jLabel55.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel55.setName("jLabel55"); // NOI18N
        jLabel55.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel55MouseClicked(evt);
            }
        });
        jLabel55.setBounds(550, 260, 120, 20);
        jLayeredPane6.add(jLabel55, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel46.setFont(jLabel46.getFont().deriveFont(jLabel46.getFont().getSize()+13f));
        jLabel46.setText(bundle.getString("FeedViewer.jLabel46.text")); // NOI18N
        jLabel46.setName("jLabel46"); // NOI18N
        jLabel46.setBounds(230, 10, 280, 70);
        jLayeredPane6.add(jLabel46, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane9.setName("jScrollPane9"); // NOI18N

        jList5.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList5.setName("jList5"); // NOI18N
        jList5.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList5ValueChanged(evt);
            }
        });
        jScrollPane9.setViewportView(jList5);

        jScrollPane9.setBounds(10, 90, 170, 320);
        jLayeredPane6.add(jScrollPane9, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel54.setFont(jLabel54.getFont().deriveFont(jLabel54.getFont().getSize()+1f));
        jLabel54.setText(bundle.getString("FeedViewer.jLabel54.text")); // NOI18N
        jLabel54.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel54.setName("jLabel54"); // NOI18N
        jLabel54.setBounds(550, 310, 120, 40);
        jLayeredPane6.add(jLabel54, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton10.setBackground(new java.awt.Color(224, 218, 218));
        jButton10.setFont(jButton10.getFont().deriveFont(jButton10.getFont().getSize()+1f));
        jButton10.setText(bundle.getString("FeedViewer.jButton10.text")); // NOI18N
        jButton10.setFocusable(false);
        jButton10.setName("jButton10"); // NOI18N
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jButton10.setBounds(370, 380, 90, 30);
        jLayeredPane6.add(jButton10, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox5.setMaximumRowCount(10);
        jComboBox5.setFocusable(false);
        jComboBox5.setName("jComboBox5"); // NOI18N
        jComboBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox5ActionPerformed(evt);
            }
        });
        jComboBox5.setBounds(10, 20, 170, 22);
        jLayeredPane6.add(jComboBox5, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane6.TabConstraints.tabTitle"), jLayeredPane6); // NOI18N

        jLayeredPane7.setName("jLayeredPane7"); // NOI18N

        jButton12.setBackground(new java.awt.Color(224, 218, 218));
        jButton12.setFont(jButton12.getFont().deriveFont(jButton12.getFont().getSize()+1f));
        jButton12.setText(bundle.getString("FeedViewer.jButton12.text")); // NOI18N
        jButton12.setFocusable(false);
        jButton12.setName("jButton12"); // NOI18N
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jButton12.setBounds(370, 380, 90, 30);
        jLayeredPane7.add(jButton12, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel64.setFont(jLabel64.getFont().deriveFont(jLabel64.getFont().getSize()+1f));
        jLabel64.setText(bundle.getString("FeedViewer.jLabel64.text")); // NOI18N
        jLabel64.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel64.setName("jLabel64"); // NOI18N
        jLabel64.setBounds(230, 110, 280, 30);
        jLayeredPane7.add(jLabel64, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField6.setText(bundle.getString("FeedViewer.jTextField6.text")); // NOI18N
        jTextField6.setName("jTextField6"); // NOI18N
        jTextField6.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField6KeyTyped(evt);
            }
        });
        jTextField6.setBounds(10, 55, 140, 23);
        jLayeredPane7.add(jTextField6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane12.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane12.setName("jScrollPane12"); // NOI18N

        jTextPane6.setBorder(null);
        jTextPane6.setContentType(bundle.getString("FeedViewer.jTextPane6.contentType")); // NOI18N
        jTextPane6.setEditable(false);
        jTextPane6.setFont(jTextPane6.getFont());
        jTextPane6.setText(bundle.getString("FeedViewer.jTextPane6.text")); // NOI18N
        jTextPane6.setName("jTextPane6"); // NOI18N
        jTextPane6.setOpaque(false);
        jTextPane6.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane6HyperlinkUpdate(evt);
            }
        });
        jScrollPane12.setViewportView(jTextPane6);

        jScrollPane12.setBounds(230, 200, 280, 170);
        jLayeredPane7.add(jScrollPane12, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel63.setFont(jLabel63.getFont().deriveFont(jLabel63.getFont().getSize()+1f));
        jLabel63.setText(bundle.getString("FeedViewer.jLabel63.text")); // NOI18N
        jLabel63.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel63.setName("jLabel63"); // NOI18N
        jLabel63.setBounds(550, 370, 120, 40);
        jLayeredPane7.add(jLabel63, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel58.setFont(jLabel58.getFont().deriveFont(jLabel58.getFont().getSize()+1f));
        jLabel58.setText(bundle.getString("FeedViewer.jLabel58.text")); // NOI18N
        jLabel58.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel58.setName("jLabel58"); // NOI18N
        jLabel58.setBounds(550, 350, 120, 20);
        jLayeredPane7.add(jLabel58, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel57.setFont(jLabel57.getFont().deriveFont(jLabel57.getFont().getSize()+13f));
        jLabel57.setText(bundle.getString("FeedViewer.jLabel57.text")); // NOI18N
        jLabel57.setName("jLabel57"); // NOI18N
        jLabel57.setBounds(230, 10, 280, 70);
        jLayeredPane7.add(jLabel57, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel59.setFont(jLabel59.getFont().deriveFont(jLabel59.getFont().getSize()+1f));
        jLabel59.setText(bundle.getString("FeedViewer.jLabel59.text")); // NOI18N
        jLabel59.setName("jLabel59"); // NOI18N
        jLabel59.setBounds(550, 290, 120, 20);
        jLayeredPane7.add(jLabel59, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane11.setName("jScrollPane11"); // NOI18N

        jList6.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList6.setName("jList6"); // NOI18N
        jList6.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList6ValueChanged(evt);
            }
        });
        jScrollPane11.setViewportView(jList6);

        jScrollPane11.setBounds(10, 90, 170, 320);
        jLayeredPane7.add(jScrollPane11, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel65.setFont(jLabel65.getFont().deriveFont(jLabel65.getFont().getSize()+1f));
        jLabel65.setText(bundle.getString("FeedViewer.jLabel65.text")); // NOI18N
        jLabel65.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel65.setName("jLabel65"); // NOI18N
        jLabel65.setBounds(550, 310, 120, 40);
        jLayeredPane7.add(jLabel65, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel61.setFont(jLabel61.getFont().deriveFont(jLabel61.getFont().getSize()+1f));
        jLabel61.setText(bundle.getString("FeedViewer.jLabel61.text")); // NOI18N
        jLabel61.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel61.setName("jLabel61"); // NOI18N
        jLabel61.setBounds(230, 140, 280, 30);
        jLayeredPane7.add(jLabel61, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel56.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel56.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel56.setText(bundle.getString("FeedViewer.jLabel56.text")); // NOI18N
        jLabel56.setName("jLabel56"); // NOI18N
        jLabel56.setBounds(520, 10, 180, 240);
        jLayeredPane7.add(jLabel56, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel66.setFont(jLabel66.getFont().deriveFont(jLabel66.getFont().getStyle() | java.awt.Font.BOLD, jLabel66.getFont().getSize()+1));
        jLabel66.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel66.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel66.setText(bundle.getString("FeedViewer.jLabel66.text")); // NOI18N
        jLabel66.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel66.setName("jLabel66"); // NOI18N
        jLabel66.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel66MouseClicked(evt);
            }
        });
        jLabel66.setBounds(550, 260, 120, 20);
        jLayeredPane7.add(jLabel66, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton11.setText(bundle.getString("FeedViewer.jButton11.text")); // NOI18N
        jButton11.setFocusable(false);
        jButton11.setName("jButton11"); // NOI18N
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jButton11.setBounds(150, 54, 30, 25);
        jLayeredPane7.add(jButton11, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator6.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator6.setName("jSeparator6"); // NOI18N
        jSeparator6.setBounds(200, 20, 10, 390);
        jLayeredPane7.add(jSeparator6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel62.setFont(jLabel62.getFont().deriveFont(jLabel62.getFont().getSize()+1f));
        jLabel62.setText(bundle.getString("FeedViewer.jLabel62.text")); // NOI18N
        jLabel62.setName("jLabel62"); // NOI18N
        jLabel62.setBounds(230, 170, 140, 20);
        jLayeredPane7.add(jLabel62, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel60.setFont(jLabel60.getFont().deriveFont(jLabel60.getFont().getSize()+1f));
        jLabel60.setText(bundle.getString("FeedViewer.jLabel60.text")); // NOI18N
        jLabel60.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel60.setName("jLabel60"); // NOI18N
        jLabel60.setBounds(230, 80, 280, 30);
        jLayeredPane7.add(jLabel60, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox6.setMaximumRowCount(10);
        jComboBox6.setFocusable(false);
        jComboBox6.setName("jComboBox6"); // NOI18N
        jComboBox6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox6ActionPerformed(evt);
            }
        });
        jComboBox6.setBounds(10, 20, 170, 22);
        jLayeredPane7.add(jComboBox6, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane7.TabConstraints.tabTitle"), jLayeredPane7); // NOI18N

        jLayeredPane8.setName("jLayeredPane8"); // NOI18N

        jButton13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/find.png"))); // NOI18N
        jButton13.setText(bundle.getString("FeedViewer.jButton13.text")); // NOI18N
        jButton13.setFocusable(false);
        jButton13.setName("jButton13"); // NOI18N
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        jButton13.setBounds(150, 54, 30, 25);
        jLayeredPane8.add(jButton13, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel72.setFont(jLabel72.getFont().deriveFont(jLabel72.getFont().getSize()+1f));
        jLabel72.setText(bundle.getString("FeedViewer.jLabel72.text")); // NOI18N
        jLabel72.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel72.setName("jLabel72"); // NOI18N
        jLabel72.setBounds(230, 140, 280, 30);
        jLayeredPane8.add(jLabel72, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane13.setName("jScrollPane13"); // NOI18N

        jList7.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList7.setName("jList7"); // NOI18N
        jList7.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList7ValueChanged(evt);
            }
        });
        jScrollPane13.setViewportView(jList7);

        jScrollPane13.setBounds(10, 90, 170, 320);
        jLayeredPane8.add(jScrollPane13, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel73.setFont(jLabel73.getFont().deriveFont(jLabel73.getFont().getSize()+1f));
        jLabel73.setText(bundle.getString("FeedViewer.jLabel73.text")); // NOI18N
        jLabel73.setName("jLabel73"); // NOI18N
        jLabel73.setBounds(230, 170, 140, 20);
        jLayeredPane8.add(jLabel73, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel75.setFont(jLabel75.getFont().deriveFont(jLabel75.getFont().getSize()+1f));
        jLabel75.setText(bundle.getString("FeedViewer.jLabel75.text")); // NOI18N
        jLabel75.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel75.setName("jLabel75"); // NOI18N
        jLabel75.setBounds(230, 110, 280, 30);
        jLayeredPane8.add(jLabel75, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jComboBox7.setMaximumRowCount(10);
        jComboBox7.setFocusable(false);
        jComboBox7.setName("jComboBox7"); // NOI18N
        jComboBox7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox7ActionPerformed(evt);
            }
        });
        jComboBox7.setBounds(10, 20, 170, 22);
        jLayeredPane8.add(jComboBox7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel76.setFont(jLabel76.getFont().deriveFont(jLabel76.getFont().getSize()+1f));
        jLabel76.setText(bundle.getString("FeedViewer.jLabel76.text")); // NOI18N
        jLabel76.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel76.setName("jLabel76"); // NOI18N
        jLabel76.setBounds(550, 310, 120, 40);
        jLayeredPane8.add(jLabel76, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel71.setFont(jLabel71.getFont().deriveFont(jLabel71.getFont().getSize()+1f));
        jLabel71.setText(bundle.getString("FeedViewer.jLabel71.text")); // NOI18N
        jLabel71.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel71.setName("jLabel71"); // NOI18N
        jLabel71.setBounds(230, 80, 280, 30);
        jLayeredPane8.add(jLabel71, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton14.setBackground(new java.awt.Color(224, 218, 218));
        jButton14.setFont(jButton14.getFont().deriveFont(jButton14.getFont().getSize()+1f));
        jButton14.setText(bundle.getString("FeedViewer.jButton14.text")); // NOI18N
        jButton14.setFocusable(false);
        jButton14.setName("jButton14"); // NOI18N
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });
        jButton14.setBounds(370, 380, 90, 30);
        jLayeredPane8.add(jButton14, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator7.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator7.setName("jSeparator7"); // NOI18N
        jSeparator7.setBounds(200, 20, 10, 390);
        jLayeredPane8.add(jSeparator7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel68.setFont(jLabel68.getFont().deriveFont(jLabel68.getFont().getSize()+13f));
        jLabel68.setText(bundle.getString("FeedViewer.jLabel68.text")); // NOI18N
        jLabel68.setName("jLabel68"); // NOI18N
        jLabel68.setBounds(230, 10, 280, 70);
        jLayeredPane8.add(jLabel68, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel69.setFont(jLabel69.getFont().deriveFont(jLabel69.getFont().getSize()+1f));
        jLabel69.setText(bundle.getString("FeedViewer.jLabel69.text")); // NOI18N
        jLabel69.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel69.setName("jLabel69"); // NOI18N
        jLabel69.setBounds(550, 350, 120, 20);
        jLayeredPane8.add(jLabel69, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTextField7.setText(bundle.getString("FeedViewer.jTextField7.text")); // NOI18N
        jTextField7.setName("jTextField7"); // NOI18N
        jTextField7.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField7KeyTyped(evt);
            }
        });
        jTextField7.setBounds(10, 55, 140, 23);
        jLayeredPane8.add(jTextField7, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel74.setFont(jLabel74.getFont().deriveFont(jLabel74.getFont().getSize()+1f));
        jLabel74.setText(bundle.getString("FeedViewer.jLabel74.text")); // NOI18N
        jLabel74.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel74.setName("jLabel74"); // NOI18N
        jLabel74.setBounds(550, 370, 120, 40);
        jLayeredPane8.add(jLabel74, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel77.setFont(jLabel77.getFont().deriveFont(jLabel77.getFont().getStyle() | java.awt.Font.BOLD, jLabel77.getFont().getSize()+1));
        jLabel77.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel77.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel77.setText(bundle.getString("FeedViewer.jLabel77.text")); // NOI18N
        jLabel77.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel77.setName("jLabel77"); // NOI18N
        jLabel77.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel77MouseClicked(evt);
            }
        });
        jLabel77.setBounds(550, 260, 120, 20);
        jLayeredPane8.add(jLabel77, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel70.setFont(jLabel70.getFont().deriveFont(jLabel70.getFont().getSize()+1f));
        jLabel70.setText(bundle.getString("FeedViewer.jLabel70.text")); // NOI18N
        jLabel70.setName("jLabel70"); // NOI18N
        jLabel70.setBounds(550, 290, 120, 20);
        jLayeredPane8.add(jLabel70, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane14.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane14.setName("jScrollPane14"); // NOI18N

        jTextPane7.setBorder(null);
        jTextPane7.setContentType(bundle.getString("FeedViewer.jTextPane7.contentType")); // NOI18N
        jTextPane7.setEditable(false);
        jTextPane7.setFont(jTextPane7.getFont());
        jTextPane7.setText(bundle.getString("FeedViewer.jTextPane7.text")); // NOI18N
        jTextPane7.setName("jTextPane7"); // NOI18N
        jTextPane7.setOpaque(false);
        jTextPane7.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane7HyperlinkUpdate(evt);
            }
        });
        jScrollPane14.setViewportView(jTextPane7);

        jScrollPane14.setBounds(230, 200, 280, 170);
        jLayeredPane8.add(jScrollPane14, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel67.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel67.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel67.setText(bundle.getString("FeedViewer.jLabel67.text")); // NOI18N
        jLabel67.setName("jLabel67"); // NOI18N
        jLabel67.setBounds(520, 10, 180, 240);
        jLayeredPane8.add(jLabel67, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab(bundle.getString("FeedViewer.jLayeredPane8.TabConstraints.tabTitle"), jLayeredPane8); // NOI18N

        jTabbedPane1.addTab(bundle.getString("FeedViewer.availablePackagesTabs.TabConstraints.tabTitle"), jTabbedPane2); // NOI18N

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N

        jLabel88.setFont(jLabel88.getFont().deriveFont(jLabel88.getFont().getStyle() | java.awt.Font.BOLD, jLabel88.getFont().getSize()+1));
        jLabel88.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel88.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/url-icon.png"))); // NOI18N
        jLabel88.setText(bundle.getString("FeedViewer.jLabel88.text")); // NOI18N
        jLabel88.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel88.setName("jLabel88"); // NOI18N
        jLabel88.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel88MouseClicked(evt);
            }
        });
        jLabel88.setBounds(550, 280, 120, 20);
        jLayeredPane1.add(jLabel88, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel84.setFont(jLabel84.getFont().deriveFont(jLabel84.getFont().getSize()+1f));
        jLabel84.setText(bundle.getString("FeedViewer.jLabel84.text")); // NOI18N
        jLabel84.setName("jLabel84"); // NOI18N
        jLabel84.setBounds(230, 170, 140, 20);
        jLayeredPane1.add(jLabel84, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton15.setBackground(new java.awt.Color(224, 218, 218));
        jButton15.setFont(jButton15.getFont().deriveFont(jButton15.getFont().getSize()+1f));
        jButton15.setText(bundle.getString("FeedViewer.jButton15.text")); // NOI18N
        jButton15.setName("jButton15"); // NOI18N
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });
        jButton15.setBounds(29, 390, 130, 30);
        jLayeredPane1.add(jButton15, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jButton16.setBackground(new java.awt.Color(224, 218, 218));
        jButton16.setFont(jButton16.getFont().deriveFont(jButton16.getFont().getSize()+1f));
        jButton16.setText(bundle.getString("FeedViewer.jButton16.text")); // NOI18N
        jButton16.setName("jButton16"); // NOI18N
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });
        jButton16.setBounds(370, 400, 90, 30);
        jLayeredPane1.add(jButton16, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane15.setName("jScrollPane15"); // NOI18N

        jList8.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList8.setName("jList8"); // NOI18N
        jList8.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList8ValueChanged(evt);
            }
        });
        jScrollPane15.setViewportView(jList8);

        jScrollPane15.setBounds(10, 20, 170, 360);
        jLayeredPane1.add(jScrollPane15, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel81.setFont(jLabel81.getFont().deriveFont(jLabel81.getFont().getSize()+1f));
        jLabel81.setText(bundle.getString("FeedViewer.jLabel81.text")); // NOI18N
        jLabel81.setName("jLabel81"); // NOI18N
        jLabel81.setBounds(550, 310, 120, 20);
        jLayeredPane1.add(jLabel81, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel83.setFont(jLabel83.getFont().deriveFont(jLabel83.getFont().getSize()+1f));
        jLabel83.setText(bundle.getString("FeedViewer.jLabel83.text")); // NOI18N
        jLabel83.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel83.setName("jLabel83"); // NOI18N
        jLabel83.setBounds(230, 140, 280, 30);
        jLayeredPane1.add(jLabel83, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel85.setFont(jLabel85.getFont().deriveFont(jLabel85.getFont().getSize()+1f));
        jLabel85.setText(bundle.getString("FeedViewer.jLabel85.text")); // NOI18N
        jLabel85.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel85.setName("jLabel85"); // NOI18N
        jLabel85.setBounds(550, 390, 120, 40);
        jLayeredPane1.add(jLabel85, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel82.setFont(jLabel82.getFont().deriveFont(jLabel82.getFont().getSize()+1f));
        jLabel82.setText(bundle.getString("FeedViewer.jLabel82.text")); // NOI18N
        jLabel82.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel82.setName("jLabel82"); // NOI18N
        jLabel82.setBounds(230, 80, 280, 30);
        jLayeredPane1.add(jLabel82, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel79.setFont(jLabel79.getFont().deriveFont(jLabel79.getFont().getSize()+13f));
        jLabel79.setText(bundle.getString("FeedViewer.jLabel79.text")); // NOI18N
        jLabel79.setName("jLabel79"); // NOI18N
        jLabel79.setBounds(230, 10, 280, 70);
        jLayeredPane1.add(jLabel79, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel80.setFont(jLabel80.getFont().deriveFont(jLabel80.getFont().getSize()+1f));
        jLabel80.setText(bundle.getString("FeedViewer.jLabel80.text")); // NOI18N
        jLabel80.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel80.setName("jLabel80"); // NOI18N
        jLabel80.setBounds(550, 370, 120, 20);
        jLayeredPane1.add(jLabel80, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jScrollPane16.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane16.setName("jScrollPane16"); // NOI18N

        jTextPane8.setBorder(null);
        jTextPane8.setContentType(bundle.getString("FeedViewer.jTextPane8.contentType")); // NOI18N
        jTextPane8.setEditable(false);
        jTextPane8.setFont(jTextPane8.getFont());
        jTextPane8.setText(bundle.getString("FeedViewer.jTextPane8.text")); // NOI18N
        jTextPane8.setName("jTextPane8"); // NOI18N
        jTextPane8.setOpaque(false);
        jTextPane8.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jTextPane8HyperlinkUpdate(evt);
            }
        });
        jScrollPane16.setViewportView(jTextPane8);

        jScrollPane16.setBounds(230, 200, 280, 190);
        jLayeredPane1.add(jScrollPane16, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jSeparator8.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator8.setName("jSeparator8"); // NOI18N
        jSeparator8.setBounds(200, 20, 10, 420);
        jLayeredPane1.add(jSeparator8, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel87.setFont(jLabel87.getFont().deriveFont(jLabel87.getFont().getSize()+1f));
        jLabel87.setText(bundle.getString("FeedViewer.jLabel87.text")); // NOI18N
        jLabel87.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel87.setName("jLabel87"); // NOI18N
        jLabel87.setBounds(550, 330, 120, 40);
        jLayeredPane1.add(jLabel87, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel78.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel78.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/canucksoftware/wosqi/resources/no-screenshot.png"))); // NOI18N
        jLabel78.setText(bundle.getString("FeedViewer.jLabel78.text")); // NOI18N
        jLabel78.setName("jLabel78"); // NOI18N
        jLabel78.setBounds(520, 20, 180, 240);
        jLayeredPane1.add(jLabel78, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel86.setFont(jLabel86.getFont().deriveFont(jLabel86.getFont().getSize()+1f));
        jLabel86.setText(bundle.getString("FeedViewer.jLabel86.text")); // NOI18N
        jLabel86.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel86.setName("jLabel86"); // NOI18N
        jLabel86.setBounds(230, 110, 280, 30);
        jLayeredPane1.add(jLabel86, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane1.addTab(bundle.getString("FeedViewer.jLayeredPane1.TabConstraints.tabTitle"), jLayeredPane1); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 720, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextPane1HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane1HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane1HyperlinkUpdate

    private void jTextPane2HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane2HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane2HyperlinkUpdate

    private void jTextPane3HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane3HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane3HyperlinkUpdate

    private void jTextPane4HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane4HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane4HyperlinkUpdate

    private void jTextPane5HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane5HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane5HyperlinkUpdate

    private void jTextPane6HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane6HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane6HyperlinkUpdate

    private void jTextPane7HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane7HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane7HyperlinkUpdate

    private void jTextPane8HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane8HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane8HyperlinkUpdate

    private void jTextPane9HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane9HyperlinkUpdate
        if(evt.getEventType()==javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            openUrl(evt.getURL());
        }
    }//GEN-LAST:event_jTextPane9HyperlinkUpdate

    private void jLabel11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel11MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel11MouseClicked

    private void jLabel22MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel22MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel22MouseClicked

    private void jLabel33MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel33MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel33MouseClicked

    private void jLabel44MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel44MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel44MouseClicked

    private void jLabel99MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel99MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel99MouseClicked

    private void jLabel55MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel55MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel55MouseClicked

    private void jLabel66MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel66MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel66MouseClicked

    private void jLabel77MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel77MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel77MouseClicked

    private void jTabbedPane2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane2StateChanged
        if(loaded) {
            loadTab();
        }
    }//GEN-LAST:event_jTabbedPane2StateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        query();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        query();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        query();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        query();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        query();
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        query();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        query();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        query();
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        install();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        install();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        install();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        install();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        install();
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        install();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        install();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        install();
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jList1ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList1ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList1ValueChanged

    private void jList2ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList2ValueChanged
        if(loaded) {
            displayPackage(jTabbedPane2.getSelectedIndex());
        }
    }//GEN-LAST:event_jList2ValueChanged

    private void jList3ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList3ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList3ValueChanged

    private void jList4ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList4ValueChanged
         displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList4ValueChanged

    private void jList9ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList9ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList9ValueChanged

    private void jList5ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList5ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList5ValueChanged

    private void jList6ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList6ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList6ValueChanged

    private void jList7ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList7ValueChanged
        displayPackage(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jList7ValueChanged

    private void jTextField7KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField7KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField7KeyTyped

    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField1KeyTyped

    private void jTextField2KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField2KeyTyped

    private void jTextField3KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField3KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField3KeyTyped

    private void jTextField4KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField4KeyTyped

    private void jTextField9KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField9KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField9KeyTyped

    private void jTextField5KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField5KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField5KeyTyped

    private void jTextField6KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField6KeyTyped
        if(evt.getKeyChar()=='\n' || evt.getKeyChar()=='\r') {
            query();
        }
    }//GEN-LAST:event_jTextField6KeyTyped

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jComboBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox3ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox3ActionPerformed

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox4ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox4ActionPerformed

    private void jComboBox9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox9ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox9ActionPerformed

    private void jComboBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox5ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox5ActionPerformed

    private void jComboBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox6ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox6ActionPerformed

    private void jComboBox7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox7ActionPerformed
        loadCategory(jTabbedPane2.getSelectedIndex());
    }//GEN-LAST:event_jComboBox7ActionPerformed

    private void jLabel88MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel88MouseClicked
        openHomepage();
    }//GEN-LAST:event_jLabel88MouseClicked

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if(loaded) {
            if(jTabbedPane1.getSelectedIndex()==0) {
                loadTab();
            } else {
                loadUpdates();
            }
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        updateAll();
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        update();
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jList8ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList8ValueChanged
        displayPackage(packages.length-1);
    }//GEN-LAST:event_jList8ValueChanged
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JComboBox jComboBox5;
    private javax.swing.JComboBox jComboBox6;
    private javax.swing.JComboBox jComboBox7;
    private javax.swing.JComboBox jComboBox9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel78;
    private javax.swing.JLabel jLabel79;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel80;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel82;
    private javax.swing.JLabel jLabel83;
    private javax.swing.JLabel jLabel84;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel87;
    private javax.swing.JLabel jLabel88;
    private javax.swing.JLabel jLabel89;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel90;
    private javax.swing.JLabel jLabel91;
    private javax.swing.JLabel jLabel92;
    private javax.swing.JLabel jLabel93;
    private javax.swing.JLabel jLabel94;
    private javax.swing.JLabel jLabel95;
    private javax.swing.JLabel jLabel96;
    private javax.swing.JLabel jLabel97;
    private javax.swing.JLabel jLabel98;
    private javax.swing.JLabel jLabel99;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jLayeredPane3;
    private javax.swing.JLayeredPane jLayeredPane4;
    private javax.swing.JLayeredPane jLayeredPane5;
    private javax.swing.JLayeredPane jLayeredPane6;
    private javax.swing.JLayeredPane jLayeredPane7;
    private javax.swing.JLayeredPane jLayeredPane8;
    private javax.swing.JLayeredPane jLayeredPane9;
    private javax.swing.JList jList1;
    private javax.swing.JList jList2;
    private javax.swing.JList jList3;
    private javax.swing.JList jList4;
    private javax.swing.JList jList5;
    private javax.swing.JList jList6;
    private javax.swing.JList jList7;
    private javax.swing.JList jList8;
    private javax.swing.JList jList9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane16;
    private javax.swing.JScrollPane jScrollPane17;
    private javax.swing.JScrollPane jScrollPane18;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPane3;
    private javax.swing.JTextPane jTextPane4;
    private javax.swing.JTextPane jTextPane5;
    private javax.swing.JTextPane jTextPane6;
    private javax.swing.JTextPane jTextPane7;
    private javax.swing.JTextPane jTextPane8;
    private javax.swing.JTextPane jTextPane9;
    // End of variables declaration//GEN-END:variables

    class DoDispose extends TimerTask  {
        public void run() {
            dispose();
        }
    }

    class DoLoad extends TimerTask  {
        public void run() {
            loaded = true;
            if(jTabbedPane1.getSelectedIndex()==0) {
                loadTab();
            } else {
                loadUpdates();
            }
            for(int i=0; i<NAME.length; i++) {
                NAME[i].setVisible(true);
            }
            t.schedule(new DoSecondaryLoad(), 200);
        }
    }

    class DoSecondaryLoad extends TimerTask  {
        public void run() {
            query();
        }
    }

    class LoadScreenshot extends Thread  {
        private int tab;
        public LoadScreenshot(int tab) {
            this.tab = tab;
        }
        @Override
        public void run() {
            if(selected[tab].source.has("Screenshots")) {
                try {
                    JSONArray ss = selected[tab].source.getJSONArray("Screenshots");
                    if(ss.length()>0) {
                        SCREENSHOT[tab].setIcon(null);
                        SCREENSHOT[tab].setText("<html><center><img src=\"" +
                                ss.getString(0) + "\" width=\"180\" " +
                                "height=\"240\"></center>");
                    } else {
                        SCREENSHOT[tab].setText("");
                        SCREENSHOT[tab].setIcon(NO_SCREENSHOT);
                    }
                } catch(Exception e) {}
            } else {
                SCREENSHOT[tab].setText("");
                SCREENSHOT[tab].setIcon(NO_SCREENSHOT);
            }
        }
    }

    class ImageLayeredPane extends JLayeredPane {
        private Image img;
        public ImageLayeredPane(String img) {
            this(new ImageIcon(img).getImage());
        }
        public ImageLayeredPane(Image img) {
            super();
            this.img = img;
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }
        @Override
        public void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0, null);
        }
    }
}
