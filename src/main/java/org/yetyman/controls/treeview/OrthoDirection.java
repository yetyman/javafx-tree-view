package org.yetyman.controls.treeview;

public enum OrthoDirection {
    UP, RIGHT, DOWN, LEFT;

    OrthoDirection opposite(){
        return rotate(values().length/2);
    }

    OrthoDirection rotate(int add){
        return values()[(ordinal() + add) % values().length];
    }
}
