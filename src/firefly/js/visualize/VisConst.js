/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Pure constants extracted from ImagePlotCntlr.js and MultiViewCntlr.js into a
 * separate leaf file to break the circular dependency between those two modules.
 *
 * ImagePlotCntlr ↔ MultiViewCntlr form a circular dep.  When Rollup bundles
 * ApiUtilImage.jsx (which re-exports from both) into a namespace, it serialises
 * module evaluation order.  Because the cycle defers one module's body, the
 * `const` values it owns can still be in the Temporal Dead Zone when the
 * namespace object is constructed, causing a ReferenceError at load time.
 *
 * Placing these constants here, with no dependency on either controller file,
 * ensures Rollup initialises them before the namespace object is built.
 */

import Enum from 'enum';

// ─── From ImagePlotCntlr.js ──────────────────────────────────────────────────

/** @typedef ExpandType
 * enum can be one of
 * @prop COLLAPSE
 * @prop GRID
 * @prop SINGLE
 * @type {Enum}
 */
/** @type ExpandType */
export const ExpandType = new Enum(['COLLAPSE', 'GRID', 'SINGLE']);

/**
 * @typedef {Object} WcsMatchType
 * enum can be one of
 * @prop Standard
 * @prop Target
 * @prop Pixel
 * @prop PixelCenter
 * @type {Enum}
 */
/** @type WcsMatchType */
export const WcsMatchType = new Enum(['Standard', 'Target', 'Pixel', 'PixelCenter']);

/**
 * @typedef ActionScope
 * enum can be one of
 * @prop GROUP
 * @prop SINGLE
 * @prop LIST
 * @type {Enum}
 * @public
 * @global
 */
/** @type ActionScope */
export const ActionScope = new Enum(['GROUP', 'SINGLE', 'LIST']);

// ─── From MultiViewCntlr.js ──────────────────────────────────────────────────

export const IMAGE = 'image';
export const EXPANDED_MODE_RESERVED = 'EXPANDED_MODE_RESERVED';

/**
 * @typedef NewPlotMode
 * enum one of
 * @prop create_replace
 * @prop replace_only
 * @prop none
 * @type {Enum}
 */
/** @type NewPlotMode */
export const NewPlotMode = new Enum(['create_replace', 'replace_only', 'none']);
