import React, {useState} from 'react';
import PropTypes from 'prop-types';
import SplitPane from 'react-split-pane';
import {Stack, Box, Sheet, Tooltip, IconButton} from '@mui/joy';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useStoreConnector} from '../SimpleComponent.jsx';
import {ArrowDropDown, ArrowDropUp} from '@mui/icons-material';


/**
 * A wrapper for SplitPane with persistent split position
 * @param p  component properties
 * @param p.children  pass through children component
 * @param p.defaultSize   the default split size
 * @param p.pKey  an identifier for this panel.  One will be created if not given
 * @returns {JSX.Element}
 */
export const SplitPanel = ({children, defaultSize, pKey, ...rest}) => {
    pKey = 'SplitPanel-' + pKey;
    const {pos}  = useStoreConnector(() => getComponentState(pKey));
    const onChange = (pos) => dispatchComponentStateChange(pKey, {pos});

    return (
        <SplitPane split='horizontal' defaultSize={pos ?? defaultSize} onChange={onChange} {...rest}>
            {children}
        </SplitPane>
    );
};


/**
 * decorate the content with DockLayoutPanel's look and feel.
 * @param p  component props
 * @param p.sx
 * @param p.style  additional style to container; (deprecated use sx instead)
 * @param p.className  additional className to container
 * @param p.children  content of this panel
 */
export function SplitContent({sx={}, style={}, className='', children}) {
    return ( <Stack overflow='hidden' position='relative' width={1} m={1/2}>
                <Box overflow='hidden' position='absolute' sx={{inset:'0', ...style, ...sx}} className={className}>
                    {children}
                </Box>
             </Stack>
            );
}


/**
 * SplitPanel's content panel that has a toggle button to collapse/expand the panel.
 * This is a controlled component, i.e., the parent component manages its state.
 *
 * @param p other than the below keys, it's same as Sheet props
 * @param p.sx
 * @param p.panelTitle {string} the title of this panel (appears in the tooltip)
 * @param p.isOpen {boolean} whether the panel is open
 * @param p.onToggle {function():void} fired when the toggle button is clicked
 * @param p.children {JSX.Element} the content of this panel
 * @returns {Element}
 */
export function CollapsibleSplitContent({sx={}, panelTitle, isOpen, onToggle, children, ...props}) {
    // TODO: dynamically calculate styles based on the position of toggle button and split direction (passed as props)
    //  e.g. for the redesign of EmbeddedSearchPositionPanel, the toggle button should appear on the middle of the right side of a vertical split
    return (
        <Sheet variant='outlined'
               sx={{display: 'flex', flexGrow: 1, position: 'relative',
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
            {children}
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
 * @param [p.collapseSecondContent] {boolean} whether the second content panel is `CollapsibleContentPanel` or not
 * @param p.openSize {string|number} the size of the collapsible content panel when it is open. Can be a relative size like 'x%'.
 * @param [p.collapsedSize] {number} the size of the collapsible content panel when it is collapsed. Must be in pixels.
 * @returns {CollapsibleSplitState}
 */
export const useCollapsibleSplitLayout = ({collapseSecondContent=true, openSize, collapsedSize=0}) => {
    const [isCollapsibleOpen, setIsCollapsibleOpen] = useState(true);
    const [isSplitterDragging, setIsSplitterDragging] = useState(false);

    // TODO: animate width if the split is vertical
    const animationStyle = {transition: isSplitterDragging ? 'none' : 'height 0.2s ease-in-out'};

    return {
        panel: {
            // to let SplitPane manage sizing of the collapsible content panel, and let the other content panel grow/shrink
            primary: collapseSecondContent ? 'second' : 'first',
            // control the sizing of collapsible content panel
            size: isCollapsibleOpen ? openSize : collapsedSize,
            minSize: collapsedSize,
            // to create collapsing animation effect only when not dragging otherwise it will be jerky
            onDragStarted: () => setIsSplitterDragging(true),
            onDragFinished: (currentSize) => {
                setIsSplitterDragging(false);
                setIsCollapsibleOpen(currentSize > collapsedSize); // update the open state after dragging
            },
            [collapseSecondContent ? 'pane2Style' : 'pane1Style']: animationStyle,
        },
        collapsibleContent: {
            isOpen: isCollapsibleOpen,
            onToggle: () => setIsCollapsibleOpen(!isCollapsibleOpen),
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
                <SplitContent>
                    {items[top.index]}
                </SplitContent>
                <SplitContent>
                    {items[bottom.index]}
                </SplitContent>
            </SplitPanel>

        );
    } else if (config.east || config.west) {
        const left = config.east || config.center;
        const right = config.west || config.center;
        return (
            <SplitPanel split='vertical' {...left} pKey='one'>
                <SplitContent>
                    {items[left.index]}
                </SplitContent>
                <SplitContent>
                    {items[right.index]}
                </SplitContent>
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
                    <SplitContent>
                        {items[config.north.index]}
                    </SplitContent>
                    <SplitPanel  {...two} pKey='two'>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[config.south.index]}
                        </SplitContent>
                    </SplitPanel>
                </SplitPanel>
            );
        } else {
            const two = config.east || config.center;
            const three = config.west || config.center;
            return (
                <SplitPanel  {...config.north} pKey='one'>
                    <SplitContent>
                        {items[config.north.index]}
                    </SplitContent>
                    <SplitPanel split='vertical' {...two.config} pKey='two'>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[three.index]}
                        </SplitContent>
                    </SplitPanel>
                </SplitPanel>
            );
        }
    } else {
        if (config.south) {
            const one = config.east || config.center;
            const two = config.west || config.center;
            return (
                <SplitPanel  {...config.south} pKey='one'>
                    <SplitPanel split='vertical' {...one} pKey='two'>
                        <SplitContent>
                            {items[one.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                    </SplitPanel>
                    <SplitContent>
                        {items[config.south.index]}
                    </SplitContent>
                </SplitPanel>
            );
        } else {
            return (
                <SplitPanel split='vertical' {...config.east} pKey='one'>
                    <SplitContent>
                        {items[config.east.index]}
                    </SplitContent>
                    <SplitPanel split='vertical' {...config.west} pKey='two'>
                        <SplitContent>
                            {items[config.center.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[config.west.index]}
                        </SplitContent>
                    </SplitPanel>
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
            <Box position='absolute' sx={{inset:'0'}}>
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









