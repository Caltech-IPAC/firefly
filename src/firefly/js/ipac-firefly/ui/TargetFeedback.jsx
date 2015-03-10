/*jshint browserify:true*/
"use strict";

var React= require('react/addons');

var TargetFeedback = module.exports= React.createClass(
{

  mixins : [React.addons.PureRenderMixin],


  render: function() {
      var topDivStyle= {
          paddingTop: "5px",
          position: "relative",
          height : "50px"
      };
      var exDivStyle= {
          display : 'inline-block'
      };
      var titleStyle= {
          display : 'inline-block',
          verticalAlign: 'top'
      };

      var retval;
      if (this.props.showHelp) {
          retval= (
             <div  style={topDivStyle}>
                 <div >
                     <div style={titleStyle}>
                         <i>Examples:</i>
                     </div>
                     <div style={exDivStyle}>
                         'm81'{"\u00a0"} {"\u00a0"} {"\u00a0"}  'ngc 13'{"\u00a0"} {"\u00a0"} {"\u00a0"} '12.34 34.89 {"\u00a0"} {"\u00a0"} {"\u00a0"} '46.53, -0.251 gal'
                         <br />
                         '19h17m32s 11d58m02s equ j2000'{"\u00a0"} {"\u00a0"} {"\u00a0"} '12.3, 8.5 b1950'
                     </div>
                 </div>
             </div>
          );
      }
      else {
          retval= (
             <div style={topDivStyle}>

                <span dangerouslySetInnerHTML={{
                    __html : this.props.feedback
                }}/>
             </div>
          );
      }
      return retval;
  }
});


//{this.props.feedback}
//</div>
