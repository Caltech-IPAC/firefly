package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation panel that shows links on one side and deck panel on the other, which
 * shows the content, associated with the appropriate link.
 * <h3>CSS Style Rules<h3>
 * <ul>
 * <li>.link-list {list of links}
 * <li>.link { link }
 * <li>.used-link { currently viewed link }
 * <li>.view-area {right view panel}
 * </ul>
 * @author tatianag
 * @version $Id: LinksPanel.java,v 1.8 2011/10/20 18:33:46 loi Exp $
 */
public class LinksPanel extends Composite {

    private ULList lst = new ULList();
    private List<ListItem> links = new ArrayList<ListItem>();
    private List<String> ids = new ArrayList<String>();
    private DeckPanel deckPanel = new DeckPanel();
    private ListItem current = null;

    public LinksPanel() {
        HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
        initWidget(mainPanel);
        //lst.setStyleName("link-list");
        lst.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) {
                selectLink(lst.getLastClicked());
            }

        });
        
        //linkPanel.add(lst);
        deckPanel.setStyleName("content-panel");

        //mainPanel.add(linkPanel);
        mainPanel.add(lst);
        mainPanel.setSplitPosition("25%");
        mainPanel.add(deckPanel);
//        mainPanel.setStyleName("links-panel");
    }

    public void addLink(String id, String linkText, Widget widget) {
        ListItem li = new ListItem(id, linkText);
//        li.setStyleName("link");
        DOM.setStyleAttribute(li.getElement(), "margin", "10px");
        links.add(li);
        ids.add(id);
        lst.add(li);

        deckPanel.add(widget);
    }

    public void selectLink(String id) {
        int idx = 0;
        for (String s : ids) {
            if (s.equals(id)) {
                selectLink(idx);
            }
            idx++;
        }
    }
        
    public void selectLink(int idx) {
        selectLink(links.get(idx));    
    }

    public String getCurrentId() {
        if (current != null) return current.getId();
        else return null;
    }

    private void selectLink(ListItem li) {
        int idx = 0;
        for (ListItem l : links) {
            if (li == l) {
                if (current != null) {
//                    current.setStyleName("link");
                }
                if (current!=null) current.setCurrent(false);
                current = l;
                current.setCurrent(true);

//                current.setStyleName("used-link");
                current.addStyleName("highlight-text");
                deckPanel.showWidget(idx);
                deckPanel.setVisible(true);
                return;
            }
            idx++;
        }
    }

    public static class ULList extends ComplexPanel implements HasClickHandlers {

        private ListItem lastClicked= null;

        public ULList() {
            this(null);
        }

        public ULList(String title) {
            setElement(DOM.createElement("ul"));
            if (!StringUtils.isEmpty(title)) {
                getElement().setInnerHTML("<b>" + title + "</b>");
            }
        }

        @Override
        public void add(Widget w) {
            super.add(w, getElement());
        }

        public void insert(Widget w, int beforeIndex) {
            super.insert(w, getElement(), beforeIndex, true);
        }

        void dispatchClick(ListItem which, ClickEvent ev) {
            lastClicked= which;
            fireEvent(ev);
        }

        public HandlerRegistration addClickHandler(ClickHandler h) {
            return addHandler(h,ClickEvent.getType());
        }

        public ListItem getLastClicked() { return lastClicked; }
    }




    public static class ListItem extends Widget implements HasText {
        Element div;
        boolean current= false;

        public ListItem(String id, String text) {
            setElement(DOM.createElement("li"));
            div = DOM.createDiv();
            ListItem.this.addStyleName("linkTypeButton");
            ListItem.this.addStyleName("highlight-text");
            DOM.appendChild(getElement(), div);

            this.addDomHandler(new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    ULList list = (ULList) getParent();
                    if (list != null) list.dispatchClick(ListItem.this,ev);
                }
            }, ClickEvent.getType());

            this.addDomHandler(new MouseOverHandler(){
                public void onMouseOver(MouseOverEvent event) {
                    if (!current) {
                        ListItem.this.removeStyleName("highlight-text");
                        ListItem.this.addStyleName("marked-text");
                        DOM.setStyleAttribute(ListItem.this.getElement(), "fontWeight", "normal");
                    }
                }
            }, MouseOverEvent.getType());

            this.addDomHandler(new MouseOutHandler(){
                public void onMouseOut(MouseOutEvent event) {
                    if (!current) {
                        ListItem.this.removeStyleName("marked-text");
                        ListItem.this.addStyleName("highlight-text");
                        DOM.setStyleAttribute(ListItem.this.getElement(), "fontWeight", "normal");
                    }
                }

            }, MouseOutEvent.getType());






            setId(id);
            setText(text);
        }


        public void setCurrent(boolean current)  {
            this.current= current;
            if (current) {
                removeStyleName("marked-text");
                addStyleName("highlight-text");
                DOM.setStyleAttribute(ListItem.this.getElement(), "fontWeight", "bold");
            }
            else {
                removeStyleName("marked-text");
                addStyleName("highlight-text");
                DOM.setStyleAttribute(ListItem.this.getElement(), "fontWeight", "normal");
            }
        }

        public void setId(String id) {
            DOM.setElementAttribute(div, "id", id == null ? "" : id);
        }

        public String getId() {
            return DOM.getElementAttribute(div, "id");
        }

        public String getText() {
            return DOM.getInnerText(div);
        }

        public void setText(String text) {
            DOM.setInnerText(div, text == null ? "" : text);
        }

//        @Override
//        public void onBrowserEvent(Event event) {
//            if (DOM.eventGetType(event) == Event.ONCLICK) {
//                ULList list = (ULList) getParent();
//                if (list != null) {
//                    list.dispatchClick(this);
//                }
//            }
//            super.onBrowserEvent(event);
//        }
    }
}
