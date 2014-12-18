package edu.caltech.ipac.firefly.util.event;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 12:45 PM
 */


/**
 * @author Trey Roby
 */
public class ServerSentEventNames {

    public static final Name SVR_BACKGROUND_REPORT=
            new Name("SvrBackgroundReport", "Background Report sent from server");

    private static final Name _allEvNames[]= {SVR_BACKGROUND_REPORT};



    public static Name getEvName(String evStr)  {
        Name retval= null;
        for (Name n : _allEvNames) {
            if (n.getName().equals(evStr)) {
                retval= n;
                break;
            }
        }
        return retval;
    }



}

