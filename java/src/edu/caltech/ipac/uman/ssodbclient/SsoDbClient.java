package edu.caltech.ipac.uman.ssodbclient;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.security.JOSSOAdapter;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.uman.server.SsoDataManager;
import edu.caltech.ipac.uman.server.SsoDataManager.Response;
import edu.caltech.ipac.uman.ssodbclient.Params.Command;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static edu.caltech.ipac.uman.data.UmanConst.*;


/**
 * Date: 4/12/12
 *
 * @author loi
 * @version $Id: SsoDbClient.java,v 1.23 2012/12/03 22:15:07 loi Exp $
 */
public class SsoDbClient {
    public static final String TYPE = "Type";

    private Params params;

    public SsoDbClient(Params params) {
        this.params = params;
        CacheManager.setDisabled(true);
    }

    public void run() {

        if (params.getCommand() == Command.VERSION) {
            showVersion();
            System.exit(0);
        }

        if (!setupDB()) {
            System.err.println("Fail to establish connect to the database.");
            System.exit(1);
        }

        if (!StringUtils.isEmpty(params.getUserId())) {
            if (!login()) {
                System.err.println("Fail to log into the SSO system as " + params.getUserId());
                System.exit(1);
            }
        }

        int exitCode;
        if (params.getCommand() == Command.IMPORT) {
            final Ref<Integer> rcode = new Ref<Integer>(0);


            final DataSourceTransactionManager man = new DataSourceTransactionManager(JdbcFactory.getDataSource(DbInstance.josso));
            TransactionTemplate txTemplate = new TransactionTemplate(man);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
//            txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);



//            TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(JdbcFactory.getDataSource(DbInstance.josso));
            txTemplate.execute(new TransactionCallbackWithoutResult() {
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        rcode.setSource(importData(new File(params.getCmdValue())));
                        if (rcode.getSource() > 0) {
                            status.setRollbackOnly();
                        }
                    } catch (RuntimeException e) {
                        status.setRollbackOnly();
                    }
                    throw new RuntimeException("blah");
                }
            });
            exitCode = rcode.getSource();
        } else {
            ServerRequest req = new ServerRequest();
            SsoDataManager.Response<DataGroup> res = null;
            String typeStr = null;

            if (params.getCommand() == Command.LIST_ACCESS) {
                typeStr = ActionType.Type.access.getTypeStr();
                if (!StringUtils.isEmpty(params.getCmdValue())) {
                    req.setParam(MISSION_NAME, params.getCmdValue());
                }
                res = SsoDataManager.showAccess(req);
            } else if (params.getCommand() == Command.LIST_ROLE) {
                typeStr = ActionType.Type.role.getTypeStr();
                if (!StringUtils.isEmpty(params.getCmdValue())) {
                    req.setParam(MISSION_NAME, params.getCmdValue());
                }
                res = SsoDataManager.showRoles(req);
            } else if (params.getCommand() == Command.LIST_USER) {
                typeStr = ActionType.Type.user.getTypeStr();
                if (!StringUtils.isEmpty(params.getCmdValue())) {
                    req.setParam(LOGIN_NAME, params.getCmdValue());
                }
                res = SsoDataManager.showUsers(req, params.isBrief());
            }
            exitCode = res.isOk() ? 0 : 1;
            if (res != null && res.isOk()) {
                printResult(typeStr, res.getValue());
            } else {
                String msg = res == null ? "Unexpected error while executing " + params.getCommand() : res.getMessage();
                System.err.println(msg);
            }
        }
        System.exit(exitCode);
    }

    private boolean login() {

        UserInfo userInfo = JOSSOAdapter.login(params.getUserId(), params.getPasswd());
        if (userInfo != null) {
            ServerContext.getRequestOwner().setUserInfo(userInfo);
            return true;
        }
        return false;
    }

    private boolean setupDB() {
        if (params.getDb().equals("OPS")) {
            AppProperties.setProperty("josso.db.url", "jdbc:mysql://***REMOVED***/sso_user_management");
            AppProperties.setProperty("josso.db.userId", "sso_client");
//        } else if (params.getDb().equals("TEST")) {
        } else {
            AppProperties.setProperty("josso.db.url", "jdbc:mysql://kane.ipac.caltech.edu:3306/sso_user_management");
            AppProperties.setProperty("josso.db.userId", "sso_client");
        }

        AppProperties.setProperty("josso.use.connection.pool", "false");
        AppProperties.setProperty("josso.db.driver", "com.mysql.jdbc.Driver");
        AppProperties.setProperty("josso.db.password", "B0wser");

        // allowing connection info to be overridden via system and environment variables.
        for (Map.Entry prop : System.getProperties().entrySet()) {
            AppProperties.setProperty(String.valueOf(prop.getKey()), String.valueOf(prop.getValue()));
        }
        for (Map.Entry prop : System.getenv().entrySet()) {
            AppProperties.setProperty(String.valueOf(prop.getKey()), String.valueOf(prop.getValue()));
        }

        String driver = AppProperties.getProperty("josso.db.driver");
        String url = AppProperties.getProperty("josso.db.url");
        String userId = AppProperties.getProperty("josso.db.userId");
        String password = AppProperties.getProperty("josso.db.password");

        if (StringUtils.isEmpty(driver) ||
                StringUtils.isEmpty(url) ||
                StringUtils.isEmpty(userId) ||
                StringUtils.isEmpty(password)) {
            return false;
        }
        try {
            JdbcFactory.getDataSource(DbInstance.josso);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private int importUsers(ActionType type, DataGroup data) {
        List<ServerRequest> users = makeUserRequests(data);
        if (type.getAction() == ActionType.Action.delete) {
            if (!confirmDelete("You are about to remove " + users.size() + " users from the system.")) {
                System.out.println("Delete cancelled.");
                return 0;
            }
        }
        try {
            boolean hasError = false;
            for(ServerRequest user : users) {
                Response<UserInfo> r = null;
                String addtlMsg = null;
                if (type.getAction() == ActionType.Action.delete) {
                    r = SsoDataManager.removeUser(user);
                } else if (type.getAction() == ActionType.Action.update) {
                    r = SsoDataManager.updateUser(user);
                } else {
                    if (StringUtils.isEmpty(user.getParam(PASSWORD))) {
                        user.setParam(GEN_PASS, "true");
                    }
                    r = SsoDataManager.addUser(user);
                    if (r.isOk() && params.isDoSendEmail()) {
                        UserInfo userInfo = r.getValue();
                        String sendTo = StringUtils.isEmpty(params.getEmail()) ? userInfo.getEmail() : params.getEmail();
                        String ssoBaseUrl = params.getDb().equals("OPS")? "http://irsa.ipac.caltech.edu" :
                                "http://***REMOVED***";
                        SsoDataManager.sendUserAddedEmail(ssoBaseUrl, sendTo, userInfo);
                        addtlMsg = "    Email sent to " + sendTo + ".";
                    }
                }
                if (r.isError()) {
                    hasError = true;
                    System.err.println(r.getMessage());
                } else {
                    if (!params.isBrief()) {
                        System.out.println(r.getMessage());
                        if (addtlMsg != null) {
                            System.out.println(addtlMsg);
                        }
                    }
                }
            }
            return hasError ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Unexpected error while importing user data.");
            return 1;
        }
    }

    private int importRoles(ActionType type, DataGroup data) {
        List<ServerRequest> roles = makeRoleRequest(data);
        if (type.getAction() == ActionType.Action.delete) {
            if (!confirmDelete("You are about to remove " + roles.size() + " roles from the system.")) {
                System.out.println("Delete cancelled.");
                return 0;
            }
        }
        try {
            boolean hasError = false;
            for(ServerRequest role : roles) {
                Response r = null;
                if (type.getAction() == ActionType.Action.delete) {
                    r = SsoDataManager.removeRole(role);
                } else {
                    r = SsoDataManager.addRole(role);
                }
                if (r.isError()) {
                    hasError = true;
                    System.err.println(r.getMessage());
                } else {
                    System.out.println(r.getMessage());
                }
            }
            return hasError ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Unexpected error while importing role data.");
            return 1;
        }
    }

    private int importAccess(ActionType type, DataGroup data) {
        List<ServerRequest> mappings = makeAccessRequest(data);
        if (type.getAction() == ActionType.Action.delete) {
            if (!confirmDelete("You are about to remove " + mappings.size() + " access entries from the system.")) {
                System.out.println("Delete cancelled.");
                return 0;
            }
        }
        try {
            boolean hasError = false;
            for(ServerRequest ure : mappings) {
                Response r = null;
                if (type.getAction() == ActionType.Action.delete) {
                    r = SsoDataManager.removeAccess(ure);
                } else {
                    r = SsoDataManager.addAccess(ure);
                }
                if (r.isError()) {
                    hasError = true;
                    System.err.println(r.getMessage());
                } else {
                    System.out.println(r.getMessage());
                }
            }
            return hasError ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Unexpected error while importing access data.");
            return 1;
        }
    }

    private int importData(File infile) {
        if (infile == null || !infile.canRead()) {
            System.err.println("Unable to read file: " + infile);
            return 1;
        }

        DataGroup dg = null;
        try {
            dg = DataGroupReader.read(infile,false, false);
        } catch (IOException e) {
            System.err.println("Unable to read input file:" + infile);
            return 1;
        }

        ActionType type = ActionType.getType(dg);

        Response res = null;

        if (type.getType() == ActionType.Type.user) {
            return importUsers(type, dg);
        } else if (type.getType() == ActionType.Type.role) {
            return importRoles(type, dg);
        } else if (type.getType() == ActionType.Type.access) {
            return importAccess(type, dg);
        } else {
            System.err.println("Unrecognized import Type.");
            return 1;
        }
    }

    private boolean confirmDelete(String s) {
        System.out.println(s);
        System.out.print("Do you want to continue?[y|n] ");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            return String.valueOf(br.readLine()).equalsIgnoreCase("y");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private List<ServerRequest> makeUserRequests(DataGroup dg) {

        List<ServerRequest> users = new ArrayList<ServerRequest>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == ActionType.Type.user) {
            for(int i = 0; i < dg.size(); i++) {
                ServerRequest user = new ServerRequest();
                DataObject row = dg.get(i);
                user.setParam(ADDRESS, (getData(row, DB_ADDRESS)));
                user.setParam(CITY, getData(row, DB_CITY));
                user.setParam(COUNTRY, getData(row, DB_COUNTRY));
                user.setParam(EMAIL, getData(row, DB_EMAIL));
                user.setParam(FIRST_NAME, getData(row, DB_FNAME));
                user.setParam(LAST_NAME, getData(row, DB_LNAME));
                user.setParam(INSTITUTE, getData(row, DB_INSTITUTE));
                user.setParam(PHONE, getData(row, DB_PHONE));
                user.setParam(POSTCODE, getData(row, DB_POSTCODE));
                user.setParam(PASSWORD, getData(row, DB_PASSWORD));
                String loginName = getData(row, DB_LOGIN_NAME);
                String email = getData(row, DB_EMAIL);

                loginName = StringUtils.isEmpty(loginName) ? email : loginName;
                email = StringUtils.isEmpty(email) ? loginName : email;

                user.setParam(LOGIN_NAME, loginName);
                user.setParam(EMAIL, email);

                users.add(user);
            }
        }
        return users;
    }

    private List<ServerRequest> makeRoleRequest(DataGroup dg) {

        List<ServerRequest> roles = new ArrayList<ServerRequest>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == ActionType.Type.role) {
            for(int i = 0; i < dg.size(); i++) {
                roles.add(insertRoleInfo(null, dg, dg.get(i)));
            }
        }
        return roles;
    }

    private List<ServerRequest> makeAccessRequest(DataGroup dg) {

        List<ServerRequest> mappings = new ArrayList<ServerRequest>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == ActionType.Type.access) {
            for(int i = 0; i < dg.size(); i++) {
                DataObject row = dg.get(i);
                String loginName = getData(row, DB_LOGIN_NAME, "");
                if (StringUtils.isEmpty(loginName)) {
                    loginName = getData(row, DB_EMAIL);
                }
                ServerRequest sr = insertRoleInfo(null, dg, row);
                sr.setParam(LOGIN_NAME, loginName);
                mappings.add(sr);
            }
        }
        return mappings;
    }
    
    private ServerRequest insertRoleInfo(ServerRequest sr, DataGroup dg, DataObject row) {
        if (sr == null) {
            sr = new ServerRequest();
        }
        sr.setParam(MISSION_NAME, getData(row, DB_MISSION, getHeader(dg, DB_MISSION + ".value")));
        sr.setParam(MISSION_NAME, getData(row, DB_MISSION_ID, getHeader(dg, DB_MISSION_ID + ".value")));
        sr.setParam(MISSION_NAME, getData(row, DB_GROUP, getHeader(dg, DB_GROUP + ".value")));
        sr.setParam(MISSION_NAME, getData(row, DB_GROUP_ID, getHeader(dg, DB_GROUP_ID + ".value")));
        sr.setParam(MISSION_NAME, getData(row, DB_PRIVILEGE, getHeader(dg, DB_PRIVILEGE + ".value")));

        return sr;
    }

    private String getData(DataObject row, String key) {
        return getData(row, key, "");
    }

    private String getData(DataObject row, String key, String def) {
        if ( hasCol(row, key)) {
            Object val = row.getDataElement(key);
            return val == null ? def : val.toString().trim();
        }
        return def;
    }

    private String getHeader(DataGroup dg, String key) {
        String sval = "";
        DataGroup.Attribute att = dg.getAttribute(key);
        if ( att != null) {
            sval = att.formatValue().trim();
        }
        return sval;
    }

    private boolean hasCol(DataObject row, String key) {
        DataType[] keys = row.getDataDefinitions();
        if (key != null && keys != null && keys.length > 0) {
            for (DataType dt : keys) {
                if (dt.getKeyName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printResult(String desc, DataGroup data) {
        try {
            if (!StringUtils.isEmpty(params.getFilter())) {
                DataGroupQuery.DataFilter[] filters = DataGroupQueryStatement.parseForStmt(params.getFilter());
                DataGroupQuery query = new DataGroupQuery();
                query.addDataFilters(filters);
                data = query.doQuery(data);
            }


            if (!StringUtils.isEmpty(desc)) {
                System.out.println(desc);
            }
            if (data == null || data.size() == 0) {
                System.out.println("No Data Found.");
            } else {
                data.shrinkToFitData();
                IpacTableWriter.save(System.out, data);
            }
        } catch (IOException e) {
            System.err.println("Unexpected Exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void showVersion() {
        try {
            Enumeration<URL> resources = SsoDbClient.class.getClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest mf = new Manifest(resources.nextElement().openStream());
                Attributes att= mf.getAttributes("client");
                if (att!=null && att.containsKey(new Attributes.Name("version"))) {
                    System.out.println(att.getValue("version"));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String promptInput(String question, boolean isPassword) {

        String q = question + "? ";

        Console console = System.console();

        String v = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {

            if (console != null) {
                if (isPassword) {
                    char pswd[] = console.readPassword(q);
                    v = new String(pswd);
                } else {
                    v = console.readLine(q);
                }
            } else {
                System.err.print(q);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        System.in));
                v = new String(reader.readLine().toCharArray());
            }

        } catch (Exception ex) {
            System.out.println("Unable to read your input!");
            System.exit(1);
        }
        return v;
    }

//====================================================================
//
//====================================================================

    public static final void main(String[] args) {

        Logger.getRootLogger().setLevel(Level.OFF);

        if (args == null || args.length == 0) {
            Params.showUsage(System.out);
            System.exit(0);
        }
        Params params = new Params(args);
        String errmsg = params.isValid();
        if (!StringUtils.isEmpty(errmsg)) {
            System.err.println(errmsg);
            Params.showUsage(System.err);
            System.exit(1);
        }

        if (!StringUtils.isEmpty(params.getUserId()) && StringUtils.isEmpty(params.getPasswd()) ) {
            String v = promptInput("Enter password for " + params.getUserId(), true);
            params.setPasswd(v);
        }

        SsoDbClient ssoClient = new SsoDbClient(params);
        ssoClient.run();
        
    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
