import React from 'react';
import SplitPane from 'react-split-pane';

/**
 * decorate the content with DockLayoutPanel's look and feel.
 * @param content
 * @returns {XML}
 */
export function createContentWrapper(content) {
   return ( <div className='wrapper'> <div className='content'>{content}</div> </div> );
}

function one(config, items){
    config = config || {center: {index: 0}};
    const item = config.center || config.north || config.east || config.west || config.south;

    return (
        <div className='Pane vertical' style={{height: '100%'}}>
            {createContentWrapper(items[item.index])}
        </div>
    );
}

function two(config, items){
    config = config || {east: {index: 0}, west: {index: 1}};

    if (config.north || config.south) {
        const top = config.north || config.center;
        const bottom = config.south || config.center;
        return (
            <SplitPane split='horizontal'  {...top}>
                {createContentWrapper(items[top.index])}
                {createContentWrapper(items[bottom.index])}
            </SplitPane>

        );
    } else if (config.east || config.west) {
        const left = config.east || config.center;
        const right = config.west || config.center;
        return (
            <SplitPane split='vertical' {...left}>
                {createContentWrapper(items[left.index])}
                {createContentWrapper(items[right.index])}
            </SplitPane>
        );
    }
}

function three(config, items){
    config = config || {east: {index: 0}, west: {index: 1}, south: {index:2}};

    if (config.north) {
        if (config.south) {
            const two = config.east || config.center || config.west;
            return (
                <SplitPane split='horizontal' {...config.north}>
                    {createContentWrapper(items[config.north.index])}
                    <SplitPane split='horizontal' {...two}>
                        {createContentWrapper(items[two.index])}
                        {createContentWrapper(items[config.south.index])}
                    </SplitPane>
                </SplitPane>
            );
        } else {
            const two = config.east || config.center;
            const three = config.west || config.center;
            return (
                <SplitPane split='horizontal' {...config.north}>
                    {createContentWrapper(items[config.north.index])}
                    <SplitPane split='vertical' {...two.config}>
                        {createContentWrapper(items[two.index])}
                        {createContentWrapper(items[three.index])}
                    </SplitPane>
                </SplitPane>
            );
        }
    } else {
        if (config.south) {
            const one = config.east || config.center;
            const two = config.west || config.center;
            return (
                <SplitPane split='horizontal' {...config.south}>
                    <SplitPane split='vertical' {...one}>
                        {createContentWrapper(items[one.index])}
                        {createContentWrapper(items[two.index])}
                    </SplitPane>
                    {createContentWrapper(items[config.south.index])}
                </SplitPane>
            );
        } else {
            return (
                <SplitPane split='vertical' {...config.east}>
                    {createContentWrapper(items[config.east.index])}
                    <SplitPane split='vertical' {...config.west}>
                        {createContentWrapper(items[config.center.index])}
                        {createContentWrapper(items[config.west.index])}
                    </SplitPane>
                </SplitPane>
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
        <div style={{position: 'relative',  flex: 'auto'}}>
            <div style={{position: 'absolute', top: '0', bottom: 0, left: 0, right: 0}}>
                {layoutDom(config, children)}
            </div>
        </div>
    );
};

DockLayoutPanel.propTypes = {
    north: React.PropTypes.number,
    south: React.PropTypes.number,
    east: React.PropTypes.number,
    west: React.PropTypes.number,
    center: React.PropTypes.number,
    config: React.PropTypes.object
};


export default DockLayoutPanel;









