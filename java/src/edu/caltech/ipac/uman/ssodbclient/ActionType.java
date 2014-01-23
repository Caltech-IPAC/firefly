package edu.caltech.ipac.uman.ssodbclient;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.StringUtils;

public class ActionType {

    public static enum Type {user, role, access, unknown;
        public String getKey() { return "Type";}
        public String getTypeStr() { return "\\" + getKey() + "=" + toString();}
    };
    public static enum Action {add, delete, update};


    private Type type;
    private Action action;

    public ActionType(Type type, Action action) {
        this.type = type;
        this.action = action;
    }

    private static Type parseType(String s) {
        try {
            return Type.valueOf(s.trim());
        } catch (Exception ex) {
            return Type.unknown;
        }
    }

    private static Action parseAction(String s) {
        try {
            return Action.valueOf(s.trim());
        } catch (Exception ex) {
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
        DataGroup.Attribute type = dg.getAttribute(SsoDbClient.TYPE);
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