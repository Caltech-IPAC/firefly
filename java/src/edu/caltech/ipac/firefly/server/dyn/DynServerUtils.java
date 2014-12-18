package edu.caltech.ipac.firefly.server.dyn;

import edu.caltech.ipac.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class DynServerUtils {

    public static Object copy(Object orig) {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
            in.close();
        }

        catch (Exception e) {
            obj = null;
        }

        return obj;
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static String convertUnixToMJD(String unix) {
        if (!StringUtils.isEmpty(unix)) {
            long unixL = Long.parseLong(unix);

            // derived from http://en.wikipedia.org/wiki/Julian_day
            long mdjL = (unixL / 86400000) + 40587;

            return Long.toString(mdjL);
            
        } else {
            return null;
        }
    }

}

