package org.yetyman.controls.treeview;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

public class TreeItem<T> {
    public final ObjectProperty<T> valueProperty = new SimpleObjectProperty<>(null);
    public final ObjectProperty<TreeItem<T>> parentProperty = new SimpleObjectProperty<>(null) {
        @Override
        public void set(TreeItem<T> newValue) {
            TreeItem<T> oldValue = super.get();
            super.set(newValue);
            parentChangedHandler(parentProperty, oldValue, newValue);
        }
    };
    public final SimpleListProperty<TreeItem<T>> childrenProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    public final BooleanProperty showChildrenProperty = new SimpleBooleanProperty(false) {
        @Override
        public void set(boolean newValue) {
            boolean oldValue = super.get();
            super.set(newValue);
            showChildrenChangedHandler(showChildrenProperty, oldValue, newValue);
        }
    };

    final ReadOnlyObjectWrapper<TreeCell<T>> settableCellProperty = new ReadOnlyObjectWrapper<>(null);
    public final ReadOnlyObjectProperty<TreeCell<T>> currentCellProperty = settableCellProperty.getReadOnlyProperty();

    final ReadOnlyObjectWrapper<TreeCaretNode<T>> settableCaretProperty = new ReadOnlyObjectWrapper<>(null);
    public final ReadOnlyObjectProperty<TreeCaretNode<T>> currentCaretProperty = settableCaretProperty.getReadOnlyProperty();

    final ReadOnlyListWrapper<TreeGuideNode<T>> settableChildGuidesProperty = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    public final ReadOnlyListProperty<TreeGuideNode<T>> currentChildGuidesProperty = settableChildGuidesProperty.getReadOnlyProperty();


    final ReadOnlyObjectWrapper<TreeGuideNode<T>> settableParentGuideProperty = new ReadOnlyObjectWrapper<>(null);
    public final ReadOnlyObjectProperty<TreeGuideNode<T>> currentParentGuideProperty = settableParentGuideProperty.getReadOnlyProperty();


    private boolean descCntDirty = true;

