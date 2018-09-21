//import Color from '../util/Color.js';

// Sequential color scales from colorlover
// roughly matching the first 8 trace colors:
// '#1f77b4', '#2ca02c', '#d62728', '#9467bd',
// '#8c564b', '#e377c2', '#7f7f7f', '#17becf'.
// They all start from the pale and end with the dark color
//   and are good to represent density maps.
export const SevenColorSequential = {
    'BlueSeq': [
        [0.0, 'rgb(239,243,255)'],
        [0.1, 'rgb(198,219,239)'],
        [0.2, 'rgb(158,202,225)'],
        [0.4, 'rgb(107,174,214)'],
        [0.6, 'rgb(66,146,198)'],
        [0.8, 'rgb(33,113,181)'],
        [1.0, 'rgb(8,69,148)']
    ],
    'GreenSeq': [
        [0.0, 'rgb(237,248,233)'],
        [0.1, 'rgb(199,233,192)'],
        [0.2, 'rgb(161,217,155)'],
        [0.4, 'rgb(116,196,118)'],
        [0.6, 'rgb(65,171,93)'],
        [0.8, 'rgb(35,139,69)'],
        [1.0, 'rgb(0,90,50)']
    ],
    'RedSeq': [
        [0.0, 'rgb(254,229,217)'],
        [0.1, 'rgb(252,187,161)'],
        [0.2, 'rgb(252,146,114)'],
        [0.4, 'rgb(251,106,74)'],
        [0.6, 'rgb(239,59,44)'],
        [0.8, 'rgb(203,24,29)'],
        [1.0, 'rgb(153,0,13)']
    ],
    'PurpleSeq': [
        [0.0, 'rgb(242,240,247)'],
        [0.1, 'rgb(218,218,235)'],
        [0.2, 'rgb(188,189,220)'],
        [0.4, 'rgb(158,154,200)'],
        [0.6, 'rgb(128,125,186)'],
        [0.8, 'rgb(106,81,163)'],
        [1.0, 'rgb(74,20,134)']
    ],
    'BrownSeq': [
        [0.0, 'rgb(249,237,234)'],
        [0.1, 'rgb(236,203,193)'],
        [0.2, 'rgb(220,167,151)'],
        [0.4, 'rgb(202,131,111)'],
        [0.6, 'rgb(182,98,75)'],
        [0.8, 'rgb(161,65,41)'],
        [1.0, 'rgb(135,20,0)']
    ],
    'RdPuSeq': [
        [0.0, 'rgb(254,235,226)'],
        [0.1, 'rgb(252,197,192)'],
        [0.2, 'rgb(250,159,181)'],
        [0.4, 'rgb(247,104,161)'],
        [0.6, 'rgb(221,52,151)'],
        [0.8, 'rgb(174,1,126)'],
        [1.0, 'rgb(122,1,119)']
    ],
    'GreySeq': [
        [0.0, 'rgb(247,247,247)'],
        [0.1, 'rgb(217,217,217)'],
        [0.2, 'rgb(189,189,189)'],
        [0.4, 'rgb(150,150,150)'],
        [0.6, 'rgb(115,115,115)'],
        [0.8, 'rgb(82,82,82)'],
        [1.0, 'rgb(37,37,37)']
    ],
    'GnBuSeq': [
        [0.0, 'rgb(240,249,232)'],
        [0.1, 'rgb(204,235,197)'],
        [0.2, 'rgb(168,221,181)'],
        [0.4, 'rgb(123,204,196)'],
        [0.6, 'rgb(78,179,211)'],
        [0.8, 'rgb(43,140,190)'],
        [1.0, 'rgb(8,88,158)']
    ]
};

// Plotly colorscales are purpose-based
// for example:
// Reds - for non-negative numeric values
// Blues - for non-positive numeric values
// RdBu or Picnic - diverging scales to show departure from middle values
export const PlotlyCS = [
    'Greys','YlGnBu','Greens','YlOrRd','Bluered','RdBu',
    'Reds','Blues','Picnic','Rainbow','Portland','Jet',
    'Hot','Blackbody','Earth','Electric','Viridis','Cividis'];

export const OneColorSequentialCS = Object.keys(SevenColorSequential);

export const ALL_COLORSCALE_NAMES = Object.keys(SevenColorSequential).concat(PlotlyCS);

export function colorscaleNameToVal(name) {
    const colorscale = SevenColorSequential[name];
    if (colorscale) {
        return colorscale;
    } else if (PlotlyCS.includes(name)) {
        return name;
    }
    return undefined;

}

// does not work well
// function createColorScale(baseColor) {
//     const numcols = 5;
//     return [[0.0, 'rgb(237, 237, 237)']].concat(
//         Color.makeSimpleColorMap(baseColor, numcols, true).map((e,i) => {
//             const [r, g, b] = Color.toRGB(e);
//             let p = 1.0;
//             if (i < (numcols-1)) p = (i+1)*(1/numcols);
//             return [p, `rgb(${r},${g},${b})`];
//         })
//     );
// }