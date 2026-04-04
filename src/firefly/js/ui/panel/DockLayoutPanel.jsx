import React, {createContext, useContext, useState} from 'react';
import PropTypes from 'prop-types';
import {SplitPane, Pane} from 'react-split-pane';
import {Stack, Box, Sheet, Tooltip, IconButton} from '@mui/joy';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useStoreConnector} from '../SimpleComponent.jsx';
import {ArrowDropDown, ArrowDropUp} from '@mui/icons-material';


const SplitContext = createContext({});
export {Pane};

/**
 * A wrapper for SplitPane with persistent split position.
 *
 * @param p  props accepted by `SplitPane` (from react-split-pane) besides the following:
 * @param p.children pass-through children component, usually two `Pane`s wrapping `SplitContent`s. If one of the
 * `SplitContent`s has `isCollapsible` prop set as true, then its state will be implicitly managed by `SplitPanel`.
 * @param p.pKey {string}  an identifier for this panel.
 * @param p.primary {'first'|'second'} which pane receives sizing props such as `defaultSize`, `size`, `minSize`,
 * and `maxSize`.
 * @param p.openCollapsibleSize {string|number} the size of the collapsible content panel when it is open.
 * Can be a relative size like 'm%' or absolute size like n (in px). Will be used only if one of the `SplitContent`
 * has `isCollapsible` prop set as true, otherwise ignored.
 * @returns {JSX.Element}
 */
export const SplitPanel = ({children, pKey, openCollapsibleSize='50%', ...props}) => {
    pKey = 'SplitPanel-' + pKey;

    const {direction = 'vertical', primary = 'first', defaultSize,
        minSize, maxSize, size, onChange, style, divider, ...rest
    } = props;

    //react-split-pane (v3 onwards) sizes individual Pane elements; primary selects which pane owns the sizing props
    const primaryIdx = primary === 'second' ? 1 : 0;
    //negative maxSize is translated into minSize on the non-primary pane (to support behavior of older versions of react-split-pane)
    const oppositeIdx = primaryIdx === 0 ? 1 : 0;
    const {pos} = useStoreConnector(() => getComponentState(pKey));
    const childArray = React.Children.toArray(children).filter(Boolean);
    const collapsibleContentIdx = childArray.findIndex((c) => {
        return React.Children.toArray(c?.props?.children).filter(Boolean).some((child) => child?.props?.isCollapsible);
    });

    const splitLayoutState = useCollapsibleSplitLayout({
        collapseSecondContent:
            collapsibleContentIdx === 1 ? true :
                (collapsibleContentIdx === 0 ? false : null),
        openSize: openCollapsibleSize,
        collapsedSize: minSize,
    });

    const getPaneProps = (idx) => {
        const paneProps = {};
        if (collapsibleContentIdx === -1 && idx === primaryIdx) { //neither pane is collapsible, apply sizing props to the primary pane
            paneProps.defaultSize = pos ?? defaultSize;
            if (minSize !== undefined) paneProps.minSize = minSize;
            if (size !== undefined) paneProps.size = size;
            if (maxSize !== undefined && !(typeof maxSize === 'number' && maxSize < 0)) paneProps.maxSize = maxSize;
        }

        const splitLayoutPanel = idx === 0 ? splitLayoutState.panel1 : splitLayoutState.panel2;
        if (splitLayoutPanel) Object.assign(paneProps, splitLayoutPanel);

        if (idx === oppositeIdx && typeof maxSize === 'number' && maxSize < 0 && !splitLayoutState.isCollapsed) {
            //older versions (v0) of react-split-pane allowed maxSize={-N}, effectively setting minSize={N} on the non-primary pane.
            //this is not supported in react-split-pane versions v3 onwards, so we expicityly translate it here.
            paneProps.minSize = Math.max(paneProps.minSize ?? 0, Math.abs(maxSize));
        }

        return paneProps;
    };

    const handleResize = (sizes, event) => {
        splitLayoutState.onResize?.(sizes, event);
        const primarySize = sizes?.[primaryIdx];
        dispatchComponentStateChange(pKey, {pos: primarySize});
        onChange?.(primarySize, sizes, event);
    };

    const handleResizeStart = (event) => {
        splitLayoutState.onResizeStart?.(event);
    };

    const handleResizeEnd = (sizes, event) => {
        splitLayoutState.onResizeEnd?.(sizes, event);
    };

    return (
        <SplitContext.Provider value={splitLayoutState.collapsibleContent}>
            <SplitPane
                {...rest}
                direction={direction}
                divider={divider}
                style={style}
                onResize={handleResize}
                onResizeStart={handleResizeStart}
                onResizeEnd={handleResizeEnd}
            >
                {childArray.map((child, idx) => {
                    const paneProps = getPaneProps(idx);
                    const paneStyle = {
                        overflow: 'hidden',
                        minWidth: 0,
                        minHeight: 0,
                        ...paneProps.style,
                        ...child.props?.style,
                    };
                    return React.cloneElement(child, {
                        key: child.key ?? idx,
                        ...paneProps,
                        ...child.props,
                        style: paneStyle,
                    });
                })}
            </SplitPane>
        </SplitContext.Provider>
    );
};


