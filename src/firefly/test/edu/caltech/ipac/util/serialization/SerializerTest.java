/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.core.background.JobUtil;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.events.ReplicatedQueueList;
import edu.caltech.ipac.firefly.server.events.ServerEventQueue;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.visualize.ProgressStat;
import edu.caltech.ipac.firefly.util.event.Name;
import org.junit.Test;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.serialization.Serializer.getJsonMapper;
import static org.junit.Assert.*;
import static edu.caltech.ipac.firefly.core.Util.Try;

public class SerializerTest {

    private JobInfo createSampleJob() {
        String json = """
                {
                  "phase": "EXECUTING",
                  "jobId": "job-123",
                  "creationTime": "2025-01-01T10:00:00Z",
                  "meta": {
                    "summary": "Running",
                    "sendNotif": false,
                    "monitored": false
                  },
                  "executionDuration": 3600,
                  "ownerId": "user1",
                  "parameters": {
                    "ra": "10.5",
                    "dec": "-2.1"
                  },
                  "errorSummary": {
                    "message": "error message"
                  },
                  "results": [
                    {
                      "size": "12345",
                      "id": "result1",
                      "href": "http://example/result.fits",
                      "mimeType": "image/fits"
                    }
                  ],
                  "jobInfo": {
                    "jobUrl": "http://example/job/123",
                    "userName": "Jane Doe",
                    "progress": {
                        "percentage": 75,
                        "itemsProcessed": 75,
                        "totalItems": 100,
                        "message": "Processing"
                    }
                  }
                }
            """;

        JobInfo job = Serializer.fromJson(json, JobInfo.class);
        assertNotNull("Failed to parse JobInfo from JSON test fixture", job);
        return job;
    }

    @Test
    public void reducedPayloadTest() {
        JobInfo job = createSampleJob();

        byte[] json = JobUtil.toJsonObject(job).toJSONString().getBytes();
        byte[] msgpack = Serializer.toMessagePack(job);
        String base64 = Util.serialize(job);

        assertNotNull(json);
        assertNotNull(msgpack);
        assertNotNull(base64);
        // Size reduced from 549 to 436 (~23% smaller)
        assertTrue(
                "Expected MessagePack payload to be smaller than JSON",
                msgpack.length < json.length
        );
        // Size reduced from 2768 to 436 (~85% smaller)
        assertTrue(
                "Expected MessagePack payload to be smaller than base64",
                msgpack.length < base64.length()
        );
    }

    @Test
    public void jobInfoMsgPackRoundTrip() {
        JobInfo original = createSampleJob();

        byte[] bytes = Serializer.toTypedMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        JobInfo decoded = (JobInfo) Serializer.fromTypedMessagePack(bytes);
        assertNotNull(decoded);

        assertEquals(original.getJobId(), decoded.getJobId());
        assertEquals(original.getOwnerId(), decoded.getOwnerId());
        assertEquals(original.getPhase(), decoded.getPhase());
        assertEquals(original.getExecutionDuration(), decoded.getExecutionDuration());
        assertEquals(original.getParameters(), decoded.getParameters());
        assertEquals(
                original.getAux().getProgress(),
                decoded.getAux().getProgress()
        );
        assertEquals(
                original.getAux().getUserName(),
                decoded.getAux().getUserName()
        );
    }

    @Test
    public void jobInfoJsonRoundTrip() {
        JobInfo original = createSampleJob();

        String json = Serializer.toJsonString(original);
        assertNotNull(json);
        assertTrue(json.startsWith("{"));

        JobInfo decoded = Serializer.fromJson(json, JobInfo.class);
        assertNotNull(decoded);

        assertEquals(original.getJobId(), decoded.getJobId());
        assertEquals(original.getPhase(), decoded.getPhase());
    }

