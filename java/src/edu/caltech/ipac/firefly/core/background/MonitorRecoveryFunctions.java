package edu.caltech.ipac.firefly.core.background;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
public class MonitorRecoveryFunctions {

    private static final BackgroundMonitor _monitor= Application.getInstance().getBackgroundMonitor();

    private static final SearchServicesAsync _dserv=SearchServices.App.getInstance();
    private static final String DELIM= "-:::-";
    private static final String SUB_DELIM = ":";
    private static final String ACTDELIM= "-";

    private static final int ID_POS = 0;
    private static final int TITLE_POS = 1;
    private static final int WATCH_POS = 2;
    private static final int ATYPE_POS = 3;
    private static final int ASTAT_POS = 4;
    private static final int SUBREP_POS= 5;




    public static String serializeMonitorList(List<MonitorItem> itemList) {
        StringBuilder sb= new StringBuilder(200);

        for(Iterator<MonitorItem> i= itemList.iterator();(i.hasNext());) {
            MonitorItem item= i.next();
            MonitorItem adItem= item instanceof MonitorItem ? (MonitorItem)item : null;
            boolean recreatable= adItem!=null ? !adItem.getStatus().isFail() : true;
            if (recreatable) {
                sb.append(item.getID());
                sb.append(DELIM);
                sb.append(item.getTitle());
                sb.append(DELIM);
                sb.append(item.isWatchable());
                sb.append(DELIM);
                sb.append(adItem != null ? adItem.getBackgroundUIType().toString() : BackgroundUIType.NONE);
                sb.append(DELIM);
                int cnt= item.getStatus().getPackageCount();
                if (adItem!=null) {
                    if (cnt==0) cnt=1;
                    for(int k=0; (k<cnt); k++) {
                        sb.append(adItem.isActivated(k));
                        if (k<cnt-1) sb.append(ACTDELIM);
                    }
                }
                else {
                    sb.append(false);
                }
                if (item.isComposite()) {
                    sb.append(DELIM);
                    for(Iterator<MonitorItem> j= item.getCompositeList().iterator(); (j.hasNext());) {
                        MonitorItem subi= j.next();
                        sb.append(subi.getID());
                        if (j.hasNext()) sb.append(SUB_DELIM);
                    }
                }
                if (i.hasNext()) sb.append(",");
            }
        }
        return sb.toString();
    }



