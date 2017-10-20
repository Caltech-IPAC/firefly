package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsException;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.firefly.server.ws.WsUtil;
import edu.caltech.ipac.util.AppProperties;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * LOCAL filesystem implementation of workspace- NOT FULLY IMPLEMENTED
 * For testing purposes so far
 * Created by ejoliet on 6/21/17.
 */
public class LocalFSWorkspace implements WorkspaceManager {
    //TODO We might need to change those properties if we need to deal with 2 or more workspaces at the same time
    String WS_HOST_URL = AppProperties.getProperty("workspace.host.url", "https://irsa.ipac.caltech.edu");


    private final WsCredentials wscred;

    public LocalFSWorkspace() {
        String userHome = System.getProperty("user.home");
        String userKey = userHome;
        if (userHome.lastIndexOf(File.separator) > 0) {
            userKey = userHome.substring(userHome.lastIndexOf(File.separator) + 1, userHome.length());
        }
        wscred = new WsCredentials(userKey);
    }

    public LocalFSWorkspace(WsCredentials cred) {
        wscred = cred;
    }

    public static final String WS_ROOT_DIR = ".";

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private void createIfNotExist(String parent) {
        new File(parent).mkdirs();
    }


    @Override
    public String getProp(PROPS prop) {
        PROPS propFound = null;
        for (PROPS p : PROPS.values()) {
            if (p.name().equalsIgnoreCase(prop.name())) {
                propFound = p;
            }
        }
        String propVal = prop + " doesn't exist";
        if (propFound.equals(PROPS.ROOT_URL)) {
            propVal = WS_HOST_URL;
        } else if (propFound.equals(PROPS.ROOT_DIR)) {
            propVal = WS_ROOT_DIR;
        } else if (propFound.equals(PROPS.AUTH)) {
            propVal = "NONE";
        } else if (propFound.equals(PROPS.PROTOCOL)) {
            propVal = PROTOCOL.WEBDAV.name();
        }

        return propVal;
    }


    @Override
    public WsResponse getList(String parentUri, int depth) throws IOException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsResponse getFile(String fileUri, Consumer consumer) throws WsException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsResponse getFile(String fileUri, File outputFile) throws WsException {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(getResourceUrl(fileUri)));
            FileUtils.writeByteArrayToFile(outputFile, bytes);
            return WsUtil.success(200, "Downloaded", outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        WsResponse resp = WsUtil.error(500);
        return resp;
    }

    String getResourceUrl(String res) {
        return getWsHome() + "/" + res;
    }

    @Override
    public WsResponse putFile(String relUri, File item, String contentType) throws WsException {

        File parent = new File(getResourceUrl(relUri));
        Path fileUpload = Paths.get("", item.getAbsolutePath());
        if (!parent.exists()) {
            createIfNotExist(relUri);
        }
        try {
            String newFile = parent.getAbsolutePath() + File.separator + item.getName();
            Files.copy(fileUpload, new FileOutputStream(newFile));
            return WsUtil.success(200, "Created", newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WsUtil.error(500);
    }

    @Override
    public File createWsLocalFile(String wspaceSaveDirectory, String filePrefix, String fileExt) {
        return null;
    }

    @Override
    public WsResponse delete(String uri) throws WsException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsResponse createParent(String newParentRelUri) throws WsException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsResponse renameFile(String originalFileRelPath, String fileName, boolean b) throws WsException {

        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsResponse moveFile(String originalFileRelPath, String newfilepath, boolean shouldOverwrite) throws WsException {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WspaceMeta getMeta(String uri, WspaceMeta.Includes includes) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public boolean setMeta(WspaceMeta... metas) {
        return false;
    }

    @Override
    public WspaceMeta newLocalWsMeta(File file, String propName, String value) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public WsCredentials getCredentials() {
        return wscred;
    }

    @Override
    public String getWsHome() {
        return WS_ROOT_DIR;
    }

    @Override
    public PROTOCOL getProtocol() {
        return PROTOCOL.LOCAL;
    }
}
