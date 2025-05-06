package org.yetyman.controls.treeview;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.CacheHint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import org.apache.commons.math3.util.FastMath;

public class BasicTreeCaret<T> extends TreeCaretNode<T> {
    private double caretProp = .6;
    public SimpleObjectProperty<Shape> caretShape = new SimpleObjectProperty<>(new Polygon(caretProp, 0, 0, -caretProp, -caretProp, 0, 0, 0, 0, caretProp, 0, 0));

    public BasicTreeCaret() {
        caretShape.get().fillProperty().set(Color.BLACK);

        setupCaretShape();
        setManaged(false);


        caretShape.addListener((s,a,b)->{
            if(a!=null)
                getChildren().remove(a);
            if(b!=null)
                getChildren().add(b);

            setCaretState();

        });
//                Border b = new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(1)));
//                setBorder(b);
        setCacheShape(true);
        caretShape.get().setCache(true);
        caretShape.get().setCacheHint(CacheHint.QUALITY);
    }

    protected void setupCaretShape() {
        getChildren().add(caretShape.get());

        widthProperty().addListener((s,a,b)->{
            caretShape.get().setTranslateX(getWidth()/2+.5);
            caretShape.get().setTranslateY(getHeight()/2);
            caretShape.get().setScaleX(FastMath.min(getWidth(), getHeight())/2);
            caretShape.get().setScaleY(FastMath.min(getWidth(), getHeight())/2);
        });
        heightProperty().addListener((s,a,b)->{
            caretShape.get().setTranslateX(getWidth()/2+.5);
            caretShape.get().setTranslateY(getHeight()/2);
            caretShape.get().setScaleX(FastMath.min(getWidth(), getHeight())/2);
            caretShape.get().setScaleY(FastMath.min(getWidth(), getHeight())/2);
        });
        disclosure.addListener((s,a,b)->{
            caretShape.get().setRotate(b ? 180 : 90);
        });

        setCaretState();
    }

    private void setCaretState() {
        caretShape.get().setRotate(disclosure.get() ? 180 : 90);
        caretShape.get().setTranslateX(getWidth()/2+.5);
        caretShape.get().setTranslateY(getHeight()/2);
        caretShape.get().setScaleX(FastMath.min(getWidth(), getHeight())/2);
        caretShape.get().setScaleY(FastMath.min(getWidth(), getHeight())/2);
    }

    @Override
    public void updateDisclosure(T value, boolean showChildren, TreeItem<T> wrapper, TreeCell<T> pairedVisual) {

    }
}