package edu.caltech.ipac.firefly.resbundle.css;

import com.google.gwt.resources.client.CssResource;

public interface FireflyCss extends CssResource {
    String bodyColor();
    String borderColor();
    String markedColor();
    String highlightColor();
    String alternateColor();
    String tabsColor();
    String popupBackgroundColor();
    String titleBackgroundColor();
    String selectedColor();
    String whiteColor();
    String blackColor();
    String alertColor();

    @ClassName("popup-background")
    String popupBackground();


    @ClassName("standard-border")
    String standardBorder();

    @ClassName("table-row-back-alt1")
    String tableRowBackAlt1();

    @ClassName("table-row-back-alt2")
    String tableRowBackAlt2();


    @ClassName("highlight-text")
    String highlightText();

    @ClassName("marked-text")
    String markedText();

    @ClassName("rotate-left")
    String rotateLeft();

    @ClassName("faded-text")
    String fadedText();

    @ClassName("title-bg-color")
    String titleBgColor();

    @ClassName("title-color")
    String titleColor();

    @ClassName("title-font-family")
    String titleFontFamily();

    @ClassName("normal-text")
    String normalText();

    @ClassName("global-settings")
    String globalSettings();
}

