import React from 'react';


export const ConstraintContext = React.createContext({});

export function hasAdqlConstraint(constraintObj) {
    return Boolean(
        constraintObj?.adqlConstraint ||
        constraintObj?.adqlConstraintsAry?.length
    );
}

function getAdqlConstraintsList(constraintObj) {
    if (constraintObj?.adqlConstraintsAry?.length) return constraintObj.adqlConstraintsAry;
    if (constraintObj?.adqlConstraint) return [constraintObj.adqlConstraint];
    return [];
}

function isConstraintsValid(constraintObj) {
    return !constraintObj?.constraintErrors?.length;
}

export function isTapUpload(tapBrowserState) {
    const {constraintFragments}= tapBrowserState;
    return [...constraintFragments.values()]
        .some((c) => Boolean(c.uploadFile && c.TAP_UPLOAD && hasAdqlConstraint(c)));
}

/**
 *
 * @param tapBrowserState
 * @returns {ConstraintResult}
 */
export function getUploadConstraint(tapBrowserState) {
    const {constraintFragments}= tapBrowserState;
    return [...constraintFragments.values()]
        .find((c) => Boolean(c.uploadFile && c.TAP_UPLOAD && hasAdqlConstraint(c)));
}

export function getTapUploadSchemaEntry(tapBrowserState) {
    const c= getUploadConstraint(tapBrowserState);
    if (!c) return {};
    return c.TAP_UPLOAD[c.uploadFile];
}

export const getUploadServerFile= (tapBrowserState) => getTapUploadSchemaEntry(tapBrowserState).serverFile;

export const getUploadTableName= (tapBrowserState) => getTapUploadSchemaEntry(tapBrowserState).table;

/**
 *
 * @param {TapBrowserState} tapBrowserState
 * @returns {Object}
 */
export function getHelperConstraints(tapBrowserState) {
    const {constraintFragments}= tapBrowserState;
    const adqlConstraints = [];
    const adqlConstraintErrorsArray = [];

    Array.from(constraintFragments.values()).forEach((constraintObj) => {
        if (isConstraintsValid(constraintObj)) {
            adqlConstraints.push(...getAdqlConstraintsList(constraintObj));
        } else {
            adqlConstraintErrorsArray.push(...constraintObj.constraintErrors);
        }
    });

    return {
        valid: adqlConstraintErrorsArray.length === 0,
        messages: adqlConstraintErrorsArray,
        where: adqlConstraints.join('\n      AND ')
    };
}

export function makeAdqlQueryRangeFragment(lowerBound, upperBound, rangeList, contains = false) {
    const adqlFragmentList = [];
    rangeList.forEach((rangePair) => {
        const [lowerValue, upperValue] = rangePair;
        const query = [];
        if (contains && lowerValue !== '-Inf' && !upperValue.endsWith('Inf')) {
            if (lowerValue === upperValue) {
                query.push(lowerValue, 'BETWEEN', lowerBound, 'AND', upperBound);
            } else if (lowerBound === upperBound) {
                query.push(lowerBound, 'BETWEEN', lowerValue, 'AND', upperValue);
            } else {
                query.push(
                    lowerValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                    'AND',
                    upperValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                );
            }
        } else {
            if (!upperValue.endsWith('Inf')) {
                query.push(lowerBound, '<=', upperValue);
            }
            if (!lowerValue.endsWith('Inf') && !upperValue.endsWith('Inf')) {
                query.push('AND');
            }
            if (!lowerValue.endsWith('Inf')) {
                query.push(lowerValue, '<=', upperBound);
            }
        }
        if (query.length > 1) {
            adqlFragmentList.push('( ' + query.join(' ') + ' )');
        }
    });
    const adqlFragment = adqlFragmentList.join(' OR ');
    return adqlFragmentList.length > 1 ? `( ${adqlFragment} )` : adqlFragment;
}

/*
 * Takes a ranges list and returns a list of sia constraints (query params)
 */
export function siaQueryRange(keyword, rangeList) {
    const siaFragmentList = [];
    rangeList.forEach((rangePair) => {
        const [lowerValue, upperValue] = rangePair;
        if (lowerValue === upperValue) {
            siaFragmentList.push(`${keyword}=${lowerValue}`);
        } else {
            siaFragmentList.push(`${keyword}=${lowerValue} ${upperValue}`);
        }
    });
    return siaFragmentList;
}