/**
 * Decorated content panel to be used inside `SplitPanel`.
 *
 * @param p other than the below keys, same as `Sheet` or `Box` props
 * @param p.sx
 * @param p.style additional style to container; (DEPRECATED: use sx instead)
 * @param p.panelTitle {string} the title of this panel (appears in the collapsible tooltip)
 * @param p.isCollapsible {boolean} whether to render this panel as a collapsible or not
 * @param p.children {JSX.Element} the content of this panel
 */
export function SplitContent({sx={}, style={}, children, panelTitle,
                                 isCollapsible=false, ...props}) {
    return isCollapsible
        ? (
            <CollapsibleSplitContent sx={sx} panelTitle={panelTitle} {...props}>
                {children}
            </CollapsibleSplitContent>
        )
        : (
            <Stack overflow='hidden' position='relative'
                   sx={{width: 1, height: 1, minWidth: 0, minHeight: 0, p: 1/4, boxSizing: 'border-box'}}>
                <Box overflow='hidden' position='relative'
                     sx={{width: 1, height: 1, minWidth: 0, minHeight: 0, ...style, ...sx}} {...props}>
                    {children}
                </Box>
            </Stack>
        );
}


/**
 * Special case of `SplitContent` that has a toggle button to collapse/expand the content panel.
 *
 * @param p other than the below keys, it's same as Sheet props
 * @param p.sx
 * @param p.panelTitle {string} the title of this panel (appears in the tooltip)
 * @param p.children {JSX.Element} the content of this panel
 * @returns {Element}
 */
function CollapsibleSplitContent({sx={}, panelTitle, children, ...props}) {
    const {isOpen, onToggle} = useContext(SplitContext);

    // TODO: dynamically calculate styles based on the position of toggle button and split direction (passed as props)
    // e.g. for the redesign of EmbeddedSearchPositionPanel, the toggle button should appear on the middle of the right side of a vertical split

    return (
        <Sheet variant='outlined'
               aria-label={panelTitle}
               sx={{display: 'flex', flexGrow: 1, width: 1, height: 1, position: 'relative',
                   overflow: 'visible', minWidth: 0, minHeight: 0,
                   borderRadius: '5px', borderTopRightRadius: 0, // since toggle btn is right positioned
                   ...sx}}
               {...props}>
            <Tooltip title={`${isOpen ? 'Collapse' : 'Expand'} ${panelTitle ?? 'this panel'}`}>
                <IconButton variant='outlined'
                            onClick={onToggle}
                            sx={{position: 'absolute', bottom: '100%', right: '-1px', zIndex: 999,
                                borderBottomLeftRadius: 0, borderBottomRightRadius: 0, borderBottomStyle: 'none',
                                height: '1.25rem', width: '2.5rem', minHeight: 'auto',
                                boxShadow: '1px -2px 4px 0px rgba(var(--joy-shadowChannel) / var(--joy-shadowOpacity))',
                                backgroundColor: 'background.surface'}}>
                    {isOpen ? <ArrowDropDown/> : <ArrowDropUp/>}
                </IconButton>
            </Tooltip>
            <Box sx={{display: 'flex', flexGrow: 1, width: 1, height: 1, overflow: 'hidden', minWidth: 0, minHeight: 0}}>
                {children}
            </Box>
        </Sheet>
    );
}

