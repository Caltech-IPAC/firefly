/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Card, Stack, Typography} from '@mui/joy';
import React, {useContext, useEffect} from 'react';
import {object, bool, number, string, shape} from 'prop-types';
import Enum from 'enum';
import {getAppOptions} from '../core/AppDataCntlr.js';

import {ValidationField} from './ValidationField';
import {TargetPanel} from './TargetPanel';
import {VisualPolygonPanel} from '../visualize/ui/TargetHiPSPanel.jsx';

import {isHiPS} from '../visualize/WebPlot.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import Validate from '../util/Validate.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {UploadOptionsDialog} from './UploadOptionsDialog.jsx';
import {getWorkspaceConfig} from '../visualize/WorkspaceCntlr.js';
import {FieldGroup, FieldGroupCtx} from './FieldGroup.jsx';
import {useFieldGroupValue, useFieldGroupWatch} from './SimpleComponent';

import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getActivePlotView, getFoV} from '../visualize/PlotViewUtil.js';
import {makeImagePt, makeWorldPt, makeScreenPt, makeDevicePt, parseWorldPt} from '../visualize/Point.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {getValueInScreenPixel} from '../visualize/draw/ShapeDataObj.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';

export const CatalogSearchMethodType= ({groupKey, sx, ...rest}) => (
        <FieldGroup groupKey={groupKey} keepState={true} sx={{height:1, ...sx}}>
            <CatalogSearchMethodTypeImpl {...rest}/>
        </FieldGroup>
    );

CatalogSearchMethodType.propTypes = {
    groupKey: string.isRequired,
    polygonDefWhenPlot: bool,
    searchOptionsMask: string,  //string of comma separate options 'Cone,Elliptical,Box,Polygon,Multi-Object,All Sky'
    coneMax: number,
    boxMax: number,
    withPos: bool,
    sx: object
};

/*
 Component which suppose to handle the catalog method search such as cone, elliptical,etc.
 each of the option has different option panel associated
 */
export function CatalogSearchMethodTypeImpl({polygonDefWhenPlot, withPos=true, searchOptionsMask, coneMax, boxMax}) {
    const {getFld,setFld,groupKey} = useContext(FieldGroupCtx);
    const coneSize= useFieldGroupValue('conesize')[0]();
    const spatial= useFieldGroupValue('spatial')[0]() ?? SpatialMethod.Cone.value;
    const imageCornerCalc= useFieldGroupValue('imageCornerCalc')[0]() ?? 'image';

    useEffect(() => {
        const max= spatial===SpatialMethod.Box.value ? boxMax : coneMax;
        if (getFld('conesize')?.max!==max) setFld('conesize', {min:1/3600,max});
    }, [coneSize,spatial]);

    const plot = primePlot(visRoot());
    const polyIsDef= polygonDefWhenPlot && plot;
    const searchType = withPos ? spatial : SpatialMethod['All Sky'].value;

    return (
        <Card sx={{height:1}}>
            <Stack spacing={2}>
                {spatialSelection(withPos, polyIsDef, searchOptionsMask)}
                {renderTargetPanel(groupKey, searchType)}
                <SizeArea {...{groupKey, searchType, imageCornerCalc}}/>
            </Stack>
        </Card>
    );
}


const spatialOptions = (searchTypes) => {
    const l = [];
    SpatialMethod.enums.forEach(function (enumItem) {
            if (!searchTypes || searchTypes.includes(enumItem.value)) {
                l.push({label: enumItem.key, value: enumItem.value});
            }
        }
    );
    return l;
};

const spatialSelection = (withPos, polyIsDef, searchOptionsMask) => {
    const searchOption=
        searchOptionsMask?.trim() ?
            searchOptionsMask.trim().split(',')
                .map( (s) => SpatialMethod.get(s)?.value)
                .filter(Boolean) : undefined;
    const spatialWithPos = (
        <ListBoxInputField
            fieldKey='spatial'
            initialState={{
                              tooltip: 'Enter a search method',
                              label : 'Search Method:',
                              value: polyIsDef ? SpatialMethod.Polygon.value : SpatialMethod.Cone.value
                         }}
            options={ spatialOptions(searchOption) }
            wrapperStyle={{marginRight:'15px', padding:'10px 0 5px 0'}}
            multiple={false}
        />
    );
    const spatialWithoutPos = (
        <Stack {...{py: 1, mr: 2, width: 180, spacing:2, alignItems:'center'}}>
            <Typography >Search Method</Typography>
            <Typography level='body-lg'>All Sky</Typography>
        </Stack>
    );

    return withPos ?  spatialWithPos : spatialWithoutPos;

};

const maxHipsRadiusSearch = 5; //degrees

