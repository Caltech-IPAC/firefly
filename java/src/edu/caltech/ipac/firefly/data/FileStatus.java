package edu.caltech.ipac.firefly.data;

import java.io.Serializable;

/**
 * Date: Jun 17, 2009
 *
 * @author loi
 * @version $Id: FileStatus.java,v 1.3 2012/06/16 00:21:53 loi Exp $
 */
public class FileStatus implements Serializable {
    public static enum State {COMPLETED, INPROGRESS, FAILED}
    private static final String TOKEN_SEP = ":";

    private State state = State.FAILED;
    private int rowCount;

    public FileStatus() {
    }

    public FileStatus(State state, int rowCount) {
        this.state = state;
        setRowCount(rowCount);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount < 0 ? 0 : rowCount;
    }
    
    public static FileStatus parse(String s) {
        String[] parts = s.split(TOKEN_SEP, 2);
        if (parts.length == 2) {
            return new FileStatus(State.valueOf(parts[1]), Integer.parseInt(parts[0]));
        }
        return null;
    }

    @Override
    public String toString() {
        return rowCount + TOKEN_SEP + state.toString();
    }
}
