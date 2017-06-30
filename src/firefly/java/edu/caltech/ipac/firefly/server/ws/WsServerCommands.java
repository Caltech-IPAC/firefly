/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.ws;


import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.SrvParam;

/**
 * Handle the commands to manage ws
 *
 * @author ejoliet
 */
public class WsServerCommands {


    public static class WsList extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            throw new IllegalArgumentException("Not implemented yet");
        }
    }

    public static class WsGetFile extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            throw new IllegalArgumentException("Not implemented yet");
        }
    }

    public static class WsPutFile extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            throw new IllegalArgumentException("Not implemented yet");
        }
    }

}

