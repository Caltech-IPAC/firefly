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
