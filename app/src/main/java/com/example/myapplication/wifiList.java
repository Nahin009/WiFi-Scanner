package com.example.myapplication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class wifiList implements Serializable {
    private String device_model;
    private String formattedDateTime;
    private String studentID;
    public HashMap<Integer, wifiNodeList> listOfWifiList;

    // Required default constructor
    public wifiList() {
    }

    public wifiList(String device_model, String formattedDateTime, String studentId, HashMap<Integer, wifiNodeList> listOfWifiList) {
        this.device_model = device_model;
        this.formattedDateTime = formattedDateTime;
        this.listOfWifiList = listOfWifiList;
        this.studentID = studentId;
    }

    public String getDeviceModel() {
        return device_model;
    }

    public void setDeviceModel(String device_model) {
        this.device_model = device_model;
    }

    public String getFormattedDateTime() {
        return formattedDateTime;
    }

    public void setFormattedDateTime(String formattedDateTime) {
        this.formattedDateTime = formattedDateTime;
    }

    public HashMap<Integer, wifiNodeList> getlistOfWifiList() {
        return listOfWifiList;
    }

    public void setlistOfWifiList(HashMap<Integer, wifiNodeList> listOfWifiList) {
        this.listOfWifiList = listOfWifiList;
    }

    public String getStudentID() {
        return studentID;
    }

    public void setStudentID(String studentID) {
        this.studentID = studentID;
    }
}
