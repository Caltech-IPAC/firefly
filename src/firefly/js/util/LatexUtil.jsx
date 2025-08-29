import React from 'react';
import {MathJaxContext} from 'better-react-mathjax';
import mathjaxSrc from 'mathjax-full/es5/tex-svg.js?url'; // to emit it as a separate file whose URL string can be used for MathJaxContext 'src'


const PLOTLY_FONT_SCALE = 1.25; // to make Plotly MathJax fonts bigger (otherwise Plotly renders them too small)
const UI_FONT_SCALE = 0.9; // to make MathJax fonts in UI a bit smaller (otherwise they look bigger compared to MUI Joy typography)

const mathJaxConfig = {
    tex: {
        // From TeX Input Processor Options: https://docs.mathjax.org/en/v3.2/options/input/tex.html
        inlineMath: [['$', '$'], ['\\(', '\\)']],
    },
    svg: {
        // From SVG Output Processor Options: https://docs.mathjax.org/en/v3.2/options/output/svg.html
        scale: PLOTLY_FONT_SCALE, // will apply globally but is needed because plotly dynamically creates temporary MathJax containers and injects its nodes into the chart SVG
        mtextInheritFont: true,
    },
};

export const latexRootStyles = {
    // selects all the MathJax containers in the UI, aka under .Mui* root nodes like app, dialog, listbox, etc.
    // (but avoids Plotly’s temporary staging mjx-containers that are directly under body)
    'body [class^="Mui"] mjx-container.MathJax[jax="SVG"]': {
        // apply just the UI scale to the active font size, overriding the global plotly scale
        fontSize: `${UI_FONT_SCALE}em !important`
    },

};


/**
 * A component that provides its children LaTeX rendering capabilities using MathJax.
 * **Must be used only once in the app, typically at the root level**
 *
 * @param props MathJaxContext props to override the defaults, see
 * https://github.com/fast-reflexes/better-react-mathjax/blob/master/README.md#mathjaxcontext-component
 * @param {Element} props.children the children that may contain LaTeX to be rendered (`<MathJax/>` elements)
 * @returns {Element}
 */
export function LatexProvider({children, ...props}) {
    return (
        <MathJaxContext config={mathJaxConfig} asyncLoad={true} src={mathjaxSrc}
                        onError={(e) => console.error(`Error in loading MathJax: ${e.message}`)}
                        {...props}>
            {children}
        </MathJaxContext>
    );
}


/**
 * @typedef {string} LatexFragment
 * Raw LaTeX without delimiters (`$...$`), safe to concatenate with plain text or other LaTeX fragments.
 */
/**
 * @typedef {string} LatexDelimited
 * LaTeX string wrapped in delimiters (`$...$`), ready to be rendered by MathJax (in Plotly or in UI through <MathJax/>).
 */
/**
 * Wrap a raw LaTeX string with delimiters that MathJax understands.
 * @param {LatexFragment} str - the raw LaTeX fragment
 * @param {Array<string>} delimiterPair - the pair of delimiters to use, default is ['$', '$'] because that is the only choice
 * Plotly supports whereas <MathJax> elements supports this and more choices.
 * @return {LatexDelimited} the wrapped string
 */
export const latexStr = (str, delimiterPair=['$', '$']) => delimiterPair[0] + str + delimiterPair[1];
