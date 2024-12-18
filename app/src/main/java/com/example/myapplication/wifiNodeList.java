package com.example.myapplication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class wifiNodeList implements Serializable {
    private HashMap<Integer, wifiNode> nodeList;

    public wifiNodeList() {
    }

    public wifiNodeList(HashMap<Integer, wifiNode> nodeList) {
        this.nodeList = nodeList;
    }

    public HashMap<Integer, wifiNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(HashMap<Integer, wifiNode> nodeList) {
        this.nodeList = nodeList;
    }

    public void addNode(wifiNode node) {
        this.nodeList.put(this.nodeList.size(), node);
    }
}