/**@typedef {Object} CollapsibleSplitState Collapsible Split Layout state
 * @property {Object} panel stateful props to be passed to SplitPanel
 * @property {Object} collapsibleContent stateful props to be passed to CollapsibleSplitContent
 */

/**
 * A hook to manage the state of a split layout in which one of the content panels is collapsible.
 *
 * This returns the stateful props to be passed to the SplitPanel and CollapsibleSplitContent. The consumer controls the
 * layout of these interdependent components, yet can use this hook to handle the common logic related to collapsing behavior.
 *
 * @param p
 * @param p.collapseSecondContent {boolean|null} whether the second content panel is `CollapsibleContentPanel` or not.
 * Use `null` for no operation from this hook (in case there is no CollapsibleContentPanel).
 * @param p.openSize {string|number} the size of the collapsible content panel when it is open. Can be a relative size like 'x%'.
 * @param [p.collapsedSize] {number} the size of the collapsible content panel when it is collapsed. Must be in pixels.
 * @returns {CollapsibleSplitState}
 */
const useCollapsibleSplitLayout = ({collapseSecondContent, openSize, collapsedSize=0}) => {
    const [isCollapsibleOpen, setIsCollapsibleOpen] = useState(true);
    const [isSplitterDragging, setIsSplitterDragging] = useState(false);
    const [collapsibleSize, setCollapsibleSize] = useState(openSize);

    // TODO: animate width if the split is vertical
    const animationStyle = {transition: isSplitterDragging ? 'none' : 'height 0.2s ease-in-out'};

    if (collapseSecondContent === null) {
        return {
            panel1: null,
            panel2: null,
            isCollapsed: false,
            onResizeStart: null,
            onResize: null,
            onResizeEnd: null,
            collapsibleContent: {},
        };
    }

    const collapsiblePane = {
        size: isCollapsibleOpen ? collapsibleSize : collapsedSize,
        minSize: collapsedSize,
        style: {...animationStyle, overflow: 'visible'},
    };

    const collapsibleIdx = collapseSecondContent ? 1 : 0;

    return {
        // to let SplitPane manage sizing of the collapsible content panel, and let the other content panel grow/shrink
        panel1: collapseSecondContent ? null : collapsiblePane,
        panel2: collapseSecondContent ? collapsiblePane : null,
        isCollapsed: !isCollapsibleOpen,
        // to create collapsing animation effect only when not dragging otherwise it will be jerky
        onResizeStart: () => setIsSplitterDragging(true),
        // control the sizing of collapsible content panel
        onResize: (sizes) => {
            const nextSize = sizes?.[collapsibleIdx];
            if (nextSize !== undefined) setCollapsibleSize(nextSize);
        },
        onResizeEnd: (sizes) => {
            setIsSplitterDragging(false);
            const collapsibleSize = sizes?.[collapsibleIdx];
            setIsCollapsibleOpen((collapsibleSize ?? 0) > collapsedSize);
        },
        collapsibleContent: {
            isOpen: isCollapsibleOpen,
            onToggle: () => {
                if (!isCollapsibleOpen && collapsibleSize <= collapsedSize) setCollapsibleSize(openSize);
                setIsCollapsibleOpen(!isCollapsibleOpen); // update the open state after dragging
            },
        }
    };
};


function one(config, items){
    config = config || {center: {index: 0}};
    const item = config.center || config.north || config.east || config.west || config.south;

    return (
        <Stack height={1} direction='row'>
            <SplitContent>
                {items[item.index]}
            </SplitContent>
        </Stack>
    );
}

