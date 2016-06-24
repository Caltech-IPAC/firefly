/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {get} from 'lodash';

import FormPanel from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ServerParams} from '../data/ServerParams.js';

import Validate from '../util/Validate.js';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';

import Enum from 'enum';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {FieldGroupTabs, Tab} from './panel/TabPanel.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SizeInputFields, sizeFromDeg} from './SizeInputField.jsx';
import {InputAreaFieldConnected} from './InputAreaField.jsx';
import {parseWorldPt} from '../visualize/Point.js';
import * as TblUtil from '../tables/TableUtil.js';
import {dispatchAddImages,getAViewFromMultiView} from '../visualize/MultiViewCntlr.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import {dispatchPlotImage} from '../visualize/ImagePlotCntlr.js';
import {FileUpload} from '../ui/FileUpload.jsx';
import {getActiveTarget} from '../core/AppDataCntlr.js';

import './CatalogSearchMethodType.css';
/*
 Component which suppose to handle the catalog method search such as cone, elliptical,etc.
 each of the option has different option panel associated
 */
export class CatalogSearchMethodType extends Component {

    constructor(props) {
        super(props);
        this.state = {
            fields: FieldGroupUtils.getGroupFields(this.props.groupKey)
        };
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
            if (this.iAmMounted) this.setState({fields});
        });
    }

    render() {
        const {fields}= this.state;
        const searchType = get(fields, 'spatial.value', SpatialMethod.Cone.value);
        const max = get(fields, 'conesize.max', 10);
        const {groupKey} = this.props;

        return (
            <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
                {renderTargetPanel(groupKey, searchType)}
                <div
                    style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                    <ListBoxInputField
                        fieldKey='spatial'
                        initialState={{
                                          tooltip: 'Enter a search method',
                                          label : 'Method Search:',
                                          labelWidth: 80,
                                          value:SpatialMethod.Cone.value
                                      }}
                        options={ spatialOptions() }
                        wrapperStyle={{marginRight:'15px', padding:'10px 0 5px 0'}}
                        multiple={false}
                    />
                    {sizeArea(searchType, max)}
                </div>
            </div>
        );

    }


}
/*
 [
 {value: 'Cone', label: 'Cone' },
 {value: 'Elliptical', label: 'Elliptical' },
 {value: 'Box', label: 'Box' },
 {value: 'Polygon', label: 'Polygon' },
 {value: 'Multi-Object', label: 'Multi-Object' },
 {value: 'All-Sky', label: 'All Sky' }
 ]*/
const spatialOptions = () => {
    var l = [];
    SpatialMethod.enums.forEach(function (enumItem) {
            var o = {};
            o.label = enumItem.key;
            o.value = enumItem.value;
            l.push(o);
        }
    );
    return l;
};

/**
 * Return a {SizeInputFields} component by passing in the few paramters needed only.
 * labelwidth = 100 is fixed.
 * @param {string} label by default is 'Radius'
 * @param {string} tooltip by default is the radius size tooltip
 * @param {number} min by default 1 arcsec
 * @param {number} max by default is 1 degree (3600 arcsec)
 * @returns {XML} SizeInputFields component
 */