    @Test
    public void msgPackOrJson() {
        JobInfo original = createSampleJob();

        byte[] jsonBytes = Serializer.toJsonBytes(original);

        JobInfo decoded = Serializer.fromMessagePackOrJson(
                jsonBytes,
                JobInfo.class
        );

        assertNotNull(decoded);
        assertEquals(original.getJobId(), decoded.getJobId());
        assertEquals(original.getPhase(), decoded.getPhase());
    }

    @Test
    public void nullInputs() {
        assertNull(Serializer.fromMessagePack(null, JobInfo.class));
        assertNull(Serializer.fromJson((byte[]) null, JobInfo.class));
        assertNull(Serializer.fromJson((String) null, JobInfo.class));
    }

    @Test
    public void jsonJobInfoBackwardCompatible() {
        JobInfo job = createSampleJob();
        String cJsonString = Serializer.toJsonString(job);
        String pJsonString = JobUtil.toJsonObject(job).toJSONString();
        ObjectMapper mapper = getJsonMapper();
        JsonNode c = Try.it(() -> mapper.readTree(cJsonString)).get();
        JsonNode p = Try.it(() -> mapper.readTree(pJsonString)).get();

        assertEquals("JSON output should match previous implementation", c, p);
    }

    @Test
    public void fileInfoTest() {
        FileInfo fi = new FileInfo("/abc/xyz.txt", "xyz.txt", 1024L);
        fi.setDesc("test description");
        fi.setSuffix("txt");
        fi.setHasAccess(true);

        RelatedData rd = RelatedData.makeMaskRelatedData(111, "file.fits",
                List.of(new RelatedData.MaskEntry("header",0,"the header"),
                        new RelatedData.MaskEntry("value",1,"the value")),
                false, 2, "mask-data-key");
        fi.addRelatedData(rd);

        HttpServiceInput si = new HttpServiceInput("http://example.com");
        si.setFile("file.fits", new File("/abc/file.fits"));
        si.setHeader("X-Custom-Header", "CustomValue");
        fi.setRequestInfo(si);

        byte[] msgpack = Serializer.toMessagePack(fi);
        FileInfo mp = Serializer.fromMessagePack(msgpack, FileInfo.class);
        assertNotNull(mp);

        // ---------- identity / core fields ----------
        assertEquals(fi.getInternalFilename(), mp.getInternalFilename());
        assertEquals(fi.getExternalName(), mp.getExternalName());
        assertEquals(fi.getSizeInBytes(), mp.getSizeInBytes());

        // ---------- attribute-backed fields ----------
        assertEquals(fi.getDesc(), mp.getDesc());
        assertEquals(fi.getSuffix(), mp.getSuffix());
        assertEquals(fi.hasAccess(), mp.hasAccess());

        // ---------- derived/default behavior ----------
        assertFalse(mp.isBlank());
        assertEquals(200, mp.getResponseCode());
        assertNull(mp.getResponseCodeMsg());
        assertNull(mp.getContentType());

        // ---------- attribute map integrity ----------
        assertEquals(fi.getAttributeMap(), mp.getAttributeMap());

        // ---------- relatedData (should survive) ----------
        assertNotNull(mp.getRelatedData());
        assertEquals(1, mp.getRelatedData().size());

        RelatedData mpRd = mp.getRelatedData().get(0);
        assertEquals(rd.getDataType(), mpRd.getDataType());
        assertEquals(rd.getDataKey(), mpRd.getDataKey());
        assertEquals(rd.getDesc(), mpRd.getDesc());
        assertEquals(rd.getHduIdx(), mpRd.getHduIdx());
        assertEquals(rd.getSearchParams(), mpRd.getSearchParams());

        // ---------- requestInfo (should survive if serializable) ----------
        assertNotNull(mp.getRequestInfo());
        assertEquals(fi.getRequestInfo().getRequestUrl(), mp.getRequestInfo().getRequestUrl());
        assertEquals(fi.getRequestInfo().getHeaders(), mp.getRequestInfo().getHeaders());
        assertEquals( fi.getRequestInfo().getFiles().keySet(), mp.getRequestInfo().getFiles().keySet());

        // ---------- transient / non-serialized fields ----------
        assertFalse(mp.hasFileNameResolver());

    }

