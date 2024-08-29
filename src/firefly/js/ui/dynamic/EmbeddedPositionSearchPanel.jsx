import {Sheet, Stack, Typography} from '@mui/joy';
import {isFunction} from 'lodash';
import React, {Fragment, useContext, useEffect, useState} from 'react';
import {oneOfType, oneOf, element, bool, string, number, arrayOf, object, func, shape} from 'prop-types';
import CoordinateSys from '../../visualize/CoordSys.js';
import {CONE_AREA_OPTIONS, CONE_AREA_OPTIONS_UPLOAD, CONE_CHOICE_KEY, POLY_CHOICE_KEY, UPLOAD_CHOICE_KEY
} from '../../visualize/ui/CommonUIKeys.js';
import {HiPSTargetView} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {Slot, useFieldGroupRerender, useFieldGroupValue} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../TargetPanel.jsx';
import {CONE_AREA_KEY} from './DynamicDef.js';
import {DEF_AREA_EXAMPLE, PolygonField} from './DynComponents.jsx';

import {UploadTableSelectorPosCol} from 'firefly/ui/UploadTableSelectorPosCol';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {CollapsibleGroup, CollapsibleItem} from 'firefly/ui/panel/CollapsiblePanel';
import {FormPanel} from 'firefly/ui/FormPanel';
import {parseWorldPt} from 'firefly/visualize/Point';
import {formatWorldPtToString} from 'firefly/visualize/ui/WorldPtFormat';
import {getFieldGroupResults} from 'firefly/fieldGroup/FieldGroupUtils';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
/**
 * Create search panel with HiPS Viewer with an embedded target/area selection
 * All properties are optional
 * @param {Object} props
 * @param {String} [props.toolbarHelpId] - help id for the toolbar
 * @param {JSX.Element} [props.WrapperComponent] - A component to wrap all the widgets in the embedded UI
 * @param {String} [props.hipsUrl] - url for the hips url
 * @param [props.hipsFOVInDeg] - field of view of the initial HiPS display
 * @param {WorldPt} [props.initCenterPt] - center point of the initial HiPS display - string - 1.1;2.2;EQ_J2000
 * @param {Array.<{mocUrl:String, mocColor:String, title:String}>} [props.mocList] - a list of MOCS to display, an array of MOC URLs
 * @param {String} [props.sRegion] - an sRegion to display
 * @param {String} [props.coordinateSys] - coordinate system of HiPS - must be 'EQ_J2000' or 'GALACTIC'
 * @param {String} [props.targetKey] - field group key for the target field
 * @param {String} [props.polygonKey] - field group key for the polygon field
 * @param {String} [props.sizeKey] - field group key for the size field
 * @param {String} [props.plotId] - plotId for the HiPS display
 * @param {String} [props.initSelectToggle]
 * @param {number} [props.minValue] - min value for the search area in degrees
 * @param {number} [props.maxValue] - max value for the search area in degrees
 * @param {number} [props.searchAreaInDeg] - default value for the search area in degrees
 * @param {Array.<String>} [props.targetPanelExampleRow1] - string for examples for target for row 1, 3 max in array
 * @param {Array.<String>} [props.targetPanelExampleRow2] - string for examples for target for row 2, 3 max in array
 * @param {Array.<String>} [props.polygonExampleRow1] - string for examples for polygon for row 1, 2 max in array
 * @param {Array.<String>} [props.polygonExampleRow2] - string for examples for polygon for row 2, 2 max in array
 * @param {Boolean} [props.nullAllowed] - null input is allowed
 * @param {Boolean} [props.insetSpacial] - true if spacial layout is inset
 * @param {JSX.Element} [props.otherComponents] - More component that can be added under target/polygon components
 * @param {Boolean} [props.usePosition] - true to use the target
 * @param {Boolean} [props.usePolygon] - true to use the polygon
 * @param {Boolean} [props.useUpload] - true to allow uploads
 * @param {Object} [props.searchItem] - used for URL API (similar to SearchPanel)
 * @param {Object} [props.initArgs] - used for URL API (similar to SearchPanel)
 * @param {function} [props.doSearch] - used for FormPanel's onSuccess search func from collapsed search panel
 * @return {JSX.Element|boolean}
 * @constructor
 */
