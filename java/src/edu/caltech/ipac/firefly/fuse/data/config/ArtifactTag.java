package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * Date: 7/23/14
 *
 * @author loi
 */
@XStreamAlias("Artifact")
public class ArtifactTag implements Serializable {

    @XStreamAlias("ArgCols")
    @XStreamAsAttribute
    protected String argCols;

    @XStreamAlias("ArgHeaders")
    @XStreamAsAttribute
    protected String argHeaders;

    @XStreamAlias("Title")
    @XStreamAsAttribute
    protected String title;

    @XStreamAlias("SYMBOL")
    @XStreamAsAttribute
    protected String symbol;

    @XStreamAlias("COLOR")
    @XStreamAsAttribute
    protected String color;

    @XStreamAlias("DataSource")
    private DataSourceTag dataSource;

    public String getArgCols() {
        return argCols;
    }

    public void setArgCols(String argCols) {
        this.argCols = argCols;
    }

    public String getArgHeaders() {
        return argHeaders;
    }

    public void setArgHeaders(String argHeaders) {
        this.argHeaders = argHeaders;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

}

