/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * loosely based on http://www.highcharts.com/plugin-registry/single/38/error_bar
 * (using highcharts 5 source)
 * @param Highcharts
 */
export function xyErrorBarExtension(Highcharts) {
    (function (H) {
        'use strict';
        var each = H.each,
            noop = H.noop,
            pick = H.pick,
            seriesType = H.seriesType,
            seriesTypes = H.seriesTypes;

        /**
         * The boxplot series type.
         *
         * @constructor seriesTypes.boxplot
         * @augments seriesTypes.column
         */
        seriesType('error_bar', 'column', {
            threshold: null,
            tooltip: {
                pointFormat: '<span style="color:{point.color}">\u25CF</span>' + // eslint-disable-line no-dupe-keys
                    '<b>{point.left}</b> - <b>{point.right}</b><br/>' +
                    '<b>{point.low}</b> - <b>{point.high}</b><br/>'
            },
            grouping: false,
            color: '#000000',
            fillColor: '#ffffff',
            lineWidth: 1,
            medianWidth: 2,
            states: {
                hover: {
                    brightness: -0.3
                }
            },
            //stemColor: null,
            //stemDashStyle: 'solid'
            //stemWidth: null,

            //whiskerColor: null,
            whiskerLength: '3',
            whiskerWidth: 2,
            format: 'xy'


        }, /** @lends seriesTypes.boxplot */ {
            pointArrayMap: ['x','y', 'left', 'right', 'low', 'high'], // array point configs are mapped to this
            xpointArrayMap: ['x', 'left', 'right'],
            ypointArrayMap: ['y', 'low', 'high'],
            toYData(point) { // return a plain array for speedy calculation
                return [point.low, point.high];
            },
            pointValKey: 'y', // defines the top of the tracker


            /**
             * Get presentational attributes
             * @param point
             */
            pointAttribs(point) {
                var options = this.options,
                    color = (point && point.color) || this.color;

                return {
                    'fill': point.fillColor || options.fillColor || color,
                    'stroke': options.lineColor || color,
                    'stroke-width': options.lineWidth || 0
                };
            },


            /**
             * Disable data labels for box plot
             */
            drawDataLabels: noop,

            /**
             * Translate data points from raw values x and y to plotX and plotY
             */
            translate() {
                var series = this,
                    xAxis = series.xAxis,
                    yAxis = series.yAxis,
                    xpointArrayMap = series.xpointArrayMap,
                    ypointArrayMap = series.ypointArrayMap;

                seriesTypes.column.prototype.translate.apply(series);

                // do the translation on each point dimension
                each(series.points, function(point) {
                    each(xpointArrayMap, function(key) {
                        if (point[key] !== null) {
                            point[key + 'Plot'] = xAxis.translate(point[key], 0, 0, 0, 1);
                        }
                    });
                    each(ypointArrayMap, function(key) {
                        if (point[key] !== null) {
                            point[key + 'Plot'] = yAxis.translate(point[key], 0, 1, 0, 1);
                        }
                    });
                });
            },

            /**
             * Draw the data points
             */
            drawPoints() {
                var series = this, //state = series.state,
                    points = series.points,
                    options = series.options,
                    chart = series.chart,
                    renderer = chart.renderer,
                    xPlot,
                    yPlot,
                    leftPlot,
                    rightPlot,
                    highPlot,
                    lowPlot,
                    xWhiskerLow,
                    xWhiskerHigh,
                    yWhiskerLeft,
                    yWhiskerRight,
                    whiskerLength = series.options.whiskerLength,
                    format = series.options.format;


                each(points, function(point) {

                    var graphic = point.graphic,
                        verb = graphic ? 'animate' : 'attr';


                    var stemAttr = {},
                        whiskersAttr = {},
                        color = point.color || series.color;

                    if (point.plotY !== undefined) {

                        xPlot  = Math.floor(point.xPlot);
                        yPlot =  Math.floor(point.yPlot);
                        leftPlot = Math.floor(point.leftPlot);
                        rightPlot = Math.floor(point.rightPlot);

                        highPlot = Math.floor(point.highPlot);
                        lowPlot = Math.floor(point.lowPlot);

                        xWhiskerLow = Math.floor(yPlot - whiskerLength);
                        xWhiskerHigh = Math.floor(yPlot + whiskerLength);

                        yWhiskerLeft = Math.floor(xPlot - whiskerLength);
                        yWhiskerRight = Math.floor(xPlot + whiskerLength);

                        if (!graphic) {
                            point.graphic = graphic = renderer.g('point')
                                .add(series.group);

                            point.stem = renderer.path()
                                .addClass('highcharts-boxplot-stem')
                                .add(graphic);

                            if (whiskerLength) {
                                point.whiskers = renderer.path()
                                    .addClass('highcharts-boxplot-whisker')
                                    .add(graphic);
                            }

                            // Stem attributes
                            stemAttr.stroke = point.stemColor || options.stemColor || color;
                            stemAttr['stroke-width'] = pick(point.stemWidth, options.stemWidth, options.lineWidth);
                            stemAttr.dashstyle = point.stemDashStyle || options.stemDashStyle;
                            point.stem.attr(stemAttr);

                            // Whiskers attributes
                            if (whiskerLength) {
                                whiskersAttr.stroke = point.whiskerColor || options.whiskerColor || color;
                                whiskersAttr['stroke-width'] = pick(point.whiskerWidth, options.whiskerWidth, options.lineWidth);
                                point.whiskers.attr(whiskersAttr);
                            }
                        }

                        switch (format) {
                            case 'x':
                                point.stem[verb]({
                                    d: [
                                        'M',
                                        leftPlot, yPlot,
                                        'L',
                                        rightPlot, yPlot,
                                        'z'
                                    ]});

                                if (whiskerLength) {
                                    point.whiskers[verb]({
                                        d: [
                                            'M',
                                            leftPlot, xWhiskerLow,
                                            'L',
                                            leftPlot, xWhiskerHigh,

                                            'M',
                                            rightPlot, xWhiskerLow,
                                            'L',
                                            rightPlot, xWhiskerHigh,

                                            'z'
                                        ]
                                    });
                                }
                                break;

                            case 'y':
                                point.stem[verb]({
                                    d: [
                                        'M',
                                        xPlot, lowPlot,
                                        'L',
                                        xPlot, highPlot,
                                        'z'
                                    ]});

                                if (whiskerLength) {
                                    point.whiskers[verb]({
                                        d: [
                                            'M',
                                            yWhiskerLeft, lowPlot,
                                            'L',
                                            yWhiskerRight, lowPlot,

                                            'M',
                                            yWhiskerLeft, highPlot,
                                            'L',
                                            yWhiskerRight, highPlot,

                                            'z'
                                        ]
                                    });
                                }
                                break;

                            case 'xy':
                            default:
                                point.stem[verb]({
                                    d: [
                                        'M',
                                        leftPlot, yPlot,
                                        'L',
                                        rightPlot, yPlot,
                                        'M',
                                        xPlot, lowPlot,
                                        'L',
                                        xPlot, highPlot,
                                        'z'
                                    ]});

                                if (whiskerLength) {
                                    point.whiskers[verb]({
                                        d: [
                                            'M',
                                            leftPlot, xWhiskerLow,
                                            'L',
                                            leftPlot, xWhiskerHigh,

                                            'M',
                                            rightPlot, xWhiskerLow,
                                            'L',
                                            rightPlot, xWhiskerHigh,

                                            'M',
                                            yWhiskerLeft, lowPlot,
                                            'L',
                                            yWhiskerRight, lowPlot,

                                            'M',
                                            yWhiskerLeft, highPlot,
                                            'L',
                                            yWhiskerRight, highPlot,

                                            'z'
                                        ]
                                    });
                                }
                                break;
                        }
                    }
                });

            },
            setStackedPoints: noop // #3890


        });

        /* ****************************************************************************
         * End error_bars series code												*
         *****************************************************************************/

    }(Highcharts));
}

