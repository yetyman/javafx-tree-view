package org.yetyman.controls.treeview;

import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import org.yetyman.controls.PseudoClassHelper;

public abstract class TreeCaretNode<T> extends Region {
    private CellLayoutConstraints cellLayoutConstraints;
    private TreeItem<T> treeItem;
    private int depth;
    private int visibleIndex;

    private final transient ObjectProperty<Integer> indexClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "v-%d", null);
    private final transient ObjectProperty<Integer> depthClass = PseudoClassHelper.getPseudoClassObjectProperty(this, "d-%d", null);

    //TODO: plan to expand this to be bound to the same value in the tree item at all times it is bound to one
    private final ReadOnlyBooleanWrapper disclosureInternal = new ReadOnlyBooleanWrapper(false);
    public final ReadOnlyBooleanProperty disclosure = disclosureInternal.getReadOnlyProperty();
    public TreeCaretNode(){
        getStyleClass().add("-j-tree-caret-node");


        setOnMouseClicked(me->{//easily overridden by changing the setOnMouseClick event.
            if(isVisible() && treeItem != null)
                treeItem.showChildrenProperty.set(!treeItem.showChildrenProperty.get());
        });
        disclosure.addListener((s,a,b)->{
            if(b)
                getStyleClass().add("expanded");
            else
                getStyleClass().remove("expanded");
        });
    }

    void updateDisclosureBase(T value, boolean showChildren, TreeItem<T> wrapper, TreeCell<T> pairedVisual) {
        disclosureInternal.set(showChildren);
        updateDisclosure(value, showChildren, wrapper, pairedVisual);
    }
    public abstract void updateDisclosure(T value, boolean showChildren, TreeItem<T> wrapper, TreeCell<T> pairedVisual);

    void setCachedConstraints(CellLayoutConstraints cellLayoutConstraints) {
        this.cellLayoutConstraints = cellLayoutConstraints;
    };
    CellLayoutConstraints getCachedConstraints() { return cellLayoutConstraints; };

    void setTreeItem(TreeItem<T> treeItem) { this.treeItem = treeItem; };
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
}
