package edu.caltech.ipac.firefly.rpc;
/**
 * User: roby
 * Date: 3/12/12
 * Time: 12:03 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.ScriptAttributes;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileStatus;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class SearchServicesJson implements SearchServicesAsync {

    private final boolean doJsonP;

    public SearchServicesJson(boolean doJsonP) {
        this.doJsonP = doJsonP;
    }

    public void getRawDataSet(TableServerRequest request, final AsyncCallback<RawDataSet> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.REQUEST, request.toString()));
        JsonUtils.doService(doJsonP, ServerParams.RAW_DATA_SET, paramList, async, new JsonUtils.Converter<RawDataSet>() {
            public RawDataSet convert(String s) {
                return RawDataSet.parse(s);
            }
        });

    }

    public void getFileStatus(String filePath, AsyncCallback<FileStatus> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.SOURCE, filePath));
        JsonUtils.doService(doJsonP, ServerParams.CHK_FILE_STATUS, paramList, async, new JsonUtils.Converter<FileStatus>() {
            public FileStatus convert(String s) {
                return FileStatus.parse(s);
            }
        });
    }

    public void packageRequest(DownloadRequest dataRequest, AsyncCallback<BackgroundStatus> async) {
    }

    public void submitBackgroundSearch(TableServerRequest request, Request clientRequest, int waitMillis, AsyncCallback<BackgroundStatus> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.REQUEST, request.toString()));
        if (clientRequest!=null) {
            paramList.add(new Param(ServerParams.CLIENT_REQUEST, clientRequest.toString()));
        }
        paramList.add(new Param(ServerParams.WAIT_MILS, waitMillis+""));
        JsonUtils.doService(doJsonP, ServerParams.SUB_BACKGROUND_SEARCH, paramList, async, new JsonUtils.Converter<BackgroundStatus>() {
            public BackgroundStatus convert(String s) {
                return BackgroundStatus.parse(s);
            }
        });

    }

    public void getStatus(String id, AsyncCallback<BackgroundStatus> async) {
        List<Param> paramList = new ArrayList<Param>(1);
        paramList.add(new Param(ServerParams.ID, id));
        JsonUtils.doService(doJsonP, ServerParams.GET_STATUS, paramList, async, new JsonUtils.Converter<BackgroundStatus>() {
            public BackgroundStatus convert(String s) {
                return BackgroundStatus.parse(s);
            }
        });
    }

    public void cancel(String id, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(1);
        paramList.add(new Param(ServerParams.ID, id));
        JsonUtils.doService(doJsonP, ServerParams.CANCEL, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return Boolean.TRUE;
            }
        });
    }

    public void cleanup(String id, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(1);
        paramList.add(new Param(ServerParams.ID, id));
        JsonUtils.doService(doJsonP, ServerParams.CLEAN_UP, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return Boolean.TRUE;
            }
        });
    }

    public void getDownloadProgress(String fileKey, AsyncCallback<SearchServices.DownloadProgress> async) {
        List<Param> paramList = new ArrayList<Param>(1);
        paramList.add(new Param(ServerParams.FILE, fileKey));
        JsonUtils.doService(doJsonP, ServerParams.DOWNLOAD_PROGRESS, paramList, async, new JsonUtils.Converter<SearchServices.DownloadProgress>() {
            public SearchServices.DownloadProgress convert(String s) {
                return Enum.valueOf(SearchServices.DownloadProgress.class,s);
            }
        });
    }

    public void getEnumValues(String filePath, AsyncCallback<RawDataSet> async) throws RPCException {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.SOURCE, filePath));
        JsonUtils.doService(doJsonP, ServerParams.GET_ENUM_VALUES, paramList, async, new JsonUtils.Converter<RawDataSet>() {
            public RawDataSet convert(String s) {
                return RawDataSet.parse(s);
            }
        });
    }

    public void setEmail(String id, String email, AsyncCallback<Boolean> async) {
        setEmail(Arrays.asList(id),email,async);
    }

    public void setEmail(List<String> idList, String email, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(15);
        for (String id : idList) {
            paramList.add(new Param(ServerParams.ID, id));
        }
        paramList.add(new Param(ServerParams.EMAIL, email));
        JsonUtils.doService(doJsonP, ServerParams.SET_EMAIL, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return Boolean.TRUE;
            }
        });
    }

    public void setAttribute(String id, JobAttributes attribute, AsyncCallback<Boolean> async) {
        setAttribute(Arrays.asList(id), attribute, async);
    }

    public void setAttribute(List<String> idList, JobAttributes attribute, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(15);
        for (String id : idList) {
            paramList.add(new Param(ServerParams.ID, id));
        }
        paramList.add(new Param(ServerParams.ATTRIBUTE, attribute.toString()));
        JsonUtils.doService(doJsonP, ServerParams.SET_ATTR, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return Boolean.TRUE;
            }
        });
    }

    public void getEmail(String id, AsyncCallback<String> async) {
        List<Param> paramList = new ArrayList<Param>(1);
        paramList.add(new Param(ServerParams.ID, id));
        JsonUtils.doService(doJsonP, ServerParams.GET_EMAIL, paramList, async, new JsonUtils.Converter<String>() {
            public String convert(String s) { return s; }
        });
    }

    public void resendEmail(List<String> idList, String email, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(15);
        for (String id : idList) {
            paramList.add(new Param(ServerParams.ID, id));
        }
        paramList.add(new Param(ServerParams.EMAIL, email));
        JsonUtils.doService(doJsonP, ServerParams.RESEND_EMAIL, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return Boolean.TRUE;
            }
        });
    }

    public void createDownloadScript(String id,
                                     String fname,
                                     String dataSource,
                                     List<ScriptAttributes> attributes,
                                     AsyncCallback<String> async) {
        List<Param> paramList = new ArrayList<Param>(15);
        paramList.add(new Param(ServerParams.ID, id));
        paramList.add(new Param(ServerParams.FILE, fname));
        paramList.add(new Param(ServerParams.SOURCE, dataSource));
        for (ScriptAttributes att : attributes) {
            paramList.add(new Param(ServerParams.ATTRIBUTE, att.toString()));
        }
        JsonUtils.doService(doJsonP, ServerParams.CREATE_DOWNLOAD_SCRIPT, paramList, async, new JsonUtils.Converter<String>() {
            public String convert(String s) { return s; }
        });
    }

    public void getDataFileValues(String filePath, List<Integer> rows, String colName, AsyncCallback<List<String>> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.SOURCE, filePath));
        paramList.add(new Param(ServerParams.ROWS, CollectionUtil.toString(rows)));
        paramList.add(new Param(ServerParams.COL_NAME, colName));
        JsonUtils.doService(doJsonP, ServerParams.GET_DATA_FILE_VALUES, paramList, async, new JsonUtils.Converter<List<String>>() {
            public List<String> convert(String s) {
                return Arrays.asList(s.split(", "));
            }
        });

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
