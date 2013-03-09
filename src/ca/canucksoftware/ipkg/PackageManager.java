
package ca.canucksoftware.ipkg;

import ca.canucksoftware.webos.InstalledEntry;
import ca.canucksoftware.webos.DeviceInfo;
import ca.canucksoftware.wosqi.WebOSQuickInstallApp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.JTextArea;
import org.json.JSONArray;

/**
 * @author Jason Robitaille
 */
public class PackageManager {
    public final String ALL_CATEGORIES;
    private final String[] defaultFeeds;
    private boolean[] feedStates;
    private String[] feedUrls;
    private PackageFeed[] feeds;
    private ArrayList<PackageEntry> packages;
    private ArrayList<PackageEntry> filtered;
    private ArrayList<InstalledEntry> installed;
    private DeviceInfo device;
    private JTextArea label;
    private ResourceBundle bundle;

    public PackageManager(DeviceInfo info, ArrayList<InstalledEntry> apps) {
        this(info, apps, null);
    }

    public PackageManager(DeviceInfo info, ArrayList<InstalledEntry> apps, JTextArea display) {
        device = info;
        installed = apps;
        label = display;
        bundle = WebOSQuickInstallApp.bundle;
        ALL_CATEGORIES = "--" + bundle.getString("ALL") + "--";
        defaultFeeds = new String[] {
            "http://ipkg.preware.org/feeds/precentral/Packages.gz",
            "http://ipkg.preware.org/feeds/webos-internals/all/Packages.gz",
            "http://ipkg.preware.org/feeds/webos-internals/" + device.arch() + "/Packages.gz",
            "http://ipkg.preware.org/feeds/webos-patches/" + device.version() + "/Packages.gz",
            "http://ipkg.preware.org/feeds/webos-kernels/" + device.version() + "/Packages.gz",
            "http://www.prethemer.com/feeds/preware/themes/Packages.gz",
            "http://ipkg.preware.org/feeds/precentral-themes/Packages.gz",
            "http://webos-clock-themer.googlecode.com/svn/trunk/WebOS%20Clock%20Theme%20Builder/feed/Packages.gz"
        };
        Preferences prefs = Preferences.systemRoot();
        int numFeeds = prefs.getInt("numCustomFeeds", 0);
        feedStates = new boolean[defaultFeeds.length + numFeeds];
        feedUrls = new String[defaultFeeds.length + numFeeds];
        for(int i=0; i<defaultFeeds.length; i++) {
            feedStates[i] = prefs.getBoolean("defaultFeedState-" + i, true);
        }

        feeds = new PackageFeed[defaultFeeds.length + numFeeds];
        packages = new ArrayList(0);
        for(int i=0; i<defaultFeeds.length; i++) {
            feedUrls[i] = defaultFeeds[i];
            if(feedStates[i]) {
                System.out.println("Downloading and parsing: " + feedUrls[i]);
                if(label!=null) {
                    label.setText(feedUrls[i]);
                }
                feeds[i] = PackageFeed.Download(feedUrls[i]);
                if(feeds[i]!=null) {
                    addToPackages(feeds[i].packages);
                } else {
                    System.err.println("Failed to load feed: " + feedUrls[i]);
                }
            }
        }
        for(int i=defaultFeeds.length; i<feedUrls.length; i++) {
            String currUrl = prefs.get("customfeed" + (i-defaultFeeds.length), null);
            if(currUrl!=null) {
                feedUrls[i] = currUrl;
                feedStates[i] = prefs.getBoolean("customFeedState-" + (i-defaultFeeds.length), true);
                if(feedStates[i]) {
                    System.out.println("Downloading and parsing: " + feedUrls[i]);
                    if(label!=null) {
                        label.setText(feedUrls[i]);
                    }
                    feeds[i] = PackageFeed.Download(feedUrls[i]);
                    if(feeds[i]!=null) {
                        addToPackages(feeds[i].packages);
                    } else {
                        System.err.println("Failed to load feed: " + feedUrls[i]);
                    }
                }
            }
        }
        Collections.sort(packages, new PackageSorter(true));
        filtered = packages;
    }

    private void addToPackages(ArrayList<PackageEntry> list) {
        for(int i=0; i<list.size(); i++) {
            PackageEntry curr = list.get(i);
            int index = indexOf(curr.id);
            if(index>-1) {
                PackageEntry existing = packages.get(index);
                if(curr.newerThan(existing)) {
                    packages.set(index, curr);
                }
            } else {
                packages.add(curr);
            }
        }
    }

    public PackageEntry getEntryById(String id) {
        PackageEntry result = null;
        int index = indexOf(id);
        if(index>-1) {
            result = packages.get(index);
        }
        return result;
    }

    private int indexOf(String id) {
        int result = -1;
        for(int i=0; i<packages.size(); i++) {
            if(packages.get(i).id.equals(id)) {
                result = i;
                break;
            }
        }
        return result;
    }

