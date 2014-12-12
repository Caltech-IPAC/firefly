package edu.caltech.ipac.targetgui.net;

import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.ThreadedService;
import edu.caltech.ipac.target.NedAttribute;
import edu.caltech.ipac.target.PTFAttribute;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.SimbadAttribute;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * This is the new class that all the Target related network request 
 * go through.  It is a static class that uses 
 * client.net.NetCache to determine if it
 * can get a request from the cache or it has to go to the network.
 * There is one public method here for each network request the 
 * the visualization package does.  When we add a new type of 
 * request we will add new methods here.
 */
public class TargetNetwork {


    private static final ClassProperties _prop= new ClassProperties(TargetNetwork.class);
    private static Object _naifHeaders[]= { _prop.getTitle("name"),
                                            _prop.getTitle("id"),
                                            _prop.getTitle("des"),
    };

    public final static int TWO_MONTHS= 60 * 86400;

    public static ResolvedWorldPt resolveToWorldPt(String objName, Resolver resolver) throws FailedRequestException {
        ResolvedWorldPt retval;
        PositionJ2000 pos;
        switch (resolver) {
            case NED :
                pos= getNedPosition( new NedParams(objName),null).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(),objName,Resolver.NED);
                break;
            case Simbad :
                pos= getSimbadPosition( new SimbadParams(objName),null).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
                break;
            case SimbadThenNed :
                retval= getSimbadThenNed(objName);
                break;
            case NedThenSimbad :
                retval= getNedThenSimbad(objName);
                break;
            case PTF :
                pos= getPtfPosition(new PTFParams(objName), null).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.PTF);
                break;
            case Smart :
                retval= null;
                break;
            case NONE :
                throw new FailedRequestException("Cannot resolved: resolver set to NONE");
            default:
                retval= null;
                Assert.argTst(false, "resolver must be NED, Simbad, or NONE");
                break;
        }
        return retval;
    }


    public static ResolvedWorldPt getNedThenSimbad(String objName) throws FailedRequestException {
        ResolvedWorldPt wp= null;
        try {
            PositionJ2000 pos= getNedPosition( new NedParams(objName),null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null)  {
            PositionJ2000 pos= getSimbadPosition( new SimbadParams(objName),null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        }
        return wp;
    }

    public static ResolvedWorldPt getSimbadThenNed(String objName) throws FailedRequestException {
        ResolvedWorldPt wp= null;
        try {
            PositionJ2000 pos= getSimbadPosition(new SimbadParams(objName), null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null) {
            PositionJ2000 pos= getNedPosition(new NedParams(objName), null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        }
        return wp;
    }

   public static NedAttribute getNedPosition(NedParams params, Window w)
                                               throws FailedRequestException {
      NedAttribute na= (NedAttribute) CacheHelper.getObj(params);
      if (na==null)  {          // if not in cache
          if (CacheHelper.isServer()) {
              PositionJ2000 pos= NedNameResolver.getPositionVOTable(params.getName(),null);
              na= new NedAttribute(pos);
          }
          else {
              na= NedNameResolver.getPosition(params.getName(), w, true); // use NED public VOTable
          }
         CacheHelper.putObj(params,na);
      }
      return na;
   }

   public static SimbadAttribute getSimbadPosition(SimbadParams params,
                                                   Window       w)
                                               throws FailedRequestException {
       SimbadAttribute sa= (SimbadAttribute)CacheHelper.getObj(params);
       if (sa == null)  {          // if not in cache
           if (CacheHelper.isServer()) {
               sa = SimbadNameResolver.lowlevelNameResolver(params.getName());
           }
           else {
               sa = SimbadNameResolver.getPosition(params.getName(), w);
           }
           CacheHelper.putObj(params,sa);
       }
       return sa;
   }

    public static PTFAttribute getPtfPosition(PTFParams params,
                                              Window       w) throws FailedRequestException {
        PTFAttribute pa= (PTFAttribute)CacheHelper.getObj(params);
        if (pa == null)  {          // if not in cache
            if (CacheHelper.isServer()) {
                pa = PTFNameResolver.lowlevelNameResolver(params.getName());
            }
            else {
                pa = PTFNameResolver.getPosition(params.getName(), w);
            }
            CacheHelper.putObj(params,pa);
        }
        return pa;
    }



    public static boolean isNedPositionCached(NedParams params) {
        return CacheHelper.isObjCached(params);
    }

    public static boolean isSimbadPositionCached(SimbadParams params) {
        return CacheHelper.isObjCached(params);
    }

    public static boolean isHorizonsObjCached(String obj) {
        return CacheHelper.isObjCached(new HorizonsParams(obj));
    }

   public static HorizonsEphPairs.HorizonsResults[]
                   getEphIDInfo(String name, boolean showError, JFrame f)
                                 throws FailedRequestException{
       return getEphInfo(name,showError,f);
   }

    public static HorizonsEphPairs.HorizonsResults[]
                getEphNameInfo(String id, boolean showError, JFrame f) throws FailedRequestException {
        return getEphInfo(id,showError, f);
    }


    private static HorizonsEphPairs.HorizonsResults[]
                   getEphInfo(String nameOrId, boolean showError, JFrame f)
                                     throws FailedRequestException {
        HorizonsEphPairs.HorizonsResults res[];
        HorizonsParams params= new HorizonsParams(nameOrId);
        res= (HorizonsEphPairs.HorizonsResults[])CacheHelper.getObj(params);
        if (res==null) {
            if (CacheHelper.isServer()) {
                res= HorizonsEphPairs.lowlevelGetEphInfo(nameOrId,null);
            }
            else {
                res= HorizonsEphPairs.getEphInfo(nameOrId,showError, f);
            }

            CacheHelper.putObj(params,res,TWO_MONTHS);
        }
        return res;
    }



    public static HorizonsEphPairs.HorizonsResults chooseOneNaifObject(
                   HorizonsEphPairs.HorizonsResults items[], Component c) {
        Assert.tst(!CacheHelper.isServer());
        int selectedIdx= -1;
        Object values[][]= new Object[items.length][3];
        for(int i=0; i<values.length; i++) {
            values[i][0]= items[i].getName();
            values[i][1]= items[i].getNaifID();
            values[i][2]= items[i].getPrimaryDes();
        }
        JTable tab= new JTable(values, _naifHeaders);

        JScrollPane scroll= new JScrollPane(tab);
        tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panel= new JPanel(new BorderLayout());
        panel.add(new JLabel(_prop.getName("ChooseOne")), BorderLayout.NORTH);
        panel.add(scroll,BorderLayout.CENTER);

        while (selectedIdx<0) {
            tab.getSelectionModel().setLeadSelectionIndex(0);
           // OptionPaneWrap.showMess(c, panel,
           //                         _prop.getTitle("ChooseOne"),
           //                         JOptionPane.QUESTION_MESSAGE);
            selectedIdx= tab.getSelectedRow();
        }
        return items[selectedIdx];
    }


    public static byte[] lowlevelGetEphFile(HorizonsFileParams params,
                                            ThreadedService    ts)
                                        throws FailedRequestException {
        byte retval[];

        File cacheFile= CacheHelper.getFile(params);

        try {
        if (cacheFile == null)  {          // if not in cache
            retval= HorizonsEphFile.lowlevelGetEphFile(params,ts);
            File f= CacheHelper.makeFile(params.toString());
            FileOutputStream out= new FileOutputStream(f);
            out.write(retval);
            out.close();
            CacheHelper.putFile(params,cacheFile);
        }
        else {
            FileInputStream in= new FileInputStream(cacheFile);
            int available= in.available();
            retval= new byte[available];
            int size= in.read(retval);
            if (size!=retval.length) {
                throw new FailedRequestException(
                               _prop.getError("cacheFileLoad"),
                               "expected size: "+available+
                               ", actual size: "+size );
            }
            in.close();
        }
        } catch (IOException e) {
            throw new FailedRequestException(_prop.getError("ephRetrieve"),
                                             e.toString(), e);
        }
        return retval;
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
