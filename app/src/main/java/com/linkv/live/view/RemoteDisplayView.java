package com.linkv.live.view;

import com.linkv.rtc.render.LVDisplayView;

public class RemoteDisplayView {

    private LVDisplayView LVDisplayView;
    private int width;
    private int height;

    public LVDisplayView getCMDisplayView() {
        return LVDisplayView;
    }

    public RemoteDisplayView setCMDisplayView(LVDisplayView LVDisplayView) {
        this.LVDisplayView = LVDisplayView;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public RemoteDisplayView setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public RemoteDisplayView setHeight(int height) {
        this.height = height;
        return this;
    }

}
