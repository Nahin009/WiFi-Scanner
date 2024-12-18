package com.example.myapplication;

import java.io.Serializable;

public class wifiNode implements Serializable {
    public String SSID;
    public int strength;
    public String macAddress;
    public int frequency;


    public wifiNode(){
    }

    public wifiNode(String SSID, int strength, String macAddress, int frequency) {
        this.SSID = SSID;
        this.strength = strength;
        this.macAddress = macAddress;
        this.frequency = frequency;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setBSSID(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

}
