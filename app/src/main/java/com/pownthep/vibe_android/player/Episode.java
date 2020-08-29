package com.pownthep.vibe_android.player;

public class Episode {
    private String name;
    private String id;
    private String size;

    public String getBytes() {
        return bytes;
    }

    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    private String bytes;
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSize() {
        return size;
    }
    public void setSize(String size) {
        this.size = size;
    }
}
