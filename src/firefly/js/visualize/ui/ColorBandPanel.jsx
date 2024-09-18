/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Skeleton, Stack, Typography} from '@mui/joy';
import React, {memo, useState, useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {debounce, get} from 'lodash';
import {sprintf} from '../../externalSource/sprintf';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RangeSlider} from '../../ui/RangeSlider.jsx';
import {callGetColorHistogram} from '../../rpc/PlotServicesJson.js';
import {
    PERCENTAGE,  ABSOLUTE,SIGMA, STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL,
    STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../RangeValues.js';
import {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils.js';
import ImagePlotCntlr, {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {getActivePlotView, isThreeColor} from '../PlotViewUtil.js';
import {makeSerializedRv} from './ColorDialog.jsx';
import {getFluxUnits} from '../WebPlot';
import { getColorModel, makeColorHistImage, makeColorTableImage
} from 'firefly/visualize/rawData/rawAlgorithm/ColorTable.js';
import {useWatcher} from 'firefly/ui/SimpleComponent.jsx';
import {dispatchForceFieldGroupReducer} from 'firefly/fieldGroup/FieldGroupCntlr.js';


const LABEL_WIDTH= 105;
const HIST_WIDTH= 340;
const HIST_HEIGHT= 55;
const maskWrapper= {position:'absolute', left:0, top:0, width:'100%', height:'100%' };
const textPadding= {paddingBottom:3};
const cbarImStyle= {width:'100%', height:10, padding: '0 2px 0 2px', boxSizing:'border-box' };

const histImStyle= {
    width:HIST_WIDTH,
    height:HIST_HEIGHT,
    margin: '2px auto 3px auto',
    boxSizing:'border-box',
    border:'1px solid black',
    display: 'block'
};


export const ColorBandPanel= memo(({fields,plot,band, groupKey}) => {
    const [exit, setExit]= useState(true);
    const [{dataHistogram, dataBinMeanArray, dataBinColorIdx}, setDisplayState]= useState({});
    const [histReadout, setHistReadout]= useState({histValue:0,histMean:0,histIdx:0});
    const {plotId, plotState}= plot ?? {};
    const doMask= false;
    const {current:lastProps} = useRef({ rvStr:'',plotId:'',groupKey:'',band:'' });

    useEffect(() => {
        dispatchForceFieldGroupReducer(groupKey);
    },[]);

    useWatcher([ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW],
        (action) => {
            dispatchForceFieldGroupReducer(groupKey, action);
        }
     );

    useEffect(() => {
        let mounted= true;
        const retFunc= () => void (mounted= false);
        if (plot.plotState.getRangeValues(band).toString()===lastProps.rvStr &&
            lastProps.plotId===plotId && lastProps.groupKey===groupKey && lastProps.band===band ) {
            return retFunc;
        }
        callGetColorHistogram(plotState,band).then(  (result) => {
            const dataHistogram= result.DataHistogram;
            const dataBinMeanArray= result.DataBinMeanArray;
            const dataBinColorIdx= result.DataBinColorIdx;
            lastProps.rvStr= plot.plotState.getRangeValues(band).toString();
            lastProps.plotId= plotId;
            lastProps.groupKey=groupKey;
            lastProps.band= band;
            mounted && setDisplayState({dataHistUrl,cbarUrl,dataHistogram,dataBinMeanArray, dataBinColorIdx});
        });
        return retFunc;
    }, [plotId, plotState, groupKey, band] );

    const mouseMove = (ev) => {
        const {offsetX:x}= ev.nativeEvent;
        if (!dataHistogram || !dataBinMeanArray) return;
        const idx = Math.trunc((x * (dataHistogram.length / HIST_WIDTH)));
        setHistReadout({histIdx: idx, histValue:dataHistogram[idx], histMean:dataBinMeanArray[idx]});
        setExit(false);
    };

    const bandReplot= debounce(getReplotFunc(groupKey, band), 600);

    const ctOrBand= isThreeColor(plot)?band:getColorModel(plot.colorTableId);
    const cbarUrl= makeColorTableImage(ctOrBand, 300,10).toDataURL('image/png');
    const dataHistUrl= dataHistogram && dataBinColorIdx &&
        makeColorHistImage(ctOrBand, Number(plot.colorTableId),
            HIST_WIDTH,HIST_HEIGHT,dataHistogram,dataBinColorIdx).toDataURL('image/png');

    return (
        <Stack {...{spacing:2, minHeight:305, minWidth:360, padding:.5, position:'relative'}}>
            {doMask && <Skeleton sx={{inset:0}}/>}
            <Stack>
                {dataHistUrl && <img style={histImStyle} src={dataHistUrl} key={dataHistUrl}
                                     onMouseMove={mouseMove} onMouseLeave={() => setExit(true)} />}
                <ReadoutPanel
                    width={HIST_WIDTH} exit={exit} idx={histReadout.histIdx} histValue={histReadout.histValue}
                    histMean={histReadout.histMean} plot={plot} band={band}
                />
            </Stack>
            <StretchTypeField/>
            <ZscaleCheckbox/>
            <ColorInput fields={fields} bandReplot={bandReplot}/>
            <Stack {...{spacing:1, direction:'column', alignItems:'center'}}>
                <SuggestedValuesPanel {...{plot,band}}/>
                <img style={cbarImStyle} src={cbarUrl} key={cbarUrl}/>
            </Stack>
       </Stack>
   );
});

ColorBandPanel.propTypes= {
    groupKey : PropTypes.string.isRequired,
    band : PropTypes.object.isRequired,
    plot : PropTypes.object.isRequired,
    fields : PropTypes.object
};

function ColorInput({fields,bandReplot}) {
    if (!fields) return renderStandard();
    const a= Number.parseInt(fields.algorithm?.value);
    const isZ= fields.zscale?.value==='zscale';
    if (a===STRETCH_ASINH)  return renderAsinH(fields, isZ ? renderZscale() : getUpperAndLowerFields(), bandReplot);
    else if (a===STRETCH_POWERLAW_GAMMA) return renderGamma(fields);
    else if (isZ) return renderZscale();
    else return renderStandard();
}

const readTopBaseStyle= { fontSize: '11px', paddingBottom:5, height:16 };
const dataStyle= { color: 'red' };

function SuggestedValuesPanel({plot,band}) {
    const fitsData= plot.webFitsData[band.value];
    const {dataMin, dataMax} = fitsData;
    const dataMaxStr = `Data Max: ${sprintf('%.6f',dataMax)} `;
    const dataMinStr = `Data Min: ${sprintf('%.6f', dataMin)}`;

    return (
        <Stack sx={{alignItems:'center'}}>
            {(dataMin || dataMax) &&
                <Typography level='body-xs'>
                    {dataMinStr}
                    <span style={{paddingLeft:'2rem'}}/>
                    {dataMaxStr}
                </Typography>
            }
        </Stack>
    );
}

function ReadoutPanel({exit, plot,band,idx,histValue,histMean}) {
    const level='body-xs';
    if (exit) {
        return (
            <Stack {...{alignContent:'flex-end'}}>
                <Typography level='body-xs' sx={{textAlign:'right'}}>
                    Move mouse over graph to see values
                </Typography>
            </Stack>
        );
    }
    else {
        return (
            <Stack {...{direction:'row'}}>
                <Typography {...{level}}>Histogram: index:</Typography>
                <Typography {...{level, color:'warning'}}>{idx}</Typography>
                <Typography {...{level, pl:.5}}>Size:</Typography>
                <Typography {...{level, color:'warning'}}>{histValue}</Typography>
                <Typography {...{level, pl:.5}}>Mean Value:</Typography>
                <Typography {...{level, color:'warning'}}>{formatFlux(histMean, plot, band)}</Typography>
            </Stack>
        );
    }
}

ReadoutPanel.propTypes= {
    exit : PropTypes.bool,
    band : PropTypes.object,
    plot : PropTypes.object,
    idx : PropTypes.number,
    histValue :PropTypes.number,
    histMean :PropTypes.number,
    width :PropTypes.number
};

//===============================================================================
//===============================================================================
//================ Helpers
//===============================================================================
//===============================================================================



export function getTypeMinField(lowerWhich='lowerWhich') {
    return (
        <ListBoxInputField fieldKey={lowerWhich}
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Sigma', value: SIGMA}
                                       ]}
                           multiple={false}
        />
    );
}

function getTypeMaxField() {
    return (
        <ListBoxInputField fieldKey='upperWhich'
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Sigma', value: SIGMA}
                                                  ]}
                           multiple={false}
        />
    );
}

