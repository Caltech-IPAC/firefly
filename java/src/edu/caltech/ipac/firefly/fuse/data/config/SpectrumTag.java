package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;

import java.io.Serializable;

/**
 * Date: 2/13/14
 *
 * @author loi
 * @version $Id: $
 */
@XStreamAlias("Spectrum")
public class SpectrumTag implements Serializable {
    @XStreamAsAttribute
    private String name;

    @XStreamAsAttribute
    private String infoProviderId;

    @XStreamAlias("Title")
    private String title;

    @XStreamAlias("Desc")
    private String desc;

    @XStreamAlias("DataSource")
    private DataSourceTag dataSource;

    @XStreamAlias("Form")
    private FormTag form;

    @XStreamAlias("Download")
    private DownloadTag download;

    public String getName() {
        return name;
    }

    public String getInfoProviderId() {
        return infoProviderId;
    }

    public void setInfoProviderId(String infoProviderId) {
        this.infoProviderId = infoProviderId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public DataSourceTag getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceTag dataSource) {
        this.dataSource = dataSource;
    }

    public FormTag getForm() {
        return form;
    }

    public void setForm(FormTag form) {
        this.form = form;
    }

    public DownloadTag getDownload() {
        return download;
    }

    public void setDownload(DownloadTag download) {
        this.download = download;
    }
}
