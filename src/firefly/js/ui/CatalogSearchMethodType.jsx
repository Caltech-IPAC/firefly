/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import {ValidationField} from '../ui/ValidationField.jsx';
import {VALUE_CHANGE, dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import {TargetPanel} from '../ui/TargetPanel.jsx';

import {PlotAttribute} from '../visualize/WebPlot.js';
import Validate from '../util/Validate.js';
import Enum from 'enum';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {clone} from '../util/WebUtil.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {InputAreaFieldConnected} from './InputAreaField.jsx';
import {FileUpload} from '../ui/FileUpload.jsx';
import {FieldGroup} from './FieldGroup.jsx';

import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getActivePlotView} from '../visualize/PlotViewUtil.js';
import { makeImagePt, makeWorldPt, makeScreenPt} from '../visualize/Point.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';

import './CatalogSearchMethodType.css';
/*
 Component which suppose to handle the catalog method search such as cone, elliptical,etc.
 each of the option has different option panel associated
 */
export class CatalogSearchMethodType extends PureComponent {

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

    componentWillReceiveProps(nextProps) {
        // const fields= FieldGroupUtils.getGroupFields(this.nextProps.groupKey);
        const {coneMax, boxMax, groupKey}= nextProps;
        if (coneMax && boxMax && groupKey) {
            const fields= FieldGroupUtils.getGroupFields(this.props.groupKey);
            if (this.iAmMounted) this.setState({fields});
            const searchType = get(fields, 'spatial.value', SpatialMethod.Cone.value);
            const max= searchType===SpatialMethod.Box.value ? boxMax : coneMax;

            if (fields.conesize.max!==max) {
                dispatchValueChange({fieldKey:'conesize', groupKey, max, min:1/3600});
            }
        }
    }

    render() {
        const {fields}= this.state;
        const {groupKey, polygonDefWhenPlot}= this.props;

        const polyIsDef= polygonDefWhenPlot && primePlot(visRoot());


        const searchType = get(fields, 'spatial.value', SpatialMethod.Cone.value);

        return (
            <FieldGroup groupKey={groupKey} reducerFunc={searchMethodTypeReducer} keepState={true}
                style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
                {renderTargetPanel(groupKey, searchType)}
                <div
                    style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center' }}>
                    <ListBoxInputField
                        fieldKey='spatial'
                        initialState={{
                                          tooltip: 'Enter a search method',
                                          label : 'Method Search:',
                                          labelWidth: 80,
                                          value: polyIsDef ? SpatialMethod.Polygon.value : SpatialMethod.Cone.value
                                      }}
                        options={ spatialOptions(this.props.searchOption) }
                        wrapperStyle={{marginRight:'15px', padding:'10px 0 5px 0'}}
                        multiple={false}
                    />
                    {sizeArea(searchType, get(fields, 'imageCornerCalc.value', 'image'))}
                </div>
            </FieldGroup>
        );

    }


}
/*
Display the speicified options in case a list of options is given, otherwise display all options.
 [
 {value: 'Cone', label: 'Cone' },
 {value: 'Elliptical', label: 'Elliptical' },
 {value: 'Box', label: 'Box' },
 {value: 'Polygon', label: 'Polygon' },
 {value: 'Multi-Object', label: 'Multi-Object' },
 {value: 'All-Sky', label: 'All Sky' }
 ]*/
const spatialOptions = (searchTypes) => {
    var l = [];
    SpatialMethod.enums.forEach(function (enumItem) {
            if (!searchTypes || searchTypes.includes(enumItem.value)) {
                var o = {};
                o.label = enumItem.key;
                o.value = enumItem.value;
                l.push(o);
            }
        }
    );
    return l;
};



function calcCornerString(pv, method) {
    if (method==='clear' || !pv) return '';
    const f5 = (v) => v.toFixed(5);

    var pt1, pt2, pt3, pt4;
    const plot = primePlot(pv);
    const sel= plot.attributes[PlotAttribute.SELECTION];
    var w = plot.dataWidth;
    var h = plot.dataHeight;
    var cc = CsysConverter.make(plot);
    if (method==='image' || (!sel && method==='area-selection') ) {
        pt1 = cc.getWorldCoords(makeImagePt(0,0));
        pt2 = cc.getWorldCoords(makeImagePt(w, 0));
        pt3 = cc.getWorldCoords(makeImagePt(w, h));
        pt4 = cc.getWorldCoords(makeImagePt(0, h));
    }
    else if (method==='viewport') {
        const {viewDim, scrollX, scrollY}= pv;
        const {screenSize}= plot;
        var sx1, sx3, sy1, sy3;
        if (viewDim.width<screenSize.width) {
            sx1= scrollX;
            sx3= scrollX+ viewDim.width;
        }
        else {
            sx1= 0;
            sx3= screenSize.width;
        }
        if (viewDim.height<screenSize.height) {
            sy1= scrollY;
            sy3= scrollY+ viewDim.height;
        }
        else {
            sy1= 0;
            sy3= screenSize.height;
        }
        pt1= cc.getWorldCoords(makeScreenPt(sx1,sy1));
        pt2= cc.getWorldCoords(makeScreenPt(sx3,sy1));
        pt3= cc.getWorldCoords(makeScreenPt(sx3,sy3));
        pt4= cc.getWorldCoords(makeScreenPt(sx1,sy3));
    }
    else if (method==='area-selection') {
        pt1 = cc.getWorldCoords(sel.pt0);
        pt3 = cc.getWorldCoords(sel.pt1);
        pt2 = makeWorldPt( pt3.x, pt1.y, pt1.cSys );
        pt4 = makeWorldPt( pt1.x, pt3.y, pt1.cSys );
    }
    return (pt1 && pt2 && pt3 && pt4) ?
             `${f5(pt1.x)} ${f5(pt1.y)}, ${f5(pt2.x)} ${f5(pt2.y)}, ${f5(pt3.x)} ${f5(pt3.y)}, ${f5(pt4.x)} ${f5(pt4.y)}` :
             '';
}



