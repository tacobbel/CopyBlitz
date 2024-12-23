package sk.upjs.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class IntegrityChecker {

    // calculates CRC32 hash of a file
    public static String calculateHash(File file) throws IOException {
        CRC32 crc = new CRC32();

        try (CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), crc)) {
            byte[] buffer = new byte[1024];
            while (cis.read(buffer) != -1) {
                // data is read and CRC is automatically updated
            }
        }
        // returns hash in hexadecimal format
        return Long.toHexString(crc.getValue());
    }

    public static boolean compareFiles(File file1, File file2) {
        try {
            String hash1 = calculateHash(file1);
            String hash2 = calculateHash(file2);
            return hash1.equals(hash2);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
