package edu.caltech.ipac.uman.ssodbclient;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.EMailUtil;
import edu.caltech.ipac.firefly.server.util.EMailUtilException;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.uman.data.UserRoleEntry;
import edu.caltech.ipac.uman.server.persistence.SsoDao;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.mail.Session;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    public static final String AUTO_FILL = "AutoFill";

    /**
     * string1: user's name
     * string2: sso_base_url, ie.  http://***REMOVED***
     * string3: user's password
     */
    private static final String NEW_ACCT_MSG = "Dear %s,\n" +
            "\nA new IPAC account has been created for you." +
            "\nYour password is: %s\n" +
            "\nTo log in, enter your Email and password at our Login page:\n" +
            "\n%s/account/signon/login.do\n" +
            "\n\nOnce you have successfully logged in, you should change your password to something you can remember on your profile page.";
    private boolean isUseOpsDB;
    private boolean doSendEmail;
    private boolean initDb;
    private String emailTo = null;
    private String filterBy;
    private boolean isBrief;

    public static enum Type {user, role, access, unknown;
                    public String getKey() { return "Type";}
                    public String getTypeStr() { return "\\" + getKey() + "=" + toString();}
                };
    public static enum Action {add, delete, update};

    public void setBrief(boolean brief) {
        isBrief = brief;
    }

    public void setDbSource(String source) {
        if (source == null) return;
        if (source.equals("OPS")) {
            isUseOpsDB = true;
            AppProperties.setProperty("josso.db.url", "jdbc:mysql://***REMOVED***/sso_user_management");
            AppProperties.setProperty("josso.db.userId", "it_user");
            AppProperties.setProperty("josso.db.password", "yoshi");
        } else if (source.equals("TEST")) {
            isUseOpsDB = false;
            AppProperties.setProperty("josso.db.url", "jdbc:mysql://kane.ipac.caltech.edu:3306/sso_user_management");
            AppProperties.setProperty("josso.db.userId", "it_user");
            AppProperties.setProperty("josso.db.password", "yoshi");
        }
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public void setDoSendEmail(boolean doSendEmail) {
        this.doSendEmail = doSendEmail;
    }

    public void setFilterBy(String filterBy) {
        this.filterBy = filterBy;
    }

    public String updateUser(UserInfo user) throws DataAccessException {
        String msg  = "";
        if (getSsoDao().isUser(user.getLoginName())) {
            if (getSsoDao().updateUser(user)) {
                msg = String.format("User updated: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
            } else {
                msg = String.format("User NOT Updated: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
            }
        } else {
            msg = String.format("Update cancelled.  User does not exists: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
        }
        return msg;
    }
    
    public String addUser(UserInfo user) throws DataAccessException {
        String msg = "";

        if (!getSsoDao().isUser(user.getLoginName())) {
            if (StringUtils.isEmpty(user.getPassword())) {
                user.setPassword(RandomStringUtils.randomAlphanumeric(8));  // make this the default password if not given.
            }
            if (getSsoDao().addUser(user)) {
                msg = String.format("User added: %s, %s (%s)", user.getLastName(), user.getFirstName(), user.getEmail());

                if (doSendEmail) {
                    Properties props = new Properties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.host", "mail0.ipac.caltech.edu");
                    props.put("mail.smtp.auth", "false");
                    props.put("mail.smtp.port", "587");
                    props.put("mail.smtp.from", "donotreply@ipac.caltech.edu");
                    props.put("mail.smtp.starttls.enable", "true");

                    Session mailSession = Session.getDefaultInstance(props);
                    String ssoBaseUrl = isUseOpsDB? "http://irsa.ipac.caltech.edu" :
                            "http://***REMOVED***";
                    
                    String sendTo = StringUtils.isEmpty(emailTo) ? user.getEmail() : emailTo;
                    try {
                        EMailUtil.sendMessage(new String[]{sendTo}, null, null, "New IPAC Account created",
                                String.format(NEW_ACCT_MSG, user.getEmail(), user.getPassword(), ssoBaseUrl),
                                mailSession, false);
                        msg += " ==> email sent";
                    } catch (EMailUtilException e) {
                        System.out.print("Unable to send email to user/email:" + user.getLoginName() + "/" + user.getEmail());
                        e.printStackTrace();
                    }
                }

            } else {
                msg = String.format("User NOT added: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
            }
        } else {
            msg = String.format("This user already exists.  User NOT added: %s, %s (%s)", user.getLastName(), user.getFirstName(), user.getEmail());
        }
        return msg;
    }

    public String removeUser(UserInfo user) throws DataAccessException {
        String msg = "";
        if (getSsoDao().isUser(user.getLoginName())) {
            getSsoDao().removeUser(user.getLoginName());
            msg = String.format("User removed: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
        } else {
            msg = String.format("User not found: %s, %s (%s)", user.getLastName(), user.getFirstName(),user.getEmail());
        }
        return msg;
    }

    public String addRole(RoleList.RoleEntry role, boolean isAutoFill) throws DataAccessException {

        int missionId = role.getMissionId();
        String missionName = role.getMissionName();

        // check mission
        if (StringUtils.isEmpty(missionName)) {
           throwError("mission_name may not be null");
        } else if(missionName.equals(RoleList.ALL)) {
            if (isAutoFill && missionId == -1) {
                role.setMissionId(-99);
            }
        } else {
            int dbMissionId = getSsoDao().getMissionID(missionName);
            if (missionId == -1) {
                if (isAutoFill) {
                    if (dbMissionId == SsoDao.UNKNOW_INT) {
                        role.setMissionId(getSsoDao().getNextMissionID());
                    } else {
                        role.setMissionId(dbMissionId);
                    }
                }
            } else {
                if (missionId != dbMissionId) {
                    throwError("mission_id does not match what is in the system");
                }
            }
        }

        int groupId = role.getGroupId();
        String groupName = role.getGroupName();
        // check group
        if (StringUtils.isEmpty(groupName)) {
            if (groupId != -1) {
                throwError("Group name may not be null when ID is not -1");
            }
        } else if(groupName.equals(RoleList.ALL)) {
            if (isAutoFill && groupId == -1) {
                role.setGroupId(-99);
            }
        } else {
            int dbGroupId = getSsoDao().getGroupID(missionName, groupName);
            if (groupId == -1) {
                if (isAutoFill) {
                    if (dbGroupId == SsoDao.UNKNOW_INT) {
                        role.setGroupId(getSsoDao().getNextGroupID(missionName));
                    } else {
                        role.setGroupId(dbGroupId);
                    }
                }
            } else {
                if (groupId != groupId) {
                    throwError("group ID does not match what is in the system");
                }
            }
        }

        if (getSsoDao().roleExists(role)) {
            throwError(role + " already exists in the system.  Request ignored.");
        } else {
            if (getSsoDao().addRole(role)) {
               return String.format("Role added: %s", role);
            }
        }
        return "";
    }

    public String removeRole(RoleList.RoleEntry role) throws DataAccessException {
        if (getSsoDao().roleExists(role)) {
            if (getSsoDao().removeRole(role)) {
                return String.format("Role removed: %s", role);
            }
        } else {
            throwError(role + " does not exists in the system.  Request ignored.");
        }
        return "Fail to remove role: " + role;
    }


    public String addRoleMapping(UserRoleEntry ure) throws DataAccessException {

        String email = ure.getLoginName();

        if (!getSsoDao().isUser(email)) {
            throwError(email + " is not a user of the system");
        }

        RoleList.RoleEntry role = getSsoDao().findRole(ure.getRole());

        if (role == null) {
            throwError(role + " does not exists in the system");
        }

        if (getSsoDao().addAccess(ure)) {
            return role + " added to " + email;
        } else {
            throwError(role + " not added to " + email);
        }
        return "";
    }

    public String removeRoleMapping(UserRoleEntry ure) throws DataAccessException {

        if (getSsoDao().removeAccess(ure)) {
            return String.format("Role mapping removed: %s - %s", ure.getLoginName(), ure.getRole());
        }
        return String.format("Fail to remove mapping: %s - %s", ure.getLoginName(), ure.getRole());
    }

    private void throwError(String s)  throws DataAccessException {
        throw new DataAccessException(s);
    }


    public void importData(File infile) {
        if (infile == null || !infile.canRead()) {
            System.out.println("Unable to read file: " + infile);
            return;
        }

        DataGroup dg = null;
        try {
            dg = DataGroupReader.read(infile,false, false);
        } catch (IOException e) {
            System.out.println("Unable to read input file:" + infile);
            e.printStackTrace();
            return;
        }
        DataGroup.Attribute autoFill = dg.getAttribute(AUTO_FILL);
        boolean isAutoFill = Boolean.parseBoolean(autoFill == null ? "false" : String.valueOf(autoFill.getValue()));

        ActionType type = ActionType.getType(dg);
        
        if (type.getType() == Type.user) {
            List<UserInfo> users = getUsers(dg);
            if (type.getAction() == Action.delete) {
                if (!confirmDelete("You are about to remove " + users.size() + " users from the system.")) {
                    return;
                }
            }
            for(UserInfo user : users) {
                try {
                    String msg = "";
                    if (type.getAction() == Action.delete) {
                        msg = removeUser(user);
                    } else if (type.getAction() == Action.update) {
                        msg = updateUser(user);
                    } else {
                        msg = addUser(user);
                    }
                    System.out.println(msg);

                } catch (Exception e) {
                    System.out.println(String.format("Fail to %s %s, %s (%s)", type.getAction(), user.getLastName(), user.getFirstName(), user.getEmail()) + " - " + e.getMessage());
                }
            }

        } else if (type.getType() == Type.role) {
            List<RoleList.RoleEntry> roles = getRoles(dg);
            if (type.getAction() == Action.delete) {
                if (!confirmDelete("You are about to remove " + roles.size() + " roles from the system.")) {
                    return;
                }
            }
            for(RoleList.RoleEntry role : roles) {
                try {
                    String msg = "";
                    if (type.getAction() == Action.delete) {
                        msg = removeRole(role);
                    } else {
                        msg = addRole(role, isAutoFill);
                    }
                    System.out.println(msg);

                } catch (Exception e) {
                    System.out.println(String.format("Fail to %s %s - %s", type.getAction(), role, e.getMessage()));
                }
            }
        } else if (type.getType() == Type.access) {
                List<UserRoleEntry> mappings = getRoleMappings(dg);
                if (type.getAction() == Action.delete) {
                    if (!confirmDelete("You are about to remove " + mappings.size() + " access entries from the system.")) {
                        return;
                    }
                }
                for(UserRoleEntry ure : mappings) {
                    try {
                        String msg = "";
                        if (type.getAction() == Action.delete) {
                            msg = removeRoleMapping(ure);
                        } else {
                            msg = addRoleMapping(ure);
                        }
                        System.out.println(msg);

                    } catch (Exception e) {
                        System.out.println(String.format("Fail to %s %s to %s - %s", type.getAction(), ure.getRole(), ure.getLoginName(), e.getMessage()));
                    }
            }
        } else {
            System.out.println("Unrecognized import Type.");
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

    public List<UserInfo> getUsers(DataGroup dg) {

        List<UserInfo> users = new ArrayList<UserInfo>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == Type.user) {
            for(int i = 0; i < dg.size(); i++) {
                UserInfo user = new UserInfo();
                DataObject row = dg.get(i);
                user.setAddress(getData(row, DB_ADDRESS));
                user.setCity(getData(row, DB_CITY));
                user.setCountry(getData(row, DB_COUNTRY));
                user.setEmail(getData(row, DB_EMAIL));
                user.setFirstName(getData(row, DB_FNAME));
                user.setLastName(getData(row, DB_LNAME));
                user.setInstitute(getData(row, DB_INSTITUTE));
                user.setPhone(getData(row, DB_PHONE));
                user.setPostcode(getData(row, DB_POSTCODE));
                user.setPassword(getData(row, DB_PASSWORD));
                String loginName = getData(row, DB_LOGIN_NAME);
                if (StringUtils.isEmpty(loginName)) {
                    loginName = user.getEmail();
                }
                if (StringUtils.isEmpty(user.getEmail())) {
                    user.setEmail(loginName);
                }
                user.setLoginName(loginName);

                users.add(user);
            }
        }
        return users;
    }

    public List<RoleList.RoleEntry> getRoles(DataGroup dg) {

        List<RoleList.RoleEntry> roles = new ArrayList<RoleList.RoleEntry>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == Type.role) {
            for(int i = 0; i < dg.size(); i++) {
                roles.add(getRole(dg, dg.get(i)));
            }
        }
        return roles;
    }

    public List<UserRoleEntry> getRoleMappings(DataGroup dg) {

        List<UserRoleEntry> mappings = new ArrayList<UserRoleEntry>();
        ActionType type = ActionType.getType(dg);
        if (type.getType() == Type.access) {
            for(int i = 0; i < dg.size(); i++) {
                DataObject row = dg.get(i);
                String loginName = getData(row, DB_LOGIN_NAME, "");
                if (StringUtils.isEmpty(loginName)) {
                    loginName = getData(row, DB_EMAIL);
                }
                RoleList.RoleEntry role = getRole(dg, row);
                mappings.add( new UserRoleEntry(loginName, role));
            }
        }
        return mappings;
    }
    
    private RoleList.RoleEntry getRole(DataGroup dg, DataObject row) {

        String mission = getData(row, DB_MISSION, getHeader(dg, DB_MISSION + ".value"));
        int missionId = getDataInt(row, DB_MISSION_ID, getHeaderInt(dg, DB_MISSION_ID + ".value"));
        String group = getData(row, DB_GROUP, getHeader(dg, DB_GROUP + ".value"));
        int groupId = getDataInt(row, DB_GROUP_ID, getHeaderInt(dg, DB_GROUP_ID + ".value"));
        String access = getData(row, DB_PRIVILEGE, getHeader(dg, DB_PRIVILEGE + ".value"));

        return new RoleList.RoleEntry(mission, missionId, group, groupId, access, -1);
    }

//    private String getString(DataGroup.Attribute attr) {
//        return getString(attr == null ? null : attr.getValue(), "");
//    }
//
//    private int getInt(DataGroup.Attribute attr) {
//        return getDataInt(attr == null ? null : attr.getValue(), -1);
//    }
//

    private String getData(DataObject row, String key) {
        return getData(row, key, "");
    }

    private String getData(DataObject row, String key, String def) {
//        String sval = def;
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

    private int getDataInt(DataObject row, String key, int def){
        return getInt(getData(row, key, ""), def);
    }

    private int getHeaderInt(DataGroup dg, String key) {
        return getInt(getHeader(dg, key), -1);
    }
    
    private int getInt(Object o, int def) {
        int val = def;
        if (o != null) {
            try {
                val = Integer.parseInt(o.toString());
            } catch (Exception ex) {}
        }
        return val;
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
            if (!StringUtils.isEmpty(filterBy)) {
                DataGroupQuery.DataFilter[] filters = DataGroupQueryStatement.parseForStmt(filterBy);
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
            System.out.println("Unexpected Exception:");
            e.printStackTrace();
        }
    }

    private void listAccess(String mission, String user) {
        DataGroup dg = getSsoDao().getAccess(mission, user);
//        if (dg != null) {
//            DataType email = dg.getDataDefintion("login_name");
//            if (email != null) {
//                email.setKeyName(DB_EMAIL);
//            }
//        }
        printResult(Type.access.getTypeStr(), dg);
    }

    private void listRoles(String mission) {
        DataGroup dg = getSsoDao().getRoles(mission);
        if (dg == null) {
            System.out.println("ERROR >> mission not found: " + mission);
        } else {
            printResult(Type.role.getTypeStr(), dg);
        }
    }

    private void listUsers(String user) {
        DataGroup dg = getSsoDao().getUserInfo(user, isBrief);
        if (dg == null) {
            System.out.println("ERROR >> user not found: " + user);
        } else {
            DataType addr = dg.getDataDefintion("address1");
            if (addr != null) {
                addr.setKeyName(DB_ADDRESS);
            }
            printResult(Type.user.getTypeStr(), dg);
        }
    }

    SsoDao getSsoDao() {
        initDb();
        return SsoDao.getInstance();
    }

    void initDb() {
        if (!initDb) {
            AppProperties.setProperty("josso.use.connection.pool", "false");
            AppProperties.setProperty("josso.db.driver", "com.mysql.jdbc.Driver");

            for (Map.Entry prop : System.getProperties().entrySet()) {
                AppProperties.setProperty(String.valueOf(prop.getKey()), String.valueOf(prop.getValue()));
            }
            for (Map.Entry prop : System.getenv().entrySet()) {
                AppProperties.setProperty(String.valueOf(prop.getKey()), String.valueOf(prop.getValue()));
            }

            AppProperties.setProperty("josso.use.connection.pool", "false");

            String driver = AppProperties.getProperty("josso.db.driver");
            String url = AppProperties.getProperty("josso.db.url");
            String userId = AppProperties.getProperty("josso.db.userId");
            String password = AppProperties.getProperty("josso.db.password");

            if (StringUtils.isEmpty(driver) ||
                    StringUtils.isEmpty(url) ||
                    StringUtils.isEmpty(userId) ||
                    StringUtils.isEmpty(password)) {

                if (StringUtils.isEmpty(driver) ) {
                    String v = promptInput("josso.db.driver");
                    AppProperties.setProperty("josso.db.driver", v);
                }

                if (StringUtils.isEmpty(url) ) {
                    String v = promptInput("database hostname(kane|alcazar)");
                    AppProperties.setProperty("josso.db.url", String.format("jdbc:mysql://%s.ipac.caltech.edu:3306/sso_user_management", v));
                }

                if (StringUtils.isEmpty(userId) ) {
                    String v = promptInput("josso.db.userId");
                    AppProperties.setProperty("josso.db.userId", v);
                }

                if (StringUtils.isEmpty(password) ) {
                    String v = promptInput("josso.db.password");
                    AppProperties.setProperty("josso.db.password", v);
                }
            }
            initDb = true;
        }
    }

    private static String promptInput(String question) {

        System.out.print(question + "? =");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String v = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        try {
            v = br.readLine();
        } catch (IOException ioe) {
            System.out.println("Unable to read your input!");
            System.exit(1);
        }
        return v;
    }

    private static void printDbConnInfo() {
        System.out.println("connecting to database using these properties:");
        System.out.println("josso.db.driver= '" + AppProperties.getProperty("josso.db.driver") + "'");
        System.out.println("josso.db.url= '" + AppProperties.getProperty("josso.db.url") + "'");
        System.out.println("josso.db.userId= '" + AppProperties.getProperty("josso.db.userId") + "'");
        System.out.println("josso.db.password= '" + AppProperties.getProperty("josso.db.password") + "'");
    }



//====================================================================
//
//====================================================================

    public static final void showUsage() {
        System.out.println("\n\nUsage:  java -jar ssodb_client.jar [-v] [-b] [-email] [-ops|-test] -filter [-import=<data_file_name> | -lu[=<user>] | -lr[=<mission>] | -la[=<mission>] ]");
        System.out.println("\n");
        System.out.println("    -import: import the data file into the sso database");
        System.out.println("        Type header format is:  \\Type=data[:action]");
        System.out.println("            data may be 'user', 'role', or 'access'");
        System.out.println("            action may be 'add', 'update', or 'delete', default to 'add'");
        System.out.println("    -lu[=<user>]: list users.  if user is provided, it will display user's info including his/her access.");
        System.out.println("    -lr[=<mission>]: list roles.  filter by mission if provided");
        System.out.println("    -la[=<mission>]: list access.  filter by mission if provided");
        System.out.println("    -v:  version");
        System.out.println("    -b:  brief or short format");
        System.out.println("    -email[=mailTo]:  send email with password to new users.  if mailTo is provided, send email to mailTo instead.");
        System.out.println("    -filter=condition(s): one or more conditions.  conditions are separated by ' and '");
        System.out.println("                          operators are one of > < = ! >= <= IN.  A condition is 'col op value'");
        System.out.println("                          enclose this whole parameter in double-quote when there are spaces.");
        System.out.println("  DB connection is optional.  if not given, it will prompt you.");
        System.out.println("    -ops: connect to ops database");
        System.out.println("    -test: connect to test database\n");
    }

    
    enum Operation {IMPORT, LIST_USER, LIST_ROLE, LIST_ACCESS};
    
    public static final void main(String[] args) {

        Logger.getRootLogger().setLevel(Level.OFF);
        if (args == null || args.length> 3 || args.length < 1) {
            showUsage();
            System.exit(0);
        }
        SsoDbClient ssoClient = new SsoDbClient();
        
        Operation op = null;
        String user = null;
        String mission = null;
        String infile = null;
        for (String s : args) {
            if (s != null) {
                if (s.equals("-v")) {
                    showVersion();
                    System.exit(0);
                } else if(s.startsWith("-b")) {
                    ssoClient.setBrief(true);
                } else if(s.startsWith("-email")) {
                    ssoClient.setDoSendEmail(true);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        ssoClient.setEmailTo(parts[1]);
                    }
                } else if(s.startsWith("-import")) {
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        infile = parts[1];
                        op = Operation.IMPORT;
                    }
                } else if(s.startsWith("-lu")) {
                    op = Operation.LIST_USER;
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        user = parts[1];
                    }
                } else if(s.startsWith("-lr")) {
                    op = Operation.LIST_ROLE;
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        mission = parts[1];
                    }
                } else if(s.startsWith("-la")) {
                    op = Operation.LIST_ACCESS;
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        mission = parts[1];
                    }
                } else if(s.startsWith("-filter")) {
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        ssoClient.setFilterBy(parts[1]);
                    }
                } else if(s.startsWith("-ops")) {
                    ssoClient.setDbSource("OPS");
                } else if(s.startsWith("-test")) {
                    ssoClient.setDbSource("TEST");
                }
            }
        }

        if (op == Operation.IMPORT && infile != null) {
            ssoClient.importData(new File(infile));
        } else if (op == Operation.LIST_USER) {
            ssoClient.listUsers(user);
            if (!StringUtils.isEmpty(user)) {
                ssoClient.listAccess(null, user);
            }
        } else if (op == Operation.LIST_ROLE) {
            ssoClient.listRoles(mission);
        } else if (op == Operation.LIST_ACCESS) {
            ssoClient.listAccess(mission, null);
        } else {
            showUsage();
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

    private static class ActionType {
        Type type;
        Action action;

        public ActionType(Type type, Action action) {
            this.type = type;
            this.action = action;
        }
        
        private static Type parseType(String s) {
            try {
                return Type.valueOf(s.trim());
            } catch(Exception ex) {
                return Type.unknown;
            }
        }

        private static Action parseAction(String s) {
            try {
                return Action.valueOf(s.trim());
            } catch(Exception ex) {
                return Action.add;
            }
        }

        public Type getType() {
            return type;
        }

        public Action getAction() {
            return action;
        }
        
        public static ActionType getType(DataGroup dg) {
            DataGroup.Attribute type = dg.getAttribute(TYPE);
            String v = type == null ? null : (String) type.getValue();
            return parse(v);
        }
        
        public static ActionType parse(String s) {
            String t = null, a = null;
            if (!StringUtils.isEmpty(s)) {
                String[] parts = s.split(":", 2);
                if (parts.length > 0) {
                    t = parts[0];
                }
                if (parts.length > 1) {
                    a = parts[1];
                }
            }
            return new ActionType(parseType(t), parseAction(a));
        }
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
