/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import org.us_vo.www.SimpleResource;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;

import java.util.regex.Pattern;
import java.io.Serializable;


public class NVOResource implements Serializable {

    public enum Coverage {
        MULTI (0, "Multi", null), // More than one CoverageSpectral field
        OPTICAL(1, "Optical", Pattern.compile("^Optical")),
        INFRARED(2, "Infrared", Pattern.compile("^Infrared")),
        RADIO(3, "Radio", Pattern.compile("^Radio")),
        XRAY(4, "X-ray", Pattern.compile("^X-[R,r]ay")),
        UV(5, "Ultraviolet", Pattern.compile("^(Ultraviolet|UV)")),
        OTHER(6, "Other", null);  // missing or unrecognized CoverageSpectral field

        private final int idx;
        private final String desc;
        private final Pattern pattern;
        Coverage(int idx, String desc, Pattern pattern) {
            this.idx = idx;
            this.desc = desc;
            this.pattern=pattern;
        }
        public int getIdx() {return this.idx;}
        public Pattern getPattern() {return this.pattern;}
        public String getDesc() {return this.desc;}
    }
    

    SimpleResource _resource; // simple NVO resource
    Coverage       _coverage; // spectral coverage
    FixedObjectGroup _fixedGroup; // matching images from VOTable

    public NVOResource(SimpleResource resource, Coverage coverage, FixedObjectGroup fixedGroup) {
        _resource = resource;
        _coverage = coverage;
        _fixedGroup = fixedGroup;
    }

    public SimpleResource getResource() { return _resource; }
    public Coverage getCoverage() { return _coverage; }
    public FixedObjectGroup getGroup() { return _fixedGroup; }

}