function radiusInField({label = 'Radius:', tooltip = 'Enter radius of the search', min = 1 / 3600, max = 1}) {
    return (
        <SizeInputFields fieldKey='conesize' showFeedback={true}
                         wrapperStyle={{padding:5, margin: '5px 0 5px 0'}}
                         initialState={{
                                               value:initRadiusArcSec(max),
                                               tooltip: {tooltip},
                                               unit: 'arcsec',
                                               min:  {min},
                                               max:  {max},
                                               labelWidth : 100
                                           }}
                         label={label}/>
    );
}
function sizeArea(searchType, max) {

    if (searchType === SpatialMethod.Cone.value) {
        return (
            <div style={{border: '1px solid #a3aeb9'}}>
                {radiusInField({max})}
            </div>
        );
    } else if (searchType === SpatialMethod.Elliptical.value) {
        return (
            <div
                style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center', border:'solid #a3aeb9 1px' }}>
                {radiusInField({label:'Semi-major Axis:', tooltip:'Enter the semi-major axis of the search'})}
                <ValidationField fieldKey='posangle'
                                 forceReinit={true}
                                 initialState={{
                                          fieldKey: 'posangle',
                                          value: '0',
                                          validator: Validate.floatRange.bind(null, 0, 360, 0,'Position Angle'),
                                          tooltip: 'Enter the Position angle (in deg) of the search, e.g - 52 degrees',
                                          label : 'Position Angle:',
                                          labelWidth : 100
                                      }}/>
                <ValidationField fieldKey='axialratio'
                                 forceReinit={true}
                                 initialState={{
                                          fieldKey: 'axialratio',
                                          value: '.26',
                                          validator: Validate.floatRange.bind(null, 0, 1, 0,'Axial Ratio'),
                                          tooltip: 'Enter the Axial ratio of the search e.g - 0.26',
                                          label : 'Axial Ratio:',
                                          labelWidth : 100
                                      }}/>
            </div>
        );
    } else if (searchType === SpatialMethod.Box.value) {

        return (
            <div style={{border: '1px solid #a3aeb9'}}>
                {radiusInField({label:'Side:', tooltip:'Enter side size of the box search', min:1 / 3600, max:7200 / 3600})}
            </div>

        );
    } else if (searchType === SpatialMethod.get('Multi-object').value) {
        return (
            <div
                style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center', border:'solid #a3aeb9 1px' }}>
                <FileUpload
                    wrapperStyle={{padding:10, margin: '5px 0'}}
                    fieldKey='fileUpload'
                    initialState={{
                        tooltip: 'Select a  file to upload',
                        label: 'Filename:'}}
                />
                {radiusInField({})}
            </div>
        );
    } else if (searchType === SpatialMethod.Polygon.value) {

        return (
            <div
                style={{padding:5, border:'solid #a3aeb9 1px' }}>
                <InputAreaFieldConnected fieldKey='polygoncoords'
                                         wrapperStyle={{padding:5}}
                                         style={{overflow:'auto',height:'65px', maxHeight:'200px', width:'220px', maxWidth:'300px'}}
                                         initialState={{
                                               tooltip:'Enter polygon coordinates search',
                                               labelWidth:70
                                            }}
                                         label='Coordinates:'
                                         tooltip='Enter polygon coordinates search'
                />
                <ul>
                    <li>- Each vertex is defined by a J2000 RA and Dec position pair</li>
                    <li>- A max of 15 and min of 3 vertices is allowed</li>
                    <li>- Vertices must be separated by a comma (,)</li>
                    <li>- Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5</li>
                </ul>
            </div>
        );

    } else {
        return (

            <div style={{border: '1px solid #a3aeb9', padding:'30px 30px'}}>
                Search the catalog with no spacial constraints
            </div>
        );
    }
}

function renderTargetPanel(groupKey, searchType) {
    const visible = searchType === SpatialMethod.Cone.value || searchType === SpatialMethod.Box.value || searchType === SpatialMethod.Elliptical.value;
    return (
        visible && <div className="intarget">
            <TargetPanel wrapperStyle={{width:'200px'}} labelWidth={90} groupKey={groupKey}/>
            <ListBoxInputField
                fieldKey='targettry'
                options={[{label: 'Try NED then Simbad', value: 'NED'},
                                   {label: 'Try Simbad then NED', value: 'simbad'}
                              ]}
                multiple={false}
                label=''
                labelWidth={3}
            />
        </div>
    );


}

CatalogSearchMethodType.propTypes = {
    groupKey: PropTypes.string.isRequired
};

/*CatalogSearchMethodType.contextTypes = {
 groupKey: PropTypes.string
 };*/

// Enumerate spatial methods - see SearchMethod values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
export const SpatialMethod = new Enum({
        'Cone': 'Cone',
        'Elliptical': 'Eliptical',
        'Box': 'Box',
        'Polygon': 'Polygon',
        'Multi-Object': 'Table',
        'All Sky': 'AllSky'
    },
    {ignoreCase: true}
);

var initRadiusArcSec = (max) => {
    if (max >= 10/3600) {
        return parseFloat(10 / 3600).toString();
    } else {
        return parseFloat(1 / 3600).toString();
    }
};