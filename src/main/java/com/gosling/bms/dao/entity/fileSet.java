package com.gosling.bms.dao.entity;

import java.util.ArrayList;

public class fileSet {
    public fileSet() {

    }

    @Override
    public String toString() {
        return "file{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", fileDate='" + fileDate + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", lngmax='" + lngmax + '\'' +
                ", latmax='" + latmax + '\'' +
                ", lngmin='" + lngmin + '\'' +
                ", latmin='" + latmin + '\'' +
                '}';
    }

    public fileSet(ArrayList<String> id, String fileName, ArrayList<String> path, String fileDate, Float fileSize, String fileType, Float lngmax, Float latmax, Float lngmin, Float latmin) {
        this.id = id;
        this.fileName = fileName;
        this.path = path;
        this.fileDate = fileDate;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.lngmax = lngmax;
        this.latmax = latmax;
        this.lngmin = lngmin;
        this.latmin = latmin;
    }

    private ArrayList<String> id;
    private String fileName;
    private ArrayList<String> path;
    private String fileDate;
    private Float fileSize;
    private String fileType;
    private Float lngmax;
    private Float latmax;
    private Float lngmin;
    private Float latmin;

    public ArrayList<String> getId() {
        return id;
    }

    public void setId(ArrayList<String> id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ArrayList<String> getPath() {
        return path;
    }

    public void setPath(ArrayList<String> path) {
        this.path = path;
    }

    public String getFileDate() {
        return fileDate;
    }

    public void setFileDate(String fileDate) {
        this.fileDate = fileDate;
    }

    public Float getFileSize() {
        return fileSize;
    }

    public void setFileSize(Float fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Float getLngmax() {
        return lngmax;
    }

    public void setLngmax(Float lngmax) {
        this.lngmax = lngmax;
    }

    public Float getLatmax() {
        return latmax;
    }

    public void setLatmax(Float latmax) {
        this.latmax = latmax;
    }

    public Float getLngmin() {
        return lngmin;
    }

    public void setLngmin(Float lngmin) {
        this.lngmin = lngmin;
    }

    public Float getLatmin() {
        return latmin;
    }

    public void setLatmin(Float latmin) {
        this.latmin = latmin;
    }
}
