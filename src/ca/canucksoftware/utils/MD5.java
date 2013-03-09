
package ca.canucksoftware.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * @author Jason Robitaille
 */
public class MD5 {
    private static final byte[] HEX_CHAR_TABLE = {
            (byte)'0', (byte)'1', (byte)'2', (byte)'3',
            (byte)'4', (byte)'5', (byte)'6', (byte)'7',
            (byte)'8', (byte)'9', (byte)'a', (byte)'b',
            (byte)'c', (byte)'d', (byte)'e', (byte)'f'
        };

    public static byte[] createChecksum(File f) throws Exception {
        InputStream fis =  new FileInputStream(f);
        byte[] buffer = new byte[2048];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static String getMD5Checksum(File f) {
        String result = null;
        try {
            byte[] raw = createChecksum(f);
            result = getHexString(raw);
        } catch(Exception e) {}
        return result;
    }

    private static String getHexString(byte[] raw) throws Exception {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }
}