    public static void deserializeAndLoadMonitor(BackgroundMonitor monitor, String serState) {
        String keyAry[]= serState.split(",");
        if (keyAry.length>0) {
            for(String entry : keyAry) {
                String s[]= entry.split(DELIM,6);
                String idAry[]= null;
                if (s.length==6) {
                    idAry= s[SUBREP_POS].split(SUB_DELIM);
                }
                if (s.length==5 || s.length==6) {
                    String actStr[]= s[ASTAT_POS].split(ACTDELIM);
                    boolean act[]= new boolean[actStr.length];
                    for(int m=0; (m<act.length); m++)  act[m]= Boolean.parseBoolean(actStr[m]);
                    try {

                        boolean watch= Boolean.parseBoolean(s[WATCH_POS]);
                        BackgroundUIType atype= Enum.valueOf(BackgroundUIType.class ,s[ATYPE_POS]);
                        if (idAry==null) {
                            String id= s[ID_POS];
                            if (!monitor.isMonitored(id) && !monitor.isDeleted(id)) {
                                MonitorRecoveryFunctions.checkStatusThenMakeNew(s[ID_POS], atype,
                                                                                s[TITLE_POS], watch, act);
                            }
                        }
                        else {
                            boolean check= true;
                            for(String id : idAry)  {
                                if (monitor.isMonitored(id) || monitor.isDeleted(id)) {
                                    check= false;
                                    break;
                                }
                            }
                            if (check) {
                                MonitorRecoveryFunctions.checkGroupStatusThenMakeNew(atype, s[TITLE_POS],
                                                                                     watch, idAry, act);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // do nothing, just ignore and move on
                    }
                }
            }
        }
    }




    public static void checkStatusThenMakeNew(String id,
                                              final BackgroundUIType type,
                                              final String title,
                                              final boolean watchable,
                                              final boolean actAry[]) {


        if (!ActivationFactory.getInstance().isSupported(type)) return;

        _dserv.getStatus(id, new AsyncCallback<BackgroundStatus>() {
            public void onFailure(Throwable caught) { /* do nothing - can't make new */ }
            public void onSuccess(final BackgroundStatus bgStat) {
                if (!bgStat.isFail()) {
                    executeSupportedReport(bgStat,title,watchable, type,actAry);
                }
            }
        });
    }


    private static void checkGroupStatusThenMakeNew(final BackgroundUIType type,
                                                   final String title,
                                                   final boolean watchable,
                                                   final String subIDAry[],
                                                   final boolean actAry[]) {
        final GroupCheck groupCheck= new GroupCheck(subIDAry);
        for(String id : subIDAry) {
            _dserv.getStatus(id, new GroupCall(groupCheck,id,type,title,watchable,subIDAry,actAry) );
        }
    }

    private static void executeSupportedReport(BackgroundStatus bgStat,
                                               String title,
                                               boolean watchable,
                                               BackgroundUIType type,
                                               boolean actAry[]) {
        if (bgStat.getFilePath()!=null && bgStat.getState()==BackgroundState.SUCCESS && watchable) {
            _dserv.getDownloadProgress(bgStat.getFilePath(), new FileKeyProgress(bgStat,title,type));
        }
        else {
            _monitor.addItem(makeItem(title,bgStat,watchable, type, actAry));
        }
    }


    private static class FileKeyProgress implements AsyncCallback<SearchServices.DownloadProgress> {

        private String title;
        private BackgroundStatus bgStat;
        private BackgroundUIType type;

        FileKeyProgress(BackgroundStatus bgStat,
                        String title,
                        BackgroundUIType type ) {
            this.title= title;
            this.bgStat= bgStat;
            this.type= type;

        }

        public void onFailure(Throwable caught) {
            _monitor.addItem(makeItem(title,bgStat,true,type,null));
        }
        public void onSuccess(SearchServices.DownloadProgress progress) {
            if (progress!= SearchServices.DownloadProgress.DONE) {
                _monitor.addItem(makeItem(title,bgStat,true,type, null));
            }
        }
    }

    private static MonitorItem makeItem(String title,
                                            BackgroundStatus bgStat,
                                            boolean watchable,
                                            BackgroundUIType type,
                                            boolean actAry[] ) {
        MonitorItem item;
        if (type== BackgroundUIType.NONE) {
            item= new MonitorItem(title);
        }
        else {
            MonitorItem adItem= new MonitorItem(title,  type,watchable);
            if (actAry!=null) {
                for(int i=0; (i<actAry.length); i++) adItem.setActivated(i,actAry[i]);
            }
            item= adItem;
        }
        item.setStatus(bgStat);
        return item;
    }


    private static class GroupCheck {
        private Map<String, BackgroundStatus> _stat= new HashMap <String, BackgroundStatus>(5);

        public GroupCheck(String ids[]) {
            for(String id : ids) {
                _stat.put(id,null);
            }
        }

        public void markDone(String id, BackgroundStatus bgStat) {
            _stat.put(id,bgStat);
        }

        boolean isAllDone() {
            boolean allDone= true;
            for(BackgroundStatus value : _stat.values()) {
                if (value==null) {
                    allDone= false;
                    break;
                }
            }
            return allDone;
        }

        boolean isSuccess() {
            boolean success= true;
            for(BackgroundStatus report : _stat.values()) {
                if (report==null || report.isFail()) {
                    success= false;
                    break;
                }
            }
            return success;
        }


        Collection<BackgroundStatus> getStatusGroup() {
            return _stat.values();
        }


    }


    private static class GroupCall implements AsyncCallback<BackgroundStatus> {

        private final String _id;
        private final GroupCheck _check;
        private final String _title;
        private final BackgroundUIType _type;
        private String _subIDAry[];
        private boolean _actAry[];
        private boolean _watchable;

        GroupCall(GroupCheck check,
                  String id,
                  BackgroundUIType type,
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
            _check.markDone(_id, BackgroundStatus.createUnknownStat());
        }

        public void onSuccess(final BackgroundStatus bgStat) {
            _check.markDone(_id,bgStat);
            if (_check.isAllDone() && _check.isSuccess()) {
                MonitorItem item= makeItem(_title,null,_watchable,_type,_actAry);
                List<BackgroundStatus> statList= new ArrayList<BackgroundStatus>(_subIDAry.length);
                statList.addAll(_check.getStatusGroup());
                item.initStatusList(statList);
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
