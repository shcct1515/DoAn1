package com.calculator.model;

/** Đơn vị góc: độ, radian, grad. */
public enum AngleMode {
    DEG, RAD, GRAD;

    public AngleMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
