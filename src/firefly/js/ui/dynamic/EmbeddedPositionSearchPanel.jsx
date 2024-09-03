import {Box, Sheet, Stack, Typography} from '@mui/joy';
import React, {Fragment, useContext, useEffect, useState} from 'react';
import {oneOfType, oneOf, element, bool, string, number, arrayOf, object, func, shape} from 'prop-types';
import CoordinateSys from '../../visualize/CoordSys.js';
import {CONE_AREA_OPTIONS, CONE_AREA_OPTIONS_UPLOAD, CONE_CHOICE_KEY, POLY_CHOICE_KEY, UPLOAD_CHOICE_KEY
} from '../../visualize/ui/CommonUIKeys.js';
import {HiPSTargetView} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {showInfoPopup} from '../PopupUtil';
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


const DEFAULT_FOV_DEG= 30;
const DEFAULT_HIPS= 'ivo://CDS/P/DSS2/color';
const DEFAULT_PLOT_ID= 'defaultHiPSTargetSearch';
const DEFAULT_TARGET_PANEL_WIDTH= '34rem';
const DEFAULT_SIZE_KEY= 'radius';
const DEFAULT_INIT_SIZE_VALUE= .005;
const DEFAULT_POLYGON_KEY= 'Polygon';





export function EmbeddedPositionSearchPanel({
                                                initSelectToggle= CONE_AREA_KEY,
                                                nullAllowed= false,
                                                insetSpacial=true,
                                                usePosition= true,
                                                useUpload = false,
                                                usePolygon= true,
                                                slotProps={},
                                                doSearch,
                                                children
                                            } ) {

    const {groupKey}= useContext(FieldGroupCtx);
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

    if (!usePolygon && !usePosition && !useUpload) return false;
    const doToggle= usePosition && usePolygon;
    const initToggle= initSelectToggle;


    const doGetConeAreaOp= () => {
        if (doToggle) return getConeAreaOp() ?? initToggle;
        if (usePolygon) return POLY_CHOICE_KEY;
        if (useUpload) return UPLOAD_CHOICE_KEY;
        return CONE_CHOICE_KEY;
    };

    const {targetKey=DEF_TARGET_PANEL_KEY}= slotProps.targetPanel ?? {};
    const {polygonKey=DEFAULT_POLYGON_KEY, }= slotProps.polygonField ?? {};
    const { sizeKey= DEFAULT_SIZE_KEY, min= 1 / 3600, max= 1}= slotProps.sizeInput ?? {};


    const {
        hipsUrl= DEFAULT_HIPS,
        hipsFOVInDeg = DEFAULT_FOV_DEG,
        plotId= DEFAULT_PLOT_ID,
        initCenterPt= undefined,
        mocList= undefined,
        sRegion= undefined,
        toolbarHelpId= undefined,
        coordinateSys : csysStr = 'EQ_J2000',
        sx:hipsTargetViewSx={},
    }= slotProps.hipsTargetView ?? {};

    const defFormPanelProps= slotProps.formPanel ? {
        help_id: 'embeddedDefaultSearchPanelHelp',
        cancelText:'',
        completeText:'Submit', groupKey,
        onError:() => showInfoPopup('Fix errors and search again', 'Error'),
    } : {};


    return (
        <Stack key='targetGroup' alignItems='center' height='100%' paddingBottom={insetSpacial ? 0 : 20}
           onMouseDown={() => {
               setIsHovered(isSearchPanel);
           }}
           sx={{alignSelf: 'stretch', position: 'relative'}}>
            <HiPSTargetView
                {...{
                    hipsUrl, centerPt:initCenterPt, hipsFOVInDeg, mocList,
                    coordinateSys: CoordinateSys.parse(csysStr) ?? CoordinateSys.EQ_J2000,
                    sRegion, plotId,
                    minSize: min, maxSize: max, toolbarHelpId,
                    whichOverlay: doGetConeAreaOp(), setWhichOverlay: doToggle ? setConeAreaOp : undefined,
                    targetKey, sizeKey, polygonKey,
                    sx: {minHeight: 300, alignSelf: 'stretch', flexGrow:1, ...hipsTargetViewSx}
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
                            '&.Mui-expanded': { height: '1rem ' }
                    }}}>
                    <CollapsibleItem {...{
                        componentKey:'embedSearchPanel', isOpen:true, title:'Please select a search type',
                        header: (isOpen) => (<Header {...{isOpen, doSearch, targetKey, sizeKey, polygonKey, slotProps}}/>),
                        slotProps: {
                            header: {
                                sx: {
                                    whiteSpace: 'normal',
                                    '& .MuiAccordionSummary-button': { minBlockSize: '1rem' }
                                },
                                slotProps: {
                                    button: {
                                        component:'div',
                                    }
                                }
                            },
                            content: {
                                sx: { '& .MuiAccordionDetails-content.Mui-expanded': { padding: 0 } }
                            }
                        } }}>
                        <Slot {...{ component: slotProps.formPanel ? FormPanel : Box,
                            slotProps: slotProps.formPanel,
                            ...defFormPanelProps}} >
                            <SpatialSearch {...{slotProps,insetSpacial,uploadInfo, setUploadInfo,
                                coneAreaOp:doGetConeAreaOp(), doToggle,initToggle, nullAllowed, useUpload}}/>
                            {children}
                        </Slot>
                    </CollapsibleItem>
                </CollapsibleGroup>
            </Sheet>
        </Stack>
    );
}


