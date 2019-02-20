package com.aziz.drive_it.DriveUtils.model;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DIFile {
    private String kind;
    @SerializedName("size")
    private Long size;
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("mimeType")
    private String mimeType;
    @SerializedName("description")
    private String description;
    private String modifiedTime;
    @SerializedName("parents")
    private List<String> parents = new ArrayList<>();
    private File file;

    public DIFile(String id) {
        this.id = id;
    }

    public DIFile() {

    }

    public void setFile(File file, String desc) {
        this.file = file;
        setName(file.getName());
        setDescription(desc);
        setMimeType("*/*"); //TODO set mime type
    }

    public void setFile(File file) {
        this.file = file;
        setName(file.getName());
        setMimeType("*/*"); //TODO set mime type
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }


    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "\n\nid " + getId() + "\n"
                + "name " + getName() + "\n"
                + "mDate " + getModifiedTime() + "\n"
                + "size " + getSize() + "\n"
                + "mime " + getMimeType();
    }

    public File getFile() {
        return file;
    }
}
