package org.yetyman.controls.treeview.test;

import org.yetyman.controls.treeview.*;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import structures.directions.OrthoDirection;

import java.util.List;

public class TreeViewTestController extends GridPane {

    public TreeViewTestController() {
        getColumnConstraints().setAll(
                new ColumnConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, HPos.CENTER, true),
                new ColumnConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, HPos.CENTER, true),
                new ColumnConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, HPos.CENTER, true),
                new ColumnConstraints(100, 200, Double.MAX_VALUE, Priority.SOMETIMES, HPos.CENTER, true)
        );
        getRowConstraints().setAll(
                new RowConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, VPos.CENTER, true),
                new RowConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, VPos.CENTER, true),
                new RowConstraints(0, 200, Double.MAX_VALUE, Priority.SOMETIMES, VPos.CENTER, true)
        );
//        treeView.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(2))));

        Button btn = new Button("up");
        Button btn2 = new Button("down");

        setConstraints(btn, 3, 0, 1, 1, HPos.CENTER, VPos.TOP);
        setConstraints(btn2, 3, 0, 1, 1, HPos.CENTER, VPos.BOTTOM);

        TreeView<Something> treeView = addTree(btn, btn2, this);
        TreeView<Something> treeView2 = addTree(btn, btn2, this);
        TreeView<Something> treeView3 = addTree(btn, btn2, this);
        TreeView<Something> treeView4 = addTree(btn, btn2, this);
        TreeView<Something> treeView5 = addTree(btn, btn2, this);
        TreeView<Something> treeView6 = addTree(btn, btn2, this);
        TreeView<Something> treeView7 = addTree(btn, btn2, this);
        TreeView<Something> treeView8 = addTree(btn, btn2, this);
        TreeView<Something> treeView9 = addTree(btn, btn2, this);

        setConstraints(treeView, 0, 0, 1, 1);
        setConstraints(treeView2, 1, 1, 1, 1);
        setConstraints(treeView3, 2, 2, 1, 1);
        setConstraints(treeView4, 2, 0, 1, 1);
        setConstraints(treeView5, 1, 0, 1, 1);
        setConstraints(treeView6, 0, 1, 1, 1);
        setConstraints(treeView7, 0, 2, 1, 1);
        setConstraints(treeView8, 2, 1, 1, 1);
        setConstraints(treeView9, 1, 2, 1, 1);

        getChildren().addAll(btn, btn2);