    @Test
    public void UserInfoTest() throws Exception {

        UserInfo original = new UserInfo("test-user", "Test");
        original.setEmail("user@acme.org");
        original.setLastName("User");
        original.setFirstName("Test");
        original.setUserId(123);
        original.setProperty("prop1", "value1");
        original.setProperty("prop2", "value2");
        RoleList roles= new RoleList();
        roles.add(new RoleList.RoleEntry("admin",1,"group1",10,"READ"));
        roles.add(new RoleList.RoleEntry("missionX",-1,"ALL",-1,"ADMIN"));
        original.setRoles(roles);

        byte[] bytes = Serializer.toMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        UserInfo decoded = Serializer.fromMessagePack(bytes, UserInfo.class);

        assertNotNull(decoded);
        assertEquals(original.getLoginName(), decoded.getLoginName());
        assertEquals(original.getPassword(), decoded.getPassword());
        assertEquals(original.getFirstName(), decoded.getFirstName());
        assertEquals(original.getLastName(), decoded.getLastName());
        assertEquals(original.getUserId(), decoded.getUserId());
        assertEquals(original.getProperties().size(), decoded.getProperties().size());
        assertEquals(original.getProperties().get("prop1"), decoded.getProperties().get("prop1"));
        assertEquals(original.getProperties().get("prop2"), decoded.getProperties().get("prop2"));
        assertNotNull(decoded.getRoles());
        assertEquals(2, decoded.getRoles().size());
        RoleList.RoleEntry re1= decoded.getRoles().get(0);
        assertEquals("admin", re1.getMissionName());
        assertEquals(1, re1.getMissionId());
        assertEquals("group1", re1.getGroupName());
        assertEquals(10, re1.getGroupId());
        assertEquals("READ", re1.getPrivilege());
        RoleList.RoleEntry re2= decoded.getRoles().get(1);
        assertEquals("missionX", re2.getMissionName());
        assertEquals(-1, re2.getMissionId());
        assertEquals("ALL", re2.getGroupName());
        assertEquals(-1, re2.getGroupId());
        assertEquals("ADMIN", re2.getPrivilege());
    }

    @Test
    public void serverEventTest() throws Exception {

        ServerEvent.EventTarget target = new ServerEvent.EventTarget(ServerEvent.Scope.USER, "c1", "ch1", "u1");
        ServerEvent original = new ServerEvent(
                Name.EVT_CONN_EST,
                target,
                ServerEvent.DataType.JSON,
                "{\"x\":1}",
                "from-123"
        );

        byte[] bytes = Serializer.toMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        ServerEvent decoded = Serializer.fromMessagePack(bytes, ServerEvent.class);

        assertNotNull(decoded);
        assertEquals(original.getName(), decoded.getName());

        assertNotNull(decoded.getTarget());
        assertEquals(original.getTarget().getScope(), decoded.getTarget().getScope());
        assertEquals(original.getTarget().getConnID(), decoded.getTarget().getConnID());
        assertEquals(original.getTarget().getChannel(), decoded.getTarget().getChannel());
        assertEquals(original.getTarget().getUserKey(), decoded.getTarget().getUserKey());

        assertEquals(original.getDataType(), decoded.getDataType());
        assertEquals(original.getData(), decoded.getData());
        assertEquals(original.getFrom(), decoded.getFrom());
    }

    @Test
    public void serverEventQueueTest() throws Exception {
        ServerEventQueue original = new ServerEventQueue(
                "conn-123",
                "channel-xyz",
                "userkey-abc",
                null
        );
        byte[] bytes = Serializer.toTypedMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        ServerEventQueue decoded = (ServerEventQueue) Serializer.fromTypedMessagePack(bytes);
        assertNotNull(decoded);
        assertEquals(original.getConnID(), decoded.getConnID());
        assertEquals(original.getChannel(), decoded.getChannel());
        assertEquals(original.getUserKey(), decoded.getUserKey());
    }

