package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.rpc.SearchServicesImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class SearchServerCommands {

    public static abstract class BaseSearchServerCommand extends ServerCommandAccess.ServCommand {
        public boolean getCanCreateJson() { return false; }
    }

    public static class GetRawDataSet extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String reqString = sp.getRequired(ServerParams.REQUEST);
            TableServerRequest request = TableServerRequest.parse(reqString);
            RawDataSet dataSet = new SearchManager().getRawDataSet(request);
            return dataSet.serialize();
        }

    }

    public static class ChkFileStatus extends BaseSearchServerCommand {

        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            File f= new File(sp.getRequired(ServerParams.SOURCE));
            FileStatus fstatus = new SearchManager().getFileStatus(f);
            return fstatus.toString();
        }

    }


    public static class SubmitBackgroundSearch extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String servReqStr = sp.getRequired(ServerParams.REQUEST);
            int waitMil = sp.getRequiredInt(ServerParams.WAIT_MILS);

            TableServerRequest serverRequest= ServerRequest.parse(servReqStr, new TableServerRequest());
            Request clientRequest= null;
            if (sp.contains(ServerParams.CLIENT_REQUEST))  {
                clientRequest= ServerRequest.parse(sp.getOptional(ServerParams.CLIENT_REQUEST),
                                                   new Request());
            }
            BackgroundReport report= new SearchServicesImpl().submitBackgroundSearch(
                                                   serverRequest,clientRequest,waitMil);
            return report.serialize();
        }
    }

    public static class GetStatus extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            BackgroundReport report= new SearchServicesImpl().getStatus(sp.getID());
            return report.serialize();
        }
    }

    public static class Cancel extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            new SearchServicesImpl().cancel(sp.getID());
            return "true";
        }
    }

    public static class CleanUp extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            new SearchServicesImpl().cancel(sp.getID());
            return "true";
        }
    }

    public static class DownloadProgress extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String file= sp.getRequired(ServerParams.FILE);
            SearchServices.DownloadProgress dp= new SearchServicesImpl().getDownloadProgress(file);
            return dp.toString();
        }
        public boolean getCanCreateJson() { return false; }
    }

    public static class SetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList = sp.getIDList();
            String email = sp.getRequired(ServerParams.EMAIL);
            new SearchServicesImpl().setEmail(idList,email);
            return "true";
        }
    }

    public static class SetAttribute extends BaseSearchServerCommand  {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList= sp.getIDList();
            String attStr= sp.getRequired(ServerParams.ATTRIBUTE);
            BackgroundReport.JobAttributes att = Enum.valueOf(BackgroundReport.JobAttributes.class,attStr);
            new SearchServicesImpl().setAttribute(idList, att);
            return "true";
        }
    }

    public static class GetEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            String id = new SrvParam(paramMap).getID();
            return new SearchServicesImpl().getEmail(id);
        }
    }

    public static class ResendEmail extends BaseSearchServerCommand {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            List<String> idList = sp.getIDList();
            String email= sp.getRequired(ServerParams.EMAIL);
            new SearchServicesImpl().resendEmail(idList, email);
            return "true";
        }
    }

    public static class CreateDownloadScript extends BaseSearchServerCommand  {
        @Override
        public String doCommand(Map<String, String[]> paramMap) throws Exception {
            SrvParam sp= new SrvParam(paramMap);
            String id = sp.getID();
            String file = sp.getRequired(ServerParams.FILE);
            String source = sp.getRequired(ServerParams.SOURCE);
            List<String> attStrList = sp.getOptionalList(ServerParams.ATTRIBUTE);
            List<BackgroundReport.ScriptAttributes> attList= new ArrayList<BackgroundReport.ScriptAttributes>(5);
            for(String a : attStrList) {
                attList.add(Enum.valueOf(BackgroundReport.ScriptAttributes.class,a));
            }
            return new SearchServicesImpl().createDownloadScript(id,file,source,attList);
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
