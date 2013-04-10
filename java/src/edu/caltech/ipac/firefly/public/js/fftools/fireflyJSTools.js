

/**
 * @namespace the firefly object.
 * Access to the Firefly tools utilities.
 */
firefly= function() {

    var globalPlotParams= null;
    var retFF= null;
    var ffPrivate= new Object();

    if (!("firefly" in window))  retFF= new Object();
    else  retFF= firefly;

    if (!("debug" in retFF)) {
        if ("fireflyDebug" in window)  retFF.debug= fireflyDebug;
        else retFF.debug= false;
    }


//===================================================================================
//----------- ImageViewer Viewer base object ----------------------------------------
//===================================================================================


    /**
     * Create a new ImageViewer object in the specified div.
     * @param div The div to put the ImageViewer in.
     * @param group - (optional) The plot group to associate this image viewer.  All ImageViewers with the same group name will
     operate together.
     */
    var ImageViewer= function(div,group) {
        var plotGroup= group ? group : null;
        var defaultParams= null;
        /**
         * plot a fits image
         * @param params
         */
        this.plot= function(params) {
            params= this.combineParams(params);
            if (plotGroup==null) {
                ffPrivate.plotImageToDiv(div,params);
            }
            else {
                ffPrivate.plotGroupedImageToDiv(div,params,plotGroup);
            }
        };

        this.setDefaultParams= function(params) {
            defaultParams= params;
        };


        this.merge= function(fromP,toP) {
            if (fromP && toP) {
                for (var key in fromP) {
                    if (fromP.hasOwnProperty(key) && !toP.hasOwnProperty(key)) {
                        toP[key] = fromP[key];
                    }
                }
            }
            return toP;
        };


        this.combineParams= function(params) {
            this.merge(defaultParams,params);
            this.merge(globalPlotParams,params);
            return params;
        };
        this.setGroup= function(group) { plotGroup= group; };
    };

    ImageViewer.prototype.serializeRangeValues= function() { return firefly.serializeRangeValues(); };
    ImageViewer.prototype.makeURLRequest= function(url)       { return {"URL" : url}; };
    ImageViewer.prototype.makeFileRequest= function(fileName) { return {"File" : fileName}; };
    ImageViewer.prototype.makeFileThenURLRequest= function(fileName,url) {
        return {
            "File" : fileName,
            "URL" : url,
            "Type" : "TRY_FILE_THEN_URL"
        };
    };



    ImageViewer.prototype.plotURL= function(url) { this.plot(this.makeURLRequest(url)); };
    ImageViewer.prototype.plotFile= function(file) { this.plot(this.makeFileRequest(file)); };
    ImageViewer.prototype.plotFileOrURL= function(file,url) { this.plot(this.makeFileThenURLRequest(file,url)); };

//===================================================================================
//----------- Expanded Viewer  ------------------------------------------------------
//===================================================================================

    /**
     * Define ExpandedViewer Object, inherit from ImageViewer
     */
    var ExpandedViewer= function() {
        var windowClose= false;
        var fullControl= false;
        this.plot= function(params) {
            params= this.combineParams(params);
            firefly.plotAsExpanded(params,fullControl);
        };

        this.setWindowClose= function(close) {
            ffPrivate.setCloseButtonClosesWindow(close);
            windowClose= close;
        }
        this.setFullControl= function(full) {
            fullControl= full;
        }
    };
    ExpandedViewer.prototype= new ImageViewer();
    delete ExpandedViewer.prototype.setGroup;
    ExpandedViewer.prototype.plotURL= function(url) { this.plot(this.makeURLRequest(url)); };
    ExpandedViewer.prototype.plotFile= function(file) { this.plot(this.makeFileRequest(file)); };
    ExpandedViewer.prototype.plotFileOrURL= function(file,url) { this.plot(this.makeFileThenURLRequest(file,url)); };

//===================================================================================
//----------- External Viewer ------------------------------------------------------
//===================================================================================
    /**
     * Define ExpandedViewer Object, inherit from ImageViewer
     */
    var ExternalViewer= function() {

        var targetWindow= null;
        this.setTarget= function(target) {
           targetWindow= target;
        };
        this.plot= function(params) {
            params= this.combineParams(params);
            firefly.plotExternal(params,targetWindow);
        };
    };
    ExternalViewer.prototype= new ImageViewer();
    delete ExternalViewer.prototype.setGroup;
    ExternalViewer.prototype.plotURL= function(url) { this.plot(this.makeURLRequest(url)); };
    ExternalViewer.prototype.plotFile= function(file) { this.plot(this.makeFileRequest(file)); };
    ExternalViewer.prototype.plotFileOrURL= function(file,url) { this.plot(this.makeFileThenURLRequest(file,url)); };

//===================================================================================
//----------- Firefly Object methods -----------------------------------------------
//===================================================================================


    /**
     * Create a ImageViewer
     * @param div
     * @param group
     * @param div The div to put the ImageViewer in.
     * @param group - (optional) The plot group to associate this image viewer.  All ImageViewers with the same group name will
     */
    retFF.makeImageViewer= function(div,group) { return new ImageViewer(div,group); };
    retFF.setGlobalDefaultParams= function(params) { globalPlotParams= params; };

    /**
     *
     * @param div the div to put the tabPanel into
     * @param tpName the name of tabPanel
     */
    retFF.makeTabGroup= function(div, tpName) {
        ffPrivate.makeTabPanel(div,tpName);
    };


    var expandViewer= null;
    /**
     * Singleton factory method to get the ExpandedViewer
     */
    retFF.getExpandViewer= function() {
        if (!expandViewer) {
            expandViewer= new ExpandedViewer();
        }
        return expandViewer;
    };

    var externalViewer= null;
    /**
     * Singleton factory method to get the ExternalViewer
     */
    retFF.getExternalViewer= function() {
        if (!externalViewer) {
            externalViewer= new ExternalViewer();
        }
        return externalViewer;
    };



    retFF.addPrivate= function(property,func) {
        ffPrivate[property]= func;
    };


    var ALIVE = "Alive";
    var ALIVE_CHECK = "AliveCheck";



    retFF.enableDocListening= function(methodToCall,key) {

        var receive= function(ev) {
            if (ev.data==ALIVE_CHECK) {
                ev.source.postMessage(ALIVE,ev.origin);
            }
            else {
                methodToCall(key,ev.source,ev.data,ev.origin);
            }
        };
        window.addEventListener("message", receive,false);
    };


    retFF.postMessage= function(target, msg, origin) {
        target.postMessage(msg,origin);
    };


    return retFF;






}();




