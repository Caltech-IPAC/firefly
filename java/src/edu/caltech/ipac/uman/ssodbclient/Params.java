package edu.caltech.ipac.uman.ssodbclient;

import edu.caltech.ipac.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

/**
 * Date: 12/5/13
 *
 * @author loi
 * @version $Id: $
 */
public class Params {

    public static final String USER_ADDR = "$user";

    enum Command {UNKNOWN, IMPORT, LIST_USER, LIST_USER_ACCESS, LIST_ROLE, LIST_ACCESS, VERSION}

    private boolean brief;
    private boolean doSendEmail;
    private String emailTo = USER_ADDR;
    private String emailMsg;
    private String emailFrom;
    private String emailSubject;
    private String db = "TEST";
    private String filter;
    private Command command = Command.UNKNOWN;
    private String cmdValue;
    private boolean moreThanOneCmd = false;
    private String userId;
    private String passwd;
    private String output;

    public Params(String... args) {

        checkOptionsFile(args);
        for (String s : args) {
            if (s != null) {
                s = s.trim();
                if (s.equals("-v")) {
                    setCommand(Command.VERSION);
                } else if (s.startsWith("-import")) {
                    setCommand(Command.IMPORT);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        setCmdValue(parts[1]);
                    }
                } else if (s.startsWith("-lua")) {
                    setCommand(Command.LIST_USER_ACCESS);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        setCmdValue(parts[1]);
                    }
                } else if (s.startsWith("-lu")) {
                    setCommand(Command.LIST_USER);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        setCmdValue(parts[1]);
                    }
                } else if (s.startsWith("-lr")) {
                    setCommand(Command.LIST_ROLE);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        setCmdValue(parts[1]);
                    }
                } else if (s.startsWith("-la")) {
                    setCommand(Command.LIST_ACCESS);
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        setCmdValue(parts[1]);
                    }
                } else {
                    registerOption(s);
                }
            }
        }
    }

    private void checkOptionsFile(String... args) {
        for (String s : args) {
            if (s != null) {
                s = s.trim();
                if (s.startsWith("-pfile")) {
                    String[] parts = s.split("=", 2);
                    if (parts.length == 2) {
                        String input = parts[1];
                        if (input != null && !StringUtils.isEmpty(input)) {
                            File inf = new File(input);
                            if (inf.canRead()) {
                                Properties p = new Properties();
                                try {
                                    p.load(new BufferedInputStream(new FileInputStream(inf)));
                                    for (String key : p.stringPropertyNames()) {
                                        String v = (String) p.get(key);
                                        v = StringUtils.isEmpty(v) ? "" : "=" + v;
                                        registerOption(key + v);
                                    }
                                } catch (IOException e) {
                                    System.out.println("Unable to read pfile:" + input);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void registerOption(String s) {
        if (s == null) return;
        s = s.trim();

        if (s.equals("-b")) {
            setBrief(true);
        } else if (s.equals("-email")) {
            setDoSendEmail(true);
        } else if (s.startsWith("-emailTo")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setEmailTo(parts[1]);
            }
        } else if (s.startsWith("-emailMsg")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setEmailMsg(parts[1]);
            }
        } else if (s.startsWith("-emailFrom")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setEmailFrom(parts[1]);
            }
        } else if (s.startsWith("-emailSubj")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setEmailSubject(parts[1]);
            }
        } else if (s.startsWith("-userid")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setUserId(parts[1]);
            }
        } else if (s.startsWith("-passwd")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setPasswd(parts[1]);
            }
        } else if (s.startsWith("-output")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setOutput(parts[1]);
            }
        } else if (s.startsWith("-filter")) {
            String[] parts = s.split("=", 2);
            if (parts.length == 2) {
                setFilter(parts[1]);
            }
        } else if (s.equals("-ops")) {
            setDb("OPS");
        } else if (s.equals("-test")) {
            setDb("TEST");
        }
    }

    public String isValid() {

        if (moreThanOneCmd) {
            return "More than one commands requested.";
        }

        if (command == Command.UNKNOWN) {
            return "No command to perform.";
        }

        if (command == Command.IMPORT) {
            if (StringUtils.isEmpty(cmdValue)) {
                return "Import is requested without specifying an input file.";
            } else if (!new File(cmdValue).canRead()) {
                return "Cannot read input file: " + cmdValue;
            }
        }
        return null;
    }


    public static final void showUsage(PrintStream ps) {
        ps.println("\n\nUsage:  java -jar ssodb_client.jar [options] command");
        ps.println("\n");
        ps.println("  Commands:");
        ps.println("    -import: import the data file into the sso database");
        ps.println("                Type header format is:  \\Type=data[:action]");
        ps.println("                    data may be 'user', 'role', or 'access'");
        ps.println("                    action may be 'add', 'update', or 'delete', default to 'add'");
        ps.println("    -lu[=<user>]    : list users.  if user is provided, it will display user's info including his/her access.");
        ps.println("    -lr[=<mission>] : list roles.  filter by mission if provided");
        ps.println("    -la[=<mission>] : list accesses.  filter by mission if provided");
        ps.println("    -v              :  version");
        ps.println();
        ps.println("  Options:");
        ps.println("    -pfile=<file_path>  : an input file containing options.");
        ps.println("    -output=<file_path> : send output to this file.");
        ps.println("    -userid=<user>      : user ID to login into the SSO system.  Will login as Guest if not given.");
        ps.println("    -passwd=<password>  : password for the given userid.  If you specify userid without a passwd, you'll be prompted for a password.");
        ps.println("    -b                  : brief format. only affects user listing for now.");
        ps.println("    -filter=condition(s): one or more conditions.  conditions are separated by ' and '");
        ps.println("                          operators are one of > < = ! >= <= IN.  A condition is 'col op value'");
        ps.println("                          enclose this whole parameter in double-quote when there are spaces.");
        ps.println("    -ops                : connect to ops database");
        ps.println("    -test               : connect to test database.  Default database if not provided.");
        ps.println("    -email              : send email with password when adding new users.");
        ps.println("    -emailTo=<addresses>: send email to these addresses.  addressses are separated by comma.");
        ps.println("                          default to $user if not given. ");
        ps.println("    -emailFrom=<address>: email sent from this address.  default to donotreply@ipac.caltech.edu");
        ps.println("    -emailSubj=<subject>: email subject.  default to 'New IPAC Account created.'");
        ps.println("    -emailMsg=<msg>     : custom message.  this will override the default message. use \\n for new line.");
        ps.println("                          message may include %1$s for user's email, %2$s for password, and %3$s for url to login page.");
        ps.println();
    }

    public static Params parse(String... args) {
        return new Params(args);
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isBrief() {
        return brief;
    }

    public void setBrief(boolean brief) {
        this.brief = brief;
    }

    public boolean isDoSendEmail() {
        return doSendEmail;
    }

    public void setDoSendEmail(boolean doSendEmail) {
        this.doSendEmail = doSendEmail;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public String getEmailMsg() {
        return emailMsg;
    }

    public void setEmailMsg(String emailMsg) {
        this.emailMsg = emailMsg;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Command getCommand() {
        return command;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public void setCommand(Command command) {
        if (this.command != Command.UNKNOWN) {
            moreThanOneCmd = true;
        }
        this.command = command;
    }

    public String getCmdValue() {
        return cmdValue;
    }

    public void setCmdValue(String cmdValue) {
        this.cmdValue = cmdValue;
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
