/*
 * PackageFeed.java
 */

package ca.canuckcoding.ipkg;

import ca.canuckcoding.utils.OnlineFile;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;

public class PackageFeed {
    public ArrayList<PackageEntry> packages;
    private File feed;
    private String urlBase;

    public PackageFeed(File f, String base){
        feed = f;
        urlBase = base;
        packages = new ArrayList(0);
        scan();
    }

    private void scan() {
        BufferedReader input = null;
        packages.clear();
        try {
            if(feed.getName().toLowerCase().endsWith(".gz")) {
                input = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(feed)), "UTF-8"));
            } else {
                input = new BufferedReader(new InputStreamReader(
                        new FileInputStream(feed), "UTF-8"));
            }
        } catch(Exception e) {
            System.err.println("Error opening package list " + feed.getAbsolutePath());
            input = null;
        }
        if(input!=null) {
            try {
                String line = input.readLine();
                while(line!=null) {
                    PackageEntry curr = new PackageEntry();
                    line = line.trim();
                    while(!line.equals("")) {
                        if(!line.endsWith(":")) {
                            if(line.startsWith("Package")) {
                                curr.id = line.substring(line.indexOf(":")+1).trim();
                            } else if(line.startsWith("Description")){
                                curr.name =line.substring(line.indexOf(":")+1).trim();
                            } else if(line.startsWith("Version")){
                                curr.version = line.substring(line.indexOf(":")+1).trim();
                            } else if(line.startsWith("Depends")) {
                                curr.depends = line.substring(line.indexOf(":")+1).split(",");
                                for(int i=0; i<curr.depends.length; i++) {
                                    curr.depends[i] = curr.depends[i].trim();
                                }
                            } else if(line.startsWith("Maintainer")){
                                curr.developer = line.substring(line.indexOf(":")+1)
                                        .replaceAll("\\<.*?\\>", "").trim();
                            } else if(line.startsWith("Size")) {
                                String size = line.substring(line.indexOf(":")+1).trim();
                                if(size.length()>0) {
                                    curr.size = Long.parseLong(size);
                                }
                            } else if(line.startsWith("Architecture")) {
                                curr.arch = line.substring(line.indexOf(":")+1).trim().toLowerCase();
                            } else if(line.startsWith("MD5Sum")) {
                                curr.md5sum = line.substring(line.indexOf(":")+1).trim();
                            } else if(line.startsWith("Filename")) {
                                curr.file = urlBase + line.substring(line.indexOf(":")+1).trim();
                            } else if(line.startsWith("Source")) {
                                try {
                                    curr.source = new JSONObject(line.substring(line.indexOf(":")+1).trim());
                                } catch(Exception e) {
                                    if(curr.id!=null) {
                                        System.err.println("Error reading source JSON of " + curr.id);
                                        curr.id = null;
                                        break;
                                    }
                                }
                            }
                        }
                        line = input.readLine();
                    }
                    if(curr.id!=null && curr.name!=null) {
                        if(!packages.contains(curr)) {
                            packages.add(curr);
                        }
                    }
                    line = input.readLine();
                }
            } catch(Exception e) {
                System.err.println("Error parsing package feed: " + e.getMessage());
            }
        }
        if(input!=null)
            try {
                input.close();
            } catch(Exception e) {}
    }

    public int indexOf(String id) {
        int index = -1;
        for(int i=0; i<packages.size(); i++) {
            if(packages.get(i).id.equalsIgnoreCase(id)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static PackageFeed Download(String url) {
        PackageFeed result = null;
        String base = null;
        int index = url.lastIndexOf("/");
        if(index>-1) {
            base = url.substring(0, index+1);
        }
        OnlineFile online = new OnlineFile(url);
        File f = online.download();
        if(f==null) { //2nd try if 1st failed
            f = online.download();
        }
        if(base!=null && f!=null) {
            result = new PackageFeed(f, base);
        }
        return result;
    }
}