export function EmbeddedPositionSearchPanel({
                                                toolbarHelpId= undefined,
                                                WrapperComponent,
                                                hipsUrl= 'ivo://CDS/P/DSS2/color',
                                                hipsFOVInDeg = 30,
                                                initCenterPt= undefined,
                                                mocList= undefined,
                                                sRegion= undefined,
                                                coordinateSys: csysStr = 'EQ_J2000',
                                                targetKey= DEF_TARGET_PANEL_KEY,
                                                polygonKey= 'Polygon',
                                                sizeKey= 'radius',
                                                initSelectToggle= CONE_AREA_KEY,
                                                plotId= 'defaultHiPSTargetSearch',
                                                minValue= 1 / 3600,
                                                maxValue= 1,
                                                searchAreaInDeg= .005,
                                                targetPanelExampleRow1,
                                                targetPanelExampleRow2,
                                                polygonExampleRow1=DEF_AREA_EXAMPLE,
                                                polygonExampleRow2,
                                                nullAllowed= false,
                                                insetSpacial=true,
                                                otherComponents= undefined,
                                                usePosition= true,
                                                useUpload = false,
                                                usePolygon= true,
                                                searchItem,
                                                initArgs,
                                                slotProps,
                                                doSearch
                                            }
) {

    const [getConeAreaOp, setConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY);
    const [getUploadInfo, setUploadInfo]= useFieldGroupValue('uploadInfo');
    const uploadInfo= getUploadInfo() || undefined;

    const [isHovered, setIsHovered] = useState(true);
    const [isSearchPanel, setIsSearchPanel] = useState(false);

    //conditionally show UploadTableChooser only when uploadInfo is empty - TAP like behavior
    useEffect(() => {
        if (doGetConeAreaOp() === UPLOAD_CHOICE_KEY) {
            if (!uploadInfo.columns) showUploadTableChooser(setUploadInfo);
            else setUploadInfo(uploadInfo);
        }
    }, [uploadInfo]);

    const coordinateSys = CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000;
    if (!usePolygon && !usePosition && !useUpload) return false;
    const doToggle= usePosition && usePolygon;
    const initToggle= initSelectToggle;


    const doGetConeAreaOp= () => {
        if (doToggle) return getConeAreaOp() ?? initToggle;
        if (usePolygon) return POLY_CHOICE_KEY;
        if (useUpload) return UPLOAD_CHOICE_KEY;
        return CONE_CHOICE_KEY;
    };

    const internals= (
        <Stack spacing={0.5} sx={{pt: insetSpacial ? 0 : 1}} {...slotProps?.searchInnerLayout}>
            {doToggle && <RadioGroupInputField {...{
                sx:{alignSelf: 'center'},
                fieldKey: CONE_AREA_KEY, orientation: 'horizontal',
                tooltip: 'Chose type of search', initialState: {value: initToggle}, options: useUpload ? CONE_AREA_OPTIONS_UPLOAD : CONE_AREA_OPTIONS
            }} />}
            {doGetConeAreaOp() === CONE_CHOICE_KEY &&
                <Stack {...slotProps?.searchTypeCone}>
                    <TargetPanel {...{
                        sx:{width:'34rem'},
                        fieldKey:targetKey, nullAllowed,
                        targetPanelExampleRow1, targetPanelExampleRow2,
                        slotProps: {
                            feedback:{sx: {alignSelf:'center'} },
                        }
                    }}/>
                    <SizeInputFields {...{
                        fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                        // orientation:'horizontal',
                        label: 'Search Radius',
                        initialState: {unit: 'arcsec', value: searchAreaInDeg + '', min:minValue, max:maxValue},
                        sx: {'.ff-Input': {width: 1}},
                        slotProps: {
                            feedback:{sx: {alignSelf:'center'} },
                        }
                    }} />
                </Stack>
            }
            {doGetConeAreaOp() === POLY_CHOICE_KEY &&
                <PolygonField {...{
                    hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey,
                    targetDetails: {targetPanelExampleRow1: polygonExampleRow1, targetPanelExampleRow2:polygonExampleRow2},
                    placeholder: 'Coordinates',
                    manageHiPS:false,
                }} />}
            {doGetConeAreaOp() === UPLOAD_CHOICE_KEY &&
                <Stack pb={0.5}>
                    <UploadTableSelectorPosCol {...{uploadInfo, setUploadInfo,
                        slotProps: {
                            centerColsInnerStack: {sx: {ml: 1, pt: 1.5}}
                        }
                    }}/>
                    <SizeInputFields {...{
                        fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                        // orientation:'horizontal',
                        label: 'Search Radius',
                        initialState: {unit: 'arcsec', value: searchAreaInDeg + '', min:minValue, max:maxValue},
                        sx: {'.ff-Input': {width: 1}, pt:0.5},
                        slotProps: {
                            feedback:{sx: {alignSelf:'center'} },
                        }
                    }} />
                </Stack>}
                {isFunction(otherComponents) ? otherComponents() : otherComponents}
        </Stack>
    );

    const additionalProps= {searchItem, initArgs};
    const wrappedInternals = WrapperComponent
        ? <WrapperComponent {...additionalProps}>{internals}</WrapperComponent>
        : internals;

    return (
        <Stack key='targetGroup' alignItems='center' height='100%' paddingBottom={insetSpacial ? 0 : 20}
           onMouseDown={() => {
               setIsHovered(isSearchPanel);
           }}
           sx={{alignSelf: 'stretch', position: 'relative'}}>
            <HiPSTargetView
                {...{
                hipsUrl, centerPt:initCenterPt, hipsFOVInDeg, mocList, coordinateSys, sRegion, plotId,
                minSize: minValue, maxSize: maxValue, toolbarHelpId,
                whichOverlay: doGetConeAreaOp(), setWhichOverlay: doToggle ? setConeAreaOp : undefined,
                targetKey, sizeKey, polygonKey, sx: {minHeight: 300, alignSelf: 'stretch', flexGrow:1}
            }}/>
            <Sheet
                onMouseEnter={() => {
                    setIsHovered(true);
                    setIsSearchPanel(true);
                }}
                onMouseLeave={() => {
                    setIsSearchPanel(false);
                }}
                {...{className:`FFepsp-content ${insetSpacial ? 'inset' : ''}`,
                    sx: (theme) => (
                        {
                            alignItems: 'center',
                            alignSelf: 'stretch',
                            borderRadius: '5px 5px 2px 2px',
                            border: `3px solid ${theme.vars.palette['neutral']?.softActiveBg}`,
                            position: 'absolute',
                            px: 1/2,
                            bottom: '1.5rem',
                            maxWidth: '50em',
                            left: 3,
                            opacity: isHovered ? '100%' : '40%',
                            ...slotProps?.searchRoot?.sx
                        })
                }}>
                <CollapsibleGroup variant={'plain'}
                    sx={{'& .MuiAccordionSummary-root': {
                            paddingBlockStart: '0.5rem',
                            paddingBlockEnd: '0.5rem',
                            minBlockSize: '1rem',
                            '&.Mui-expanded': {
                                height: '1rem '
                            }
                    }}}>
                    <CollapsibleItem componentKey='embedSearchPanel'
                         slotProps={{
                             header: {sx: { whiteSpace: 'normal',
                                     '& .MuiAccordionSummary-button': {
                                         minBlockSize: '1rem'
                             }}},
                             content: { sx: {
                                 '& .MuiAccordionDetails-content.Mui-expanded': {
                                     padding: 0
                             }}}
                         }}
                         header={(isOpen) => <Header isOpen={isOpen} doSearch={doSearch} targetKey={targetKey} sizeKey={sizeKey} polygonKey={polygonKey}/>} isOpen={true} title='Please select a search type'>
                            {wrappedInternals}
                    </CollapsibleItem>
                </CollapsibleGroup>
            </Sheet>
        </Stack>
    );
}

