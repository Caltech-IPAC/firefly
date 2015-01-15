/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Nov 9, 2010
*
* @author loi
* @version $Id: BGSearchItem.java,v 1.5 2010/12/03 22:34:21 loi Exp $
*/
public class BGSearchItem extends SearchSummaryItem {
//    public static final String[] HEADERS = new String[]{"Name", "Row(s) Found"};

    private static final String NO_ROW_FOUND = "no_row_found";
    private TableConfig config = null;
    private String filePath;
    private boolean isStarted = false;
    private String countColName;

    public BGSearchItem() {
        this(null);
    }

    public BGSearchItem(TableConfig config) {
        setConfig(config);
        setActivation(new Activation(){
            public void activate(SearchSummaryItem ssi) {
                NewTableResults tr = new NewTableResults(BGSearchItem.this.config, WidgetFactory.TABLE);
                WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));
            }
        });

    }

    @Override
    public boolean canActivate() {
        if (this.isLoaded()) {
            return super.canActivate();
        } else {
            return false;
        }
    }

    public void setCountColName(String countColName) {
        this.countColName = countColName;
        setValue(countColName, "").setHalign(HasHorizontalAlignment.ALIGN_RIGHT);
    }

    public TableConfig getConfig() {
        return config;
    }

    public void setConfig(TableConfig config) {
        this.config = config;
    }

    public static BGSearchItem newInstance(TableServerRequest tsRequest, String title, String desc) {
        return new BGSearchItem(new BaseTableConfig<TableServerRequest>(tsRequest, title, desc));
    }

    public void checkUpdate(){
        if (! isStarted) {
            run();
        }
        checkStatus();
    }


//====================================================================
//
//====================================================================

    private void run() {
        isStarted = true;
        setActivatable(false);
        SearchServices.App.getInstance().submitBackgroundSearch(config.getSearchRequest(), null, 1, new BaseCallback<BackgroundStatus>(){
            @Override
            public void onFailure(Throwable caught) {
                setValue(countColName, "Error!");
                setLoaded(true);
                super.onFailure(caught);
            }

            public void doSuccess(BackgroundStatus result) {
                setValue(countColName, "");
                final BackgroundStatus bgStat = result;
                MonitorItem bgMonitorItem = new MonitorItem(config.getSearchRequest(), config.getTitle(),
                                                            BackgroundUIHint.QUERY);
                bgMonitorItem.setWatchable(false);
                bgMonitorItem.setStatus(bgStat);
                Application.getInstance().getBackgroundMonitor().addItem(bgMonitorItem);
                WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_UPDATE, new WebEventListener<MonitorItem>() {
                    public void eventNotify(WebEvent<MonitorItem> ev) {
                        MonitorItem mi = ev.getData();
                        BackgroundStatus s= mi.getStatus();
                        if (s.getID().equals(bgStat.getID())) {
                            BackgroundState state = s.getState();
                            if (state.equals(BackgroundState.SUCCESS)) {
                                WebEventManager.getAppEvManager().removeListener(this);
                                filePath = s.getFilePath();
                                if (filePath == null) {
                                    filePath = NO_ROW_FOUND;
                                }
                            } else if( state.equals(BackgroundState.CANCELED) ||
                                       state.equals(BackgroundState.FAIL) ||
                                       state.equals(BackgroundState.USER_ABORTED) ) {
                                setValue(countColName, "Error!");
                                setLoaded(true);
                            }
                        }
                    }
                });
            }
        });
    }

    private void checkStatus() {
        if (!StringUtils.isEmpty(filePath)) {
            if (filePath.equals(NO_ROW_FOUND)) {
                setValue(countColName, "0");
                setLoaded(true);
                setActivatable(false);
            } else {
                SearchServices.App.getInstance().getFileStatus(filePath, new BaseCallback<FileStatus>(){
                    @Override
                    public void onFailure(Throwable caught) {
                        setValue(countColName, "Error!");
                        setLoaded(true);
                        super.onFailure(caught);
                    }
                    public void doSuccess(FileStatus result) {
                        setValue(countColName, result.getRowCount()+"");
                        setLoaded(result.getState().equals(FileStatus.State.COMPLETED));
                        setActivatable(result.getRowCount() > 0);
                    }
                });
            }
        }
    }


}

