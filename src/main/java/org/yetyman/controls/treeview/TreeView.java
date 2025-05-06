package org.yetyman.controls.treeview;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import structures.directions.OrthoDirection;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class TreeView<T> extends Pane {
    private static final Logger log = LoggerFactory.getLogger(TreeView.class);
    public static final List<OrthoDirection> rootGuideLines = List.of(OrthoDirection.RIGHT);//why not
    public static final List<OrthoDirection> normalGuideLines = List.of(OrthoDirection.UP, OrthoDirection.RIGHT, OrthoDirection.DOWN);
    public static final List<OrthoDirection> lastChildGuideLines = List.of(OrthoDirection.UP, OrthoDirection.RIGHT);
    public static final List<OrthoDirection> straightGuideLines = List.of(OrthoDirection.UP, OrthoDirection.DOWN);
    private static final boolean DEBUG = false;

    private final SimpleObjectProperty<TreeItem<T>> scrollCenterItem = new SimpleObjectProperty<>(null);
    private final SimpleObjectProperty<TreeItem<T>> rootItemProperty = new SimpleObjectProperty<>(null);

    private final List<TreeCell<T>> allocatedCells = new ArrayList<>();
    private final List<TreeCaretNode<T>> allocatedCarets = new ArrayList<>();
    private final Set<TreeGuideNode<T>> usedGuides = new HashSet<>();
    private final Stack<TreeGuideNode<T>> guidePool = new Stack<>();

    private final ReadOnlyListWrapper<TreeItem<T>> modifiableVisibleItems = new ReadOnlyListWrapper<>(FXCollections.observableList(new ArrayList<>()));
    public final ReadOnlyListProperty<TreeItem<T>> visibleItems = modifiableVisibleItems.getReadOnlyProperty();
    //offset of the scroll center item in px from middle
    private final SimpleDoubleProperty centerItemScrollOffset = new SimpleDoubleProperty(0.0);

    public final SimpleDoubleProperty minDefaultCellWidth = new SimpleDoubleProperty(USE_COMPUTED_SIZE);
    public final SimpleDoubleProperty prefDefaultCellWidth = new SimpleDoubleProperty(USE_COMPUTED_SIZE);
    public final SimpleDoubleProperty maxDefaultCellWidth = new SimpleDoubleProperty(USE_COMPUTED_SIZE);
    public final SimpleDoubleProperty minDefaultCellHeight = new SimpleDoubleProperty(USE_COMPUTED_SIZE);
    public final SimpleDoubleProperty prefDefaultCellHeight = new SimpleDoubleProperty(USE_COMPUTED_SIZE);
    public final SimpleDoubleProperty maxDefaultCellHeight = new SimpleDoubleProperty(USE_COMPUTED_SIZE);

    public final SimpleDoubleProperty defaultCaretWidth = new SimpleDoubleProperty(12);
    public final SimpleDoubleProperty defaultGuideWidth = new SimpleDoubleProperty(12);

    private final Rectangle layoutBoundsRect1 = new Rectangle();
    private final Rectangle layoutBoundsRect2 = new Rectangle();
    private final Rectangle layoutBoundsRect3 = new Rectangle();

    private final Pane cellParent = new Pane() {
        @Override
        public void requestLayout() {
            TreeView.this.requestLayout();
        }
    };
    private final Pane guideParent = new Pane() {
        @Override
        public void requestLayout() {
            TreeView.this.requestLayout();
        }
    };
//    private final Pane caretParent = new Pane() {
//        @Override
//        public void requestLayout() {
//            org.yetyman.controls.treeview.treeview.TreeView.this.requestLayout();
//        }
//    };

    private double scrollTweenFrom;
    private double scrollTweenToTimeNano;
    private long tweenTransitionTimeNano;

    private Function<TreeView<T>, TreeCell<T>> cellFactory = this::defaultCellFactory;
    private Function<TreeView<T>, TreeCaretNode<T>> caretFactory = this::defaultCaretFactory;
    private Function<TreeView<T>, TreeGuideNode<T>> guideFactory = this::defaultGuideFactory;

    /**
     * This enables a feature for hovered cells to be shown at full size regardless of the tree view's clip.
     * I made this feature after seeing a similar the clever feature in Jetbrains Intellij. If you've never noticed it,
     * make a list thin enough to cut something off and hover the element. It's quite nice.
     * <br/>
     * My implementation isn't perfect yet, if you scroll while hovering it may still show for a frame out of view.
     */
    private final BooleanProperty expandHoveredCell = new SimpleBooleanProperty(true);
    private final ObjectProperty<TreeCell<T>> hoveredCell = new SimpleObjectProperty<>(null);
    private TreeCell<T> hoverCopyCell;
    private final BooleanProperty hoveredCellHovered = new SimpleBooleanProperty(false);

    //#region reproduce internals
    private static final double EPSILON = 1e-14;

    private static double getSnapScaleX(Node n) {
        return _getSnapScaleXImpl(n.getScene());
    }
    private static double _getSnapScaleXImpl(Scene scene) {
        if (scene == null) return 1.0;
        Window window = scene.getWindow();
        if (window == null) return 1.0;
        return window.getRenderScaleX();
    }

    private static double getSnapScaleY(Node n) {
        return _getSnapScaleYImpl(n.getScene());
    }
    private static double _getSnapScaleYImpl(Scene scene) {
        if (scene == null) return 1.0;
        Window window = scene.getWindow();
        if (window == null) return 1.0;
        return window.getRenderScaleY();
    }

    private static final String MARGIN = "treeview-margin";
    /**
     * Returns the caret's margin constraint if set.
     * @param child the caret node of a border pane
     * @return the margin for the caret or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN);
    }
    public static void setMargin(Node child, Insets margin) {
        setConstraint(child, MARGIN, margin);
    }
    static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            node.getProperties().remove(key);
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }
    static Object getConstraint(Node node, Object key) {
        if (node.hasProperties()) {
            return node.getProperties().get(key);
        }
        return null;
    }

    private double getSnapScaleX() {
        return _getSnapScaleXImpl(getScene());
    }

    private double getSnapScaleY() {
        return _getSnapScaleYImpl(getScene());
    }

    /**
     * If snapToPixel is true, then the treeItem is rounded using FastMath.round. Otherwise,
     * the treeItem is simply returned. This method will surely be JIT'd under normal
     * circumstances, however on an interpreter it would be better to inline this
     * method. However the use of FastMath.round here, and FastMath.ceil in snapSize is
     * not obvious, and so for code maintenance this logic is pulled out into
     * a separate method.
     *
     * @param value The treeItem that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return treeItem either as passed in or rounded based on snapToPixel
     */
    private double snapSpaceX(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleX()) : value;
    }
    private double snapSpaceY(double value, boolean snapToPixel) {
        return snapToPixel ? scaledRound(value, getSnapScaleY()) : value;
    }

    private static double snapSpace(double value, boolean snapToPixel, double snapScale) {
        return snapToPixel ? scaledRound(value, snapScale) : value;
    }

    /**
     * If snapToPixel is true, then the treeItem is ceil'd using FastMath.ceil. Otherwise,
     * the treeItem is simply returned.
     *
     * @param value The treeItem that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return treeItem either as passed in or ceil'd based on snapToPixel
     */
    private double snapSizeX(double value, boolean snapToPixel) {
        return snapToPixel ? scaledCeil(value, getSnapScaleX()) : value;
    }
    private double snapSizeY(double value, boolean snapToPixel) {
        return snapToPixel ? scaledCeil(value, getSnapScaleY()) : value;
    }

    private static double snapSize(double value, boolean snapToPixel, double snapScale) {
        return snapToPixel ? scaledCeil(value, snapScale) : value;
    }


    private static double scaledRound(double value, double scale) {
        return FastMath.round(value * scale) / scale;
    }

    /**
     * The treeItem is floored for a given scale using FastMath.floor.
     * This method guarantees that:
     *
     * scaledFloor(scaledFloor(treeItem, scale), scale) == scaledFloor(treeItem, scale)
     *
     * @param value The treeItem that needs to be floored
     * @param scale The scale that will be used
     * @return treeItem floored with scale
     */
    private static double scaledFloor(double value, double scale) {
        return FastMath.floor(value * scale + EPSILON) / scale;
    }

    /**
     * The treeItem is ceiled with a given scale using FastMath.ceil.
     * This method guarantees that:
     *
     * scaledCeil(scaledCeil(treeItem, scale), scale) == scaledCeil(treeItem, scale)
     *
     * @param value The treeItem that needs to be ceiled
     * @param scale The scale that will be used
     * @return treeItem ceiled with scale
     */
    private static double scaledCeil(double value, double scale) {
        return FastMath.ceil(value * scale - EPSILON) / scale;
    }
    static double computeYOffset(double height, double contentHeight, VPos vpos) {
        return switch (vpos) {
            case BASELINE, TOP -> 0;
            case CENTER -> (height - contentHeight) / 2;
            case BOTTOM -> height - contentHeight;
            default -> throw new AssertionError("Unhandled vPos");
        };
    }

    /**
     * Computes the treeItem based on the given min and max values. We encode in this
     * method the logic surrounding various edge cases, such as when the min is
     * specified as greater than the max, or the max less than the min, or a pref
     * treeItem that exceeds either the max or min in their extremes.
     * <p/>
     * If the min is greater than the max, then we want to make sure the returned
     * treeItem is the min. In other words, in such a case, the min becomes the only
     * acceptable return treeItem.
     * <p/>
     * If the min and max values are well ordered, and the pref is less than the min
     * then the min is returned. Likewise, if the values are well ordered and the
     * pref is greater than the max, then the max is returned. If the pref lies
     * between the min and the max, then the pref is returned.
     *
     *
     * @param min The minimum bound
     * @param pref The treeItem to be clamped between the min and max
     * @param max the maximum bound
     * @return the size bounded by min, pref, and max.
     */
    static double boundedSize(double min, double pref, double max) {
        double a = FastMath.max(pref, min);
        double b = FastMath.max(min, max);
        return FastMath.min(a, b);
    }

    double computeChildMinAreaHeight(TreeCell<T> child, double minBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top =margin != null? snapSpaceY(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null? snapSpaceX(margin.getLeft(), snap) : 0;
            double right = margin != null? snapSpaceX(margin.getRight(), snap) : 0;
            alt = snapSizeX(width != -1? boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1)) :
                    child.maxWidth(-1));
        }

        // For explanation, see computeChildPrefAreaHeight
        if (minBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapSizeY(child.minHeight(alt)) + bottom
                        + minBaselineComplement;
            } else {
                return baseline + minBaselineComplement;
            }
        } else {
            return top + snapSizeY(child.minHeight(alt)) + bottom;
        }
    }

    double computeChildPrefAreaHeight(TreeCell<T> child, double prefBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpaceY(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null ? snapSpaceX(margin.getLeft(), snap) : 0;
            double right = margin != null ? snapSpaceX(margin.getRight(), snap) : 0;
            alt = snapSizeX(boundedSize(
                    child.minWidth(-1), width != -1 ? width - left - right
                            : child.prefWidth(-1), child.maxWidth(-1)));
        }

        if (prefBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // When baseline is same as height, the preferred height of the node will be above the baseline, so we need to add
                // the preferred complement to it
                return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom
                        + prefBaselineComplement;
            } else {
                // For all other Nodes, it's just their baseline and the complement.
                // Note that the complement already contain the Node's preferred (or fixed) height
                return top + baseline + prefBaselineComplement + bottom;
            }
        } else {
            return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
        }
    }
    //#endregion reproduce internals

    /**
     * Whether resizable children will be resized to fill the full width of the org.yetyman.controls.treeview.treeview.TreeView
     * or be resized to their preferred width and aligned according to the <code>alignment</code>
     * hpos treeItem.
     */
    public final BooleanProperty fillWidth = new SimpleBooleanProperty(true);
    public void setFillWidth(boolean value) { fillWidth.set(value); }
    public boolean isFillWidth() { return fillWidth == null || fillWidth.get(); }

    public ScrollBar verticalScrollBar = new ScrollBar();
    public final SimpleDoubleProperty verticalScrollIndex = new SimpleDoubleProperty(0) {
        @Override
        public double get() {
            double d = 0;
            if(scrollCenterItem.get() != null && scrollCenterItem.get().currentCellProperty.get()!=null)
                d = scrollCenterItem.get().visibleIndexProperty.get() + centerItemScrollOffset.get()/scrollCenterItem.get().currentCellProperty.get().getCachedHeight();
            if(super.get() != d)
                super.set(d);
            return d;
        }

        @Override
        public void set(double newValue) {
            newValue/=verticalScrollBar.getMax();
            newValue*=(verticalScrollBar.getMax()-1);
            newValue+=.5;
            int index = (int) newValue;
            double offset = (newValue)%1d-.5;

            if(scrollCenterItem.get() != null) {
                scrollCenterItem.set(scrollCenterItem.get().findVisibleItemAtIndex(index));
                TreeCell<T> cell = getTreeCell(scrollCenterItem.get());
                sizeSingleCell(cell);
//                log.info("{}i, {}offset", scrollCenterItem.get().visibleIndexProperty.get(), offset);
                centerItemScrollOffset.set(-offset * scrollCenterItem.get().currentCellProperty.get().getCachedHeight());
            }

            super.set(newValue);
            super.get();
        }
    };


    public TreeView(){
        getStyleClass().add("-j-tree-view");

        verticalScrollBar.setOrientation(Orientation.VERTICAL);
        verticalScrollBar.setMinSize(USE_COMPUTED_SIZE, 0);
        verticalScrollBar.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        verticalScrollBar.setMaxSize(USE_COMPUTED_SIZE, Double.MAX_VALUE);
        verticalScrollBar.prefHeightProperty().bind(heightProperty());
        verticalScrollBar.setUnitIncrement(1);
        verticalScrollBar.setBlockIncrement(1);

        verticalScrollBar.valueProperty().bindBidirectional(verticalScrollIndex);

        var rootWhenExists = rootItemProperty.when(rootItemProperty.isNotNull());

        verticalScrollBar.visibleAmountProperty().bind(Bindings.createDoubleBinding(this::calculateVScrollBarVisibleAmount, visibleItems));
        verticalScrollBar.maxProperty().bind(Bindings.createDoubleBinding(()-> calculateVScrollBarMax(rootWhenExists), rootWhenExists, visibleItems));

        cellParent.setManaged(false);
//        caretParent.setManaged(false);
        guideParent.setManaged(false);
        guideParent.setMouseTransparent(true);
        getChildren().add(guideParent);
        getChildren().add(cellParent);
//        getChildren().add(caretParent);

        getChildren().add(verticalScrollBar);
        visibleItems.addListener((_, _, _)->{ visibleItems.get(); verticalScrollBar.maxProperty().get(); });

        AtomicBoolean nestingProtect = new AtomicBoolean(false);
        addEventFilter(ScrollEvent.ANY, evt->{
            if(!verticalScrollBar.contains(verticalScrollBar.sceneToLocal(evt.getSceneX(), evt.getSceneY())) && !nestingProtect.get()) {
                nestingProtect.set(true);
                verticalScrollBar.fireEvent(evt);
                nestingProtect.set(false);
            }
        });

        if(!DEBUG) {
            cellParent.setClip(layoutBoundsRect1);
//            caretParent.setClip(layoutBoundsRect2);
            guideParent.setClip(layoutBoundsRect3);
        }

        scrollCenterItem.addListener((s,a,b)->{ scrollCenterItem.get(); if(!Objects.equals(a, b)) requestLayout(); });
        centerItemScrollOffset.addListener((s,a,b)->{ centerItemScrollOffset.get(); if(!Objects.equals(a, b)) requestLayout(); });
    }

    private static <T> double calculateVScrollBarMax(ObservableValue<TreeItem<T>> rootWhenExists) {
        if(rootWhenExists.getValue() != null)
//                return rootWhenExists.getValue().visibleDescCountProperty.getValue()+1;
            return (double) rootWhenExists.getValue().visibleDescCountProperty.getValue() + 1d;
//                return rootWhenExists.getValue().visibleDescCountProperty.getValue()-visibleItems.getSize()+1;
        else
            return 0d;
    }

    private double calculateVScrollBarVisibleAmount() {
        double size = visibleItems.size();
        if(visibleItems.isEmpty())
            return 1;
        TreeItem<T> first = visibleItems.getFirst();
        TreeItem<T> last = visibleItems.getLast();
        double top = snapSpaceY(getInsets().getTop());
        double bottom = snapSpaceY(getInsets().getBottom());
        double contentTop = top;
        double contentBot = getHeight()-bottom;
        size -= (contentTop - first.currentCellProperty.get().getBoundsInParent().getMinY())/first.currentCellProperty.get().getCachedHeight();
        size -= (last.currentCellProperty.get().getBoundsInParent().getMaxY() - contentBot) /last.currentCellProperty.get().getCachedHeight();
        return size;
    }


    private void sizeSingleCell(TreeCell<T> cell) {
        double indentWidth = defaultGuideWidth.get();
        double depthIndent = cell.getTreeItem().depthProperty.get() * indentWidth;
        double width = getWidth();
        Insets insets = getInsets();
        double left = snapSpaceX(insets.getLeft());
        double right = snapSpaceX(insets.getRight());
        double contentWidth = width - left - right;
        double availableWidth = getAvailableWidth(isFillWidth(), contentWidth, depthIndent);
        double scrollWidth = verticalScrollBar.isVisible() ? verticalScrollBar.getWidth() : 0;
        Insets margin = cell.getMargins();
        if (cell.getCachedConstraints() == null || !cell.getCachedConstraints().match(cell.getTreeItem(), false, availableWidth-scrollWidth, isFillWidth(), margin))
            updateCachedCellConstraints(cell, availableWidth-scrollWidth, false, isFillWidth());
    }

    public void scrollUp() {
        scrollPx(scrollCenterItem.get().currentCellProperty.get().getCachedHeight());
    }
    public void scrollDown() {
        scrollPx(-scrollCenterItem.get().currentCellProperty.get().getCachedHeight());
    }
    public void scrollPx(double scrollDistancePx) {
        double scrollOffsetPx = centerItemScrollOffset.get() + scrollDistancePx;
        double maxOffsetPx = scrollCenterItem.get().currentCellProperty.get().getCachedHeight()/2;

        ItemAndOffset<T> newItemAndOffset = calculateCenterItemChange(scrollOffsetPx, maxOffsetPx);

        calculateTweenFromLocation(scrollDistancePx, newItemAndOffset.offset, maxOffsetPx);
        scrollCenterItem.set(newItemAndOffset.willBe);
        centerItemScrollOffset.set(newItemAndOffset.offset);

        verticalScrollIndex.get();
        updateModel();
        requestLayout();
    }

    private record ItemAndOffset<X>(TreeItem<X> willBe, double offset) {}

    /**
     *
     * @param scrollOffsetPx
     * @param maxOffsetPx
     * @return remaining offset in px
     */
    private ItemAndOffset<T> calculateCenterItemChange(double scrollOffsetPx, double maxOffsetPx) {
        //scroll offset is based in the center of the center item
        TreeItem<T> nextScrollCenter = scrollCenterItem.get();
        //TODO: this is where we could add logic to prevent scrolling past the edges.
        // We can calculate the min/max item and offset to line up with the ends of what's visible

        if(scrollOffsetPx > maxOffsetPx && nextScrollCenter.previousVisibleItemProperty.get() != null) {
            nextScrollCenter = nextScrollCenter.previousVisibleItemProperty.get();
            while (scrollOffsetPx > maxOffsetPx && nextScrollCenter.previousVisibleItemProperty.get() != null) {
                nextScrollCenter = nextScrollCenter.previousVisibleItemProperty.get();
                if(nextScrollCenter.currentCellProperty.get() == null)
                    nextScrollCenter.settableCellProperty.set(getTreeCell(nextScrollCenter));
                double deviation = maxOffsetPx + nextScrollCenter.currentCellProperty.get().getCachedHeight() / 2;
                maxOffsetPx = nextScrollCenter.currentCellProperty.get().getCachedHeight();
                scrollOffsetPx -= deviation;
            }
        }
        else if(scrollOffsetPx < -maxOffsetPx && nextScrollCenter.nextVisibleItemProperty.get() != null) {
            nextScrollCenter = nextScrollCenter.nextVisibleItemProperty.get();
            while (scrollOffsetPx < -maxOffsetPx && nextScrollCenter.nextVisibleItemProperty.get() != null) {
                nextScrollCenter = nextScrollCenter.nextVisibleItemProperty.get();
                if(nextScrollCenter.currentCellProperty.get() == null)
                    nextScrollCenter.settableCellProperty.set(getTreeCell(nextScrollCenter));
                double deviation = maxOffsetPx + nextScrollCenter.currentCellProperty.get().getCachedHeight() / 2;
                maxOffsetPx = nextScrollCenter.currentCellProperty.get().getCachedHeight();
                scrollOffsetPx += deviation;
            }
        }

        //if we are at the end of the list
        if(scrollOffsetPx > maxOffsetPx)
            scrollOffsetPx = maxOffsetPx;
        else if(scrollOffsetPx < -maxOffsetPx)
            scrollOffsetPx = -maxOffsetPx;

        return new ItemAndOffset<T>(nextScrollCenter, scrollOffsetPx);
    }

    private void calculateTweenFromLocation(double scrollDistance, double scrollOffset, double maxOffset) {

        if (scrollOffset > maxOffset || scrollOffset < -maxOffset) {
            //fake scroll at end of list???
            double fakeOffset = FastMath.max(-maxOffset, FastMath.min(maxOffset, scrollOffset/2));
            scrollTweenFrom = fakeOffset;
            scrollTweenToTimeNano = System.nanoTime() + tweenTransitionTimeNano;
        } else {
            //normal scroll
            scrollTweenFrom = scrollOffset - scrollDistance;
            scrollTweenToTimeNano = System.nanoTime() + tweenTransitionTimeNano;
        }
    }

    private void updateModel() {
        //is there even a model to update? the visible set relies entirely on each caret's layout bounds.
    }


    private void updateCachedCellConstraints(TreeCell<T> child, double widthAvailable, boolean minimum, boolean isFillWidth) {
        Insets margin = child.getMargins();

        double height;
        if (minimum) {
            if (widthAvailable != USE_COMPUTED_SIZE && isFillWidth) {
                height = computeChildMinAreaHeight(child, -1, margin, widthAvailable);
            } else {
                height = computeChildMinAreaHeight(child, -1, margin, -1);
            }
        } else {
            if (widthAvailable != USE_COMPUTED_SIZE && isFillWidth) {
                height = computeChildPrefAreaHeight(child, -1, margin, widthAvailable);
            } else {
                height = computeChildPrefAreaHeight(child, -1, margin, -1);
            }
        }

        child.setCachedConstraints(new CellLayoutConstraints(child.getTreeItem(), height, minimum, widthAvailable, isFillWidth, margin));
    }

    private record CellLayoutSettings<T>(TreeCell<T> cell, TreeItem<T> item, double areaX, double areaY,
                                      double areaWidth, double areaHeight,
                                      double areaBaselineOffset,
                                      Insets margin, boolean fillWidth, boolean fillHeight,
                                      HPos halignment, VPos valignment){
        @Override
        public String toString() {
            return cell.toString();
        }
    }
    private record CaretLayoutSettings<T>(TreeCaretNode<T> caret, double areaX, double areaY,
                                       double areaWidth, double areaHeight,
                                       double areaBaselineOffset,
                                       Insets margin, boolean fillWidth, boolean fillHeight,
                                       HPos halignment, VPos valignment){}
    private record GuideLayoutSettings<T>(TreeGuideNode<T> guide, double areaX, double areaY,
                                       double areaWidth, double areaHeight,
                                       double areaBaselineOffset,
                                       Insets margin, boolean fillWidth, boolean fillHeight,
                                       HPos halignment, VPos valignment){
        @Override
        public String toString() {
            return guide.toString();
        }
    }

    public int visualDistanceFrom(TreeItem<T> from, TreeItem<T> to) {
        return to.visibleIndexProperty.get() - from.visibleIndexProperty.get();
    }

    public TreeItem<T> itemAtVisualDistance(TreeItem<T> start, int visualDistance) {
        int destIndex = start.visibleIndexProperty.get() + visualDistance;
        return start.findVisibleItemAtIndex(destIndex);
    }

    private int previousScrollCenterItemVisibleIndex = -1;

    @Override
    protected void layoutChildren() {
        if(scrollCenterItem.get() == null)
            return;

        //reassess scroll center item when collapsed parents have changed it.
        if(!scrollCenterItem.get().isVisibleProperty.get())
        {
            TreeItem<T> newScrollCenter = scrollCenterItem.get().findVisibleItemAtIndex(previousScrollCenterItemVisibleIndex);
            //ideally i would love to figure out the distance difference as well, but that requires lengths up to the parent items
            scrollCenterItem.set(newScrollCenter);
        }
        previousScrollCenterItemVisibleIndex = scrollCenterItem.get().visibleIndexProperty.get();

        TreeItem<T> earlyBound = null;
        TreeItem<T> laterBound = null;
        double topOfLaidCells = Double.POSITIVE_INFINITY;
        double botOfLaidCells = Double.NEGATIVE_INFINITY;
        List<TreeCell<T>> laidCells = new LinkedList<>();
        List<TreeItem<T>> laidItems = new LinkedList<>();
        List<TreeCaretNode<T>> laidCarets = new LinkedList<>();
        Set<TreeGuideNode<T>> laidGuides = new HashSet<>();

        double caretWidth = defaultCaretWidth.get();
        double guideWidth = defaultGuideWidth.get();

        Insets insets = getInsets();
        double width = getWidth();
        double height = getHeight();

        double topPad = snapSpaceY(insets.getTop());
        double bottomPad = snapSpaceY(insets.getBottom());
        double leftPad = snapSpaceX(insets.getLeft());
        double rightPad = snapSpaceX(insets.getRight());

        cellParent.resizeRelocate(0, 0, width, height);
//        caretParent.resizeRelocate(0, 0, width, height);
        guideParent.resizeRelocate(0, 0, width, height);
        layoutBoundsRect1.setWidth(width);
        layoutBoundsRect1.setHeight(height);
        layoutBoundsRect2.setWidth(width);
        layoutBoundsRect2.setHeight(height);
        layoutBoundsRect3.setWidth(width);
        layoutBoundsRect3.setHeight(height);

        double contentAvailableHeight = height - topPad - bottomPad;
        TreeItem<T> centerItem = scrollCenterItem.get();
        double middleY = contentAvailableHeight * centerItem.visibleIndexProperty.get()/(double)(rootItemProperty.get().visibleDescCountProperty.get())+topPad-centerItemScrollOffset.get();
        middleY = snapSpaceY(middleY, true);

        // width could be -1

        final boolean isFillWidth = isFillWidth();

        List<CellLayoutSettings<T>> cellLayouts = new LinkedList<>();
        List<CaretLayoutSettings<T>> caretLayouts = new LinkedList<>();
        List<GuideLayoutSettings<T>[]> guideLayoutGrid = new ArrayList<>();

        //we aren't using it yet, but for efficiency we could cache early and later bound
        CellLayoutPass<T> result = doCellLayoutPass(earlyBound, laterBound, centerItem, topPad, height, bottomPad, middleY, leftPad, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets, topOfLaidCells, botOfLaidCells);

        //region lay out guides and carets
        int startIndex = result.earlyBound().visibleIndexProperty.get();
        int endIndex = result.laterBound().visibleIndexProperty.get();

        @SuppressWarnings("unchecked")
        TreeGuideNode<T>[][] guideGrid = new TreeGuideNode[endIndex-startIndex+1][result.maxVisibleDepth];
        @SuppressWarnings("unchecked")
        TreeCaretNode<T>[][] caretGrid = new TreeCaretNode[endIndex-startIndex+1][result.maxVisibleDepth];

        //region take care of guides from invisible parent items.
        TreeItem<T> p0 = result.earlyBound().parentProperty.get();
        while(p0 != null) {
            List<TreeGuideNode<T>> guidesInColumn = getTreeGuideNodesColumn(p0, startIndex, endIndex);
            laidGuides.addAll(guidesInColumn);

            for (TreeGuideNode<T> guideNode : guidesInColumn) {
                if (guideNode != null)
                    guideGrid[guideNode.getVisibleIndex() - startIndex][guideNode.getDepth()] = guideNode;
            }

            p0 = p0.parentProperty.get();
        }
        //endregion take care of guides from invisible parent items.

        //region take care of visible cell's guides
        TreeItem<T> c0 = result.earlyBound();
        do {
            List<TreeGuideNode<T>> guidesInColumn = getTreeGuideNodesColumn(c0, startIndex, endIndex);
            laidGuides.addAll(guidesInColumn);

            for (TreeGuideNode<T> guideNode : guidesInColumn) {
                if (guideNode != null)
                    guideGrid[guideNode.getVisibleIndex() - startIndex][guideNode.getDepth()] = guideNode;
            }

            c0 = c0.nextVisibleItemProperty.get();

        } while (c0 != result.laterBound() && c0 != null);
        //endregion take care of visible cell's guides

        //region guides were just acquired by column, but are sized according to row and column. assign their layout settings here
        for (CellLayoutSettings<T> rowItemLayout : cellLayouts) {
            TreeItem<T> rowItem = rowItemLayout.item;

            int atIndex = rowItem.visibleIndexProperty.get()-startIndex;
            int depth = rowItem.depthProperty.get();

            @SuppressWarnings("unchecked")
            GuideLayoutSettings<T>[] row = new GuideLayoutSettings[depth];
            guideLayoutGrid.add(row);

            for (int d = 0; d < depth; d++) {

                TreeGuideNode<T> guide = guideGrid[atIndex][d];

                if (guide == null)
                    continue;

                guide.setRowTreeItem(rowItem);

                //fill margin space with carets and guides. don't leave the cell's margins open
                Insets margin = rowItem.currentCellProperty.get().getMargins();
                double b = 0;// margin == null ? 0 : margin.getBottom();
                double t = 0;// margin == null ? 0 : margin.getTop();

                rowItem.settableParentGuideProperty.set(guide);

                TreeItem<T> colItem = guide.getColumnTreeItem();//if we ever decide the parent item has something to do with guide size?
                row[d] = new GuideLayoutSettings<>(guide, leftPad + guideWidth * (d), rowItemLayout.areaY - t, guideWidth, rowItemLayout.areaHeight + t + b, rowItemLayout.areaBaselineOffset + t + b, guide.getMargins(), true, true, HPos.CENTER, VPos.CENTER);
            }
        }
        //endregion guides were just acquired by column, but are sized according to row and column. assign their layout settings here

        setUnusedCellsFromUsedCells(laidCells);
        setUnusedCaretsFromUsedCarets(laidCarets);
        setUnusedGuidesFromUsedGuides(laidGuides);

        //region layout cells and carets according to their settings
        double firstHeight = cellLayouts.getFirst().areaHeight();
        double firstOff = centerItemScrollOffset.get();
        CellLayoutSettings<T> hoveredLayoutSettings = null;
        for (CellLayoutSettings<T> l : cellLayouts) {
            if(expandHoveredCell.get()) {
                if (l.cell == hoveredCell.get()) {
                    hoveredLayoutSettings = l;
                    l.cell.setVisible(false);
                } else {
                    l.cell.setVisible(true);
                }
            }
            layoutInArea(l.cell, l.areaX, l.areaY+firstOff, l.areaWidth, l.areaHeight, l.areaBaselineOffset, l.margin, l.fillWidth, l.fillHeight, l.halignment, l.valignment);
        }
        for (CaretLayoutSettings<T> l : caretLayouts) {
            layoutInArea(l.caret, l.areaX, l.areaY+firstOff, l.areaWidth, l.areaHeight, l.areaBaselineOffset, l.margin, l.fillWidth, l.fillHeight, l.halignment, l.valignment);
        }

        for (GuideLayoutSettings<T>[] ls : guideLayoutGrid) {
            for (GuideLayoutSettings<T> l : ls) {
                if (l == null)
                    continue;
//                l.guide.resizeRelocate(l.areaX, l.areaY+firstOff, l.areaWidth, l.areaHeight);
                layoutInArea(l.guide, l.areaX, l.areaY+firstOff, l.areaWidth, l.areaHeight, l.areaBaselineOffset, l.margin, l.fillWidth, l.fillHeight, l.halignment, l.valignment);
            }
        }


        //region offer this slick hover effect that intellij has where hovering a row reveals the whole row even outside the parent
        if (expandHoveredCell.get()) {
            if(hoverCopyCell == null) {
                hoverCopyCell = cellFactory.apply(this);
                hoverCopyCell.setManaged(false);
                hoverCopyCell.setVisible(false);
                hoverCopyCell.setOnMouseEntered(_-> {
                    hoveredCellHovered.set(true);
                });
                hoverCopyCell.setOnMouseExited(_-> {
                    hoveredCellHovered.set(false);
                    hoverCopyCell.setVisible(false);
                    hoveredCell.set(null);
                    requestLayout();
                });
//                hoverCopyCell.setViewOrder(-1);
                getChildren().add(hoverCopyCell);
            }

            if (hoveredLayoutSettings != null) {
                TreeCell<T> c = hoveredCell.get();
                TreeItem<T> item = c.getTreeItem();
                hoverCopyCell.setTreeItem(item);
                hoverCopyCell.updateVisuals(item.valueProperty.get(), item);
//                hoverCopyCell.applyCss();
                CellLayoutSettings<T> l = hoveredLayoutSettings;
                layoutInArea(hoverCopyCell, l.areaX, l.areaY+firstOff, l.areaWidth, l.areaHeight, l.areaBaselineOffset, l.margin, l.fillWidth, l.fillHeight, l.halignment, l.valignment);
                hoverCopyCell.setVisible(true);
            }
            else {
                hoverCopyCell.setTreeItem(null);
                hoverCopyCell.setVisible(false);
            }
        }

        //endregion
        //endregion


        //region adjust scroll bar location.
        double scrollBarEnd = rootItemProperty.get().latestVisibleDescendantProperty.get().visibleIndexProperty.get();
        double scrollBarLocation = scrollCenterItem.get().visibleIndexProperty.get();
        double scrollBarPortion = laidCells.size() / scrollBarEnd;

        double scrollBarWidth = verticalScrollBar.prefWidth(height);
        layoutInArea(verticalScrollBar, width-scrollBarWidth, 0, scrollBarWidth, height, 0, HPos.RIGHT, VPos.CENTER);
        modifiableVisibleItems.setAll(laidItems);
        verticalScrollIndex.get();
        verticalScrollBar.maxProperty().get();
        //endregion adjust scroll bar location

//        log.info("{}", scrollCenterItem.get());
//        printGrid(guideLayoutGrid);
//        log.info("{}items, {}cells, {}getChVis, {}getChildren", laidItems.size(), laidCells.size(), getChildren().stream().filter(Node::isVisible).count(), getChildren().size());

        applyCss();//force layout to avoid flickering at the top and bottom on fast scrolling
        //not sure why we're still getting hover expand flicker when it should always be in the "correct" spot

        super.layoutChildren();
    }

    private CellLayoutPass<T> doCellLayoutPass(TreeItem<T> earlyBound, TreeItem<T> laterBound, TreeItem<T> centerItem, double topPad, double height, double bottomPad, double middleY, double leftPad, boolean isFillWidth, List<CellLayoutSettings<T>> cellLayouts, List<CaretLayoutSettings<T>> caretLayouts, List<TreeCell<T>> laidCells, List<TreeItem<T>> laidItems, List<TreeCaretNode<T>> laidCarets, double topOfLaidCells, double botOfLaidCells) {

        //when carets close a branch, the tree view's central cell could still be in the closed branch. current cell needs to change after collapse and expand
        // when a node is collapsed or expanded, the new index needs to be calculated starting from that changed node down.
        // basically we need the visual index from that cell and replace the central cell with whatever cell is newly at that index.
        //  ideally we might consider this according to calculated cell size, but i don't want to presume that only cells in view can be expanded/collapsed.
        //  we could just offer an option for checking these things according to cell height instead of index? "org.yetyman.controls.treeview.treeview.AlignmentMethod.INDEX, org.yetyman.controls.treeview.treeview.AlignmentMethod.HEIGHT"

        if(earlyBound == null || laterBound == null) {
            earlyBound = centerItem;
            laterBound = centerItem;

            CellLayoutSettings<T> layoutSettings = cacheLayoutOfCenterItem(centerItem, topPad, height, bottomPad, middleY, leftPad, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);

            Insets margin = centerItem.currentCellProperty.get().getMargins();
            double cellTopMarg = margin==null ? 0 : margin.getTop();
            double cellBotMarg = margin==null ? 0 : margin.getBottom();

            topOfLaidCells = layoutSettings.areaY - cellTopMarg;
            botOfLaidCells = layoutSettings.areaY + layoutSettings.areaHeight - cellTopMarg;
        }

        //region track up
        while (topOfLaidCells >= 0) {

            CellLayoutSettings<T> layoutSettings = cacheLayoutOfOneItemUp(earlyBound, leftPad, topOfLaidCells, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
            if(earlyBound.previousVisibleItemProperty.get() == null)
                break;

            earlyBound = earlyBound.previousVisibleItemProperty.get();

            TreeCell<T> cell = earlyBound.currentCellProperty.get();
            topOfLaidCells -= cell.getHeight() + getMarginHeight(cell);
        }
        //endregion track up

        //region track up one more
        // not sure why bot and top aren't perfectly matching up at the edges, but close enough just add one more
        cacheLayoutOfOneItemUp(earlyBound, leftPad, topOfLaidCells, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
        if(earlyBound.previousVisibleItemProperty.get() != null) {
            earlyBound = earlyBound.previousVisibleItemProperty.get();

            TreeCell<T> cell = earlyBound.currentCellProperty.get();
            topOfLaidCells -= cell.getHeight() + getMarginHeight(cell);
        }
        //endregion track up one more

        //region track down
        while (botOfLaidCells <= height) {

            CellLayoutSettings<T> layoutSettings = cacheLayoutOfOneItemDown(laterBound, leftPad, botOfLaidCells, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
            if(laterBound.nextVisibleItemProperty.get() == null)
                break;

            laterBound = laterBound.nextVisibleItemProperty.get();

            TreeCell<T> cell = laterBound.currentCellProperty.get();
            botOfLaidCells += cell.getHeight() + getMarginHeight(cell);
        }
        //endregion track down

        //region track down one more
        // not sure why bot and top aren't perfectly matching up at the edges, but close enough just add one more
        cacheLayoutOfOneItemDown(laterBound, leftPad, botOfLaidCells, isFillWidth, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
        if(laterBound.nextVisibleItemProperty.get() != null) {
            laterBound = laterBound.nextVisibleItemProperty.get();

            TreeCell<T> cell = laterBound.currentCellProperty.get();
            botOfLaidCells += cell.getHeight() + getMarginHeight(cell);
        }
        //endregion track down one more

        int maxVisibleDepth = 0;
        for (TreeItem<T> laidItem : laidItems) {
            maxVisibleDepth = FastMath.max(laidItem.depthProperty.get(), maxVisibleDepth);
        }


        return new CellLayoutPass<>(earlyBound, laterBound, maxVisibleDepth);
    }

    private record CellLayoutPass<T>(TreeItem<T> earlyBound, TreeItem<T> laterBound, int maxVisibleDepth) {
    }

    private CellLayoutSettings<T> cacheLayoutOfCenterItem(TreeItem<T> centerItem, double topPad, double height, double bottomPad, double middleY, double leftPad, boolean isFillWidth, List<CellLayoutSettings<T>> cellLayouts, List<CaretLayoutSettings<T>> caretLayouts, List<TreeCell<T>> laidCells, List<TreeItem<T>> laidItems, List<TreeCaretNode<T>> laidCarets) {
        TreeCell<T> cell = getTreeCell(centerItem);
        sizeSingleCell(cell);

        Insets margin = cell.getMargins();

        double halfHeight = cell.getCachedHeight()/2;
        double topLimit = topPad;
        double botLimit = height - bottomPad;

        double min = topLimit + halfHeight;
        double max = botLimit - halfHeight;
        middleY = Math.clamp(middleY, FastMath.min(min,max), max);

        double topOfLaidCells = middleY - FastMath.ceil(halfHeight);

        return layoutOneCell(centerItem, leftPad, topOfLaidCells, isFillWidth, false, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
    }

    private CellLayoutSettings<T> cacheLayoutOfOneItemDown(TreeItem<T> laterBound, double leftPad, double botOfLaidCells, boolean isFillWidth, List<CellLayoutSettings<T>> cellLayouts, List<CaretLayoutSettings<T>> caretLayouts, List<TreeCell<T>> laidCells, List<TreeItem<T>> laidItems, List<TreeCaretNode<T>> laidCarets) {
        TreeItem<T> nextLaterBound = laterBound.nextVisibleItemProperty.get();
        if(nextLaterBound == null)
            return null;
        laterBound = nextLaterBound;

        TreeCell<T> cell = getTreeCell(laterBound);
        sizeSingleCell(cell);

        return layoutOneCell(laterBound, leftPad, botOfLaidCells + marginTop(cell), isFillWidth, false, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
    }

    private CellLayoutSettings<T> cacheLayoutOfOneItemUp(TreeItem<T> earlyBound, double leftPad, double topOfLaidCells, boolean isFillWidth, List<CellLayoutSettings<T>> cellLayouts, List<CaretLayoutSettings<T>> caretLayouts, List<TreeCell<T>> laidCells, List<TreeItem<T>> laidItems, List<TreeCaretNode<T>> laidCarets) {
        TreeItem<T> nextEarlyBound = earlyBound.previousVisibleItemProperty.get();
        if(nextEarlyBound == null)
            return null;
        earlyBound = nextEarlyBound;

        TreeCell<T> cell = getTreeCell(earlyBound);
        sizeSingleCell(cell);

        return layoutOneCell(earlyBound, leftPad, topOfLaidCells - cell.getHeight() - marginBottom(cell), isFillWidth, true, cellLayouts, caretLayouts, laidCells, laidItems, laidCarets);
    }

    private CellLayoutSettings<T> layoutOneCell(TreeItem<T> treeItem, double leftPad, double areaY, boolean isFillWidth, boolean up, List<CellLayoutSettings<T>> cellLayouts, List<CaretLayoutSettings<T>> caretLayouts, List<TreeCell<T>> laidCells, List<TreeItem<T>> laidItems, List<TreeCaretNode<T>> laidCarets) {
        TreeCell<T> cell = treeItem.currentCellProperty.get();

        //set layout values
        int depth = treeItem.depthProperty.get();
        double depthIndent = depth * defaultGuideWidth.get();
        Insets margin = cell.getMargins();

        CellLayoutSettings<T> layoutSettings = new CellLayoutSettings<>(cell, treeItem, leftPad + depthIndent, areaY,
                cell.getCachedConstraints().widthAvailable(), cell.getCachedHeight(), cell.getCachedHeight(),
                margin, isFillWidth, true, HPos.LEFT, VPos.CENTER);

        //add to lists
        if(up) {
            cellLayouts.addFirst(layoutSettings);
            laidCells.addFirst(cell);
            laidItems.addFirst(treeItem);
        } else {
            cellLayouts.addLast(layoutSettings);
            laidCells.addLast(cell);
            laidItems.addLast(treeItem);
        }

        //set up caret
        if (!treeItem.childrenProperty.isEmpty()) {
            TreeCaretNode<T> caret = getTreeCaretNode(treeItem);

            //fill margin space with carets and guides. don't leave the cell's margins open
            double b = margin == null ? 0 : margin.getBottom();
            double t = margin == null ? 0 : margin.getTop();

            //set layout values
            CaretLayoutSettings<T> caretLayoutSettings = new CaretLayoutSettings<>(caret, leftPad + depthIndent - defaultCaretWidth.get(), areaY-t, defaultCaretWidth.get(), cell.getCachedHeight()+t+b, cell.getCachedHeight()+t+b,
                    caret.getMargins(), true, true, HPos.CENTER, VPos.CENTER);

            //add to lists
            if(up) {
                caretLayouts.addFirst(caretLayoutSettings);
                laidCarets.addFirst(caret);
            } else {
                caretLayouts.addLast(caretLayoutSettings);
                laidCarets.addLast(caret);
            }
        }

        return layoutSettings;
    }

    private double marginTop(TreeCell<T> cell) {
        Insets margin = cell.getMargins();
        return margin == null ? 0 : margin.getTop();
    }

    private double marginBottom(TreeCell<T> cell) {
        Insets margin = cell.getMargins();
        return margin == null ? 0 : margin.getBottom();
    }

    private double getMarginHeight(TreeCell<T> cell) {
        Insets margin = cell.getMargins();
        if(margin!=null)
            return margin.getTop()+margin.getBottom();
        else
            return 0;
    }

    private static <T> void printGrid(T[][] grid) {
        StringBuilder s = new StringBuilder();
        for (T[] ts : grid) {
            for (T t : ts) {
                if (t == null)
                    s.append(" ");
                else
                    s.append(t);
            }
            s.append(System.lineSeparator());
        }
        log.info(s.toString());
    }
    private static <T> void printGrid(List<T[]> grid) {
        StringBuilder s = new StringBuilder();
        for (T[] ts : grid) {
            for (T t : ts) {
                if (t == null)
                    s.append(" ");
                else
                    s.append(t);
            }
            s.append(System.lineSeparator());
        }
        log.info(s.toString());

    }
    private static double getAvailableWidth(boolean isFillWidth, double contentWidth, double depthIndent) {
        double availableWidth;
        if(isFillWidth)
            availableWidth = contentWidth - depthIndent;
        else
            availableWidth = contentWidth;
        return availableWidth;
    }

    public List<TreeGuideNode<T>> getChildGuides(TreeItem<T> item) {
        int depth = item.depthProperty.get();
        if(item.currentCellProperty.get()==null)
            return List.of();
        int startRow = item.visibleIndexProperty.get() + 1;
        int guideCnt = FastMath.min(item.visibleDescCountProperty.get(), visibleItems.getSize());
        List<TreeGuideNode<T>> childGuideNodes = new ArrayList<>(guideCnt);
        for (int i = 0; i < guideCnt; i++) {
            TreeGuideNode<T> guide = findGuide(depth, startRow+i);
            if (guide != null) {
                childGuideNodes.add(guide);
            }
        }
        return childGuideNodes;
    }

    public List<TreeGuideNode<T>> getRowGuides(TreeItem<T> item) {
        int depth = item.depthProperty.get();
        if(item.currentCellProperty.get()==null)
            return List.of();
        int startRow = item.visibleIndexProperty.get();
        List<TreeGuideNode<T>> rowGuideNodes = new ArrayList<>(depth);
        for (int i = 0; i < depth; i++) {
            TreeGuideNode<T> guide = findGuide(i, startRow);
            if (guide != null) {
                rowGuideNodes.add(guide);
            }
        }
        return rowGuideNodes;
    }

    private TreeGuideNode<T> findGuide(int column, int row) {
        return null;
    }

    private TreeCell<T> getTreeCell(TreeItem<T> item) {
        TreeCell<T> cell = item.currentCellProperty.get();
        if (cell == null)
            cell = getARow(item);
        else if (cell.getCachedConstraints() == null)
            cell.updateVisuals(item.valueProperty.get(), item);
        return cell;
    }

    private TreeCell<T> getARow(TreeItem<T> item) {
        TreeCell<T> cell = null;

        for (TreeCell<T> allocatedCell : allocatedCells) {
            if (allocatedCell.getTreeItem() == null) {
                cell = allocatedCell;
                cell.setVisible(true);

                break;
            }
        }
        if(cell == null) {
            allocatedCells.add(cell = cellFactory.apply(this));
            cell.setManaged(false);
            TreeCell<T> finalCell = cell;
            cell.setOnMouseEntered(me->hoverEffect(me, finalCell));
            cell.setOnMouseExited(me->{
                if(hoveredCell.get().getTreeItem() == finalCell.getTreeItem() && !hoveredCellHovered.get())
                    hoveredCell.set(null);
            });
            cellParent.getChildren().add(cell);
        }

        cell.setTreeItem(item);
        item.settableCellProperty.set(cell);
        cell.updateVisuals(item.valueProperty.get(), item);

        return cell;
    }

    private void hoverEffect(MouseEvent mouseEvent, TreeCell<T> cell) {
        if(cell.isVisible()) {
            hoveredCell.set(cell);
            hoveredCellHovered.set(true);
            requestLayout();
        }
    }

    private TreeCaretNode<T> getTreeCaretNode(TreeItem<T> item) {
        TreeCaretNode<T> caret = item.currentCaretProperty.get();
        if (caret == null)
            caret = getACaret();

        updateCaret(caret, item);

        return caret;
    }

    private TreeCaretNode<T> getACaret() {
        TreeCaretNode<T> caret = null;

        for (TreeCaretNode<T> allocatedCaret : allocatedCarets) {
            if (allocatedCaret.getTreeItem() == null) {
                caret = allocatedCaret;
                caret.setVisible(true);
                break;
            }
        }
        if(caret == null) {
            allocatedCarets.add(caret = caretFactory.apply(this));
            cellParent.getChildren().add(caret);
        }

        caret.setCachedConstraints(null);

        return caret;
    }

    private void updateCaret(TreeCaretNode<T> caret, TreeItem<T> item) {
        caret.setTreeItem(item);
        caret.setDepth(item.depthProperty.get());
        caret.setVisibleIndex(item.visibleIndexProperty.get());
        item.settableCaretProperty.set(caret);

        caret.updateDisclosureBase(item.valueProperty.get(), item.showChildrenProperty.get(), item, item.currentCellProperty.get());
    }

    private List<TreeGuideNode<T>> getTreeGuideNodesColumn(TreeItem<T> item, int startIndex, int endIndex) {
        //start with the existing set
        List<TreeGuideNode<T>> guides = new LinkedList<>(item.currentChildGuidesProperty.get());

        if(!item.showChildrenProperty.get() || item.childrenProperty.isEmpty()) {
            item.currentChildGuidesProperty.clear();
            return List.of();
        }
        else {

            int itemIndex = item.visibleIndexProperty.get();
            int firstVisibleGuideIndex = FastMath.max(startIndex, itemIndex+1);
            //may be more efficient to get parent.children(next index).visibleIndex - 1
            int lastVisibleGuideIndex = FastMath.min(endIndex, itemIndex+item.visibleDescCountProperty.get());


            List<TreeItem<T>> children = new ArrayList<>(item.childrenProperty.get());
            Iterator<TreeItem<T>> iterator = children.iterator();
            TreeItem<T> nextChild = iterator.next();

            //iterate from start to end index
            int visibleIndex = firstVisibleGuideIndex;
            int laid = 0;

            while (nextChild.visibleIndexProperty.get() < firstVisibleGuideIndex && iterator.hasNext()) {
                nextChild = iterator.next();
            }

            //collect guides out of range
            List<TreeGuideNode<T>> nearbyUnusedGuides = new ArrayList<>();
            for (int i = 0; i < guides.size(); i++) {
                TreeGuideNode<T> guideNode = guides.get(i);
                if (guideNode.getVisibleIndex() < firstVisibleGuideIndex || guideNode.getVisibleIndex() > lastVisibleGuideIndex) {
                    nearbyUnusedGuides.add(guides.remove(i--));
                }
            }


            //add first guide if needed
            if(startIndex < endIndex && guides.isEmpty()) {
                if (!nearbyUnusedGuides.isEmpty())
                    guides.addFirst(nearbyUnusedGuides.removeLast());
                else
                    guides.addFirst(getAGuide());
            }

//            //add unused or new to beginning
//            if(guides.getFirst().getColumnTreeItem() != null && guides.getFirst().getVisibleIndex() > firstVisibleGuideIndex && guides.getFirst().getVisibleIndex() <= lastVisibleGuideIndex) {
//                for(int index = guides.getFirst().getVisibleIndex()-1; index >= firstVisibleGuideIndex; index--) {
//                    if (!nearbyUnusedGuides.isEmpty())
//                        guides.addFirst(nearbyUnusedGuides.removeLast());
//                    else
//                        guides.addFirst(getAGuide());
//                }
//            }
//
//            //add unused or new to end
//            if (guides.getLast().getColumnTreeItem() != null && guides.getLast().getVisibleIndex() >= firstVisibleGuideIndex && guides.getLast().getVisibleIndex() < lastVisibleGuideIndex) {
//                for (int index = guides.getLast().getVisibleIndex()+1; index <= lastVisibleGuideIndex; index++) {
//                    if (!nearbyUnusedGuides.isEmpty())
//                        guides.addLast(nearbyUnusedGuides.removeFirst());
//                    else
//                        guides.addLast(getAGuide());
//                }
//            }

            returnUnusedGuides(nearbyUnusedGuides);

            for (visibleIndex = firstVisibleGuideIndex; visibleIndex <= lastVisibleGuideIndex; visibleIndex++) {
                TreeGuideNode<T> guide = null;

                //we haven't necessarily reached "nextChild" yet. it is the "next child" that we will reach if at all
                boolean isChildGuide = visibleIndex == nextChild.visibleIndexProperty.get();
                int indexUnderItem = visibleIndex - itemIndex - 1;
                int guideIndex = visibleIndex - firstVisibleGuideIndex;

                boolean isLast = isChildGuide && !iterator.hasNext();

                //TODO: It would be better if we intelligently maintained state for still consistent guides instead of changing state for every guide on every scroll.
                if(guideIndex >= guides.size()) {
                    guide = getAGuide();
                    guides.add(guide);
                }
                else {
                    TreeGuideNode<T> guideAtIndex = guides.get(guideIndex);
                    if(guideAtIndex.getIndexUnderItem() > indexUnderItem) {
                        guide = getAGuide();
                        guides.add(guideIndex, guide);
                    }
                    else
                        guide = guides.get(guideIndex);
                }

                guide.setCachedConstraints(null);
                guide.setVisible(true);
//                if (guide.getIndexUnderItem() != indexUnderItem)
                updateGuide(guide, item, indexUnderItem, visibleIndex, isChildGuide ? nextChild : null, isLast);

                if (isChildGuide && iterator.hasNext()) {
                    nextChild = iterator.next();
                }

                laid++;

                if(isLast)
                    break;
            }

            //remove stale guides
            returnUnusedGuides(guides.subList(laid, guides.size()));
            guides = guides.subList(0, laid);
//            guides.forEach((g)->g.setVisible(true));
            item.settableChildGuidesProperty.setAll(guides);
            return guides;
        }

    }

    private TreeGuideNode<T> getAGuide() {
        TreeGuideNode<T> guide = null;

        //find the first unused guide node
        if(!guidePool.isEmpty())
            usedGuides.add(guide = guidePool.pop());

        //make one if there are none
        if(guide == null) {
            usedGuides.add(guide = guideFactory.apply(this));
            guideParent.getChildren().add(guide);
            guide.setManaged(false);
        }

//        guide.setManaged(false);
        return guide;
    }

    private void returnOneGuide(TreeGuideNode<T> guide) {
        TreeItem<T> columnItem = guide.getColumnTreeItem();
        if(columnItem!=null)
            columnItem.settableChildGuidesProperty.remove(guide);
        guide.setColumnTreeItem(null);

        TreeItem<T> rowItem = guide.getRowTreeItem();
        if(rowItem != null)
            rowItem.settableParentGuideProperty.set(null);
        guide.setRowTreeItem(null);
        guide.setCachedConstraints(null);
        guide.setVisible(false);
//        guide.relocate(0, -guide.getHeight()-50);

        usedGuides.remove(guide);
        guidePool.push(guide);
    }

    private static <T> void updateGuide(TreeGuideNode<T> guide, TreeItem<T> parentItem, int indexUnderItem, int visibleIndex, TreeItem<T> childAtIndex, boolean isLastGuideUnderItem) {
        guide.setColumnTreeItem(parentItem);
        guide.setIndexUnderParent(indexUnderItem,isLastGuideUnderItem);
        guide.setVisibleIndex(visibleIndex);
        guide.setDepth(parentItem.depthProperty.get());

        if(!parentItem.settableChildGuidesProperty.contains(guide))
            parentItem.settableChildGuidesProperty.add(guide);

        List<OrthoDirection> directions
                = childAtIndex!=null ? isLastGuideUnderItem ? lastChildGuideLines : normalGuideLines : straightGuideLines;

        guide.showDirectionsBase(parentItem.valueProperty.get(), directions, parentItem, childAtIndex);
    }

    private void setUnusedCellsFromUsedCells(List<TreeCell<T>> usedCells) {
        for (TreeCell<T> cell : allocatedCells) {
            if (!usedCells.contains(cell)) {
                if(cell.getTreeItem()!=null)
                    cell.getTreeItem().settableCellProperty.set(null);
                cell.setTreeItem(null);
                cell.setCachedConstraints(null);
                cell.setVisible(false);
            }
        }
    }

    /**
     * This exists as a convenient alternative to iterating now out of scope items.
     * We don't need to be aware of which branches just closed because we can check everything we didn't re-use this way
     * @param usedGuides
     */
    private void setUnusedGuidesFromUsedGuides(Set<TreeGuideNode<T>> usedGuides) {
        for (TreeGuideNode<T> guide : new HashSet<>(this.usedGuides)) {
            if (!usedGuides.contains(guide)) {
                returnOneGuide(guide);
            }
        }
    }

    private void returnUnusedGuides(List<TreeGuideNode<T>> unusedGuides) {
        for (TreeGuideNode<T> guide : unusedGuides) {
            returnOneGuide(guide);
        }
    }


    private void setUnusedCaretsFromUsedCarets(List<TreeCaretNode<T>> usedCarets) {
        for (TreeCaretNode<T> caret : allocatedCarets) {
            if (!usedCarets.contains(caret)) {
                if(caret.getTreeItem()!=null)
                    caret.getTreeItem().settableCaretProperty.set(null);
                caret.setTreeItem(null);
                caret.setCachedConstraints(null);
                caret.setVisible(false);
            }
        }
    }

    private double getFloatingHiLo(TreeItem<T> item){
        if(item.parentProperty.get()==null)
            return .7;
        return getFloatingHiLo(item.parentProperty.get()) - .05 + localEvenOdd(item) * .02;
    }
    private double localEvenOdd(TreeItem<T> item){
        return item.indexInParent() % 2.0;
    }

    public void setTweenTransitionTime(Duration tweenTransitionTime) {
        this.tweenTransitionTimeNano = tweenTransitionTime.getNano();
    }
    public Duration getTweenTransitionTime() {
        return  Duration.ofNanos(this.tweenTransitionTimeNano);
    }

    public TreeItem<T> setRootItem(T someItem) {
        TreeItem<T> root = new TreeItem<>(someItem);
        rootItemProperty.set(root);
        scrollCenterItem.set(root);
        modifiableVisibleItems.clear();
        centerItemScrollOffset.set(0);
        scrollTweenFrom = 0;
        scrollTweenToTimeNano = System.nanoTime();
        return root;
    }

    private TreeCell<T> defaultCellFactory(TreeView<T> tTreeView) {
        return new BasicTreeCell<>();
    }

    private TreeCaretNode<T> defaultCaretFactory(TreeView<T> tTreeView) {
        return new BasicTreeCaret<>();
    }

    private TreeGuideNode<T> defaultGuideFactory(TreeView<T> tTreeView) {
        return new BasicTreeGuide<>();
    }

    public Function<TreeView<T>, TreeCell<T>> getCellFactory() {
        return this.cellFactory;
    }
    public void setCellFactory(Function<TreeView<T>, TreeCell<T>> cellFactory) {
        this.cellFactory = cellFactory;
        if(this.hoverCopyCell != null) {
            getChildren().remove(this.hoverCopyCell);
            this.hoverCopyCell = null;
        }
    }

    public Function<TreeView<T>, TreeCaretNode<T>> getCaretFactory() {
        return this.caretFactory;
    }
    public void setCaretFactory(Function<TreeView<T>, TreeCaretNode<T>> caretFactory) {
        this.caretFactory = caretFactory;
    }

    public Function<TreeView<T>, TreeGuideNode<T>> getGuideFactory() {
        return this.guideFactory;
    }
    public void setGuideFactory(Function<TreeView<T>, TreeGuideNode<T>> guideFactory) {
        this.guideFactory = guideFactory;
    }
}

