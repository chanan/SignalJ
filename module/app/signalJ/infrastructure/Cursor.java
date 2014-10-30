package signalJ.infrastructure;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

public class Cursor {
    public static String GetCursorPrefix()
    {
        final SecureRandom rng = new SecureRandom();
        byte[] data = new byte[4];
        StringBuilder sb = new StringBuilder();
        rng.nextBytes(data);
        for (int i = 0; i < data.length; i++) {
            sb.append(Integer.toHexString(data[i] & 0xFF).toUpperCase());
        }
        if(sb.length() < 8) sb.append('0');
        return "d-" + sb.toString() + "-";
    }
}