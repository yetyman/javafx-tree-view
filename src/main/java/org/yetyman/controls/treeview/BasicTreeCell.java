package org.yetyman.controls.treeview;

import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

public class BasicTreeCell<T> extends TreeCell<T> {
    private final Text l;

    public BasicTreeCell() {
        setManaged(false);
        l = new Text();
        l.getStyleClass().add("text");
        l.setTextOrigin(VPos.CENTER);
        l.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        setPadding(new Insets(1));

        setContent(l);
        setMinSize(USE_COMPUTED_SIZE, USE_PREF_SIZE);
        layoutBoundsProperty().addListener((s,a,b)->{
            l.setX(getPadding().getLeft());
            l.setY(getHeight()/2);
        });
        setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        setMaxSize(USE_COMPUTED_SIZE, USE_PREF_SIZE);
//        setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(2))));
//        layoutInArea(l, 0,0,getWidth(),getHeight(),l.getBaselineOffset(), HPos.CENTER, VPos.CENTER);

        setCacheShape(true);
        setCache(true);
        l.setCache(true);
        l.setCacheHint(CacheHint.QUALITY);
    }

    @Override
    public void updateVisuals(T value, TreeItem<T> wrapper) {
        l.setText(value.toString());
    }
}