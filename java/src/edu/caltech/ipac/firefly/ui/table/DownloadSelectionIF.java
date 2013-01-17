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
