/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.server.events.ReplicatedQueueList;
import edu.caltech.ipac.firefly.server.events.ServerEventQueue;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.visualize.ProgressStat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Serializer {

    private static final ObjectMapper JSON_MAPPER = baseMapper(new ObjectMapper());
    private static final ObjectMapper MSGPACK_MAPPER = baseMapper(new ObjectMapper(new MessagePackFactory()));

    private static ObjectMapper baseMapper(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);  // Unknown properties are silently ignored
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);    // Omits fields that are empty (lists, strings, arrays, maps) during serialization
        return mapper;
    }

    public static ObjectMapper getJsonMapper() { return JSON_MAPPER; }
    public static ObjectMapper getMsgpackMapper() { return MSGPACK_MAPPER; }


    /**
     * Typed MessagePack envelope format; Class type is stored as part of the payload.
     *   [ 0xC1 0xC1 0x01 ]  (magic + version; invalid UTF-8 bytes prevent text collision)
     *   [ u16 typeId ]     (unsigned short)
     *   [ msgpack bytes ]  (payload)
     */
    private static final byte[] TYPED_MAGIC = new byte[] { (byte) 0xC1, (byte) 0xC1, (byte) 0x01 };

    /**
     * Registry: typeId <-> class.
     * Keep IDs stable over time.  Add new types by appending new IDs; don't renumber.
     * NOTE: You must register any types you intend to deserialize.
     * This keeps the payload small while still allowing automatic type resolution during deserialization.
     */
    private static final Map<Short, Class<?>> ID_TO_CLASS;
    private static final Map<Class<?>, Short> CLASS_TO_ID;

    static {
        // Register a type id <-> class mapping for typed MessagePack.
        Map<Short, Class<?>> idToClass = Map.ofEntries(
                Map.entry((short) 1, File.class),
                Map.entry((short) 2, Character.class),
                Map.entry((short) 100, JobInfo.class),
                Map.entry((short) 101, FileInfo.class),
                Map.entry((short) 102, JobManager.BackGroundInfo.class),
                Map.entry((short) 103, UserInfo.class),
                Map.entry((short) 104, ProgressStat.class),
                Map.entry((short) 105, UploadFileInfo.class),
                Map.entry((short) 106, ServerEvent.class),
                Map.entry((short) 107, ServerEventQueue.class),
                Map.entry((short) 108, ReplicatedQueueList.EventQueueList.class)
        );
        Map<Short, Class<?>> id2c = new HashMap<>(idToClass);
        Map<Class<?>, Short> c2id = new HashMap<>(id2c.size() * 2);

        for (Map.Entry<Short, Class<?>> e : id2c.entrySet()) {
            Short id = e.getKey();
            Class<?> cls = e.getValue();
            if (id == null || cls == null) continue;
            if (c2id.containsKey(cls)) {
                throw new IllegalArgumentException("Duplicate class registered: " + cls.getName());
            }
            c2id.put(cls, id);
        }
        ID_TO_CLASS = Collections.unmodifiableMap(id2c);
        CLASS_TO_ID = Collections.unmodifiableMap(c2id);
    }

