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

/**
 * decorate the content with DockLayoutPanel's look and feel.
 */
export function SplitContent({children}) {
    return ( <div className='wrapper'>
                <div className='content'>{children}</div>
             </div>
            );
}


function one(config, items){
    config = config || {center: {index: 0}};
    const item = config.center || config.north || config.east || config.west || config.south;

    return (
        <div className='Pane vertical' style={{height: '100%'}}>
            <SplitContent>
                {items[item.index]}
            </SplitContent>
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
                <SplitContent>
                    {items[top.index]}
                </SplitContent>
                <SplitContent>
                    {items[bottom.index]}
                </SplitContent>
            </SplitPane>

        );
    } else if (config.east || config.west) {
        const left = config.east || config.center;
        const right = config.west || config.center;
        return (
            <SplitPane split='vertical' {...left}>
                <SplitContent>
                    {items[left.index]}
                </SplitContent>
                <SplitContent>
                    {items[right.index]}
                </SplitContent>
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
                    <SplitContent>
                        {items[config.north.index]}
                    </SplitContent>
                    <SplitPane split='horizontal' {...two}>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[config.south.index]}
                        </SplitContent>
                    </SplitPane>
                </SplitPane>
            );
        } else {
            const two = config.east || config.center;
            const three = config.west || config.center;
            return (
                <SplitPane split='horizontal' {...config.north}>
                    <SplitContent>
                        {items[config.north.index]}
                    </SplitContent>
                    <SplitPane split='vertical' {...two.config}>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[three.index]}
                        </SplitContent>
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
                        <SplitContent>
                            {items[one.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[two.index]}
                        </SplitContent>
                    </SplitPane>
                    <SplitContent>
                        {items[config.south.index]}
                    </SplitContent>
                </SplitPane>
            );
        } else {
            return (
                <SplitPane split='vertical' {...config.east}>
                    <SplitContent>
                        {items[config.east.index]}
                    </SplitContent>
                    <SplitPane split='vertical' {...config.west}>
                        <SplitContent>
                            {items[config.center.index]}
                        </SplitContent>
                        <SplitContent>
                            {items[config.west.index]}
                        </SplitContent>
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









