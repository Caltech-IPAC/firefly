/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.core.background;

import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.util.AppProperties;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Value object containing information of a Job
 *
 * Date: 9/29/21
 *
 * @author loi
 * @version : $
 */
public class JobInfo implements Serializable {

    public enum PHASE {PENDING, QUEUED, EXECUTING, COMPLETED, ERROR, ABORTED}
    private static final int LIFE_SPAN = AppProperties.getIntProperty("job.lifespan", 60*60*24);        // default lifespan in seconds; kill job if exceed

    private String id;
    private String label;
    private PHASE phase;
    private Error error;
    private List<String> results = new ArrayList<>();
    private Map<String, String> params = new HashMap<>();
    private String owner;
    private Instant startTime;
    private Instant endTime;
    private Instant destructionTime;
    private boolean monitored;
    private int progress;
    private String progressDesc;
    private Job.Type type;
    private String summary;
    private String dataOrigin;

    // not sent to client
    private String eventConnId;


    public JobInfo(String id) {
        this.id = id;
        startTime = Instant.now();
        destructionTime = startTime.plus(7, ChronoUnit.DAYS);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public PHASE getPhase() {
        return phase;
    }

    public void setPhase(PHASE phase) {
        this.phase = phase;
        if (phase == PHASE.COMPLETED) {
            endTime = Instant.now();
            progress = 100;
        }
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        if (error != null) this.phase = PHASE.ERROR;
        this.error = error;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = new ArrayList<>(results);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String,String> params) { this.params = params; }

    public String getOwner() { return owner;}

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Instant getDestructionTime() { return destructionTime; }

    public Job.Type getType() { return type; }

    public void setType(Job.Type type) { this.type = type; }

    public String getEventConnId() { return eventConnId; }

    public void setEventConnId(String eventConnId) { this.eventConnId = eventConnId; }

    public boolean isMonitored() { return monitored;}

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }


    public int getProgress() { return progress; }

    /**
     * a number between 0 and 100 representing the job's percentage of completion.
     * @param progress
     */
    public void setProgress(int progress) { this.progress = Math.min(Math.max(progress, 0), 100); }

    public String getProgressDesc() { return progressDesc; }

    public void setProgressDesc(String progressDesc) { this.progressDesc = progressDesc;}

    public void addResult(String result) { results.add(result);}

    /**
     * @return how long this job may run in seconds.  zero implies unlimited execution duration.
     */
    public long executionDuration() {
        return LIFE_SPAN;
    }

    public String getSummary() { return summary; }

    public void setSummary(String summary) { this.summary = summary;}

    public String getDataOrigin() { return dataOrigin; }

    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin;}

    /**
     * @return a SrvParam from the flatten params map
     */
    public SrvParam getSrvParams() {
        return SrvParam.makeSrvParamSimpleMap(params);
    }

//====================================================================
//
//====================================================================


    public static final class Error implements Serializable {
        private int code;
        private String msg;

        public Error(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() { return code; }
        public String getMsg() { return msg;}
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
