/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;

/**
 * Date: Jun 11, 2009
 *
 * @author loi
 * @version $Id: DownloadSelectionIF.java,v 1.4 2011/03/18 17:11:55 loi Exp $
 */
public interface DownloadSelectionIF {
    void setDownloadRequest(DownloadRequest downloadRequest);
    void show();
    void setVisible(boolean visible);
    TableDataView getDataView();
    void setDataView(TableDataView view);
    Validator getValidator();
    void setValidator(Validator validator);


    interface Validator {
        boolean validate();
        String getErrorTitle();
        String getErrorMsg();
    }

    public static class MinMaxValidator implements Validator {
        DownloadSelectionIF dlSelector;
        int selMin = 0;
        int selMax = Integer.MAX_VALUE;
        String errMsg;
        String errTitle;
        int selCnt;

        public MinMaxValidator(DownloadSelectionIF dlSelector) {
            this(dlSelector, 1, Integer.MAX_VALUE);
        }

        public MinMaxValidator(DownloadSelectionIF dlSelector, int selMin, int selMax) {
            this.dlSelector = dlSelector;
            this.selMin = selMin;
            this.selMax = selMax;
        }

        public int getSelMin() {
            return selMin;
        }

        public int getSelMax() {
            return selMax;
        }

        public String getErrorTitle() {
            return errTitle;
        }

        public String getErrorMsg() {
            return errMsg;
        }

        public boolean validate() {

            if (dlSelector.getDataView() == null) {
                makeNotInitMsg();
                return false;
            }
            selCnt = dlSelector.getDataView().getSelectionInfo().getSelectedCount();
            if (selCnt < getSelMin()) {
                makeLessThanMinMsg();
                return false;
            } else if (selCnt > getSelMax()) {
                makeMoreThanMaxMsg();
                return false;
            } else {
                setErrTitle("");
                setErrMsg("");
            }

            return true;
        }

        protected void makeNotInitMsg() {
            setErrTitle("Validation Error");
            setErrMsg("Table is empty or is not ready.  Download is not permitted.");
        }

        protected void makeLessThanMinMsg() {
            if (selCnt == 0) {
                setErrTitle("No Data Selected");
                setErrMsg("You have not choosen any data to download.");
            } else {
                setErrTitle("Minimum selected rows not met");
                setErrMsg("You have selected " + selCnt +
                        " row(s).  You must select at least " + getSelMin() + " rows.");
            }
        }

        protected void makeMoreThanMaxMsg() {
            setErrTitle("Maximum selected rows exceeded");
            setErrMsg("You have selected " + selCnt + 
                    " rows.  You are not allow to select more than " + getSelMax() + " rows.");
        }

        protected void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }

        protected void setErrTitle(String errTitle) {
            this.errTitle = errTitle;
        }

    }

}
