/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Aug 31, 2010
 * Time: 10:49:36 AM
 */


/**
 * @author Trey Roby
 * This class is only used on the client
 */
public class CompositeJob {

    private final BackgroundState state;
    private final List<BackgroundStatus> partList;
    private String id;

    public CompositeJob(String id, List<BackgroundStatus> partList) {
        this.state= computeState(partList);
        this.partList= partList;
        this.id= id;
    }

    /**
     * Creates a new report based on this but with the one BackgroundReport changed.
     * @param deltaPart the Background report that changed
     * @return a new Composite report
     */
    public CompositeJob makeDeltaJob(BackgroundStatus deltaPart) {
        List<BackgroundStatus> list= new ArrayList<BackgroundStatus>(partList);
        for(BackgroundStatus part : partList) {
            if (ComparisonUtil.equals(part.getID(), deltaPart.getID())) {
                list.add(deltaPart);
            }
            else {
                list.add(part);
            }
        }
        return new CompositeJob(id, list);
    }

    public String getId() {
        return id;
    }

    public List<BackgroundStatus> getPartList() { return partList; }


    private static BackgroundState computeState(List<BackgroundStatus> reportParts) {
        BackgroundState state= null;
        for(BackgroundStatus s : reportParts) {
            switch (s.getState()) {

                case WAITING:
                    state= s.getState();
                    break;
                case STARTING:
                    if (state==null) state= s.getState();
                    break;
                case WORKING:
                    state= BackgroundState.WAITING;
                    break;
                case SUCCESS:
                    if (state==null) state= s.getState();
                    break;
                case USER_ABORTED:
                case UNKNOWN_PACKAGE_ID:
                case FAIL:
                case CANCELED:
                    state= s.getState();
                    break;
            }


            if (state==BackgroundState.FAIL ||
                state==BackgroundState.CANCELED ||
                state==BackgroundState.UNKNOWN_PACKAGE_ID ||
                state==BackgroundState.USER_ABORTED) {
                break;
            }
        }
        return state;

    }

}

