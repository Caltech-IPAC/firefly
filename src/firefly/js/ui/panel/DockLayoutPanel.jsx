import React from 'react';
import PropTypes from 'prop-types';
import SplitPane from 'react-split-pane';
import {Stack, Box} from '@mui/joy';

import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {useStoreConnector} from '../SimpleComponent.jsx';


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









