package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Oct 28, 2008
 * Time: 11:28:40 AM
 */


/**
 * @author Trey Roby
 */
public class MonitorFunctions {

    private static final BackgroundMonitor _monitor= Application.getInstance().getBackgroundMonitor();

    private static final SearchServicesAsync _dserv=SearchServices.App.getInstance();


    public static void checkStatus(final BackgroundMonitor.Monitor itemMon,
                                   final BackgroundReport report ) {

        SearchServicesAsync dserv=SearchServices.App.getInstance();
        dserv.getStatus(report.getID(), new AsyncCallback<BackgroundReport>() {
            public void onFailure(Throwable caught) {
                BackgroundReport newReport= report.cloneWithState(BackgroundState.FAIL);
                itemMon.updateReport(newReport);
            }

            public void onSuccess(BackgroundReport report) {
                itemMon.updateReport(report);
            }
        });
    }


    public static void checkStatusThenMakeNew(String id,
                                              final ActivationFactory.Type type,
                                              final String title,
                                              final boolean watchable,
                                              final boolean actAry[]) {


        if (!ActivationFactory.getInstance().isSupported(type)) return;

        _dserv.getStatus(id, new AsyncCallback<BackgroundReport>() {
            public void onFailure(Throwable caught) { /* do nothing - can't make new */ }
            public void onSuccess(final BackgroundReport report) {
                if (!report.isFail()) {
                    executeSupportedReport(report,title,watchable, type,actAry);
                }
            }
        });
    }


    public static void checkGroupStatusThenMakeNew(final ActivationFactory.Type type,
                                                   final String title,
                                                   final boolean watchable,
                                                   final String subIDAry[],
                                                   final boolean actAry[]) {
        final GroupCheck groupCheck= new GroupCheck(subIDAry);
        for(String id : subIDAry) {
            _dserv.getStatus(id, new GroupCall(groupCheck,id,type,title,watchable,subIDAry,actAry) );
        }
    }





    public static void cancel(String packageID) {
        _dserv.cancel(packageID, new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(Boolean ignore) { }
        });
    }

    public static void cleanup(String packageID) {
        _dserv.cleanup(packageID, new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(Boolean ignore) { }
        });
    }



    private static void executeSupportedReport(BackgroundReport report,
                                               String title,
                                               boolean watchable,
                                               ActivationFactory.Type type,
                                               boolean actAry[]) {
        if (report.getPartCount()==1 &&  report.get(0).hasFileKey() &&
                report.getPartState(0)==BackgroundState.SUCCESS &&
                watchable) {
            _dserv.getDownloadProgress(report.get(0).getFileKey(),
                                      new FileKeyProgress(report,title,type));
        }
        else {
            _monitor.addItem(makeItem(title,report,watchable, type, actAry));
        }
    }


    public static class FileKeyProgress implements AsyncCallback<SearchServices.DownloadProgress> {

        private String _title;
        private BackgroundReport _report;
        private ActivationFactory.Type _type;

        FileKeyProgress(BackgroundReport report,
                        String title,
                        ActivationFactory.Type type ) {
            _title= title;
            _report= report;
            _type= type;

        }

        public void onFailure(Throwable caught) {
            _monitor.addItem(makeItem(_title,_report,true,_type,null));
        }
        public void onSuccess(SearchServices.DownloadProgress progress) {
            if (progress!= SearchServices.DownloadProgress.DONE) {
                _monitor.addItem(makeItem(_title,_report,true,_type, null));
            }
        }
    }

    private static MonitorItem makeItem(String title,
                                        BackgroundReport report,
                                        boolean watchable,
                                        ActivationFactory.Type type,
                                        boolean actAry[] ) {
        MonitorItem item= new MonitorItem(title,  type,false,watchable);
        item.setRecreated(true);
        if (actAry!=null) {
            for(int i=0; (i<actAry.length); i++) item.setActivated(i,actAry[i]);
        }
        item.setReport(report);
        return item;
    }


    private static class GroupCheck {
        private Map<String, BackgroundReport> _stat= new HashMap <String, BackgroundReport>(5);

        public GroupCheck(String ids[]) {
            for(String id : ids) {
                _stat.put(id,null);
            }
        }

        public void markDone(String id, BackgroundReport rep) {
            _stat.put(id,rep);
        }

        boolean isAllDone() {
            boolean allDone= true;
            for(BackgroundReport value : _stat.values()) {
                if (value==null) {
                    allDone= false;
                    break;
                }
            }
            return allDone;
        }

        boolean isSuccess() {
            boolean success= true;
            for(BackgroundReport report : _stat.values()) {
                if (report==null || report.isFail()) {
                    success= false;
                    break;
                }
            }
            return success;
        }


        Collection<BackgroundReport> getReports() {
            return _stat.values();
        }


    }


    private static class GroupCall implements AsyncCallback<BackgroundReport> {

        private final String _id;
        private final GroupCheck _check;
        private final String _title;
        private final ActivationFactory.Type _type;
        private String _subIDAry[];
        private boolean _actAry[];
        private boolean _watchable;

        GroupCall(GroupCheck check,
                  String id,
                  ActivationFactory.Type type,
                  String title,
                  boolean watchable,
                  String subIDAry[],
                  boolean actAry[]) {
            _id= id;
            _check= check;
            _title= title;
            _watchable= watchable;
            _subIDAry= subIDAry;
            _actAry= actAry;
            _type= type;
        }

        public void onFailure(Throwable caught) {
            _check.markDone(_id, BackgroundReport.createUnknownReport());
        }

        public void onSuccess(final BackgroundReport report) {
            _check.markDone(_id,report);
            if (_check.isAllDone() && _check.isSuccess()) {
                MonitorItem item= new MonitorItem(_title,  _type,false,_watchable);
                item.setRecreated(true);
                if (_actAry!=null) {
                    for(int i=0; (i<_actAry.length); i++) item.setActivated(i,_actAry[i]);
                }

                List<BackgroundReport> repList= new ArrayList<BackgroundReport>(_subIDAry.length);
                repList.addAll(_check.getReports());
                item.initReportList(repList);
                _monitor.addItem(item);
            }
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