export const ZscaleCheckbox= () => (
    <CheckboxGroupInputField
        options={ [ {label: 'Use ZScale for bounds', value: 'zscale'} ] }
        fieldKey='zscale'/>
);


function renderZscale() {
    return (
        <Stack {...{spacing:1}}>
            <ValidationField fieldKey='zscaleContrast' />
            <ValidationField fieldKey='zscaleSamples' />
            <ValidationField fieldKey='zscaleSamplesPerLine' />
        </Stack>
    );
}

function renderStandard() { return  getUpperAndLowerFields(); }

function getUpperAndLowerFields() {
    return (
        <Stack spacing={1}>
            <ValidationField endDecorator={getTypeMinField()}
                             sx={{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }}}
                             fieldKey='lowerRange'
            />
            <ValidationField  endDecorator={getTypeMaxField()}
                              sx={{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }}}
                              fieldKey='upperRange'
            />
        </Stack>
    );
}

const StretchTypeField= () => (
        <div style={{paddingBottom:12}}>
            <ListBoxInputField fieldKey='algorithm'
                               options={ [
                                    {label: 'Linear',                 value: STRETCH_LINEAR},
                                    {label: 'Log',                    value: STRETCH_LOG},
                                    {label: 'Log-Log',                value: STRETCH_LOGLOG},
                                    {label: 'Histogram Equalization', value: STRETCH_EQUAL},
                                    {label: 'Squared',                value: STRETCH_SQUARED },
                                    {label: 'Sqrt',                   value: STRETCH_SQRT},
                                    {label: 'Asinh',                  value: STRETCH_ASINH},
                                    {label: 'Power Law Gamma',        value: STRETCH_POWERLAW_GAMMA}
                                 ]}
            />
        </div>
    );