EmbeddedPositionSearchPanel.propTypes= {
    initSelectToggle: string,
    nullAllowed: bool,
    insetSpacial: bool,
    otherComponents: oneOfType([func,element]),
    usePosition: bool,
    usePolygon: bool,
    useUpload: bool,
    doSearch: func,
    slotProps: shape({ // all slotProps are optional except for formPanel.onSuccess
        formPanel : shape({
            onSuccess: func,  // note- onSuccess is required for this panel to function like a FormPanel
            component: element,
            ...FormPanel.props,
        } ),
        searchRoot: shape({
            sx: object,
        }),
        hipsTargetView: shape({
            plotId: string,
            hipsUrl: string,
            mocList: arrayOf(shape( { mocUrl: string, mocColor: string, title: string} )),
            hipsFOVInDeg: number,
            sRegion: string,
            toolbarHelpId: string,
            sx: object,
            initCenterPt: object,
            coordinateSys: oneOf(['EQ_J2000','GALACTIC']),
        }),
        targetPanel: shape({
            targetKey: string,
            targetPanelExampleRow1: arrayOf(string),
            targetPanelExampleRow2: arrayOf(string),
            sx: object,
        }),
        polygonField: shape({
            polygonKey: string,
            polygonExampleRow1: arrayOf(string),
            polygonExampleRow2: arrayOf(string),
            sx: object,
        }),
        sizeInput : shape({
            sizeKey: string,
            min: number,
            max: number,
            initValue: number,
            sx: object,
        }),
        searchSummary: shape({
            component: element,
        }),
        header: object,
        spacialSearch: shape({
            sx: object
        } )
    }),
};

