/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import {isNil} from 'lodash';

import {InputGroup} from '../../ui/InputGroup.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchValueChange, dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {makeTblRequest,getTblById,getCellValue} from '../../tables/TableUtil.js';

import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {dispatchTableSearch, TABLE_HIGHLIGHT} from '../../tables/TablesCntlr.js';

import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {LC} from '../../templates/lightcurve/LcManager.js';
import {showPhaseFoldingPopup} from './LcPhaseFoldingPopup.jsx';
import {sortInfoString} from '../../tables/SortInfo.js';

import {take} from 'redux-saga/effects';
import './LCPanels.css';

export const grpkey = 'LC_FORM_Panel';

function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Light Curve'} >
                    <LcPhaseFoldingDialog  groupKey={grpkey} />
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('LcPhaseFoldingForm', popup);
        }
        return popup;
    };
}

const dialogBuilder= getDialogBuilder();

// Could be a popup form
export function showLcPhaseFoldingForm() {
    dialogBuilder();
    dispatchShowDialog('LcPhaseFoldingForm');
}

const PanelResizableStyle = {
    width: 400,
    minWidth: 400,
    height: 300,
    minHeight: 300,
    overflow: 'auto',
    padding: '2px',
    position: 'relative'
};

const Header = {
    whiteSpace: 'nowrap',
    height: 'calc(100% - 1px)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'left',
    justifyContent: 'space-between',
    padding: '2px',
    fontSize: 'large'
    };



var LcPhaseFoldingDialog = React.createClass({

    render() {
        return (
            <div style={{padding:'5px', minWidth: 480}}>
                <div>
                    <Tabs componentKey='LCInputTabs' defaultSelected={0} keepState={true} useFlex={true}>
                        <Tab name='Phase Folding'>
                            <LcPhaseFoldingForm />
                        </Tab>
                    </Tabs>
                </div>

            </div>

        );
    }
});


export class LcPhaseFoldingForm extends Component {

