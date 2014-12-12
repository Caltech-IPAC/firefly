package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.gui.BaseSaveAction;
import edu.caltech.ipac.gui.ExtensionFilter;
import edu.caltech.ipac.planner.Mode;
import edu.caltech.ipac.planner.PrimaryPlugin;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.Prop;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.target.*;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.*;
import java.util.Iterator;
import java.util.Map;


/**
 * This action will export a list of fixed single targets into a file.
 * If the target is a target group, it will export the list of fixed single
 * targets in the target group, but not the target group itself.
 * <p>
 * The values are separted by spaces.  Values containing space(s) are
 * enclosed by double-quotes.  For attributes, keyword and value are
 * considered to be one element.  If the element contain space(s), then
 * the whole key/value string is enclosed by double-quote.
 */

public class ExportFixedSingleTarget extends BaseSaveAction
                            implements PropertyChangeListener {

    public static final String COMMAND_NAME  = "exportFixedTarget";

    public static final String COORD_SYSTEM = "COORD_SYSTEM";
    public static final String EQUINOX = "EQUINOX";
    public static final String RESOLVER = "NAME-RESOLVER";

    public static final String NED_RESOLVER = "NED";
    public static final String SIMBAD_RESOLVER = "Simbad";
    public static final String ATTRIB_SEP = "=";
    public static final String KWORD_SEP = ":";

    public static final String DEF_COORD_SYS = CoordinateSys.EQUATORIAL_NAME;
    public static final String DEF_EQUINOX = "J2000";
    public static final String DEF_RESOLVER = SIMBAD_RESOLVER;

    private static final String COL_SEP = "    ";
    private static final String DIRECTORY_PROP = "sut.io.lastWorkingDirectory";

    private String _coordSys;
    private String _equinox;
    private String _resolver;

    private TargetList _targets;
    private TargetAttributesHandler _attribHandler;

    public ExportFixedSingleTarget(JFrame f,  TargetList targets) {
        super(COMMAND_NAME, f);
        _targets = targets;
        _targets.addPropertyChangeListener(this);
        setEnabled(false);
    }

    public TargetAttributesHandler getAttributesHandler() {
        return _attribHandler;
    }

    public void setAttributesHandler(TargetAttributesHandler attribHandler) {
        _attribHandler = attribHandler;
    }

    public String getCoordSys() {
        return _coordSys == null ? DEF_COORD_SYS : _coordSys;
    }

    public void setCoordSys(String coordSys) {
        _coordSys = coordSys;
    }

    public String getEquinox() {
        return _equinox == null ? DEF_EQUINOX : _equinox;
    }

    public void setEquinox(String equinox) {
        _equinox = equinox;
    }

    public String getResolver() {
        return _resolver == null ? DEF_RESOLVER : _resolver;
    }

    public void setResolver(String resolver) {
        _resolver = resolver;
    }

    protected void doSave(File f) throws Exception {

        BufferedWriter writer = new BufferedWriter( new FileWriter(f) );
        try {
            writerHeaders(writer);
            writeTargets(writer, (Target[]) _targets.getTargetList().toArray(new Target[0]));
        } finally {
            writer.close();
        }
    }

    protected FileFilter[] getFilterArray() {
        return null;
    }

    protected String getDirProperty() {
        return DIRECTORY_PROP;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (_targets != null && _targets.size() > 0 ) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

//=========================================================================
//  private methods
//=========================================================================

    private String ensureString(String s) {
        if ( s != null && s.contains(" ") ) {
            return "\"" + s + "\"";
        }
        return s == null ? "" : s;
    }

    private void writeTargets(BufferedWriter writer, Target[] targets) throws IOException {

        for (Target trg : targets ) {

            if ( trg instanceof TargetGroup ) {
                writeTargets(writer, ((TargetGroup)trg).getTargets() );
            }
            if ( trg instanceof TargetFixedSingle ) {
                TargetFixedSingle tfs = (TargetFixedSingle) trg;
                String ra="", dec="", pmRa="", pmDec="", epoch="";
                if ( tfs.getPosition() != null ) {
                    PositionJ2000 pos = tfs.getPosition();
                    ra = pos.getUserEnteredPosition().getUserLonStr();
                    dec = pos.getUserEnteredPosition().getUserLatStr();
                    epoch = String.valueOf(pos.getEpoch());
                    if ( pos.getProperMotion() != null ) {
                        pmRa = String.valueOf(pos.getProperMotion().getLonPm());
                        pmDec = String.valueOf(pos.getProperMotion().getLatPm());
                    }
                }
                writer.write( ensureString(tfs.getName()) + COL_SEP + ra + COL_SEP + dec + COL_SEP +
                            pmRa + COL_SEP + pmDec + COL_SEP + epoch + COL_SEP);

                if ( getAttributesHandler() != null ) {
                    writeAttributes(writer, getAttributesHandler().getAttributes(tfs));
                }
                writer.newLine();
            }
        }
    }

    private void writeAttributes(BufferedWriter writer, Map<String, String> attributes) throws IOException {
        if (attributes != null) {
            for(String key : attributes.keySet()) {
                writer.write( ensureString(key + ATTRIB_SEP + attributes.get(key)) + COL_SEP );
            }
        }
    }

    private void writerHeaders(BufferedWriter writer) throws IOException {
        writer.write( COORD_SYSTEM + KWORD_SEP + getCoordSys() );
        writer.newLine();
        writer.write( EQUINOX + KWORD_SEP + getEquinox() );
        writer.newLine();
        writer.write( RESOLVER + KWORD_SEP + getResolver() );
        writer.newLine();

        writer.write("#Name    RA/LON    DEC/LAT    PM-RA    PM-DEC    EPOCH    ATTRIBUTES(*)");
        writer.newLine();
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
