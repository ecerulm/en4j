/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.NoteContentViewModule;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.extend.NamespaceHandler;

/**
 *
 * @author ecerulm
 */
class ENMLNamespaceHandler implements NamespaceHandler {
    private final NamespaceHandler delegate;

    public ENMLNamespaceHandler(NamespaceHandler h) {
        this.delegate = h;
    }

    public boolean isImageElement(Element e) {
        return delegate.isImageElement(e);
    }

    public boolean isFormElement(Element e) {
        return delegate.isFormElement(e);
    }

    public StylesheetInfo[] getStylesheets(Document doc) {
        return delegate.getStylesheets(doc);
    }

    public String getNonCssStyling(Element e) {
        String toReturn = delegate.getNonCssStyling(e);
        if ("en-media".equalsIgnoreCase(e.getNodeName())) {
            toReturn = "display: block;";
        }
        Logger.getLogger(ENMLNamespaceHandler.class.getName()).info("style for ("+e.getNodeName()+") is ("+toReturn+")");
        return toReturn;
    }

    public String getNamespace() {
        return delegate.getNamespace();
    }

    public String getLinkUri(Element e) {
        return delegate.getLinkUri(e);
    }

    public String getLang(Element e) {
        return delegate.getLang(e);
    }

    public String getImageSourceURI(Element e) {
        return delegate.getImageSourceURI(e);
    }

    public String getID(Element e) {
        return delegate.getID(e);
    }

    public String getElementStyling(Element e) {
        return delegate.getElementStyling(e);
    }

    public String getDocumentTitle(Document doc) {
        return delegate.getDocumentTitle(doc);
    }

    public StylesheetInfo getDefaultStylesheet(StylesheetFactory factory) {
        return delegate.getDefaultStylesheet(factory);
    }

    public String getClass(Element e) {
        return delegate.getClass(e);
    }

    public String getAttributeValue(Element e, String namespaceURI, String attrName) {
        return delegate.getAttributeValue(e, namespaceURI, attrName);
    }

    public String getAttributeValue(Element e, String attrName) {
        return delegate.getAttributeValue(e, attrName);
    }

    public String getAnchorName(Element e) {
        return delegate.getAnchorName(e);
    }
    
}