const Header = function({isOpen, slotProps={}, targetKey, sizeKey, polygonKey}) {
    const {groupKey} = useContext(FieldGroupCtx);
    const reqObj = getFieldGroupResults(groupKey,true);

    useFieldGroupRerender([targetKey,sizeKey,polygonKey,CONE_AREA_KEY]);

    return (
        isOpen ?
            <div/>:
            <Stack p={0} width={1}>
                <FormPanel
                    onSuccess={slotProps?.formPanel.onSuccess}
                    direction='row'
                    sx={{width:1}}
                    slotProps={{
                        searchBar: {p:0, justifyContent: 'right'},
                    }}
                    cancelText=''>
                    <Stack {...{width:'100%', alignItems:'center'}}>
                        <Slot {...{component:SearchSummary, slotProps:slotProps.header, request:reqObj}}/>
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

function SpatialSearch({slotProps,insetSpacial,uploadInfo, setUploadInfo,coneAreaOp, doToggle,initToggle, nullAllowed, useUpload}) {

    return (
        <Stack spacing={0.5} sx={{pt: insetSpacial ? 0 : 1, ...slotProps.spacialSearch?.sx}}>
            {doToggle && <RadioGroupInputField {...{
                sx:{alignSelf: 'center'},
                fieldKey: CONE_AREA_KEY, orientation: 'horizontal',
                tooltip: 'Chose type of search', initialState: {value: initToggle},
                options: useUpload ? CONE_AREA_OPTIONS_UPLOAD : CONE_AREA_OPTIONS
            }} />}
            {coneAreaOp === CONE_CHOICE_KEY && <ConeOp {...{slotProps,nullAllowed}}/> }
            {coneAreaOp === POLY_CHOICE_KEY && <PolyOp {...{slotProps}}/> }
            {coneAreaOp === UPLOAD_CHOICE_KEY && <UploadOp {...{slotProps,uploadInfo,setUploadInfo}}/>}
        </Stack>
    );
}


function ConeOp({slotProps,nullAllowed}) {
    const {
        sizeKey= DEFAULT_SIZE_KEY,
        min= 1 / 3600,
        max= 1,
        initValue= DEFAULT_INIT_SIZE_VALUE
    }= slotProps.sizeInput ?? {};
    const {
        targetKey=DEF_TARGET_PANEL_KEY,
        targetPanelExampleRow1,
        targetPanelExampleRow2
    }= slotProps.targetPanel ?? {};
    return (
        <Stack>
            <TargetPanel {...{
                sx:{width:DEFAULT_TARGET_PANEL_WIDTH, ...slotProps.targetPanel?.sx},
                fieldKey:targetKey, nullAllowed,
                targetPanelExampleRow1, targetPanelExampleRow2,
                slotProps: {
                    feedback:{sx: {alignSelf:'center'} },
                }
            }}/>
            <SizeInputFields {...{
                fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                label: 'Search Radius',
                initialState: {unit: 'arcsec', value: initValue+'', min, max},
                sx: {'.ff-Input': {width: 1}},
                slotProps: {
                    feedback:{sx: {alignSelf:'center'} },
                }
            }} />
        </Stack>
    );
}

function PolyOp({slotProps}) {
    const {
        polygonKey=DEFAULT_POLYGON_KEY,
        polygonExampleRow1= DEF_AREA_EXAMPLE,
        polygonExampleRow2
    }= slotProps.polygonField ?? {};
    return (
        <PolygonField {...{
            hideHiPSPopupPanelOnDismount: false, fieldKey: polygonKey,
            targetDetails: {targetPanelExampleRow1: polygonExampleRow1, targetPanelExampleRow2:polygonExampleRow2},
            placeholder: 'Coordinates',
            manageHiPS:false,
        }} />
    );
}

function UploadOp({slotProps, uploadInfo, setUploadInfo}) {
    const {
        sizeKey= DEFAULT_SIZE_KEY,
        min= 1 / 3600,
        max= 1,
        initValue= DEFAULT_INIT_SIZE_VALUE
    }= slotProps.sizeInput ?? {};

    return (
        <Stack pb={0.5}>
            <UploadTableSelectorPosCol {...{uploadInfo, setUploadInfo,
                slotProps: {
                    centerColsInnerStack: {sx: {ml: 1, pt: 1.5}}
                }
            }}/>
            <SizeInputFields {...{
                fieldKey: sizeKey, showFeedback: true, labelWidth: 100, nullAllowed: false,
                label: 'Search Radius',
                initialState: {unit: 'arcsec', value: initValue+'', min, max},
                sx: {'.ff-Input': {width: 1}, pt:0.5},
                slotProps: {
                    feedback:{sx: {alignSelf:'center'} },
                }
            }} />
        </Stack>

        );

}