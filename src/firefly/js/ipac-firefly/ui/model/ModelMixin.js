/*jshint browserify:true*/


var modelMixin= function(model,evName,stateAry)  {
    "use strict";
   return {

       componentDidMount: function () {
           model.on(evName,this.updateState);
       },

       componentWillUnmount : function () {
           model.off(evName,this.updateState);
       },
       updateState : function() {
           //var newState= {};
           //stateAry.forEach(function(stateEntry) {
           //    newState[stateEntry]= model.get(stateEntry);
           //});
           //this.setState(newState);
           this.setState(model.changedAttributes());
       }
  };
};

module.exports= modelMixin;