    private void markDescCntDirty(){
        descCntDirty = true;
        if(parentProperty.get() != null)
            parentProperty.get().markDescCntDirty();
    }
    public final ReadOnlyIntegerProperty descCountProperty = new ReadOnlyIntegerWrapper() {
        @Override
        public int get() {
            if(descCntDirty) {
                descCntDirty = false;
                int cnt = 0;
                for (TreeItem<T> c : childrenProperty) {
                    cnt += 1 + c.descCountProperty.get();
                }
                setValue(cnt);
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean visibleDescCntDirty = true;
    private void markVisibleDescCntDirty(){
        visibleDescCntDirty = true;
        if(parentProperty.get() != null)
            parentProperty.get().markVisibleDescCntDirty();
    }
    public final ReadOnlyIntegerProperty visibleDescCountProperty = new ReadOnlyIntegerWrapper() {
        @Override
        public int get() {
            if(visibleDescCntDirty) {
                visibleDescCntDirty = false;
                int cnt = 0;
                if(showChildrenProperty.get())
                    for (TreeItem<T> c : childrenProperty) {
                        cnt += 1 + c.visibleDescCountProperty.get();
                    }
                setValue(cnt);
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean visibleIndexDirty = true;
    private void markVisibleIndexDirty(){
        visibleIndexDirty = true;
        markNextVisibleItemsIndexDirty();
    }
    private void markNextVisibleItemsIndexDirty(){
        if(nextVisibleItemProperty.get() != null)
            nextVisibleItemProperty.get().markVisibleIndexDirty();
    }
    private void markChildrenVisibleIndexDirty() {
        for (TreeItem<T> tTreeItem : childrenProperty) {
            tTreeItem.markVisibleIndexDirty();
        }
    }
    public final ReadOnlyObjectProperty<Integer> visibleIndexProperty = new ReadOnlyObjectWrapper<Integer>() {
        @Override
        public Integer get() {
            if(visibleIndexDirty) {
                visibleIndexDirty = false;
                if (parentProperty.get()!=null) {
                    if(isVisibleProperty.get()) {
                        TreeItem<T> pre = previousVisibleItemProperty.get();
                        if (pre != null)
                            set(pre.visibleIndexProperty.get() + 1);
                        else
                            throw new RuntimeException("impossible.");
                    } else
                        set(null);
                } else {
                    set(0);
                }
                if(currentCellProperty.get()!=null && isVisibleProperty.get()) {
                    currentCellProperty.get().updateIndexClass();
                    currentCellProperty.get().updateChildIndexClass();
                }

            }
            return super.get();
        }
    }.getReadOnlyProperty();


    private boolean isVisibleDirty = true;
    private void markIsVisibleDirty(){
        isVisibleDirty = true;
        markChildrenIsVisibleDirty();
    }
    private void markChildrenIsVisibleDirty(){
        for (TreeItem<T> c : childrenProperty.get()) {
            c.markIsVisibleDirty();
        }
    }
    public final ReadOnlyBooleanProperty isVisibleProperty = new ReadOnlyBooleanWrapper() {
        @Override
        public boolean get() {
            if (isVisibleDirty) {
                isVisibleDirty = false;
                if (parentProperty.get()!=null) {
                    set(parentProperty.get().showChildrenProperty.get() && parentProperty.get().isVisibleProperty.get());
                } else
                    set(true);//root is always visible
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean deptDirty = true;
    private void markDepthDirty(){
        deptDirty = true;
        for (TreeItem<T> tTreeItem : childrenProperty.get()) {
            tTreeItem.markDepthDirty();
        }
    }
    public final ReadOnlyIntegerProperty depthProperty = new ReadOnlyIntegerWrapper() {
        @Override
        public int get() {
            if(deptDirty) {
                deptDirty = false;
                TreeItem<T> pre = parentProperty.get();
                if(pre != null)
                    set(pre.depthProperty.get()+1);
                else
                    set(0);

                if(currentCellProperty.get()!=null && isVisibleProperty.get())
                    currentCellProperty.get().updateDepthClass();
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean previousVisibleItemDirty = true;
    private void markPreviousVisibleItemDirty(){
        previousVisibleItemDirty = true;
    }
    public final ReadOnlyObjectProperty<TreeItem<T>> previousVisibleItemProperty = new ReadOnlyObjectWrapper<TreeItem<T>>() {
        @Override
        public TreeItem<T> get() {
            if (previousVisibleItemDirty) {
                previousVisibleItemDirty = false;
                if(parentProperty.get()==null)
                    setValue(null);
                else {
                    int cIndex = parentProperty.get().childrenProperty.indexOf(TreeItem.this);
                    if (cIndex == 0)
                        setValue(parentProperty.get());
                    else {
                        TreeItem<T> pre = parentProperty.get().childrenProperty.get(cIndex - 1);
                        setValue(pre.latestVisibleDescendantProperty.get());
                    }
                }
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean nextVisibleItemDirty = true;
    private void markNextVisibleItemDirty(){
        nextVisibleItemDirty = true;
        markNextVisibleItemExceptChildrenDirty();
    }
    public final ReadOnlyObjectProperty<TreeItem<T>> nextVisibleItemProperty = new ReadOnlyObjectWrapper<TreeItem<T>>() {
        @Override
        public TreeItem<T> get() {
            if (nextVisibleItemDirty) {
                nextVisibleItemDirty = false;
                TreeItem<T> old = get();
                if (showChildrenProperty.get() && !childrenProperty.isEmpty())
                    set(childrenProperty.get(0));
                else
                    set(nextVisibleItemExceptChildrenProperty.get());

                if (old != get() && get() != null) {
                    get().markNextVisibleItemDirty();//since the next visible item changed, the whole run may need to recalculate
                    if(old != null)
                        old.markNextVisibleItemDirty();
                }
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean nextVisibleItemExceptChildrenDirty = true;
    private void markNextVisibleItemExceptChildrenDirty(){
        nextVisibleItemExceptChildrenDirty = true;
        if(!childrenProperty.isEmpty() && showChildrenProperty.get()) {
            if(this != latestVisibleDescendantProperty.get())
                latestVisibleDescendantProperty.get().markNextVisibleItemDirty();
            childrenProperty.get(childrenProperty.size() - 1).markNextVisibleItemExceptChildrenDirty();
        }
    }
    public final ReadOnlyObjectProperty<TreeItem<T>> nextVisibleItemExceptChildrenProperty = new ReadOnlyObjectWrapper<TreeItem<T>>() {
        @Override
        public TreeItem<T> get() {
            if (nextVisibleItemExceptChildrenDirty) {
                nextVisibleItemExceptChildrenDirty = false;
                if (parentProperty.get()==null)
                    setValue(null);
                else {
                    int cIndex = parentProperty.get().childrenProperty.indexOf(TreeItem.this);
                    if (cIndex < parentProperty.get().childrenProperty.size()-1)
                        setValue(parentProperty.get().childrenProperty.get(cIndex+1));
                    else {
                        TreeItem<T> nex = parentProperty.get().nextVisibleItemExceptChildrenProperty.get();
                        setValue(nex);
                    }
                }
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean latestVisibleDescendantDirty = true;
    private void markLatestVisibleDescendantDirty(){
        latestVisibleDescendantDirty = true;
        markNextExceptChildrenPreviousDirty();
        if(parentProperty.get()!=null && parentProperty.get().showChildrenProperty.get())
            if (parentProperty.get().childrenProperty.get(parentProperty.get().childrenProperty.size()-1) == TreeItem.this)
                parentProperty.get().markLatestVisibleDescendantDirty();
    }
    private void markNextExceptChildrenPreviousDirty() {
        if(nextVisibleItemExceptChildrenProperty.get()!=null)
            nextVisibleItemExceptChildrenProperty.get().markPreviousVisibleItemDirty();
    }
    /**
     * if a tree item is collapsed, this will return its self. otherwise this is the latest descendent item of this element
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> latestVisibleDescendantProperty = new ReadOnlyObjectWrapper<TreeItem<T>>() {
        @Override
        public TreeItem<T> get() {
            if (latestVisibleDescendantDirty) {
                latestVisibleDescendantDirty = false;
                if (!showChildrenProperty.get())
                    setValue(TreeItem.this);
                else
                    setValue(childrenProperty.get(childrenProperty.size()-1).latestVisibleDescendantProperty.get());
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    private boolean latestLogicalDescendantDirty = true;
    private void markLatestLogicalDescendantDirty(){
        latestLogicalDescendantDirty = true;
        if(parentProperty.get()!=null)
            if (parentProperty.get().childrenProperty.get(parentProperty.get().childrenProperty.size()-1) == TreeItem.this)
                parentProperty.get().markLatestLogicalDescendantDirty();
    }
    /**
     * if a tree item has no children, this will return its self. otherwise this is the latest descendent item of this element
     */
    public final ReadOnlyObjectProperty<TreeItem<T>> latestLogicalDescendantProperty = new ReadOnlyObjectWrapper<TreeItem<T>>() {
        @Override
        public TreeItem<T> get() {
            if (latestLogicalDescendantDirty) {
                latestLogicalDescendantDirty = false;
                if (childrenProperty.isEmpty())
                    setValue(TreeItem.this);
                else
                    setValue(childrenProperty.get(childrenProperty.size()-1).latestLogicalDescendantProperty.get());
            }
            return super.get();
        }
    }.getReadOnlyProperty();

    public TreeItem(T someValue) {
        this();
        valueProperty.set(someValue);
    }
    public TreeItem() {
        childrenProperty.get().addListener(this::childrenChangedHandler);
    }

    private void childrenChangedHandler(ListChangeListener.Change<? extends TreeItem<T>> c) {
        childrenProperty.get();
        boolean firstChanged = false;
        boolean lastChanged = false;
        while(c.next()) {
            if (c.wasRemoved()) {
                for (TreeItem<T> removedChild : c.getRemoved()) {
                    if (removedChild.parentProperty.get() == this)
                        removedChild.parentProperty.set(null);
                }
                if(c.getFrom() == 0)
                    firstChanged = true;
                if(c.getTo() == c.getList().size())
                    lastChanged = true;
            }
            if (c.wasAdded()) {
                for (TreeItem<T> addedChild : c.getAddedSubList()) {
                    if (addedChild.parentProperty.get() != this)
                        addedChild.parentProperty.set(this);
                }
                if(c.getFrom() == 0)
                    firstChanged = true;
                if(c.getTo() == c.getList().size())
                    lastChanged = true;
            }
        }

        markDescCntDirty();
        markVisibleDescCntDirty();
        if (firstChanged && showChildrenProperty.get())
            markNextVisibleItemDirty();
        if (lastChanged) {
            markLatestVisibleDescendantDirty();
            markLatestLogicalDescendantDirty();
            markNextExceptChildrenPreviousDirty();
        }
        requestParentLayout();
    }

    private void requestParentLayout(){
        if(currentCellProperty.get()!=null) {
            currentCellProperty.get().requestLayout();
            if(currentCellProperty.get().getParent()!=null)
                currentCellProperty.get().getParent().requestLayout();
        }
    }
    private void showChildrenChangedHandler(ObservableValue<? extends Boolean> s, Boolean a, Boolean b) {
        showChildrenProperty.get();//non lazy
        markNextExceptChildrenPreviousDirty();
        markNextVisibleItemExceptChildrenDirty();
        markChildrenIsVisibleDirty();
        markChildrenVisibleIndexDirty();
        markVisibleDescCntDirty();
        markNextVisibleItemDirty();

        markLatestVisibleDescendantDirty();
        requestParentLayout();
    }

    private void parentChangedHandler(ObservableValue<? extends TreeItem<T>> source, TreeItem<T> a, TreeItem<T> b) {
        parentProperty.get();//non lazy
        if (a != b) {
            if (a != null) {
                a.childrenProperty.remove(this);

                a.markDescCntDirty();
                a.markVisibleDescCntDirty();
                a.markDepthDirty();

            }
            if (b != null) {
                if (!b.childrenProperty.contains(this))
                    b.childrenProperty.add(this);

                b.markDescCntDirty();
                b.markVisibleDescCntDirty();
                b.markDepthDirty();

            }
            markVisibleIndexDirty();
            markIsVisibleDirty();
            if(indexInParent() == 0)
                markPreviousVisibleItemDirty();
            markNextVisibleItemDirty();
            markNextVisibleItemExceptChildrenDirty();
            requestParentLayout();
        }
    }


    public int indexInParent() {
        if(parentProperty.get()==null)
            return -1;
        else
            return parentProperty.get().childrenProperty.indexOf(TreeItem.this);
    }

    @Override
    public String toString() {
        if(valueProperty.get()!=null)
            return valueProperty.get().toString();
        else
            return super.toString();
    }

    public TreeItem<T> findVisibleItemAtIndex(int visibleIndex) {
        Integer thisIndex = visibleIndexProperty.get();
        if (thisIndex == null) {
            //if index is null, we necessarily have a parent
            return parentProperty.get().findVisibleItemAtIndex(visibleIndex);
        }
        else if (thisIndex > visibleIndex) {
            if (parentProperty.get()!=null && parentProperty.get().visibleIndexProperty.get() > visibleIndex)
                return parentProperty.get().findVisibleItemAtIndex(visibleIndex);
            else
                return previousVisibleItemProperty.get().findVisibleItemAtIndex(visibleIndex);
        }
        else if (thisIndex < visibleIndex && nextVisibleItemProperty.get() != null) {
            return nextVisibleItemProperty.get().findVisibleItemAtIndex(visibleIndex);
        }
        else
            return this;
    }

    public boolean isLastChild() {
        return indexInParent() == parentProperty.get().childrenProperty.getSize()-1;
    }

    public boolean isRoot() {
        return parentProperty.get() == null;
    }
}