    @Test
    public void eventQueueListTest() {
        List<ServerEventQueue> list = List.of(
                new ServerEventQueue("c1","ch1","u1",null),
                new ServerEventQueue("c2","ch2","u2",null),
                new ServerEventQueue("c3","ch3","u3",null)
        );
        ReplicatedQueueList.EventQueueList original = new ReplicatedQueueList.EventQueueList(list);
        byte[] bytes = Serializer.toTypedMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        ReplicatedQueueList.EventQueueList decoded = (ReplicatedQueueList.EventQueueList) Serializer.fromTypedMessagePack(bytes);
        assertNotNull(decoded);
        assertEquals(original.items().size(), decoded.items().size());
        for (int i = 0; i < original.items().size(); i++) {
            ServerEventQueue oQ = original.items().get(i);
            ServerEventQueue dQ = decoded.items().get(i);
            assertEquals(oQ.getConnID(), dQ.getConnID());
            assertEquals(oQ.getChannel(), dQ.getChannel());
            assertEquals(oQ.getUserKey(), dQ.getUserKey());
        }
    }

    @Test
    public void backgroundInfoTest() throws Exception {
        JobManager.BackGroundInfo original = new JobManager.BackGroundInfo(false, "notify@ipac.caltech.edu");

        byte[] bytes = Serializer.toMessagePack(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        JobManager.BackGroundInfo decoded = Serializer.fromMessagePack(bytes, JobManager.BackGroundInfo.class);

        assertEquals(original, decoded);
        assertEquals(original.notifEnabled(), decoded.notifEnabled());
        assertEquals(original.email(), decoded.email());
    }

    @Test
    public void uploadFileInfoTest() {

        File file = new File("/tmp/upload-test.txt");   // does not need to exist
        UploadFileInfo original = new UploadFileInfo(
                "param1",
                file,
                "upload-test.txt",
                "text/plain",
                201
        );

        // ---------- serialize ----------
        byte[] msgpack = Serializer.toMessagePack(original);
        assertNotNull(msgpack);
        assertTrue(msgpack.length > 0);

        // ---------- deserialize ----------
        UploadFileInfo decoded = Serializer.fromMessagePack(msgpack, UploadFileInfo.class);

        assertNotNull(decoded);

        assertEquals(original.getPname(), decoded.getPname());
        assertEquals(original.getFileName(), decoded.getFileName());
        assertEquals(original.getContentType(), decoded.getContentType());
        assertEquals(original.getResponseCode(), decoded.getResponseCode());
        assertEquals(original.getSize(), decoded.getSize());

        assertNotNull(decoded.getFile());
        assertEquals(
                original.getFile().getPath(),
                decoded.getFile().getPath()
        );

        // ---------- toString sanity ----------
        assertTrue(decoded.toString().contains(original.getFileName()));
    }

    @Test
    public void progressStatTest() {

        ProgressStat normal = new ProgressStat(
                "job-123",
                "plot-456",
                ProgressStat.PType.DOWNLOADING,
                "Downloading data"
        );

        ProgressStat normalDecoded =
                Serializer.fromMessagePack(
                        Serializer.toMessagePack(normal),
                        ProgressStat.class
                );

        assertNotNull(normalDecoded);
        assertEquals(normal.getKey(), normalDecoded.getKey());
        assertEquals(normal.getId(), normalDecoded.getId());
        assertEquals(normal.getMessage(), normalDecoded.getMessage());
        assertEquals(ProgressStat.PType.DOWNLOADING, normalDecoded.getType());
        assertFalse(normalDecoded.isGroup());
        assertFalse(normalDecoded.isDone());

        // ============================================================
        // group progress
        // ============================================================
        List<String> members = List.of("id-1", "id-2", "id-3");
        ProgressStat group = new ProgressStat("group-999", members);

        ProgressStat groupDecoded =
                Serializer.fromMessagePack(
                        Serializer.toMessagePack(group),
                        ProgressStat.class
                );

        assertNotNull(groupDecoded);
        assertEquals(group.getKey(), groupDecoded.getKey());
        assertEquals(ProgressStat.PType.GROUP, groupDecoded.getType());
        assertTrue(groupDecoded.isGroup());
        assertEquals(members, groupDecoded.getMemberIDList());
        assertNull(groupDecoded.getMessage());
        assertNull(groupDecoded.getId());
        assertFalse(groupDecoded.isDone());

        // ============================================================
        // terminal states (SUCCESS / FAIL)
        // ============================================================
        ProgressStat success = new ProgressStat(
                "job-success",
                "plot-1",
                ProgressStat.PType.SUCCESS,
                "Completed"
        );

        ProgressStat fail = new ProgressStat(
                "job-fail",
                "plot-1",
                ProgressStat.PType.FAIL,
                "Failed"
        );

        ProgressStat successDecoded = Serializer.fromMessagePack( Serializer.toMessagePack(success), ProgressStat.class);
        ProgressStat failDecoded = Serializer.fromMessagePack( Serializer.toMessagePack(fail), ProgressStat.class);

        assertTrue(successDecoded.isDone());
        assertTrue(failDecoded.isDone());
        assertEquals(ProgressStat.PType.SUCCESS, successDecoded.getType());
        assertEquals(ProgressStat.PType.FAIL, failDecoded.getType());
    }

    @Test
    public void unregisteredTypesTest() {

        // Common types supported without explicit type registration
        byte[] msgpack = Serializer.toTypedMessagePack("ping");
        String aString = (String) Serializer.fromTypedMessagePack(msgpack);
        assertEquals("ping", aString);

        msgpack = Serializer.toTypedMessagePack(true);
        boolean aBoolean = (boolean) Serializer.fromTypedMessagePack(msgpack);
        assertTrue(aBoolean);

        msgpack = Serializer.toTypedMessagePack(1);
        int one = (int) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(1, one);

        msgpack = Serializer.toTypedMessagePack(1_000);
        int anInt = (int) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(1_000, anInt);

        msgpack = Serializer.toTypedMessagePack(1.2);
        double aDouble = (double) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(1.2, aDouble, 0.00001);

        msgpack = Serializer.toTypedMessagePack(3_000_000_000L);
        long aLong = (long) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(3_000_000_000L, aLong);

        List aList = List.of("one", 2, 3.0);
        msgpack = Serializer.toTypedMessagePack(aList);
        List dList = (List) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(aList.size(), dList.size());
        assertEquals(aList.get(0), dList.get(0));
        assertEquals(aList.get(1), dList.get(1));
        assertEquals(aList.get(2), dList.get(2));

        Map aMap = Map.of("str", "one", "int", 2, "double", 3.0);
        msgpack = Serializer.toTypedMessagePack(aMap);
        Map dMap = (Map) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(aMap.size(), dMap.size());
        assertEquals(aMap.get("str"), dMap.get("str"));
        assertEquals(aMap.get("int"), dMap.get("int"));
        assertEquals(aMap.get("double"), dMap.get("double"));

        //====================================================================
        // Less used types that is not handled correctly... register if needed
        //====================================================================

        // Date type is saved as ISO-8601 string with offset
        Date date = new Date();
        msgpack = Serializer.toTypedMessagePack(date);
        String dDate = (String) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(date, Date.from(OffsetDateTime.parse(dDate).toInstant()));

        // array is saved as a List
        String[] ary = new String[]{"a", "b", "c"};
        msgpack = Serializer.toTypedMessagePack(ary);
        List dAry = (List) Serializer.fromTypedMessagePack(msgpack);
        assertEquals(ary.length, dAry.size());
        assertEquals(ary[0], dAry.get(0));
        assertEquals(ary[1], dAry.get(1));
        assertEquals(ary[2], dAry.get(2));


    }

}
