package org.yetyman.controls.treeview;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import org.yetyman.controls.PseudoClassHelper;

import java.util.List;

public abstract class TreeGuideNode<T> extends Region {
    private CellLayoutConstraints cellLayoutConstraints;
    private TreeItem<T> colTreeItem;
    private int indexUnderItem;
    private int depth;
    private int visibleIndex;
    private final ReadOnlyListWrapper<OrthoDirection> currentDirectionsInternal = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    public final ReadOnlyListProperty<OrthoDirection> currentDirections = currentDirectionsInternal.getReadOnlyProperty();
    private TreeItem<T> rowTreeItem;

    private final transient ObjectProperty<Integer> indexClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "v-%d", null);
    private final transient ObjectProperty<Integer> depthClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "d-%d", null);
    private final transient ObjectProperty<Integer> childIndexClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "child-%d", null);
    private final transient BooleanProperty childLastClass = PseudoClassHelper.getPseudoClassProperty(this, "child-last", false);

    public TreeGuideNode(){
        getStyleClass().add("-j-tree-guide-node");
    }
    void showDirectionsBase(T value, List<OrthoDirection> directions, TreeItem<T> parentItem, TreeItem<T> rowItem) {
        if(!directions.equals(currentDirectionsInternal))
            currentDirectionsInternal.setAll(directions);
        showDirections(value, directions, parentItem, rowItem);
    }
    public abstract void showDirections(T value, List<OrthoDirection> directions, TreeItem<T> parentItem, TreeItem<T> rowItem);

    void setCachedConstraints(CellLayoutConstraints cellLayoutConstraints) {
        this.cellLayoutConstraints = cellLayoutConstraints;
    }
    CellLayoutConstraints getCachedConstraints() { return cellLayoutConstraints; }

    void setColumnTreeItem(TreeItem<T> treeItem) { this.colTreeItem = treeItem; }
    TreeItem<T> getColumnTreeItem() { return colTreeItem; }

    public void setRowTreeItem(TreeItem<T> rowTreeItem) {
        this.rowTreeItem = rowTreeItem;
    }
    public TreeItem<T> getRowTreeItem() {
        return rowTreeItem;
    }

    T getValue() { return getColumnTreeItem().valueProperty.get(); }

    Insets getMargins() {
        return TreeView.getMargin(this);
    }

    public double getCachedHeight() {
        if(getCachedConstraints() != null && getCachedConstraints().treeItem() == getColumnTreeItem())
            return getCachedConstraints().height();
        else
            return 0;
    }

    void setIndexUnderParent(int indexUnderItem, boolean isLastGuideUnderItem) {
        this.indexUnderItem = indexUnderItem;

        childIndexClass.set(indexUnderItem);

        childLastClass.set(isLastGuideUnderItem);

    }
    public int getIndexUnderItem() {
        return indexUnderItem;
    }

    void setDepth(int depth) {
        this.depth = depth;
        depthClass.set(depth);
    }

    public int getDepth() {
        return depth;
    }

    public void setVisibleIndex(int visibleIndex) {
        this.visibleIndex = visibleIndex;

        indexClass.set(visibleIndex);
    }

    public int getVisibleIndex() {
        return visibleIndex;
    }

    @Override
    public String toString() {
        List<OrthoDirection> curr = currentDirections.get();
        if(curr == null)
            return " ";
        else if(curr.equals(TreeView.rootGuideLines))
            return "╶";
        else if(curr.equals(TreeView.straightGuideLines))
            return "│";
        else if(curr.equals(TreeView.normalGuideLines))
            return "├";
        else if(curr.equals(TreeView.lastChildGuideLines))
            return "└";
        else
            return " ";
    }
}
