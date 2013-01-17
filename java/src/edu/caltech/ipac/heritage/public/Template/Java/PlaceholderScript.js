window.addEvent('domready', function(){
   // for markup #1
   $$('input[placeholder]').each(function(el){
      new PlaceholderInput(el);
   });

   // for markup #2
   $$('label.placeholder').each(function(el){
      var target = $pick($(el.get('for')), el.getNext());
      if (target && (target.get('type') == 'text' || target.get('tag') == 'textarea')){
      	target.set('placeholder', el.get('text'));
      	el.destroy();
      	new PlaceholderInput(target);
			}
   });
   
   $('get_values_button').addEvent('click', function(){
     $('value_email').set('html', '"' + $('email').get('value') + '"');
     $('value_name').set('html', '"' + $('name').get('value') + '"');
   });
});
