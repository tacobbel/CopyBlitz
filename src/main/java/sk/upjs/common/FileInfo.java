package sk.upjs.common;

import java.io.Serial;
import java.io.Serializable;

public class FileInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -1361912600329298754L;

    public final String fileName;
    public final long size;

    public FileInfo(String FileName, long size) {
        this.fileName = FileName;
        this.size = size;
    }
    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return size;
    }
}