EmbeddedPositionSearchPanel.propTypes= {
    toolbarHelpId: string,
    WrapperComponent: oneOfType([func,element]),
    hipsUrl: string,
    hipsFOVInDeg: number,
    initCenterPt: object,
    mocList: arrayOf(shape( { mocUrl: string, title: string} )),
    sRegion: string,
    coordinateSys: oneOf(['EQ_J2000','GALACTIC']),
    targetKey: string,
    polygonKey: string,
    sizeKey: string,
    initSelectToggle: string,
    plotId: string,
    minValue: number,
    maxValue: number,
    searchAreaInDeg: number,
    targetPanelExampleRow1: arrayOf(string),
    targetPanelExampleRow2: arrayOf(string),
    polygonExampleRow1: arrayOf(string),
    polygonExampleRow2: arrayOf(string),
    nullAllowed: bool,
    insetSpacial: bool,
    otherComponents: oneOfType([func,element]),
    usePosition: bool,
    usePolygon: bool,
    useUpload: bool,
    searchItem: object,
    initArgs: object,
    doSearch: func,
    slotProps: shape({
        searchRoot: object,
        //following slotProps should be changed when this component is refactored to make it more slots-friendly
        searchInnerLayout: object,
        searchTypeCone: object,
        searchSummary: object
    }),
};

