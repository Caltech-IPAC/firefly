import {Box, Sheet, Stack, Typography} from '@mui/joy';
import {object, shape} from 'prop-types';
import React, {useRef} from 'react';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {validateUrl} from '../../util/Validate.js';
import {dispatchPlotHiPS} from '../ImagePlotCntlr.js';
import {DEFAULT_FITS_VIEWER_ID} from '../MultiViewCntlr.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {getNextHiPSPlotId} from '../PlotViewUtil.js';
import {SpacialContent} from './ImageSearchPanelV2.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getHipsUrl, HiPSImageSelect, makeHiPSWebPlotRequest} from '../../ui/HiPSImageSelect.jsx';
import {FD_KEYS} from './UIConst.js';


export function HiPSSearchPanel({initArgs= {}, name:groupKey = 'HiPSSearchPanel'}) {
    const {current:clickFuncRef} = useRef({clickFunc:undefined});

    return (
        <FieldGroup groupKey={groupKey} keepState={true} sx={{width: 1, height: 1}}>
            <Box width={1} height={1}>
                <FormPanel onSuccess={(request) => doSearch(request)} cancelText='' help_id = 'basics.searching.hips'
                            slotProps={{
                                completeBtn: {
                                    getDoOnClickFunc: (clickFunc) => (clickFuncRef.clickFunc= clickFunc),
                                    requireAllValid:false,
                                    includeUnmounted:true
                                },
                            }}>
                    <Stack width={1} height={1}>
                        <Sheet {...{variant:'outlined', sx:{position:'static', mx:0,py:1,mt:1/2,borderRadius: '5px'}}}>
                            <Stack {...{direction:'row', justifyContent:'flex-start', alignItems:'center', spacing:2}}>
                                <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>Select Image Source</Typography>
                                <RadioGroupInputField
                                    initialState = {{ defaultValue:'archive'}}
                                    options = {[{label: 'Search', value: 'archive'}, {label: 'URL', value: 'url'}]}
                                    tooltip= 'Please select the HiPS source'
                                    orientation='horizontal'
                                    fieldKey = { FD_KEYS.source }/>
                            </Stack>
                        </Sheet>
                        <Sheet className='flex-full' sx={{position:'static',mt:1/2}}>
                            <Sheet {...{position:'static', variant:'outlined', sx:{py:1, flexGrow:0,borderRadius: '5px'}}}>
                                <Stack direction={'row'} >
                                    <Typography {...{px:1, width:200, color:'primary', level:'title-md'}}>
                                        Select Target (optional)
                                    </Typography>
                                    <SpacialContent {...{isHips:true,initArgs}}/>
                                </Stack>
                            </Sheet>
                            <HiPSImageSelect {...{ variant:'plain', datasetTitleText: 'Select Data Set', urlTitleText: 'Enter URL'}} />
                        </Sheet>
                    </Stack>
                </FormPanel>
            </Box>
        </FieldGroup>
    );
}

HiPSSearchPanel.propsTypes= {
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
};



function getHipsValidateInfo(request) {
    const {txURL,imageSource}= request;
    if (imageSource === 'url') {
        if (!txURL?.trim()) return {valid:false, message:'HiPS URL is required'};
        if (!validateUrl('',txURL).valid) return {valid:false, message:'Enter valid HiPS URL'};
    } else if (!getHipsUrl()) {
        return ({valid: false, message: 'No HiPS source selected'});
    }
    return {valid:true, message: 'success'};
}

function doSearch(request) {
    const {valid,message}= getHipsValidateInfo(request);
    if (!valid)  {
        showInfoPopup(message, 'Error');
        return false;
    }
    const plotId = getNextHiPSPlotId();
    const wpRequest= makeHiPSWebPlotRequest(request, plotId);
    dispatchPlotHiPS({ plotId, viewerId: DEFAULT_FITS_VIEWER_ID, wpRequest});
}