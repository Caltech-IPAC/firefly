package edu.caltech.ipac.voservices.server.tablemapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import edu.caltech.ipac.voservices.server.VOMetadata;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/*
* The instance of this class keeps url reference to the data service,
* specifies VOTable metadata and mapping between the fields, returned by the data service
* and VOTable fields.
* TODO: is it useful to specify coordinate system?
* (Cone search, SIAP and SSA require J2000 for position input and output.)
* position input and output
* Example:
* <GROUP ID="Coo1" utype="stc:AstroCoords" >
* <PARAM ... utype="stc:AstroCoords.coord_sys_id" value="UTC-FK5-TOPO" />
* <PARAM ... utype="stc:AstroCoordSystem.SpaceFrame.CoordRefFrame.Equinox"
*  value="1991.25" />
*   </GROUP>
*  ...
* <FIELD name="RA" ref="Coo1" ....>
* Coordinate system can be applied to all fields with certain utype
*/
@XStreamAlias("TableMapper")
public class TableMapper implements VOMetadata, Serializable {

    public TableMapper() {}

    // xml attribute 'serviceurl'
    @XStreamAsAttribute
    protected String serviceurl;

    // xml attribute 'name'
    @XStreamAsAttribute
    protected String name;
    
    // xml attribute 'desc'
    @XStreamAsAttribute
    protected String desc;


    // list of VoField objects
    // xml element 'VoField+'
    @XStreamImplicit
    protected List<VoField> voFields = new ArrayList<VoField>();

    // list of VoServicePAram objects
    // xml element 'VoServiceParam*
    @XStreamImplicit
    protected List<VoServiceParam> voParams = new ArrayList<VoServiceParam>();


    public String getServiceUrl() { return serviceurl; }

    public String getTableName() { return name; }

    public String getTableDesc() {return desc; }

    public Collection<VoField> getVoFields() { return voFields; }

    public Collection<VoServiceParam> getVoParams() { return voParams; }

    public String getAsString() {
	StringBuffer sb = new StringBuffer();
	sb.append("TableMapper - name=").append(name).append("; desc=").append(desc).append("\n");
    for (VoServiceParam p : voParams) {
        sb.append(p.getAsString());
    }
	for (VoField f : voFields) {
	    sb.append(f.getAsString());
	}
	return sb.toString();
    }
}