import {debounce} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';

export const CHART_RESIZE_DEBOUNCE= 100;

export const wrapResizeMonitor= (Component, debounceMS=40) => {

    return (props) => {
        const [size,setSize]= useState({width:0,height:0});
        const {current:eRef}= useRef({ element: undefined, resizeObserver:undefined});
        const doSetSize= debounceMS ? debounce(setSize,debounceMS) : setSize;

        useEffect(() => {
            if (!eRef.element) return;
            const {clientWidth:width=0, clientHeight:height=0} = eRef.element;
            doSetSize({width,height});
            eRef.resizeObserver = new ResizeObserver(
                (entries) => {
                    for (const entry of entries) {
                        if (entry.contentRect) {
                            const {width=0, height=0} = entry.contentRect;
                            doSetSize({width, height});
                        }
                    }
                });
            if (eRef.element) eRef.resizeObserver.observe(eRef.element);

            return () => {
                if (eRef.element && eRef.resizeObserver) eRef.resizeObserver.disconnect();
            };
        },[eRef.element]);

        return (
            <div {...{
                style: {width:'100%', height:'100%'},
                ref: (c) => {
                    eRef.element=c;
                }
            }}>
                <Component {...{...props,size}} />
            </div>
        );
    };
};