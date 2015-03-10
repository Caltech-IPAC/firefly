/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/

"use strict";
var React= require('react/addons');



var InputGroup = module.exports= React.createClass(
   {

       mixins : [React.addons.PureRenderMixin],

       propTypes: {
           labelWidth   : React.PropTypes.string.isRequired
       },

       render: function() {
           /*jshint ignore:start */
           var lWidth= this.props.labelWidth;
           return (
                   <div>
                     {React.Children.map(this.props.children,function(inChild,idx) {
                         return React.addons.cloneWithProps(inChild, {labelWidth: lWidth});
                     })}
                   </div>

           );
           /*jshint ignore:end */
       }


   });


