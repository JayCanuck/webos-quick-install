
package ca.canucksoftware.ipkg;

import java.util.Comparator;

/**
 * @author Jason Robitaille
 */
public class PackageSorter implements Comparator {
    boolean ascending;
    PackageSorter(boolean ascending) {
        this.ascending = ascending;
    }
    public int compare(Object a, Object b) {
        int i;
        PackageEntry data1 = (PackageEntry)a;
        PackageEntry data2 = (PackageEntry)b;

        if (ascending) {
            i = data1.name.compareToIgnoreCase(data2.name);
            if(i==0)
                i = data1.id.compareToIgnoreCase(data2.id);
        } else {
            i =  data2.name.compareToIgnoreCase(data1.name);
            if(i==0)
                i = data2.id.compareToIgnoreCase(data1.id);
        }
        return i;
    }
}
