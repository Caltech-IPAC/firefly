package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * Date: 2/12/14
 *
 * @author loi
 * @version $Id: $
 */
@XStreamAlias("SpacialTypes")
public class SpacialTypeTag implements Serializable {

    @XStreamAsAttribute
    private String catalog;

    @XStreamAsAttribute
    private String spectrum;

    @XStreamAsAttribute
    private String imageSet;

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSpectrum() {
        return spectrum;
    }

    public void setSpectrum(String spectrum) {
        this.spectrum = spectrum;
    }

    public String getImageSet() {
        return imageSet;
    }

    public void setImageSet(String imageSet) {
        this.imageSet = imageSet;
    }
}
