package org.yetyman.controls.treeview;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import structures.directions.OrthoDirection;

import java.util.List;

public class BasicTreeGuide<T> extends TreeGuideNode<T> {
    final Region tl = new Region();
    final Region tr = new Region();
    final Region br = new Region();
    public int cornerCurve = 3;
    public BasicTreeGuide() {
//        setManaged(false);
        tl.setManaged(false);
        tr.setManaged(false);
        br.setManaged(false);
        getChildren().setAll(tl, tr, br);

        tl.setCache(true);
        tl.setCacheHint(CacheHint.QUALITY);
        tr.setCache(true);
        tr.setCacheHint(CacheHint.QUALITY);
        br.setCache(true);
        br.setCacheHint(CacheHint.QUALITY);
        setCacheShape(true);
        setCacheHint(CacheHint.QUALITY);
    }

    @Override
    public void showDirections(T value, List<OrthoDirection> directions, TreeItem<T> parentItem, TreeItem<T> childAtIndex) {
        boolean isUnderCaret = childAtIndex != null && childAtIndex.childrenProperty.getSize() > 0;

        Border tlBorder = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(
            0,
                directions.contains(OrthoDirection.UP) && directions.contains(OrthoDirection.DOWN) ? 1:0,
                directions.contains(OrthoDirection.LEFT) ? 1:0,
                0
        )));
        tl.setBorder(tlBorder);

        Border trBorder = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(0,0,0,isUnderCaret ? 0 : cornerCurve, false), new BorderWidths(
            0,
                0,
                directions.contains(OrthoDirection.RIGHT) ? 1:0,
                directions.contains(OrthoDirection.RIGHT) ? 1:0
        )));
        tr.setBorder(trBorder);

        Border brBorder = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(
                0,//directions.contains(org.yetyman.controls.treeview.treeview.OrthoDirection.RIGHT) ? 1:0) : 0,
                0,
                0,
                directions.contains(OrthoDirection.DOWN) ? 1:0
        )));
        br.setBorder(brBorder);

    }

    @Override
    protected void layoutChildren() {
        double halfWidth = getWidth()/2;
        double halfHeight = getHeight()/2;
        layoutInArea(tl, 0, 0, halfWidth+1, halfHeight+1, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(tr, halfWidth, 0, halfWidth, halfHeight+1, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(br, halfWidth, halfHeight, halfWidth, halfHeight, 0, HPos.CENTER, VPos.CENTER);
    }
}