    constructor(props)  {
        super(props);
        this.state = {fields:FieldGroupUtils.getGroupFields(grpkey)};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore(grpkey, (fields) => {
            if (fields!==this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields}= this.state;
        // if (!fields) return false;
        return <LcPFOptionsPanell fields={fields} />;
    }

}

export function LcPFOptionsPanel ({fields}) {

    var hide= true;
    if (fields) {
        const {radioGrpFld}= fields;
        hide= (radioGrpFld && radioGrpFld.value==='opt2');
    }
    //Todo: get valication Suggestion from table

    const validSuggestions = ['mjd','col1','col2'];
    //for (var i=1; i<100; i++) { validSuggestions.push(...[`mjd${i}`, `w${i}`, `w${i}mprosig`, `w${i}snr`]); }

    return (

            <FieldGroup style= {PanelResizableStyle} groupKey={grpkey} initValues={{timeCol:LC.DEF_TIME_CNAME}} keepState={true}>
                <InputGroup labelWidth={110}>

                    <span style={Header}>Phase Folding</span>
                    <br/><br/>
                    <SuggestBoxInputField
                        fieldKey='timeCol'
                        initialState= {{
                            fieldKey: 'timeCol',
                            value: '',
                            validator:  (val) => {
                                let retval = {valid: true, message: ''};
                                if (!validSuggestions.includes(val)) {
                                    retval = {valid: false, message: `${val} is not a valid column`};
                                }
                                return retval;
                            },
                            tooltip: 'Start typing and the list of suggestions will appear',
                            label : 'Time Column:',
                            labelWidth : 100
                        }}
                        getSuggestions = {(val)=>{
                            const suggestions = validSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validSuggestions;
                        }}
                    />

                    <br/>
                    <ValidationField fieldKey='flux'
                         initialState= {{
                                fieldKey: 'flux',
                                value: LC.DEF_FLUX_CNAME,
                                tooltip: 'Flux column name',
                                label : 'Flux Column:',
                                labelWidth : 100
                          }} />

                    <br/>

                    <ValidationField fieldKey='period'
                         initialState= {{
                                fieldKey: 'period',
                                value: '1.0',
                                //validator: Validate.floatRange.bind(null, 0.5, 1.5, 3,'period'),
                                tooltip: 'Period',
                                label : 'Period:',
                                labelWidth : 100
                    }} />
                    <br/>

                    <ValidationField fieldKey='cutoutSize'
                        initialState= {{
                               fieldKey: 'cutoutSize',
                               value: '0.3',
                               //validator: Validate.floatRange.bind(null, 0.1, 1, 3,'cutoutsize'),
                               tooltip: 'Cutout Size in degrees',
                               label : 'Cutout Size (deg):',
                               labelWidth : 100
                       }} />


                    <br/> <br/>

                    <button type='button' className='button std hl'  onClick={(request) => onSearchSubmit(request)}>
                        <b>Phase Folded</b>
                    </button>
                    <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                        <b>Reset</b>
                    </button>

                    <br/> <br/>

                   <button type='button' className='button std hl'
                           onClick={() => showPhaseFoldingPopup('Phase Folding')}>Phase Folding Dialog</button>
                </InputGroup>
            </FieldGroup>

    );
}


LcPFOptionsPanel.propTypes= {
    fields: PropTypes.object
};

function resetDefaults() {
    dispatchRestoreDefaults(grpkey);

}

function showResults(success, request) {
    var statStr= `validate state: ${success}`;
    console.log(statStr);
    console.log(request);

    var s= Object.keys(request).reduce(function(buildString,k,idx,array){
        buildString+=`${k}=${request[k]}`;
        if (idx<array.length-1) buildString+=', ';
        return buildString;
    },'');


    var resolver= null;
    var closePromise= new Promise(function(resolve) {
        resolver= resolve;
    });

    var results= (
        <PopupPanel title={'LC Parameters'} closePromise={closePromise} >
            {makeResultInfoContent(statStr,s,resolver)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog('ResultsFromLcPhaseFoldingForm', results);
    dispatchShowDialog('ResultsFromLcPhaseFoldingForm');

}


function makeResultInfoContent(statStr,s,closePromiseClick) {
    return (
        <div style={{padding:'5px'}}>
            <br/>{statStr}<br/><br/>{s}
            <button type='button' onClick={closePromiseClick}>Another Close</button>
            <CompleteButton dialogId='ResultsFromLcPhaseFoldingForm' />
        </div>
    );
}



function resultsFail(request) {
    showResults(false,request);
}

function resultsSuccess(request) {
    showResults(true,request);
}

function onSearchSubmit(request) {
    const fieldState = FieldGroupUtils.getGroupFields(grpkey);
    doPhaseFolding(fieldState);
}

//here to plug in the phase folding processor
function doPhaseFolding(fields) {
    let tbl = getTblById(LC.RAW_TABLE);

    // console.log(fields);

    var tReq = makeTblRequest('PhaseFoldedProcessor', LC.PHASE_FOLDED, {
        period_days: fields.period.value,
        cutout_size: fields.cutoutSize.value,
        flux: fields.flux.value,
        table_name: 'folded_table',
        x:fields.timeCol.value,
        original_table: tbl.tableMeta.tblFilePath
    },  {tbl_id:LC.PHASE_FOLDED, sortInfo:sortInfoString(LC.PHASE_CNAME)});

    dispatchTableSearch(tReq, {removable: false});
    const xyPlotParams = {x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'}, y: {columnOrExpr: tReq.flux, options:'grid,flip'}};
    loadXYPlot({chartId:LC.PHASE_FOLDED, tblId:LC.PHASE_FOLDED, xyPlotParams});
}

export function* listenerPanel() {

    while (true) {
        const action = yield take([
            TABLE_HIGHLIGHT
        ]);

        switch (action.type) {
            case TABLE_HIGHLIGHT:
                handleTableHighlight(action);
                break;
        }
    }
}

function handleTableHighlight(action) {
    const {tbl_id} = action.payload;
    const per = getPeriodFromTable(tbl_id);
    if (per) {
        dispatchValueChange({fieldKey: 'period', groupKey: grpkey, value: per});
    }
}
//export default LcPhaseFoldingForm;

/**
 * gets the period from either peak or periodogram table, these tables doesn't necesary have a period column and same name
 * @param {string} tbl_id
 * @returns {string} period value
 */
function getPeriodFromTable(tbl_id) {
    const tableModel = getTblById(tbl_id);
    if (!tableModel || isNil(tableModel.highlightedRow)) return;
    if (tbl_id === LC.PERIODOGRAM_TABLE) {
        return getCellValue(tableModel, tableModel.highlightedRow, LC.PERIOD_CNAME);
    } else if (tbl_id === LC.PEAK_TABLE) {
        return getCellValue(tableModel, tableModel.highlightedRow, LC.PERIOD_CNAME);
    }
}