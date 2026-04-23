/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;

/**
 * Ehcache 3 serializer for {@code Object} values stored in the disk-persistent
 * {@code PERM_SMALL} cache tier.  Uses standard Java serialization; cached
 * objects must implement {@link java.io.Serializable}.
 */
public class ObjectSerializer implements Serializer<Object> {

    private final ClassLoader classLoader;

    /** Required constructor signature for ehcache 3 serializer instantiation. */
    public ObjectSerializer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public ByteBuffer serialize(Object object) throws SerializerException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (IOException e) {
            throw new SerializerException("Failed to serialize object of type " +
                    (object == null ? "null" : object.getClass().getName()), e);
        }
    }

    @Override
    public Object read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        byte[] bytes = new byte[binary.remaining()];
        binary.get(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc)
                    throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, classLoader);
            }
        }) {
            return ois.readObject();
        } catch (IOException e) {
            throw new SerializerException("Failed to deserialize object", e);
        }
    }

    @Override
    public boolean equals(Object object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary.duplicate()));
    }
}
