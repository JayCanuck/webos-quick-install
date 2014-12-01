
package ca.canuckcoding.ipkg;

import ca.canuckcoding.utils.OnlineFile;
import ca.canuckcoding.webos.InstalledEntry;
import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * @author Jason Robitaille
 */
public class PackageEntry {
    public String name;
    public String id;
    public String version;
    public String developer;
    public long size;
    public String md5sum;
    public JSONObject source;
    public String[] depends;
    public String arch;
    public String file;
    
    public PackageEntry() {
        name = null;
        id = null;
        version = null;
        developer = null;
        size = 0;
        md5sum = null;
        source = null;
        depends = null;
        arch = null;
        file = null;
    }

    public boolean newerThan(PackageEntry app) {
        return isGreaterVersionThan(app.version);
    }

    public boolean isUpdate(InstalledEntry app) {
        return isGreaterVersionThan(app.getVersion());
    }

    private boolean isGreaterVersionThan(String otherVersion) {
        boolean result = false;
        Pattern p = Pattern.compile("[.-]");
        String[] self = p.split(version);
        String[] other = p.split(otherVersion);
        for(int i=0; i<other.length; i++) {
            int selfVal = Integer.parseInt(self[i]);
            int otherVal = Integer.parseInt(other[i]);
            if(selfVal > otherVal) {
                result = true;
                break;
            } else if (selfVal < otherVal) {
                result = false;
                break;
            }
            if(self.length-1==i && other.length-1>i) {
                otherVal = Integer.parseInt(other[i+1]);
                if(otherVal>=0) {
                    result = false;
                } else {
                    result = true;
                }
                break;
            }
            if(other.length-1==i && self.length-1>i) {
                selfVal = Integer.parseInt(self[i+1]);
                if(selfVal>0) {
                    result = true;
                } else {
                    result = false;
                }
                result = true;
                break;
            }
        }
        return result;
    }

    public String getDownloadUrl() {
        String url = null;
        OnlineFile online;
        if(source!=null) {
            if(source.has("Location")) {
                try {
                    online = new OnlineFile(source.getString("Location"));
                    if(online.exists()) {
                        url = online.toString();
                    }
                } catch(Exception e) {}
            }
        }
        if((url==null) && (file!=null)) {
            online = new OnlineFile(file);
            if(online.exists()) {
                url = online.toString();
            } else {
                url = null;
            }
        }
        return url;
    }

    public String getFormattedDate() {
        String result = null;
        if(source!=null) {
            if(source.has("LastUpdated")) {
                try {
                    Date d = new Date(Long.parseLong(source
                            .getString("LastUpdated"))*1000);
                    result = DateFormat.getDateTimeInstance().format(d);
                } catch(Exception e) {}
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if(o instanceof String) {
            String currId = (String) o;
            result = currId.equals(id);
        } else if(o instanceof PackageEntry) {
            PackageEntry pkgCurr = (PackageEntry) o;
            result = pkgCurr.id.equals(id);
        }
        return result;
    }
}
