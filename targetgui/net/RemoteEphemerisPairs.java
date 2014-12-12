package edu.caltech.ipac.targetgui.net;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.client.net.SoasInterface;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.util.Assert;

import java.awt.Window;
import java.io.IOException;
import java.util.Vector;


/**
 * @author Booth Hartley
 * @version $Id: RemoteEphemerisPairs.java,v 1.2 2005/12/08 22:31:12 tatianag Exp $
 */
public class RemoteEphemerisPairs extends ThreadedService {

    private final static String OP_DESC     = "VIS EphemerisPairs";


    //-------------The follwing two string come from SutAPIServices ------------
    //-------------and should be referenced from there but I am trying to
    //-------------remove dependencies and I eventully want to make this
    //-------------referece JPL directly
    public final static String CMD_VIS = "Vis";
    public final static String TYPE_VIS_EPHEMERIS_PAIRS = "EphemerisPairs";
    //-------------------------


// ------------ old String
//    private static final String DO_EPHEMERIS_PAIRS="/SutAPI/SutAPI?cmd="
//                                + SutAPIServices.CMD_VIS
//                                + "&type="
//                                + SutAPIServices.TYPE_VIS_EPHEMERIS_PAIRS
//                                + "&ver=1.1&uid=0";

    private static final String DO_EPHEMERIS_PAIRS="/SutAPI/SutAPI?cmd="
                                                   + CMD_VIS
                                                   + "&type="
                                                   + TYPE_VIS_EPHEMERIS_PAIRS
                                                   + "&ver=1.1&uid=0";



    private String  _out[] = null;
    private Vector _vIn;

    private RemoteEphemerisPairs (Vector vIn, Window w) 
    { 
        super(w);
	_vIn = vIn;
        setOperationDesc(OP_DESC);
    }

    protected void doService() throws Exception { 
	NetworkManager manager= NetworkManager.getInstance();
	HostPort server= manager.getServer(
                             NetworkManager.EPHEMERIS_PAIR_SERVER);
        Assert.tst(server);
        _out = lowlevelRunCSpice(server, _vIn); 
    }

    public String getErrorStr() 
    { 
	return "Unable to get EphemerisPairs result";
    }
    public static String[] getID(String name, Window w)
				throws FailedRequestException
    {
	Vector vIn = new Vector();
	vIn.add(name);
	RemoteEphemerisPairs action= new RemoteEphemerisPairs(vIn, w);
        ClientLog.message("queuing job; name = " + name);
        action.execute();
        ClientLog.message("got results");
	return action._out;
    }
    public static String[] getName(String id, Window w) throws FailedRequestException
    {
	Vector vIn = new Vector();
	Integer naif_id = new Integer(id);
	vIn.add(naif_id);
	RemoteEphemerisPairs action= new RemoteEphemerisPairs(vIn, w);
	 ClientLog.message("queuing job; id = " + id);
        action.execute();
	ClientLog.message("got results");
	return action._out;
    }


    public static String[] lowlevelRunCSpice(HostPort hp, Vector vIn)
                                         throws IOException, 
                                                ClassNotFoundException,
                                                FailedRequestException {
        Vector vOut= SoasInterface.processSOASRequest(vIn,  null,
                                    DO_EPHEMERIS_PAIRS, hp);
	String[] result_value = (String[])vOut.elementAt(1);
        return result_value;
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
