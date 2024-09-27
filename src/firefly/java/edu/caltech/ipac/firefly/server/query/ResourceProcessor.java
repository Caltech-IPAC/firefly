/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.SortedSet;

import static edu.caltech.ipac.firefly.server.query.ResourceProcessor.PROC_ID;
import static edu.caltech.ipac.util.FileUtil.writeStringToFile;


/**
 * Resources are persistent data taken from a unique Table Request.  Uniqueness is based on getUniqueID.
 * Once a resource is created, it will remain unchanged until deleted.
 * firefly.util.table.makeResourceRequest is created to help work with resources.
 * See "Table as a Resource" in tests-table.html for sample usage.
 *
 * Resource comes with basic security features.  This info is saved as a .acl file next to the database files.
 * scope: This controls the visibility of a resource.  It can be 'global', 'user', or 'protected'.
 *      global is public.  Anyone can access it.  If this resource is created with a secret, then that secret is needed to delete it.
 *      user is visible to only the user who created it.  User is based on a 'usrkey' cookie set on the browser.
 *      protected resource is one created with a secret token.  It can only be accessed using the same secret token.
 *
 * Any resource created without scope is considered global.
 */
@SearchProcessorImpl(id = PROC_ID, params =
        {   @ParamDoc(name = "action",          desc = "required.  one of 'create', 'query', 'delete'"),
            @ParamDoc(name = "searchRequest",   desc = "required.  the table request to create this resource from"),
            @ParamDoc(name = "scope",           desc = "visibility of this resource.  one of 'global', 'user', 'protected'.  defaults to 'global'"),
            @ParamDoc(name = "secret",          desc = "secret token used to access 'protected' resources"),
        })
public class ResourceProcessor extends EmbeddedDbProcessor {
    public static final String PROC_ID = "ResourceProcessor";
    public static final String SUBDIR_PATH = "resource-db";

    public static final String ACTION = "action";
    public static final String SCOPE = "scope";
    public static final String SECRET = "secret";

    public static final String CREATE = "create";
    public static final String QUERY = "query";
    public static final String DELETE = "delete";

    public static final String GLOBAL = "global";
    public static final String USER = "user";
    public static final String PROTECTED = "protected";


    /**
     * One database per resource with basic access information.
     * Access info is saved in a file with a .acl extension.
     */
    public DbAdapter getDbAdapter(TableServerRequest treq) {
        String resourceID = getResourceID(treq);
        return DbAdapter.getAdapter(treq, (ext) ->
                new File(ServerContext.getHiPSDir(), "%s/%s.%s".formatted(SUBDIR_PATH, resourceID, ext))
        );
    }

    /**
     * @param request  a Table Request
     * @return a unique string ID used to match TableRequest
     */
    public String getUniqueID(ServerRequest request) {
        try {
            TableServerRequest sreq = QueryUtil.getSearchRequest((TableServerRequest) request);
            SortedSet<Param> params = sreq.getSearchParams();
            return StringUtils.toString(params, "|");
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected FileInfo ingestDataIntoDb(TableServerRequest req, DbAdapter dbAdapter) throws DataAccessException {
        writeStringToFile(getAclFile(dbAdapter.getDbFile()), String.format("scope=%s\nsecret=%s\nuser=%s\n",
                req.getParam(SCOPE, GLOBAL),
                req.getParam(SECRET, ""),
                ServerContext.getRequestOwner().getUserKey()));

        return super.ingestDataIntoDb(req, dbAdapter);
    }

    public DataGroup fetchDataGroup(TableServerRequest treq) throws DataAccessException {
        try {
            TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
            SearchProcessor<?> processor = SearchManager.getProcessor(sreq.getRequestId());
            if (processor instanceof CanFetchDataGroup) {
                return ((CanFetchDataGroup)processor).fetchDataGroup(sreq);
            } else throw new IllegalArgumentException("SearchProcessor not found for the given request: " + getUniqueID(treq));
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        if (!hasAccess(treq, dbAdapter.getDbFile())) throw new DataAccessException("Access denied");
        String action = treq.getParam(ACTION, QUERY);
        if (action.equals(DELETE)) {
            dbAdapter.close(true);
            getAclFile(dbAdapter.getDbFile()).delete();
            return new DataGroupPart(new DataGroup("empty set", new DataType[]{new DataType("dummy", String.class)}), 0, 0);
        }

        if (action.equals(CREATE)) treq.setPageSize(1);            // when creating... just load database.  don't return any data. this version of hsqldb 0 return all.  so, set to 1 instead.
        return super.getResultSet(treq, dbAdapter);
    }

    public boolean doLogging() {
        return false;
    }

//====================================================================
//
//====================================================================

    private File getAclFile (File dbFile) {
        String acl = dbFile.getAbsolutePath().replaceFirst("\\.\\S+$", ".acl");
        return new File(acl);
    }

    private boolean hasAccess(TableServerRequest treq, File dbFile) {
        File aclFile = getAclFile(dbFile);
        if (aclFile.exists()) {
            Properties acl = new Properties();
            try {
                BufferedInputStream bif = new BufferedInputStream(new FileInputStream(aclFile));
                acl.load(bif);
            } catch (Exception ignore) {}    // ignore.  assume empty file
            String scope = acl.getProperty(SCOPE,GLOBAL);
            String secret = acl.getProperty(SECRET, "");
            String user = acl.getProperty(USER, "");
            String action = treq.getParam(ACTION, QUERY);
            if (action.equals(DELETE) && !StringUtils.isEmpty(secret)) {
                return treq.getParam(SECRET, "").equals(secret);      // to delete a resource with password, you need a password regardless of scope
            } else {
                switch (scope) {
                    case GLOBAL:
                        return true;
                    case USER:
                        return user.equals(ServerContext.getRequestOwner().getUserKey());
                    case PROTECTED:
                        return secret.equals(treq.getParam(SECRET, ""));
                }
            }
        }
        return true;
    }

    /**
     * This ID is created from a md5 hex of the parameters in the table request.
     * @param treq the Table Request of a Resource
     * @return a 32 characters string used as an ID for this resource backed by the given Table Request.
     */
    private String getResourceID(TableServerRequest treq) {
        return DigestUtils.md5Hex(getUniqueID(treq));
    }
}