//        setGridLinesVisible(true);

        treeView.setFillWidth(false);
        treeView2.setFillWidth(true);
        treeView3.setFillWidth(false);
        treeView4.setFillWidth(true);
        treeView5.setFillWidth(true);
        treeView6.setFillWidth(true);
        treeView7.setFillWidth(false);
        treeView8.setFillWidth(false);
        treeView9.setFillWidth(true);

        treeView.getStyleClass().addAll("depth-coloring", "corner-curves");
        treeView2.getStyleClass().addAll("depth-coloring", "corner-curves");
        treeView3.getStyleClass().addAll("depth-coloring", "corner-curves");
        treeView4.getStyleClass().addAll();
        treeView5.getStyleClass().addAll();
        treeView6.getStyleClass().addAll();
        treeView7.getStyleClass().addAll("depth-coloring", "corner-curves");
        treeView8.getStyleClass().addAll("depth-coloring", "corner-curves");
        treeView9.getStyleClass().addAll("depth-coloring");

        treeView3.setCellFactory(t->{
            BasicTreeCell<Something> cell = new BasicTreeCell<>();
            cell.setMinWidth(40);
            return cell;
        });

        Border b = new Border(new BorderStroke(Color.CADETBLUE, BorderStrokeStyle.SOLID, new CornerRadii(3), BorderWidths.DEFAULT));
        treeView.setCellFactory(t->{
            BasicTreeCell<Something> cell = new BasicTreeCell<>();
            cell.setMinWidth(50);
            cell.setPadding(new Insets(5));
            TreeView.setMargin(cell, new Insets(1));
            cell.setBorder(b);
            cell.setStyle("-fx-background-radius: 3px;");
            return cell;
        });

        treeView4.setGuideFactory(t->{
            BasicTreeGuide<Something> guide = new BasicTreeGuide<>();
            guide.cornerCurve = 0;
            return guide;
        });

        treeView6.defaultGuideWidth.set(6);
        treeView6.defaultCaretWidth.set(6);
        treeView6.setCaretFactory(t->{
            BasicTreeCaret<Something> caret = new BasicTreeCaret<>();
            Polygon p = new Polygon(-.6, .5, -.6,.6,0,.1,.6,.6,.6,.5,0,0);
//            Polygon p = new Polygon(-.6, .6,0,0,.6,.6);
            p.setFill(Color.BLACK);
            caret.caretShape.set(p);
            TreeView.setMargin(caret, new Insets(-2));
            return caret;
        });
        treeView6.setGuideFactory(t-> new TreeGuideNode<>() {
            final Region left = new Region();

            {
//                setManaged(false);
                left.setManaged(false);
                getChildren().setAll(left);
            }

            Border b1 = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(
                    0,
                    1,
                    0,
                    0
            )));

            @Override
            public void showDirections(Something value, List<OrthoDirection> directions, TreeItem<Something> parentItem, TreeItem<Something> rowItem) {
                left.setBorder(rowItem != null && !rowItem.childrenProperty.isEmpty() ? null : b1);
            }

            @Override
            protected void layoutChildren() {
                double halfWidth = getWidth() / 2;
                layoutInArea(left, 0, 0, halfWidth + 1, getHeight(), 0, HPos.CENTER, VPos.CENTER);
            }
        });

        treeView8.setBackground(new Background(new BackgroundFill(new RadialGradient(40, 1, .5,.5,1,true, CycleMethod.REFLECT, new Stop(0, Color.RED), new Stop(.5, Color.WHEAT),new Stop(1, Color.BLUE)), CornerRadii.EMPTY, Insets.EMPTY)));

        treeView9.setCellFactory(t->{
            TreeCell<Something> cell = new TreeCell<>() {
                final HBox basic = new HBox();
                final Text l = new Text();
                {
                    setMinWidth(0);
                    setPrefWidth(USE_COMPUTED_SIZE);
                    setMaxWidth(Double.MAX_VALUE);

                    basic.setSpacing(4);
                    basic.setMinWidth(0);
                    basic.setPrefWidth(USE_COMPUTED_SIZE);
                    basic.setMaxWidth(Double.MAX_VALUE);
                    basic.setPadding(new Insets(2));

                    Pane p = new Pane();

                    Button btn = new Button("E");
                    Button btn2 = new Button("X");

                    HBox.setHgrow(l, Priority.NEVER);
                    HBox.setHgrow(p, Priority.ALWAYS);
                    HBox.setHgrow(btn, Priority.NEVER);
                    HBox.setHgrow(btn2, Priority.NEVER);

                    basic.setFillHeight(true);
                    basic.getChildren().setAll(l, p, btn, btn2);
                    basic.setAlignment(Pos.CENTER_LEFT);

                    setContent(basic);
                }

                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    basic.resize(getWidth(), basic.prefHeight(getWidth()));
                }

                @Override
                public void updateVisuals(Something value, TreeItem<Something> wrapper) {
                    l.setText(value.toString());
                }
            };
            return cell;
        });
    }

    private TreeView<Something> addTree(Button btn, Button btn2, TreeViewTestController treeViewTestController) {
        TreeView<Something> treeView = new TreeView<>();
        treeViewTestController.getChildren().add(treeView);

        btn.addEventHandler(MouseEvent.MOUSE_CLICKED, _ ->{
            treeView.scrollUp();
        });
        btn2.addEventHandler(MouseEvent.MOUSE_CLICKED, _ ->{
            treeView.scrollDown();
        });

        TreeItem<Something> root = treeView.setRootItem(new Something("root"));
        populateTree(root);

        return treeView;
    }

    private static void populateTree(TreeItem<Something> root) {
        TreeItem<Something> a,b,c,d;
        TreeItem<Something> aa,ab,ac,ad;
        TreeItem<Something> ba,bb,bc,bd;
        TreeItem<Something> da,db,dc,dd;
        TreeItem<Something> aca,acb,acc,acd;
        TreeItem<Something> acaa, acaaa, acaab;

        root.childrenProperty.get().addAll(
                a = new TreeItem<>(new Something("a")),
                b = new TreeItem<>(new Something("b")),
                c = new TreeItem<>(new Something("c")),
                d = new TreeItem<>(new Something("d"))
        );

        a.childrenProperty.get().addAll(
                aa = new TreeItem<>(new Something("aa")),
                ab = new TreeItem<>(new Something("ab")),
                ac = new TreeItem<>(new Something("ac")),
                ad = new TreeItem<>(new Something("ad"))
        );

        b.childrenProperty.get().addAll(
                ba = new TreeItem<>(new Something("ba")),
                bb = new TreeItem<>(new Something("bb")),
                bc = new TreeItem<>(new Something("bc")),
                bd = new TreeItem<>(new Something("bd"))
        );

        c.childrenProperty.get().addAll(
                new TreeItem<>(new Something("ca")),
                new TreeItem<>(new Something("cb")),
                new TreeItem<>(new Something("cc")),
                new TreeItem<>(new Something("cd"))
        );

        d.childrenProperty.get().addAll(
                da = new TreeItem<>(new Something("da")),
                db = new TreeItem<>(new Something("db")),
                dc = new TreeItem<>(new Something("dc")),
                dd = new TreeItem<>(new Something("dd"))
        );

        ac.childrenProperty.get().addAll(
                aca = new TreeItem<>(new Something("aca")),
                acb = new TreeItem<>(new Something("acb")),
                acc = new TreeItem<>(new Something("acc")),
                acd = new TreeItem<>(new Something("acd"))
        );
        aca.childrenProperty.get().addAll(
                new TreeItem<>(new Something("aca")),
                new TreeItem<>(new Something("acb")),
                new TreeItem<>(new Something("acc")),
                new TreeItem<>(new Something("acd")),
                acaa = new TreeItem<>(new Something("aca")),
                new TreeItem<>(new Something("acb")),
                new TreeItem<>(new Something("acc")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd")),
                new TreeItem<>(new Something("acd"))
        );
        acaa.childrenProperty.get().addAll(
                new TreeItem<>(new Something("aca")),
                acaaa = new TreeItem<>(new Something("acaaa")),
                acaab = new TreeItem<>(new Something("acaab")),
                new TreeItem<>(new Something("acd"))
        );
        acaaa.childrenProperty.get().addAll(
                new TreeItem<>(new Something("aca")),
                new TreeItem<>(new Something("acb")),
                new TreeItem<>(new Something("acc")),
                new TreeItem<>(new Something("acd"))
        );
        acaab.childrenProperty.get().addAll(
                new TreeItem<>(new Something("aca")),
                new TreeItem<>(new Something("acb")),
                new TreeItem<>(new Something("acc")),
                new TreeItem<>(new Something("acd"))
        );

        root.showChildrenProperty.set(true);
        a.showChildrenProperty.set(true);
        b.showChildrenProperty.set(true);
        ac.showChildrenProperty.set(true);
        d.showChildrenProperty.set(true);
    }

    private record Something(String name) {

        @Override
            public String toString() {
                return name;
            }
        }
}
