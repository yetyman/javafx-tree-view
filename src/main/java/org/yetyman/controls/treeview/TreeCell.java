package org.yetyman.controls.treeview;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.yetyman.controls.PseudoClassHelper;

public abstract class TreeCell<T> extends Region {
    private CellLayoutConstraints cellLayoutConstraints;
    private TreeItem<T> treeItem;

    public TreeCell() {
        getStyleClass().add("-j-tree-cell");
    }

    private final transient ObjectProperty<Integer> indexClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "v-%d", null);
    private final transient ObjectProperty<Integer> depthClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "d-%d", null);
    private final transient ObjectProperty<Integer> childIndexClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "child-%d", null);
    private final transient BooleanProperty childLastClass = PseudoClassHelper.getPseudoClassProperty(this, "child-last", false);

    public void setContent(Node content) {
        if(!getChildren().isEmpty()) {
            getChildren().set(0, content);
        } else
        {
            getChildren().add(content);
        }
    }
    public Node getContent() {
        return getChildren().get(0);
    }

    @Override
    protected double computePrefHeight(double width) {
        return getContent().getBoundsInLocal().getHeight() + getPadding().getBottom() + getPadding().getTop();
    }

    @Override
    protected double computePrefWidth(double height) {
        return getContent().getBoundsInLocal().getWidth() + getPadding().getLeft() + getPadding().getRight();
    }

    public abstract void updateVisuals(T value, TreeItem<T> wrapper);

    void setCachedConstraints(CellLayoutConstraints cellLayoutConstraints) {
        this.cellLayoutConstraints = cellLayoutConstraints;
    };
    CellLayoutConstraints getCachedConstraints() { return cellLayoutConstraints; };

    void setTreeItem(TreeItem<T> treeItem) {
        this.treeItem = treeItem;

        updateIndexClass();
        updateDepthClass();
        updateChildIndexClass();
    }

    void updateChildIndexClass() {
        if(treeItem != null) {
            int index = treeItem.indexInParent();
            childIndexClass.set(index);

            childLastClass.set((index == getTreeItem().childrenProperty.size()-1));
        }
    }

    void updateDepthClass() {
        if(treeItem != null) {
            depthClass.set(treeItem.depthProperty.get());
        }
    }

    void updateIndexClass() {
        if(treeItem != null) {
            indexClass.set(treeItem.visibleIndexProperty.get());
        }
    }

    ;
    TreeItem<T> getTreeItem() { return treeItem; };

    T getValue() { return getTreeItem().valueProperty.get(); }

    Insets getMargins() {
        return TreeView.getMargin(this);
    }

    public double getCachedHeight() {
        if(getCachedConstraints() != null && getCachedConstraints().treeItem() == getTreeItem())
            return getCachedConstraints().height();
        else
            return 0;
    }

}
