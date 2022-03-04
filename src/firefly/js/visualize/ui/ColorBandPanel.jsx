/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useState, useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {debounce, get} from 'lodash';
import {sprintf} from '../../externalSource/sprintf';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RangeSlider} from '../../ui/RangeSlider.jsx';
import {callGetColorHistogram} from '../../rpc/PlotServicesJson.js';
import {encodeServerUrl, getRootURL} from '../../util/WebUtil.js';
import {
    PERCENTAGE,  ABSOLUTE,SIGMA, STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL,
    STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../RangeValues.js';
import {getFieldGroupResults, validateFieldGroup} from '../../fieldGroup/FieldGroupUtils.js';
import ImagePlotCntlr, {dispatchStretchChange, visRoot} from '../ImagePlotCntlr.js';
import {getActivePlotView, isThreeColor} from '../PlotViewUtil.js';
import {makeSerializedRv} from './ColorDialog.jsx';
import {getFluxUnits} from '../WebPlot';
import {SimpleCanvas} from 'firefly/visualize/draw/SimpleCanvas.jsx';
import {
    getColorModel,
    makeColorHistImage,
    makeColorTableImage
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

    const ctOrBand= isThreeColor(plot)?band:getColorModel(plot.plotState.colorTableId);
    const cbarUrl= makeColorTableImage(ctOrBand, 300,10).toDataURL('image/png');
    const dataHistUrl= dataHistogram && dataBinColorIdx &&
        makeColorHistImage(ctOrBand, Number(plot.plotState.colorTableId),
            HIST_WIDTH,HIST_HEIGHT,dataHistogram,dataBinColorIdx).toDataURL('image/png');

    return (
        <div style={{minHeight:305, minWidth:360, padding:5, position:'relative'}}>
            {dataHistUrl && <img style={histImStyle} src={dataHistUrl} key={dataHistUrl}
                 onMouseMove={mouseMove} onMouseLeave={() => setExit(true)} />}
            <ReadoutPanel
                width={HIST_WIDTH} exit={exit} idx={histReadout.histIdx} histValue={histReadout.histValue}
                histMean={histReadout.histMean} plot={plot} band={band}
            />
            <div style={{display:'table', margin:'auto auto'}}>
                {getStretchTypeField()}
            </div>
            <ColorInput fields={fields} bandReplot={bandReplot}/>
            <div style={{position:'absolute', bottom:5, left:5, right:5}}>
                <div>
                    {suggestedValuesPanel( plot,band )}
                </div>
                {getZscaleCheckbox()}
                <img style={cbarImStyle} src={cbarUrl} key={cbarUrl}/>
            </div>
            {doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
       </div>
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

function suggestedValuesPanel( plot,band ) {

    const style= { fontSize: '11px', paddingBottom:5, height:16, whiteSpace: 'pre'};
    const fitsData= plot.webFitsData[band.value];
    const {dataMin, dataMax} = fitsData;
    const dataMaxStr = `Data Max: ${sprintf('%.6f',dataMax)} `;
    const dataMinStr = `Data Min: ${sprintf('%.6f', dataMin)}`;

    return (
        <div style={style}>
                <span style={{float:'left', paddingRight:2, opacity:.5, marginLeft:40 }}>
                  {dataMinStr}            {dataMaxStr}
                </span>
        </div>
    );
}

function ReadoutPanel({exit, plot,band,idx,histValue,histMean,width}) {
    const topStyle= Object.assign({width},readTopBaseStyle);
    if (exit) {
        return (
            <div style={topStyle}>
                <span style={{float:'right', paddingRight:2, opacity:.4, textAlign: 'center' }}>
                Move mouse over graph to see values
                </span>
            </div>
        );
    }
    else {
        return (
            <div style={topStyle}> Histogram: index:
                <span style={dataStyle}>{idx}</span>, Size:
                <span style={dataStyle}>{histValue}</span>, Mean Value :
                <span style={dataStyle}> {formatFlux(histMean, plot, band)} </span>
            </div>
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
        <ListBoxInputField fieldKey={lowerWhich} inline={true} labelWidth={0}
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
        <ListBoxInputField fieldKey='upperWhich' inline={true} labelWidth={0}
                           options={ [ {label: '%', value: PERCENTAGE},
                                       {label: 'Data', value: ABSOLUTE},
                                       {label: 'Sigma', value: SIGMA}
                                                  ]}
                           multiple={false}
        />
    );
}

export function getZscaleCheckbox() {
    return (
        <div style={{display:'table', margin:'auto auto', paddingBottom:5}}>
            <CheckboxGroupInputField
                options={ [ {label: 'Use ZScale for bounds', value: 'zscale'} ] }
                fieldKey='zscale'
                labelWidth={0} />
        </div>
    );
}

function renderZscale() {
    return (
        <div>
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleContrast' />
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleSamples' />
            <ValidationField wrapperStyle={textPadding} labelWidth={LABEL_WIDTH} fieldKey='zscaleSamplesPerLine' />
        </div>
    );
}

function renderStandard() { return  getUpperAndLowerFields(); }

function getUpperAndLowerFields() {
    return (
        <div>
            <div style={{ whiteSpace:'no-wrap'}}>
                <ValidationField wrapperStyle={textPadding} inline={true}
                                 labelWidth={LABEL_WIDTH}
                                 fieldKey='lowerRange'
                />
                {getTypeMinField()}
            </div>
            <div style={{ whiteSpace:'no-wrap'}}>
                <ValidationField  wrapperStyle={textPadding} labelWidth={LABEL_WIDTH}
                                  inline={true}
                                  fieldKey='upperRange'
                />
                {getTypeMaxField()}
            </div>
        </div>
    );
}

function getStretchTypeField() {
    return (
        <div style={{paddingBottom:12}}>
            <ListBoxInputField fieldKey='algorithm' inline={true} labelWidth={67}
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
}

function renderGamma(fields) {
    const {zscale}= fields;
    const range= (zscale.value==='zscale') ? renderZscale() : getUpperAndLowerFields();
    return (
        <div>
            {range}
            <div style={{paddingTop:10}}/>
            <ValidationField wrapperStyle={textPadding}  labelWidth={LABEL_WIDTH} fieldKey='gamma' />
        </div>
    );
}

const asinhSliderMarks = { 0: '0', 5: '5', 10: '10', 15: '15', 20: '20' };
const ASINH_Q_MAX_SLIDE_VAL = 20;

export function renderAsinH(fields, renderRange, replot, wrapperStyle={paddingBottom: 60}, qOnTop=false) {
    const qvalue = get(fields, ['asinhQ', 'value'], Number.NaN);
    const label = `Q: ${Number.parseFloat(qvalue).toFixed(1)} `;

    return (
        <div style={wrapperStyle}>
            {!qOnTop && renderRange}
            <div style={{paddingTop: 5, paddingRight: 15, opacity: .4, textAlign: 'center'}}>
                Q=0 for linear stretch;<br/> increase Q to make brighter features visible
            </div>
            <RangeSlider fieldKey='asinhQ'
                         min={0}
                         minStop={0}
                         max={ASINH_Q_MAX_SLIDE_VAL}
                         maxStop={ASINH_Q_MAX_SLIDE_VAL}
                         marks={asinhSliderMarks}
                         step={0.1}
                         slideValue={qvalue}
                         label={label}
                         labelWidth={60}
                         wrapperStyle={{marginTop: 10, marginBottom: 20, marginRight: 15}}
                         decimalDig={1}
                         onValueChange={replot}
            />
            {qOnTop && renderRange}
        </div>
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