/**
 * Return a {SizeInputFields} component by passing in the few paramters needed only.
 * labelwidth = 100 is fixed.
 * @param {Object} p
 * @param {string} p.label by default is 'Radius'
 * @param {string} p.tooltip by default is the radius size tooltip
 * @param {number} p.min by default 1 arcsec
 * @param {number} p.max by default is 1 degree (3600 arcsec)
 * @returns {Object} SizeInputFields component
 */
function radiusInField({label = 'Radius:'}) {
    return (
        <SizeInputFields fieldKey='conesize' showFeedback={true}
                         wrapperStyle={{padding:5, margin: '5px 0 5px 0'}}
                         initialState={{
                                               unit: 'arcsec',
                                               labelWidth : 100
                                           }}
                         label={label}/>
    );
}
function sizeArea(searchType, imageCornerCalc) {

    if (searchType === SpatialMethod.Cone.value) {
        return (
            <div style={{border: '1px solid #a3aeb9'}}>
                {radiusInField({})}
            </div>
        );
    } else if (searchType === SpatialMethod.Elliptical.value) {
        return (
            <div
                style={{padding:5, display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center', border:'solid #a3aeb9 1px' }}>
                {radiusInField({label: 'Semi-major Axis:', tooltip: 'Enter the semi-major axis of the search'})}
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
                {radiusInField({ label: 'Side:' })}
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
        const cornerTypeOps=
            [
                {label: 'Image', value: 'image'},
                {label: 'Visible', value: 'viewport'},
                {label: 'Custom', value: 'user'}
            ];



        const pv= getActivePlotView(visRoot());
        var plot = primePlot(pv);

        if (imageCornerCalc!=='clear' && plot) {
            const sel= plot.attributes[PlotAttribute.SELECTION];
            if (sel) {
                cornerTypeOps.splice(cornerTypeOps.length-1, 0, {label: 'Selection', value: 'area-selection'});
            }
        }
        return (
            <div
                style={{padding:5, border:'solid #a3aeb9 1px' }}>
                <div style={{paddingTop: 10, paddingLeft: 5}}>
                    {pv && <RadioGroupInputField
                        inline={false}
                        labelWidth={60}
                        alignment='horizontal'
                        initialState= {{
                        tooltip: 'Choose how to init corners',
                        label : 'Search area: ',
                        value: 'image'
                    }}
                        options={cornerTypeOps}
                        fieldKey='imageCornerCalc'
                    />
                    }
                </div>
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
                Search the catalog with no spatial constraints
            </div>
        );
    }
}

function renderTargetPanel(groupKey, searchType) {
    const visible = searchType === SpatialMethod.Cone.value || searchType === SpatialMethod.Box.value || searchType === SpatialMethod.Elliptical.value;
    return (
        <div className='intarget'>
            {visible && <TargetPanel labelWidth={100} groupKey={groupKey}/>}
        </div>
    );


}

CatalogSearchMethodType.propTypes = {
    groupKey: PropTypes.string.isRequired,
    polygonDefWhenPlot: PropTypes.bool,
    searchOption: PropTypes.arrayOf(PropTypes.string)
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
    if (max >= 10 / 3600) {
        return parseFloat(10 / 3600).toString();
    } else {
        return parseFloat(1 / 3600).toString();
    }
};



function searchMethodTypeReducer(inFields, action) {
    if (!inFields)  {
        return fieldInit();
    }
    else {
        const {fieldKey}= action.payload;
        const rFields= clone(inFields);
        if (action.type===VALUE_CHANGE && fieldKey==='polygoncoords') {
            rFields.imageCornerCalc= clone(inFields.imageCornerCalc, {value:'user'});
        }
        else {
            const cornerCalcV= get(inFields.imageCornerCalc, 'value', 'user');
            const pv= getActivePlotView(visRoot());


            if (pv && (cornerCalcV==='image' || cornerCalcV==='viewport' || cornerCalcV==='area-selection')) {
                const plot = primePlot(pv);

                if (plot) {
                    const sel = plot.attributes[PlotAttribute.SELECTION];
                    if (!sel && cornerCalcV === 'area-selection') {
                        rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'image'});
                    }
                    const {value:cornerCalcV}= rFields.imageCornerCalc;
                    const v = calcCornerString(pv, cornerCalcV);
                    rFields.polygoncoords = clone(inFields.polygoncoords, {value: v});
                }
            }
        }
        return rFields;
    }
}

function fieldInit() {
    return (
    {
        conesize: {
            fieldKey: 'conesize',
            value: initRadiusArcSec(3600),
            unit: 'arcsec',
            min: 1 / 3600,
            max: 100
        },
        imageCornerCalc: {
            fieldKey: 'imageCornerCalc',
            value: 'image'
        }

    }
    );
}
