package com.google.zxing.pdf417.decoder;

final class Codeword {
    private final int bucket;
    private final int endX;
    private int rowNumber = -1;
    private final int startX;
    private final int value;

    Codeword(int i, int i2, int i3, int i4) {
        this.startX = i;
        this.endX = i2;
        this.bucket = i3;
        this.value = i4;
    }

    int getBucket() {
        return this.bucket;
    }

    int getEndX() {
        return this.endX;
    }

    int getRowNumber() {
        return this.rowNumber;
    }

    int getStartX() {
        return this.startX;
    }

    int getValue() {
        return this.value;
    }

    int getWidth() {
        return this.endX - this.startX;
    }

    boolean hasValidRowNumber() {
        return isValidRowNumber(this.rowNumber);
    }

    boolean isValidRowNumber(int i) {
        return i != -1 && this.bucket == (i % 3) * 3;
    }

    void setRowNumber(int i) {
        this.rowNumber = i;
    }

    void setRowNumberAsRowIndicatorColumn() {
        this.rowNumber = ((this.value / 30) * 3) + (this.bucket / 3);
    }

    public String toString() {
        return this.rowNumber + "|" + this.value;
    }
}