export function calcCornerString(pv, method) {
    if (method==='clear' || !pv || !hasWCSProjection(pv)) return '';
    const f5 = (v) => v.toFixed(5);

    var pt1, pt2, pt3, pt4;
    const plot = primePlot(pv);
    const sel= plot.attributes[PlotAttribute.SELECTION];
    const cc = CsysConverter.make(plot);
    let w = plot.dataWidth;
    let h = plot.dataHeight;
    const radiusSearch = getFoV(pv) > maxHipsRadiusSearch ? maxHipsRadiusSearch * 3600 : getFoV(pv) * 3600; // in arcsec

    if (method==='image' || (!sel && method==='area-selection') ) {
        pt1 = cc.getWorldCoords(makeImagePt(0,0));
        pt2 = cc.getWorldCoords(makeImagePt(w, 0));
        pt3 = cc.getWorldCoords(makeImagePt(w, h));
        pt4 = cc.getWorldCoords(makeImagePt(0, h));

        if(isHiPS(plot)){
            const centerDevPt= makeDevicePt(plot.viewDim.width/2, plot.viewDim.height/2);
            const centerWp= cc.getWorldCoords( centerDevPt, plot.imageCoordSys);
            const centerPixel = cc.getScreenCoords(centerWp);

            w = h = getValueInScreenPixel(plot, radiusSearch);

            pt1 = cc.getWorldCoords(makeScreenPt(centerPixel.x-w/2, centerPixel.y-h/2));
            pt2 = cc.getWorldCoords(makeScreenPt(centerPixel.x + w/2, centerPixel.y-h/2));
            pt3 = cc.getWorldCoords(makeScreenPt(centerPixel.x + w/2, centerPixel.y + h/2));
            pt4 = cc.getWorldCoords(makeScreenPt(centerPixel.x-w/2, centerPixel.y + h/2));
        }
    }
    else if (method==='viewport') {
        const {viewDim, scrollX, scrollY}= pv;
        const {screenSize}= plot;
        let sx1, sx3, sy1, sy3;
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

        if(isHiPS(plot)){
            const centerDevPt2= makeDevicePt(plot.viewDim.width/2, plot.viewDim.height/2);
            const centerWp2= cc.getWorldCoords( centerDevPt2, plot.imageCoordSys);
            const centerPixel2 = cc.getScreenCoords(centerWp2);
            w = h = getValueInScreenPixel(plot, radiusSearch);

            pt1 = cc.getWorldCoords(makeScreenPt(centerPixel2.x-w/2, centerPixel2.y-h/2));
            pt2 = cc.getWorldCoords(makeScreenPt(centerPixel2.x + w/2, centerPixel2.y-h/2));
            pt3 = cc.getWorldCoords(makeScreenPt(centerPixel2.x + w/2, centerPixel2.y + h/2));
            pt4 = cc.getWorldCoords(makeScreenPt(centerPixel2.x-w/2, centerPixel2.y + h/2));
        }
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
 * @returns {Object} SizeInputFields component
 */
function radiusInField({label = 'Radius'}= {}) {
    return (
        <SizeInputFields fieldKey='conesize' showFeedback={true}
                         initialState={{
                                               unit: 'arcsec',
                                               nullAllowed: false,
                                               value:  (10/3600)+'',
                                               min: 1 / 3600,
                                               max: 100
                                           }}
                         label={label}/>
    );
}

radiusInField.propTypes = {
   label: string
};

function SizeArea({groupKey, searchType, imageCornerCalc}) {
    const {setVal,getVal} = useContext(FieldGroupCtx);

    const onChangeToPolygonMethod = () => {
        const pv = getActivePlotView(visRoot());
        const plot = primePlot(pv);
        if (!plot) return;
        const cornerCalcV = getVal('imageCornerCalc');
        if ((!cornerCalcV || cornerCalcV === 'image' || cornerCalcV === 'viewport' || cornerCalcV === 'area-selection')) {
            const sel = plot.attributes[PlotAttribute.SELECTION] ?? plot.attributes[PlotAttribute.POLYGON_ARY];
            if (!sel && cornerCalcV === 'area-selection') setVal('imageCornerCalc', 'image');
            if (!cornerCalcV) {
                if (sel) setVal('imageCornerCalc', 'area-selection');
            }
            setTimeout( () => setVal('polygoncoords', calcCornerString(pv, cornerCalcV)), 4);
        }
    };

    useFieldGroupWatch(['imageCornerCalc'], () => onChangeToPolygonMethod());

    if (searchType === SpatialMethod.Cone.value) {
        return radiusInField() ;
    } else if (searchType === SpatialMethod.Elliptical.value) {
        return (
            <Stack spacing={1}>
                {radiusInField({label: 'Semi-major Axis:', tooltip: 'Enter the semi-major axis of the search'})}
                <ValidationField fieldKey='posangle'
                                 forceReinit={true}
                                 sx={{width: 0.5}}
                                 initialState={{
                                          fieldKey: 'posangle',
                                          value: '0',
                                          validator: Validate.floatRange.bind(null, 0, 360, 0,'Position Angle'),
                                          tooltip: 'Enter the Position angle (in deg) of the search, e.g - 52 degrees',
                                          label : 'Position Angle',
                                      }}/>
                <ValidationField fieldKey='axialratio'
                                 forceReinit={true}
                                 sx={{width: 0.5}}
                                 initialState={{
                                          fieldKey: 'axialratio',
                                          value: '.26',
                                          validator: Validate.floatRange.bind(null, 0, 1, 0,'Axial Ratio'),
                                          tooltip: 'Enter the Axial ratio of the search e.g - 0.26',
                                          label : 'Axial Ratio',
                                      }}/>
            </Stack>
        );
    } else if (searchType === SpatialMethod.Box.value) {

        return (
            radiusInField({ label: 'Side:' })
        );
    } else if (searchType === SpatialMethod.get('Multi-object').value) {
        const isWs = getWorkspaceConfig();
        return (

            <Stack spacing={1}>
                <UploadOptionsDialog
                    fromGroupKey={groupKey}
                    preloadWsFile={true}
                    fieldKeys={{local: 'fileUpload',
                    workspace: 'workspaceUpload',
                    location: 'fileLocation'}}
                    workspace={isWs}
                    tooltips={{local: 'Select a file to upload',
                        workspace: 'Select a file from workspace to upload'}}
                />
                {radiusInField({})}
            </Stack>
        );
    } else if (searchType === SpatialMethod.Polygon.value) {
        return <PolygonDataArea {...{imageCornerCalc}}/>;
    } else {
        return (
            <Typography level='body-lg' sx={{p:4}}>
                Search the catalog with no spatial constraints
            </Typography>
        );
    }
}

export function PolygonDataArea({imageCornerCalc, initValue='',
                                          hipsUrl= getAppOptions().coverage?.hipsSourceURL  ??  'ivo://CDS/P/2MASS/color',
                                          centerWP, fovDeg=240, showCornerTypeField=true, slotProps }) {
    let cornerTypeOps=
        [
            {label: 'Image', value: 'image'},
            {label: 'Visible', value: 'viewport'},
            {label: 'Custom', value: 'user'}
        ];

    const pv= getActivePlotView(visRoot());
    var plot = primePlot(pv);
    if(isHiPS(plot)){
        cornerTypeOps =
            [
                {label: 'Visible (limit 5 deg)', value: 'image'},
                {label: 'Custom', value: 'user'}
            ];
    }
    if (imageCornerCalc!=='clear' && plot) {
        const sel= plot.attributes[PlotAttribute.SELECTION] ?? plot.attributes[PlotAttribute.POLYGON_ARY];
        if (sel) {
            cornerTypeOps.splice(cornerTypeOps.length-1, 0, {label: 'Selection', value: 'area-selection'});
        }
    }
    const wp= parseWorldPt(centerWP);
    return (
        <Stack {...{spacing:1}}>
            {showCornerTypeField && pv && <RadioGroupInputField
                    orientation='horizontal'
                    tooltip='Choose corners of polygon'
                    label='Search area'
                    initialState= {{value: 'image' }}
                    options={cornerTypeOps}
                    fieldKey='imageCornerCalc'
                    {...slotProps?.cornerType}
                />
            }
            <VisualPolygonPanel {...{
                fieldKey:'polygoncoords',
                hipsDisplayKey:fovDeg,
                hipsUrl,
                hipsFOVInDeg:fovDeg,
                centerPt:wp,
                label:'Coordinates:',
                tooltip:'Enter polygon coordinates search',
                initValue,
                ...slotProps?.polygonPanel
            }} />
            <Typography level='body-sm' component='ul' sx={{pl:1, li: {listStyleType: 'none'}}} {...slotProps?.polygonHelp}>
                <li>- Each vertex is defined by a J2000 RA and Dec position pair</li>
                <li>- A max of 15 and min of 3 vertices is allowed</li>
                <li>- Vertices must be separated by a comma (,)</li>
                <li>- Example: 20.7 21.5, 20.5 20.5, 21.5 20.5, 21.5 21.5</li>
            </Typography>
        </Stack>
    );
}

PolygonDataArea.propTypes = {
    imageCornerCalc: string,
    initValue: string,
    hipsUrl: string,
    centerWP: string,
    fovDeg: number,
    showCornerTypeField: bool,
    slotProps: shape({
        cornerType: object,
        polygonPanel: object,
        polygonHelp: object,
    })
};

function renderTargetPanel(groupKey, searchType) {
    const visible = (searchType === SpatialMethod.Cone.value ||
                     searchType === SpatialMethod.Box.value ||
                     searchType === SpatialMethod.Elliptical.value);
    if (!visible) return ;
    return (
        <Box height={80}>
            <TargetPanel groupKey={groupKey}/>
        </Box>
    );
}


// Enumerate spatial methods - see SearchMethod values in edu.caltech.ipac.firefly.server.catquery.GatorQuery
export const SpatialMethod = new Enum({
        'Cone': 'Cone',
        'Elliptical': 'Ellipse',
        'Box': 'Box',
        'Polygon': 'Polygon',
        'Multi-Object': 'Table',
        'All Sky': 'AllSky'
    },
    {ignoreCase: true}
);

