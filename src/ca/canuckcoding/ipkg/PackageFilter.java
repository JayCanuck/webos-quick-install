
package ca.canuckcoding.ipkg;

/**
 * @author Jason Robitaille
 */
public enum PackageFilter {
    Applications("Application"),
    Kernels("Kernel"),
    Linux_Apps("Linux Application"),
    Linux_Daemons("Linux Daemon"),
    Patches("Patch"),
    Plugins("Plugin"),
    Services("Service"),
    Themes("Theme"),
    None(null);

    private String type;
    PackageFilter(String val) {
        type = val;
    }
    @Override
    public String toString() {
        return type;
    }
}
