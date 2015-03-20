/*jshint browserify:true*/
"use strict";
//-----------------
var Promise= require("es6-promise").Promise;
var PositionParser= require("ipac-firefly/util/PositionParser");
var PositionFieldDef= require("ipac-firefly/data/form/PositionFieldDef");
var Point= require("ipac-firefly/visualize/Point");
var http= require('http');
var AmpersandState = require('ampersand-state');
var _= require("underscore");





module.exports= AmpersandState.extend(
{
    props: {
        wpt :         {type : 'object', default : null},
        targetInput : {type : 'string', default : ''},
        showHelp :    {type : 'boolean',default : true},
        feedback :    {type : 'string', default : ''},
        valid    :    {type : 'boolean', default : true},
        resolving :    {type : 'boolean', default : false},
        inputType :   {type : 'string', default : PositionParser.PositionParsedInput.Position}
    },

    posFieldDef: PositionFieldDef.makePositionFieldDef(),
    abortLastRequest : function() {},

    initialize : function() {
        this.set("targetInput", "");
        this.set("WorldPt", null);
        this.set("feedback", "");

    },


    doParse : function() {

        var wpt= null;
        var valid= false;
        var targetInput= this.get("targetInput");
        var feedback= "valid: false";
        //var update= true;
        var showHelp= true;
        if (targetInput) {
            this.abortLastRequest();
            try {
                valid= this.posFieldDef.validateSoft(targetInput);
            } catch (e) {
                valid= false;
            }
            if (valid) {
                wpt= this.posFieldDef.getPosition();
                if (this.posFieldDef.getInputType()===PositionParser.PositionParsedInput.Position) {
                    showHelp= false;
                    feedback= this.posFieldDef.formatPosForHelp(wpt);
                }
                else {
                    if (this.posFieldDef.getObjectName()) {
                        showHelp= false;
                        feedback= "<i>Resolving:</i> " + this.posFieldDef.getObjectName();
                        //update= false;
                        this.resolve(this.posFieldDef.getObjectName());
                    }
                    else {
                        showHelp= true;
                    }
                }
            }
        }
        //if (update) {
            this.set({showHelp : showHelp,
                      feedback : feedback,
                      inputType: this.posFieldDef.getInputType(),
                      valid   : valid,
                      resolving : false,
                      wpt       : wpt});
        //}
    },




    resolve : _.debounce(function(objName) {
        if (!objName) {
            return;
        }
        this.abortLastRequest();
        var model= this;

        this.set({showHelp : false,
                     feedback : "<i>Resolving:</i> " + objName,
                     inputType: PositionParser.PositionParsedInput.Name,
                     valid   : true,
                     resolving : true,
                     wpt       : null});


        this.makeResolverPromise(objName).then(function(results){
            if (results) {
                if (results[0].success === "true") {
                    var wpt = Point.parseWorldPt(results[0].data);
                    model.set({feedback: model.posFieldDef.formatTargetForHelp(wpt),
                               showHelp : false,
                               valid : true,
                               resolving : false,
                               wpt: wpt} );
                }
                else {
                    model.set({feedback: "Could not resolve: " + objName,
                               showHelp : false,
                               valid : false,
                               resolving : false,
                               wpt : null} );
                }
            }
        }).catch(function(val) {
            //console.log("aborted: "+objName);
        });

    },200),


    makeResolverPromise : function(objName) {
        var url= '/fftools/sticky/CmdSrv?objName='+objName+'&resolver=nedthensimbad&cmd=CmdResolveName';
        var workerPromise= new Promise(function(resolve, reject) {
            http.get({ path : url }, function (res) {
                res.on('data', function (buf) {
                    resolve(buf);
                });

                res.on('end', function () {
                });
            }.bind(this));

        }).then(function(buf) {
                    return JSON.parse(buf);
                });

        var abortPromise= new Promise(function(resolve,reject) {
            this.abortLastRequest= function() {
                reject();
            };
        }.bind(this));

        return Promise.race([workerPromise,abortPromise]);
    }

}
);