//====================================================================
//  Typed MessagePack API
//====================================================================

    /**
     * Serializes the value as typed MessagePack if its class is registered;
     * otherwise, serializes it as plain MessagePack.
     * Supported classes are registered statically in this class at initialization time.
     */
    public static byte[] toTypedMessagePack(Object value) {
        if (value == null) return null;
        byte[] payload = toMessagePack(value);

        Short typeId = CLASS_TO_ID.get(value.getClass());
        if (typeId == null) {
            return payload;  // not registered; return plain MessagePack
        }

        // [magic][u16 typeId][payload]
        ByteBuffer bb = ByteBuffer.allocate(TYPED_MAGIC.length + 2 + payload.length);
        bb.put(TYPED_MAGIC);
        bb.putShort(typeId);
        bb.put(payload);
        return bb.array();
    }

    /**
     * Deserialize a typed MessagePack envelope into its registered Java class if registered.
     * Otherwise, deserialize as plain MessagePack.
     */
    public static Object fromTypedMessagePack(byte[] data) {
        if (data == null) return null;

        try {

            if (isTypedMessagePack(data)) {
                ByteBuffer bb = ByteBuffer.wrap(data);
                bb.position(TYPED_MAGIC.length);

                short typeId = bb.getShort();
                Class<?> cls = ID_TO_CLASS.get(typeId);
                if (cls == null) {
                    throw new SerializationException("Unknown typeId for typed MessagePack: " + (typeId & 0xFFFF), null);
                }

                byte[] payload = new byte[bb.remaining()];
                bb.get(payload);

                return fromMessagePack(payload, cls);
            } else if(isMsgPack(data)) {
                return fromMessagePack(data, Object.class); // plain MessagePack
            } else {
                // Previously, strings were stored as UTF-8 bytes, not MessagePack.
                // Temporary workaround for backward compatibility.
                return toUtf8(data);
            }
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Typed MessagePack deserialization failed", e);
        }
    }

    /** True if bytes begin with the typed MessagePack magic header. */
    public static boolean isTypedMessagePack(byte[] data) {
        if (data == null || data.length < TYPED_MAGIC.length + 2) return false; // +2 for typeId
        for (int i = 0; i < TYPED_MAGIC.length; i++) {
            if (data[i] != TYPED_MAGIC[i]) return false;
        }
        return true;
    }


//====================================================================
//  MessagePack API without type info
//====================================================================

    public static byte[] toMessagePack(Object value) {
        try {
            return getMsgpackMapper().writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("MessagePack serialization failed", e);
        }
    }

    public static <T> T fromMessagePack(byte[] data, Class<T> type) {
        if (data == null) return null;
        try {
            return getMsgpackMapper().readValue(data, type);
        } catch (Exception e) {
            throw new SerializationException("MessagePack deserialization failed", e);
        }
    }

//====================================================================
//  JSON API
//====================================================================

    public static byte[] toJsonBytes(Object value) {
        try {
            return getJsonMapper().writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("JSON serialization failed", e);
        }
    }

    public static String toJsonString(Object value) {
        try {
            return getJsonMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new SerializationException("JSON serialization failed", e);
        }
    }

    public static <T> T fromJson(byte[] data, Class<T> type) {
        if (data == null) return null;
        try {
            return getJsonMapper().readValue(data, type);
        } catch (Exception e) {
            throw new SerializationException("JSON deserialization failed", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) return null;
        try {
            return getJsonMapper().readValue(json, type);
        } catch (Exception e) {
            throw new SerializationException("JSON deserialization failed", e);
        }
    }

//====================================================================
//  Helper methods
//====================================================================

    public static boolean isMsgPack(byte[] raw) {
        if (raw == null || raw.length == 0) return false;
        try (MessageUnpacker u = MessagePack.newDefaultUnpacker(raw)) {
            u.unpackValue();                 // parse one complete value
            return !u.hasNext();             // ensure no trailing junk
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempt MessagePack first, then fall back to JSON.
     * Intended for Redis migration.
     */
    public static <T> T fromMessagePackOrJson(byte[] data, Class<T> type) {
        if (data == null) return null;
        try {
            return fromMessagePack(data, type);
        } catch (Exception mpFail) {
            return fromJson(data, type);
        }
    }

    public static boolean looksLikeJson(byte[] data) {
        if (data == null || data.length == 0) return false;
        byte b = data[0];
        return b == '{' || b == '[';
    }

    public static String toUtf8(byte[] data) { return data == null ? null : new String(data, StandardCharsets.UTF_8); }
    public static byte[] fromUtf8(String data) { return data == null ? null : data.getBytes(StandardCharsets.UTF_8); }

//====================================================================
//  Inner Classes
//====================================================================

    public static class SerializationException extends RuntimeException {
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
