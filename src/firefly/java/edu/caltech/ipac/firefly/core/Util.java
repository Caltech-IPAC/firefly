/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.firefly.server.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * Date: 11/19/24
 *
 * @author loi
 * @version : $
 */
public class Util {
    private static final Logger.LoggerImpl logger = Logger.getLogger();


    /**
     * Serializes a Java object into a Base64-encoded string.
     * <p>
     * This method converts the given object into a byte stream,
     * encodes the byte stream into a Base64 string, and returns the result.
     * The object must implement {@link java.io.Serializable} for this method to work.
     * </p>
     * @param obj the object to serialize; must implement {@link java.io.Serializable}
     * @return a Base64-encoded string representing the serialized object, or null
     */
    public static String serialize(Object obj) {
        if (obj == null) return null;
        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bstream);
            ostream.writeObject(obj);
            ostream.flush();
            byte[] bytes =  bstream.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    /**
     * Deserializes a Base64-encoded string into a Java object.
     * <p>
     * This method decodes the provided Base64 string into a byte stream,
     * then reconstructs the original object using Java's object serialization mechanism.
     * </p>
     * @param base64 the Base64-encoded string representing the serialized object
     * @return the deserialized Java object, or null.
     */
    public static Object deserialize(String base64) {
        try {
            if (base64 == null) return null;
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bstream = new ByteArrayInputStream(bytes);
            ObjectInputStream ostream = new ObjectInputStream(bstream);
            return ostream.readObject();
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }




}
