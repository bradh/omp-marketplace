define([
    'jquery',
    'underscore',
    'backbone'

], function($, _, Backbone){

    var SuperClass = Backbone.Model;

    return SuperClass.extend({
        url: function() {
            return (Marketplace.context + '/public/types/' + this.id);
        }
    });

});
