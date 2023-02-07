import React, {useContext, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {checkExposureTime} from '../TimeUIUtil.js';
import {ConstraintContext} from './Constraints.js';
import {TimeRangePanel} from './TimeRangePanel.jsx';
import {
    DebugObsCore, getPanelPrefix, LabelWidth, LeftInSearch, makeFieldErrorList,
    makePanelStatusUpdater, Width_Column, makeCollapsibleCheckHeader
} from './TableSearchHelpers.jsx';
import {getColumnAttribute, maybeQuote, tapHelpId} from './TapUtil.js';

const Temporal = 'Temporal';
const TemporalColumns = 'temporalColumns';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const FROM = 0;
const TO = 1;


const title = Temporal;
const panelValue = title;
const panelPrefix = getPanelPrefix(title);


function makeTemporalConstraints(columnsModel, fldObj) {
    let adqlConstraint = '';
    const errList= makeFieldErrorList();
    const {[TimeFrom]:timeFromField, [TimeTo]:timeToField, [TemporalColumns]:temporalColumnsField}= fldObj;

    if (timeFromField?.valid && timeToField?.valid && temporalColumnsField?.valid){
        const timeColumns = temporalColumnsField?.value?.split(',').map( (c) => c.trim()) ?? [];

        const {mjdRange, isoRange}=  checkExposureTime(timeFromField,timeToField);

        if (timeColumns.length === 1) {
            const timeColumn = timeColumns[0];

            // use MJD if time column has a numeric type, ISO otherwise
            const datatype = getColumnAttribute(columnsModel, timeColumn, 'datatype')?.toLowerCase() ?? '';
            // ISO types: char, varchar, timestamp, ?
            const useISO = !['double', 'float', 'real', 'int', 'long', 'short'].some((e) => datatype.includes(e));
            let timeRange = mjdRange;
            if (useISO) {
                // timeRange = mjdRange.map((value) => convertMJDToISO(value));
                timeRange = isoRange;
            }

            const timeConstraints = timeRange.map((oneRange, idx) => {
                if (!oneRange) return '';
                const limit  = useISO ? `'${oneRange}'` : oneRange;
                return idx === FROM ? `${maybeQuote(timeColumn)} >= ${limit}` : `${maybeQuote(timeColumn)} <= ${limit}`;
            });

            if (!timeConstraints[FROM] || !timeConstraints[TO]) {
                adqlConstraint = timeConstraints[FROM] + timeConstraints[TO];
            } else {
                adqlConstraint =`(${timeConstraints[FROM]} AND ${timeConstraints[TO]})`;
            }
        }
    }

    if (!timeFromField.value && !timeToField.value) errList.addError('Time is Required');
    errList.checkForError(timeFromField);
    errList.checkForError(timeToField);
    errList.checkForError(temporalColumnsField);
    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry:[adqlConstraint],
        siaConstraints:[], siaConstraintErrors:[]};
}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(Temporal));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldAry=[TimeTo,TimeFrom,TemporalColumns];

/**
 *
 * @param props
 * @param props.cols
 * @param props.columnsModel
 * @returns {JSX.Element}
 */
export function TemporalSearch({cols, columnsModel}) {
    const [constraintResult, setConstraintResult] = useState({});

    const {setFld,getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    useFieldGroupRerender([...fldAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change
    const timeCol= getVal(TemporalColumns);

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue, 'Error in temporal constraints');

    useFieldGroupWatch([TemporalColumns],
        ([tcVal],isInit) => {
            if (!tcVal) return;
            const timeColumns = tcVal.split(',').map( (c) => c.trim()) ?? [];
            if (timeColumns.length === 1) {
                if (!isInit) checkHeaderCtl.setPanelActive(true);
            }
            else {
                setFld(TemporalColumns, {value:tcVal, valid:false, message: 'you may only choose one column'});
            }
        });

    useEffect(() => {
        const constraints= makeTemporalConstraints(columnsModel, makeFldObj([TimeFrom,TimeTo,TemporalColumns]));
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);


    useEffect(() => {
        setFld(TemporalColumns, {validator: getColValidator(cols, true, false), valid: true});
    }, [columnsModel]);


    const temporalColumns = (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={TemporalColumns} cols={cols}
                           name='temporal column' // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           label='Temporal Column' tooltip={'Column for temporal search'}
                           labelWidth={LabelWidth}
                           validator={getColValidator(cols, true, false)}
                />
            </div>
        );

    const timeColStr= timeCol ? `(${timeCol})` : '';
    const timeRange= (//  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        <TimeRangePanel {...{turnOnPanel: () => checkHeaderCtl.setPanelActive(true),
            panelActive:checkHeaderCtl.isPanelActive(),
            minKey:TimeFrom, maxKey:TimeTo, columnsForTip:[getVal(TemporalColumns)],
            fromTip:`'from' time ${timeColStr}`,
            toTip:`'to' time ${timeColStr}`,
            style:{marginLeft: LeftInSearch, marginTop: 16, marginBottom: 16}}}/>
    );

    return (
        <CollapsibleCheckHeader title={title} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{marginTop: 5}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    {temporalColumns}
                    {timeRange}
                </ForceFieldGroupValid>
            </div>
            <DebugObsCore {...{constraintResult}}/>
        </CollapsibleCheckHeader>
    );
}

TemporalSearch.propTypes = {
    cols: ColsShape,
    columnsModel: PropTypes.object,
};
