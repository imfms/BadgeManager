package ms.imf.badge.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {

    private static final int COPY_BUFFER_SIZE = 4096;

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int readSize;
        while ((readSize = is.read(buffer)) != -1) {
            os.write(buffer, 0, readSize);
        }
    }

}