function renderGamma(fields) {
    const {zscale}= fields;
    const range= (zscale.value==='zscale') ? renderZscale() : getUpperAndLowerFields();
    return (
        <div>
            {range}
            <div style={{paddingTop:10}}/>
            <ValidationField fieldKey='gamma' />
        </div>
    );
}

const asinhSliderMarks = [
    {label: '0', value:0},
    {label: '5', value:5},
    {label: '10', value:10},
    {label: '15', value:15},
    {label: '20', value:20},
];
const ASINH_Q_MAX_SLIDE_VAL = 20;

export function renderAsinH(fields, renderRange, replot, qOnTop=false) {
    const qvalue = fields?.asinhQ?.value || 0;
    const label = `Q: ${Number.parseFloat(qvalue).toFixed(1)} `;

    return (
        <Stack spacing={1}>
            {!qOnTop && renderRange}
            <Typography level='body-sm' sx={{pt:1, textAlign: 'center'}}>
                Q=0 for linear stretch;<br/> increase Q to make brighter features visible
            </Typography>
            <RangeSlider fieldKey='asinhQ'
                         min={0}
                         minStop={0}
                         max={ASINH_Q_MAX_SLIDE_VAL}
                         maxStop={ASINH_Q_MAX_SLIDE_VAL}
                         marks={asinhSliderMarks}
                         step={0.1}
                         slideValue={qvalue}
                         label={label}
                         sx={{mt: 1, mb: 2, mr: 2}}
                         decimalDig={1}
                         onValueChange={replot}
            />
            {qOnTop && renderRange}
        </Stack>
    );
}

function getReplotFunc(groupKey, band) {
    return (val) => {
        validateFieldGroup(groupKey).then((valid) => {
            if (valid) {
                const request = getFieldGroupResults(groupKey);
                const serRv = makeSerializedRv(request);
                const stretchData = [{band: band.key, rv: serRv, bandVisible: true}];
                const pv = getActivePlotView(visRoot());
                if (pv) dispatchStretchChange({plotId: pv.plotId, stretchData});
            }
        });
    };
}

const formatFlux= (value, plot, band) => isNaN(value) ? '' : `${formatFluxValue(value)} ${getFluxUnits(plot,band)}`;

function formatFluxValue(value) {
    const absV= Math.abs(value);
    return (absV>1000||absV<.01) ? value.toExponential(6).replace('e+', 'E') : value.toFixed(6);
}
