package org.yetyman.controls.treeview;

import javafx.geometry.Insets;

public record CellLayoutConstraints(TreeItem<?> treeItem, double height, boolean min, double widthAvailable, boolean fillWidth,
                                    Insets margin) {
    public boolean match(TreeItem<?> treeItem, boolean min, double widthAvailable, boolean fillWidth, Insets margin) {
        return this.treeItem == treeItem && this.min == min && this.widthAvailable == widthAvailable && this.fillWidth == fillWidth && this.margin == margin;
    }
 }