    public void setFilter(PackageFilter filter) {
        filtered = new ArrayList(0);
        for(int i=0; i<packages.size(); i++) {
            PackageEntry curr = (PackageEntry) packages.get(i);
            if(filter==PackageFilter.None) {
                filtered.add(curr);
            } else {
                String type = PackageFilter.Applications.toString();
                if(curr.source!=null) {
                    if(curr.source.has("Type")) {
                        try {
                            type = curr.source.getString("Type");
                        } catch(Exception e) {}
                    }
                }
                if(type.equals(filter.toString())) {
                    filtered.add(curr);
                }
            }
            
        }
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> result = new ArrayList(0);
        result.add(ALL_CATEGORIES);
        for(int i=0; i<filtered.size(); i++) {
            PackageEntry curr = filtered.get(i);
            if(curr.source!=null) {
                if(curr.source.has("Category")) {
                    try {
                        String category = curr.source.getString("Category");
                        if(category.length()>0 &&
                                !category.toLowerCase().equals("unavailable")) {
                            if(!result.contains(category)) {
                                result.add(category);
                            }
                        }
                    } catch(Exception e) {}
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    public ArrayList<PackageEntry> getPackagesByCategory(String category) {
        return getPackagesByCategory(category, false);
    }

    public ArrayList<PackageEntry> getPackagesByCategory(String category,
            boolean includeInstalled) {
        ArrayList<PackageEntry> results = new ArrayList(0);
        for(int i=0; i<filtered.size(); i++) {
            PackageEntry curr = filtered.get(i);
            if(category.equals(ALL_CATEGORIES)) {
                if(okToAdd(curr, includeInstalled)) {
                    results.add(curr);
                }
            } else {
                if(curr.source!=null) {
                    if(curr.source.has("Category")) {
                        try {
                            String currCategory = curr.source.getString("Category");
                            if(currCategory.equals(category)) {
                                if(okToAdd(curr, includeInstalled)) {
                                    results.add(curr);
                                }
                            }
                        } catch(Exception e) {}
                    }
                }
            }
        }
        return results;
    }

    private boolean okToAdd(PackageEntry item, boolean includeInstalled) {
        boolean result = true;
        if(!includeInstalled) {
            result &= (installed.indexOf(new InstalledEntry(item.id))==-1);
        }
        result &= item.arch.equals(device.arch()) || item.arch.equals("any") ||
                item.arch.equals("all") || device.arch().startsWith(item.arch);
        if(result && item.source!=null) {
            if(item.source.has("DeviceCompatibility")) {
                try {
                    String deviceName = device.model().replace("Palm ", "").replace(" ", "");
                    JSONArray array = item.source.getJSONArray("DeviceCompatibility");
                    result &= array.toString().contains(deviceName);
                } catch(Exception e) {}
            }
            String[] deviceVer = device.version().split("\\.");
            if(result && item.source.has("MinWebOSVersion")) {
                try {
                    String[] min = item.source.getString("MinWebOSVersion").split("\\.");
                    for(int i=0; i<min.length; i++) {
                        int verVal = Integer.parseInt(deviceVer[i]);
                        int minVal = Integer.parseInt(min[i]);
                        if(verVal > minVal) {
                            result &= true;
                            break;
                        } else if (verVal < minVal) {
                            result &= false;
                            break;
                        }
                        if(deviceVer.length-1==i && min.length-1>i) {
                            minVal = Integer.parseInt(min[i+1]);
                            if(minVal>0) {
                                result &= false;
                            } else {
                                result &= true;
                            }
                            break;
                        }
                        if(min.length-1==i && deviceVer.length-1>i) {
                            result &=true;
                            break;
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            if(result && item.source.has("MaxWebOSVersion")) {
                try {
                    String[] max = item.source.getString("MaxWebOSVersion").split("\\.");
                    for(int i=0; i<max.length; i++) {
                        int verVal = Integer.parseInt(deviceVer[i]);
                        int maxVal = Integer.parseInt(max[i]);
                        if(verVal < maxVal) {
                            result &= true;
                            break;
                        } else if (verVal > maxVal) {
                            result &= false;
                            break;
                        }
                        if(deviceVer.length-1==i && max.length-1>i) {
                            result &= true;
                            break;
                        }
                        if(max.length-1==i && deviceVer.length-1>i) {
                            verVal = Integer.parseInt(deviceVer[i+1]);
                            if(verVal>0) {
                                result &= false;
                            } else {
                                result &= true;
                            }
                            break;
                        }
                    }
                } catch(Exception e) {}
            }
            if(result && item.source.has("Category")) {
                try {
                    String category = item.source.getString("Category");
                    if(category.toLowerCase().equals("unavailable")) {
                        result &= false;
                    }
                } catch(Exception e) {}
            }
        }
        return result;
    }

    public ArrayList<PackageEntry> getUpdates() {
        ArrayList<PackageEntry> results = new ArrayList(0);
        for(int i=0; i<packages.size(); i++) {
            PackageEntry curr = (PackageEntry) packages.get(i);
            int index = installed.indexOf(new InstalledEntry(curr.id));
            if(index>-1) {
                if(curr.isUpdate(installed.get(index))) {
                    results.add(curr);
                }
            }
        }
        return results;
    }

    public void setInstalledAppList(ArrayList<InstalledEntry> apps) {
        installed = apps;
    }

    public boolean hasPackages() {
        return (packages.size()>0);
    }

    public boolean isInstalled(PackageEntry pkg) {
        return installed.contains(new InstalledEntry(pkg.id));
    }
}