const Header = function({isOpen, doSearch, targetKey, sizeKey, polygonKey}) {
    const {groupKey} = useContext(FieldGroupCtx);
    const reqObj = getFieldGroupResults(groupKey,true);

    useFieldGroupRerender([targetKey,sizeKey,polygonKey,CONE_AREA_KEY]);

    return (
        isOpen ?
            <div/>:
            <Stack p={0}>
                <FormPanel
                    onSuccess={(request) => doSearch(request)}
                    direction={'row'}
                    width={'100%'}
                    slotProps={{
                        searchBar: {p:0, justifyContent: 'right'},
                    }}
                    cancelText=''>
                    <Stack {...{width:'100%', alignItems:'center'}}>
                        <Slot component={SearchSummary} request={reqObj}/>
                    </Stack>
                </FormPanel>
            </Stack>
    );
};


function SearchSummary({request}) {
    const searchType = request?.[CONE_AREA_KEY] === CONE_CHOICE_KEY ? 'Cone' : (request?.[CONE_AREA_KEY] === POLY_CHOICE_KEY  ? 'Polygon' : 'Multi-Object');
    const target = request?.UserTargetWorldPt || request?.circleTarget;
    const userEnterWorldPt= () =>  parseWorldPt(target);
    const coords = searchType === 'Cone' ? formatWorldPtToString(userEnterWorldPt()) : (request?.Polygon || request?.['POS-polygon']);
    const radius = request?.radius || request?.circleSize;

    //in case of Multi-Object, get the fileName & rows
    const fileName = searchType === 'Multi-Object' ? request?.uploadInfo?.fileName : undefined;
    const rows = searchType === 'Multi-Object' ? request?.uploadInfo?.totalRows : undefined;

    const keyVal = (k, v, isLast, key) => (
        <Fragment key={key+''}>
            <Typography component='span' color={'primary'}>{k}: </Typography> {v}
            {!isLast && ', '}
        </Fragment>
    );

    //Label/Key & Value pairs do display, calculating here to determine easily where the last comma should be
    const keyValuePairs = [
        { k: 'Search Type', v: searchType },
        ...(radius && searchType === 'Cone' ? [{ k: 'Search Radius', v: radius }] : []),
        ...(coords && searchType !== 'Multi-Object' ? [{ k: 'Coordinates', v: coords }] : []),
        ...(fileName && rows && searchType === 'Multi-Object' ? [
            { k: 'File Name', v: fileName },
            { k: 'Rows', v: rows }
        ] : [])
    ];

    return (
        <Stack>
            <Typography color={'neutral'} level='body-md'>
                {keyValuePairs.map((pair, index) =>
                    keyVal(pair.k, pair.v, index === keyValuePairs.length - 1, index)
                )}
            </Typography>
        </Stack>
    );
}