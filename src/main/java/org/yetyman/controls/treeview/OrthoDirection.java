package org.yetyman.controls.treeview;

import structures.directions.EvenDirection;

public class OrthoDirection implements EvenDirection<OrthoDirection> {
    private static final OrthoDirection[] _values = new OrthoDirection[4];
    public static final OrthoDirection UP = new OrthoDirection(0);
    public static final OrthoDirection RIGHT = new OrthoDirection(1);
    public static final OrthoDirection DOWN = new OrthoDirection(2);
    public static final OrthoDirection LEFT = new OrthoDirection(3);

    private final int _ordinal;
    OrthoDirection(int ord){
        _ordinal = ord;
        _values[ord] = this;
    }

    @Override
    public int ordinal() {
        return _ordinal;
    }

    @Override
    public OrthoDirection[] values() {
        return _values;
    }

}
