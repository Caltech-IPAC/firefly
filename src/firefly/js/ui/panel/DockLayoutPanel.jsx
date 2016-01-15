import React from 'react';
import SplitPane from 'react-split-pane';

function createContentWrapper(children, index) {
    var content = children[index];
   return ( <div className='wrapper'> <div className='content'>{content}</div> </div> );
}

function one(layout, items){
    layout = layout || {center: {index: 0}};
    const item = layout.center || layout.north || layout.east || layout.west || layout.south;

    return (
        <div className='Pane vertical' style={{height: '100%'}}>
            {createContentWrapper(items, item.index)}
        </div>
    );
}

function two(layout, items){
    layout = layout || {east: {index: 0}, west: {index: 1}};

    if (layout.north || layout.south) {
        const top = layout.north || layout.center;
        const bottom = layout.south || layout.center;
        return (
            <SplitPane split='horizontal'  {...top}>
                {createContentWrapper(items, top.index)}
                {createContentWrapper(items, bottom.index)}
            </SplitPane>

        );
    } else if (layout.east || layout.west) {
        const left = layout.east || layout.center;
        const right = layout.west || layout.center;
        return (
            <SplitPane split='vertical' {...left}>
                {createContentWrapper(items, left.index)}
                {createContentWrapper(items, right.index)}
            </SplitPane>
        );
    }
}

function three(layout, items){
    layout = layout || {east: {index: 0}, west: {index: 1}, south: {index:2}};

    if (layout.north) {
        if (layout.south) {
            const two = layout.east || layout.center || layout.west;
            return (
                <SplitPane split='horizontal' {...layout.north}>
                    {createContentWrapper(items, layout.north.index)}
                    <SplitPane split='horizontal' {...two}>
                        {createContentWrapper(items, two.index)}
                        {createContentWrapper(items, layout.south.index)}
                    </SplitPane>
                </SplitPane>
            );
        } else {
            const two = layout.east || layout.center;
            const three = layout.west || layout.center;
            return (
                <SplitPane split='horizontal' {...layout.north}>
                    {createContentWrapper(items, layout.north.index)}
                    <SplitPane split='vertical' {...two.layout}>
                        {createContentWrapper(items, two.index)}
                        {createContentWrapper(items, three.index)}
                    </SplitPane>
                </SplitPane>
            );
        }
    } else {
        if (layout.south) {
            const one = layout.east || layout.center;
            const two = layout.west || layout.center;
            return (
                <SplitPane split='horizontal' {...layout.south}>
                    <SplitPane split='vertical' {...one}>
                        {createContentWrapper(items, one.index)}
                        {createContentWrapper(items, two.index)}
                    </SplitPane>
                    {createContentWrapper(items, layout.south.index)}
                </SplitPane>
            );
        } else {
            return (
                <SplitPane split='vertical' {...layout.east}>
                    {createContentWrapper(items, layout.east.index)}
                    <SplitPane split='vertical' {...layout.west}>
                        {createContentWrapper(items, layout.center.index)}
                        {createContentWrapper(items, layout.west.index)}
                    </SplitPane>
                </SplitPane>
            );
        }
    }
}

function layoutDom(layout, items) {
    const count = Object.keys(layout).length;
    if (count === 1) {
        return one(layout, items);
    } else if (count === 2) {
        return two(layout, items);
    } else if (count === 3) {
        return three(layout, items);
    }
}

const DockLayoutPanel = function (props) {
    var {layout, children} = props;

    return (
        <div style={{position: 'relative',  flex: 'auto'}}>
            <div style={{position: 'absolute', top: '0', bottom: 0, left: 0, right: 0}}>
                {layoutDom(layout, children)}
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
    layout: React.PropTypes.object
};


export default DockLayoutPanel;









