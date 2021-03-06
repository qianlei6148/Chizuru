package com.chigix.resserver.interfaces.xml;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class XPathNode {

    private final String localName;

    private String contentText = null;

    private XPathNode parent = null;

    HashMap<String, ArrayList<XPathNode>> localNameMapping = new HashMap<>();

    public String getSimplePath() {
        if (parent == null) {
            return "/" + this.localName;
        } else {
            return parent.getSimplePath() + "/" + this.localName;
        }
    }

    public XPathNode(String localName) {
        this.localName = localName;
    }

    public XPathNode(String localName, XPathNode parent) {
        this.localName = localName;
        this.parent = parent;
    }

    public void appendChild(XPathNode node) {
        localNameMapping.putIfAbsent(localName, new ArrayList<>());
        localNameMapping.get(localName).add(node);
        node.parent = this;
    }

    public XPathNode getParent() {
        return parent;
    }

    public String getLocalName() {
        return localName;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentText() {
        return contentText;
    }

}
