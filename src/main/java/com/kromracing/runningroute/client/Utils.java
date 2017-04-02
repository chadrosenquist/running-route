package com.kromracing.runningroute.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

final public class Utils {
    private Utils() {
        
    }
    
    /**
     * Sets the HTML id for a widget.
     * @param widget The widget to have the id set, ex: TextBox
     * @param id ID in HTML, ex: textbox-location
     */
    static void setId(final Widget widget, final String id) {   
        if (widget instanceof CheckBox) {
            final Element checkBoxElement = widget.getElement();
            // The first element is the actual box to check.  That is the one we care about.
            final Element inputElement = DOM.getChild(checkBoxElement, 0);
            inputElement.setAttribute("id", id);
            //DOM.setElementAttribute(inputElement, "id", id);  deprecated!
        }
        else {
        	widget.getElement().setAttribute("id", id);
            //DOM.setElementAttribute(widget.getElement(), "id", id);  deprecated!
        }
    }
}
