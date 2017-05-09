import PropTypes from 'prop-types';

const axisParamsShape = PropTypes.shape({
    columnOrExpr : PropTypes.string,
    error : PropTypes.string, // for symmetric errors
    errorLow : PropTypes.string, // for asymmetric errors
    errorHigh : PropTypes.string, // for asymmetric errors
    label : PropTypes.string,
    unit : PropTypes.string,
    error: PropTypes.string,
    options : PropTypes.string // ex. 'grid,log,flip,opposite'
});

const selectionShape = PropTypes.shape({
    xMin : PropTypes.number,
    xMax : PropTypes.number,
    yMin : PropTypes.number,
    yMax : PropTypes.number
});

export const plotParamsShape = PropTypes.shape({
    plotStyle: PropTypes.oneOf(['points', 'line', 'linepoints']),
    sortColOrExpr: PropTypes.string,
    xyRatio: PropTypes.number,
    stretch: PropTypes.oneOf(['fit', 'fill']),
    selection: selectionShape,
    zoom: selectionShape,
    boundaries: selectionShape,
    userSetBoundaries: selectionShape,
    nbins: PropTypes.shape({x: PropTypes.number, y: PropTypes.number}),
    shading: PropTypes.oneOf(['lin', 'log']),
    x: axisParamsShape,
    y: axisParamsShape
});


export const plotDataShape = PropTypes.shape({
    rows: PropTypes.arrayOf(PropTypes.object), // {x,y,rowIdx} or {x,y,rowIdx,weight} or {x,y,rowIdx,right,left,low,high}
    decimateKey: PropTypes.string,
    xMin: PropTypes.number,
    xMax: PropTypes.number,
    yMin: PropTypes.number,
    yMax: PropTypes.number,
    weightMin: PropTypes.number,
    weightMax: PropTypes.number,
    idStr: PropTypes.string
});

