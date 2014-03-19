package edu.caltech.ipac.uman.data;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
`* @author loi
 * $Id: UmanConst.java,v 1.4 2012/10/03 22:18:11 loi Exp $
 */
public class UmanConst {

    // form fields
    public static final String LOGIN_NAME   = "uman.loginName";
    public static final String FIRST_NAME   = "uman.firstName";
    public static final String LAST_NAME    = "uman.lastName";
    public static final String EMAIL        = "uman.email";
    public static final String TO_EMAIL     = "uman.to_email";
    public static final String CONFIRM_TO_EMAIL        = "uman.cto_email";
    public static final String SENDTO_EMAIL = "uman.sendto_email";
    public static final String PASSWORD     = "uman.password";
    public static final String NPASSWORD    = "uman.newPassword";
    public static final String CPASSWORD    = "uman.confirmPassword";
    public static final String GEN_PASS     = "uman.generatePassword";
    public static final String ADDRESS      = "uman.address";
    public static final String CITY         = "uman.city";
    public static final String COUNTRY      = "uman.country";
    public static final String POSTCODE     = "uman.postcode";
    public static final String PHONE        = "uman.phone";
    public static final String INSTITUTE    = "uman.institute";
    public static final String MISSION_NAME = "uman.missionName";
    public static final String MISSION_ID   = "uman.missionId";
    public static final String GROUP_NAME   = "uman.groupName";
    public static final String GROUP_ID     = "uman.groupId";
    public static final String PRIVILEGE    = "uman.privilege";

    // action keys
    public static final String ACTION       = "action";
    public static final String REGISTER     = "register";
    public static final String PROFILE      = "profile";
    public static final String NEW_PASS     = "new_pass";
    public static final String RESET_PASS   = "reset_pass";
    public static final String NEW_EMAIL    = "new_email";
    public static final String SHOW_ROLES   = "show_roles";
    public static final String ADD_ROLE     = "add_roles";
    public static final String REMOVE_ROLE  = "remove_roles";
    public static final String REMOVE_USER  = "remove_user";
    public static final String SHOW_ACCESS  = "show_access";
    public static final String ADD_ACCESS   = "add_access";
    public static final String REMOVE_ACCESS  = "remove_access";
    public static final String ADD_ACCOUNT  = "add_account";
    public static final String SHOW_USERS   = "show_users";
    public static final String ROLE_LIST    = "role_list";
    public static final String ACCESS_LIST  = "access_list";
    public static final String USER_LIST    = "user_list";
    public static final String USERS_BY_ROLE  = "users_by_role";
    public static final String SHOW_MISSION_XREF  = "show_missions";
    public static final String ADD_MISSION_XREF  = "add_missions";

    // column names
    public static final String DB_LOGIN_NAME = "login_name";
    public static final String DB_EMAIL = "email";
    public static final String DB_PASSWORD = "password";
    public static final String DB_FNAME = "first_name";
    public static final String DB_LNAME = "last_name";
    public static final String DB_ADDRESS = "address";
    public static final String DB_CITY = "city";
    public static final String DB_COUNTRY = "country";
    public static final String DB_POSTCODE = "postcode";
    public static final String DB_PHONE = "phone_number";
    public static final String DB_INSTITUTE = "institute";

    public static final String DB_MISSION = "mission_name";
    public static final String DB_MISSION_ID = "mission_id";
    public static final String DB_GROUP = "group_name";
    public static final String DB_GROUP_ID = "group_id";
    public static final String DB_PRIVILEGE = "privilege";


    // everything else
    public static final String BACK_TO_URL  = "josso_back_to";
    public static final String UMAN_PROCESSOR = "UmanProcessor";
    public static final String TITLE_AREA = "title_area";
    
    public static final String ADMIN_ROLE = "::ADMIN";
    public static final String SYS_ADMIN_ROLE = "ALL::ADMIN";
}

