package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Date: Aug 4, 2010
 *
 * @author loi
 * @version $Id: ActiveTargetCreator.java,v 1.16 2012/09/24 18:31:04 roby Exp $
 */
public class ActiveTargetCreator implements EventWorkerCreator {


    public enum TargetType {TableRow,QueryCenter,TableRowByPlot}
    public enum InputFormat { DECIMAL, HMS, GUESS}

    public EventWorker create(Map<String, String> params) {


        ActiveTargetLayer worker;
        TargetType type= TargetType.QueryCenter;
        InputFormat format= InputFormat.DECIMAL;

        if (params.containsKey(CommonParams.TARGET_TYPE)) {
            String t= params.get(CommonParams.TARGET_TYPE);
            try {
                type= Enum.valueOf(TargetType.class, t);
            } catch (Exception e) {
                type= TargetType.QueryCenter;
            }


        }

        if (type!=TargetType.QueryCenter) {
            String rd= params.get(CommonParams.TARGET_COLUMNS);
            if (rd!=null) {
                String sAry[]= rd.split(",");
                if (sAry.length==2 || sAry.length==3) {
                    String ra= sAry[0];
                    String dec= sAry[1];
                    worker = new ActiveTargetLayer(ra,dec,type);
                }
                else if (sAry.length>=4){
                    List<String> raList= new ArrayList<String>(sAry.length/2);
                    List<String> decList= new ArrayList<String>(sAry.length/2);
                    int max= sAry.length - (sAry.length %2);
                    for(int i= 0; (i<max); i+=2) {
                        raList.add(sAry[i]);
                        decList.add(sAry[i+1]);
                    }
                    worker = new ActiveTargetLayer(raList,decList,type);
                }
                else {
                    worker = new ActiveTargetLayer((String)null,null,type);
                }
            }
            else {
                worker = new ActiveTargetLayer((String)null,null,type);
            }
        }
        else {
            worker = new ActiveTargetLayer();
        }

        if (params.containsKey(CommonParams.INPUT_FORMAT)) {
            String f= params.get(CommonParams.INPUT_FORMAT);
            try {
                format= Enum.valueOf(InputFormat.class, f);
            } catch (Exception e) {
                format= InputFormat.DECIMAL;
            }
        }


        worker.setParams(params);
        worker.setEventsByName(Arrays.asList(TablePreviewEventHub.ON_TABLE_SHOW, TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE));
        worker.setQuerySources(StringUtils.asList(worker.getParam(EventWorker.QUERY_SOURCE), ","));
        worker.setInputFormat(format);
        if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));


        return worker;
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
