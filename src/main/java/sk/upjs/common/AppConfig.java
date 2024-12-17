package sk.upjs.common;

import java.io.File;

public class AppConfig {

    public static final String APP_NAME = "CopyBlitz";
    public static File FILE_TO_COPY = new File("C:\\Users\\alica\\Videos\\kopr_filecopy\\League of Legends 2024.11.28 - 00.00.32.01.mp4"); ;
    public static final File COPY_TO = new File("C:\\Users\\alica\\Desktop\\New folder");
    public static final File TEMP = new File("C:\\Users\\alica\\IdeaProjects\\CopyBlitz\\src\\main\\resources\\temp.txt");
    public static final String HOST = "localhost";
    public static final int SERVER_PORT = 5000;
    public static final int BLOCK_SIZE = 16384; // 16 kB
}