function two(config, items){
    config = config || {east: {index: 0}, west: {index: 1}};

    if (config.north || config.south) {
        const top = config.north || config.center;
        const bottom = config.south || config.center;
        return (
            <SplitPanel {...top} pKey='one'>
                <Pane>
                    <SplitContent>
                        {items[top.index]}
                    </SplitContent>
                </Pane>
                <Pane>
                    <SplitContent>
                        {items[bottom.index]}
                    </SplitContent>
                </Pane>
            </SplitPanel>

        );
    } else if (config.east || config.west) {
        const left = config.east || config.center;
        const right = config.west || config.center;
        return (
            <SplitPanel direction='horizontal' {...left} pKey='one'>
                <Pane>
                    <SplitContent>
                        {items[left.index]}
                    </SplitContent>
                </Pane>
                <Pane>
                    <SplitContent>
                        {items[right.index]}
                    </SplitContent>
                </Pane>
            </SplitPanel>
        );
    }
}

function three(config, items){
    config = config || {east: {index: 0}, west: {index: 1}, south: {index:2}};

    if (config.north) {
        if (config.south) {
            const two = config.east || config.center || config.west;
            return (
                <SplitPanel  {...config.north} pKey='one'>
                    <Pane>
                        <SplitContent>
                            {items[config.north.index]}
                        </SplitContent>
                    </Pane>
                    <Pane>
                        <SplitPanel  {...two} pKey='two'>
                            <Pane>
                                <SplitContent>
                                    {items[two.index]}
                                </SplitContent>
                            </Pane>
                            <Pane>
                                <SplitContent>
                                    {items[config.south.index]}
                                </SplitContent>
                            </Pane>
                        </SplitPanel>
                    </Pane>
                </SplitPanel>
            );
        } else {
            const two = config.east || config.center;
            const three = config.west || config.center;
            return (
                <SplitPanel  {...config.north} pKey='one'>
                    <Pane>
                        <SplitContent>
                            {items[config.north.index]}
                        </SplitContent>
                    </Pane>
                    <Pane>
                        <SplitPanel direction='horizontal' {...two} pKey='two'>
                            <Pane>
                                <SplitContent>
                                    {items[two.index]}
                                </SplitContent>
                            </Pane>
                            <Pane>
                                <SplitContent>
                                    {items[three.index]}
                                </SplitContent>
                            </Pane>
                        </SplitPanel>
                    </Pane>
                </SplitPanel>
            );
        }
    } else {
        if (config.south) {
            const one = config.east || config.center;
            const two = config.west || config.center;
            return (
                <SplitPanel  {...config.south} pKey='one'>
                    <Pane>
                        <SplitPanel direction='horizontal' {...one} pKey='two'>
                            <Pane>
                                <SplitContent>
                                    {items[one.index]}
                                </SplitContent>
                            </Pane>
                            <Pane>
                                <SplitContent>
                                    {items[two.index]}
                                </SplitContent>
                            </Pane>
                        </SplitPanel>
                    </Pane>
                    <Pane>
                        <SplitContent>
                            {items[config.south.index]}
                        </SplitContent>
                    </Pane>
                </SplitPanel>
            );
        } else {
            return (
                <SplitPanel direction='horizontal' {...config.east} pKey='one'>
                    <Pane>
                        <SplitContent>
                            {items[config.east.index]}
                        </SplitContent>
                    </Pane>
                    <Pane>
                        <SplitPanel direction='horizontal' {...config.west} pKey='two'>
                            <Pane>
                                <SplitContent>
                                    {items[config.center.index]}
                                </SplitContent>
                            </Pane>
                            <Pane>
                                <SplitContent>
                                    {items[config.west.index]}
                                </SplitContent>
                            </Pane>
                        </SplitPanel>
                    </Pane>
                </SplitPanel>
            );
        }
    }
}

function layoutDom(config, items) {
    const count = Object.keys(config).length;
    if (count === 1) {
        return one(config, items);
    } else if (count === 2) {
        return two(config, items);
    } else if (count === 3) {
        return three(config, items);
    }
}

const DockLayoutPanel = function (props) {
    var {config, children} = props;

    return (
        <Box position='relative'  flex='auto'>
            <Box position='absolute' sx={{inset: 0, p: 1/4, boxSizing: 'border-box'}}>
                {layoutDom(config, children)}
            </Box>
        </Box>
    );
};

DockLayoutPanel.propTypes = {
    north: PropTypes.number,
    south: PropTypes.number,
    east: PropTypes.number,
    west: PropTypes.number,
    center: PropTypes.number,
    config: PropTypes.object
};


export default DockLayoutPanel;
