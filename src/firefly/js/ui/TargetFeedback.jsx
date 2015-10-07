import React from 'react/addons';

var TargetFeedback = React.createClass(
{

  mixins : [React.addons.PureRenderMixin],


  render: function() {
      var topDivStyle= {
          paddingTop: '5px',
          position: 'relative',
          height : '50px'
      };
      var exDivStyle= {
          display : 'inline-block'
      };
      var titleStyle= {
          display : 'inline-block',
          verticalAlign: 'top'
      };
      var spanPad= {
          paddingLeft : '15px'
      };

      var retval;
      if (this.props.showHelp) {
          //var makeSpan= function(w) {
          //    return  <span style={{paddingLeft: w+"px"}}/>
          //}
          var makeSpan= w => {return  <span style={{paddingLeft: w+'px'}}/>; };
          var spanObj= <span style={spanPad}/>;
          retval= (
             <div  style={topDivStyle}>
                 <div >
                     <div style={titleStyle}>
                         <i>Examples: </i>
                     </div>
                     <div style={exDivStyle}>
                         {makeSpan(5)} 'm81' {makeSpan(15)} 'ngc 13' {makeSpan(15)}  '12.34 34.89'  {makeSpan(15)} '46.53, -0.251 gal'
                         <br />
                         {makeSpan(5)}'19h17m32s 11d58m02s equ j2000' {makeSpan(5)}  '12.3, 8.5 b1950'
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

export default TargetFeedback;

//{this.props.feedback}
//</div>
