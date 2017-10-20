package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.data.WspaceMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Define workspaces API that will be used by UI client to connect to WS
 * To ease the interaction between client and server, the interface is defined with relative path/uri to remote ws home
 * home dir / root url <b>should/expected to</b> be defined in implementation (see {@link edu.caltech.ipac.firefly.server.WorkspaceManager.PROPS}
 * <p>
 * Created by ejoliet on 6/16/17.
 */
public interface Workspaces {

    /**
     * Gets files from relative path
     *
     * @param parentWspaceRelDir ws parent directory to get list of files
     * @param depth              if -1, give all the folders/files below the parent, 0 return the parent, 1 return children only
     * @return {@link WsResponse} - list of {@link WspaceMeta} can be found in response, see {@link WsResponse#getWspaceMeta()} resource list in response object}
     * @throws IOException handle ws error
     */
    WsResponse getList(String parentWspaceRelDir, int depth) throws IOException;

    /**
     * Get file relative to ws home
     *
     * @param wspaceRelFile remote URI relative file path , i.e. "x/y/z/aaa.tbl" will be located under WS_HOME at WS_URL
     * @param outputFile    local file downloaded
     * @return {@link WsResponse} response string should contain the abs path of the file TODO could contain more metadata
     * @throws WsException handle ws error
     */
    WsResponse getFile(String wspaceRelFile, File outputFile) throws WsException;

    /**
     * Get file via a consumer
     * @param wspaceRelFile remote URI relative file path , i.e. "x/y/z/aaa.tbl" will be located under WS_HOME at WS_URL
     * @param consumer consumer should take care of the incoming result supplied
     * @return {@link WsResponse} TODO could contain more metadata
     * @throws WsException
     */
    WsResponse getFile(String wspaceRelFile, Consumer<?> consumer) throws WsException;

    /**
     * Put the file item in a relative remote location from home directory with same name as file item
     *
     * @param wspaceRelPath relative path location to ws home
     * @param item
     * @param contentType
     * @return {@link WsResponse}
     * @throws WsException handle ws error
     */
    WsResponse putFile(String wspaceRelPath, File item, String contentType) throws WsException;


    /**
     * Delete file/folder that is in relative path to ws home
     *
     * @param wspaceRelPath path uri relative to ws home
     * @return {@link WsResponse}
     * @throws WsException handle ws error
     */
    WsResponse delete(String wspaceRelPath) throws WsException;

    /**
     * Create path to parent folder relative to ws home under, auto creating the necessary intermediate folders.
     *
     * @param newRelativeFolderPath relative to ws home folder path
     * @return {@link WsResponse}
     * @throws WsException handle ws error
     */
    WsResponse createParent(String newRelativeFolderPath) throws WsException;

    /**
     * Rename file
     *
     * @param originalFileRelPath
     * @param newfileName
     * @param shouldOverwrite
     * @return
     * @throws WsException
     */
    WsResponse renameFile(String originalFileRelPath, String newfileName, boolean shouldOverwrite) throws WsException;

    /**
     * @param originalFileRelPath relative current path
     * @param newfilepath new path
     * @param shouldOverwrite
     * @return
     * @throws WsException
     */
    WsResponse moveFile(String originalFileRelPath, String newfilepath, boolean shouldOverwrite) throws WsException;


    WspaceMeta getMeta(String uri, WspaceMeta.Includes includes);

    boolean setMeta(WspaceMeta... metas);
}
