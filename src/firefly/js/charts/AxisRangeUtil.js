/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Whether a plotly axis is reversed.
 * Plotly keeps the direction as autorange 'reversed' or as the order of the range bounds (range[0] > range[1]).
 * @param {object} axisLayout - plotly layout of the axis
 * @returns {boolean}
 */
export function isAxisReversed(axisLayout) {
    const autorange = axisLayout?.autorange;
    const range = axisLayout?.range || [];
    return (autorange === 'reversed') || (range[1] < range[0]);
}

/**
 * Get range for a plotly axis
 * Plotly requires range to be reversed if the axis is reversed,
 * and limits to be log if axis scale is log
 * @param min - minimum value
 * @param max - maximum value
 * @param isLog - true, if an axis uses log scale
 * @param isReversed - true, if the axis should be reversed
 * @returns {Array<number>} an array for axis range property in plotly layout
 */
export function getRange(min, max, isLog, isReversed) {
    const [r1, r2] = isReversed ? [max, min] : [min, max];
    return isLog ? [Math.log10(r1), Math.log10(r2)] : [r1, r2];
}

/**
 * Evaluate the plotly range changes of one axis from the chart options fields.
 *
 * Everything is decided from what the user asked for in the fields (reverse/log options, bounds, axis column change),
 * with the current axis layout used only for the current range and for whatever the fields don't specify.
 *
 * @param {object} p
 * @param {string} p.a - axis: 'x' or 'y'
 * @param {object} p.fields - chart options fields (fieldKey -> value)
 * @param {object} [p.axisLayout] - current plotly layout of the axis
 * @returns {object} changes for `layout.<a>axis.range` and `layout.<a>axis.autorange`;
 *                   empty when the axis range should be left as is
 */
export function evalAxisRangeChanges({a, fields, axisLayout={}}) {
    const rangeKey = `layout.${a}axis.range`;
    const autorangeKey = `layout.${a}axis.autorange`;

    const hasOpts = `__${a}options` in fields;
    const opts = fields[`__${a}options`] || '';
    const isCurrLog = axisLayout.type === 'log';
    const flip = hasOpts ? opts.includes('flip') : isAxisReversed(axisLayout);
    const isLog = hasOpts ? opts.includes('log') : isCurrLog;
    const scaleChanged = isLog !== isCurrLog;      // log range is in log10 units, so a switch needs a rebuild
    const reset = Boolean(fields[`__${a}reset`]);
    let min = parseFloat(fields[`fireflyLayout.${a}axis.min`]);
    let max = parseFloat(fields[`fireflyLayout.${a}axis.max`]);
    const hasBounds = !Number.isNaN(min) || !Number.isNaN(max);

    const currRange = !reset && axisLayout.range;   // reset: axis column changed, current range doesn't apply
    const autoscale = {[autorangeKey]: flip ? 'reversed' : true, [rangeKey]: undefined};

    if (!hasBounds) {
        if (!hasOpts) return {};                    // caller doesn't manage this axis
        if (!currRange) return autoscale;           // nothing to keep
        if (!scaleChanged) {                        // keep the current range, reordered to the requested direction
            const [r0, r1] = currRange;
            const needsSwap = flip ? r0 < r1 : r1 < r0;
            return needsSwap ? {[rangeKey]: [r1, r0], [autorangeKey]: false} : {[rangeKey]: currRange};
        }
        // scale switch: falls through, both bounds are taken from the current range below
    }

    // fill missing bounds from the current range, in data units
    if (currRange && hasOpts) {
        const [d0, d1] = isCurrLog ? currRange.map((e) => 10 ** e) : currRange;
        if (Number.isNaN(min)) min = Math.min(d0, d1);
        if (Number.isNaN(max)) max = Math.max(d0, d1);
    }

    if (Number.isNaN(min) || Number.isNaN(max)) return autoscale;   // a bound with nothing to fill it from
    if (isLog && (min <= 0 || max <= 0)) return autoscale;    // can't show 0 or below on a log scale
    return {[rangeKey]: getRange(min, max, isLog, flip), [autorangeKey]: false};
}
