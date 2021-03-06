package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.ResultPoint;

final class DetectionResultRowIndicatorColumn extends DetectionResultColumn {
    private final boolean isLeft;

    DetectionResultRowIndicatorColumn(BoundingBox boundingBox, boolean z) {
        super(boundingBox);
        this.isLeft = z;
    }

    private void removeIncorrectCodewords(Codeword[] codewordArr, BarcodeMetadata barcodeMetadata) {
        for (int i = 0; i < codewordArr.length; i++) {
            Codeword codeword = codewordArr[i];
            if (codewordArr[i] != null) {
                int value = codeword.getValue() % 30;
                int rowNumber = codeword.getRowNumber();
                if (rowNumber <= barcodeMetadata.getRowCount()) {
                    if (!this.isLeft) {
                        rowNumber += 2;
                    }
                    switch (rowNumber % 3) {
                        case 0:
                            if ((value * 3) + 1 == barcodeMetadata.getRowCountUpperPart()) {
                                break;
                            }
                            codewordArr[i] = null;
                            break;
                        case 1:
                            if (value / 3 != barcodeMetadata.getErrorCorrectionLevel() || value % 3 != barcodeMetadata.getRowCountLowerPart()) {
                                codewordArr[i] = null;
                                break;
                            }
                            break;
                            break;
                        case 2:
                            if (value + 1 == barcodeMetadata.getColumnCount()) {
                                break;
                            }
                            codewordArr[i] = null;
                            break;
                        default:
                            break;
                    }
                }
                codewordArr[i] = null;
            }
        }
    }

    int adjustCompleteIndicatorColumnRowNumbers(BarcodeMetadata barcodeMetadata) {
        Codeword[] codewords = getCodewords();
        setRowNumbers();
        removeIncorrectCodewords(codewords, barcodeMetadata);
        BoundingBox boundingBox = getBoundingBox();
        ResultPoint topRight = !this.isLeft ? boundingBox.getTopRight() : boundingBox.getTopLeft();
        ResultPoint bottomRight = !this.isLeft ? boundingBox.getBottomRight() : boundingBox.getBottomLeft();
        int imageRowToCodewordIndex = imageRowToCodewordIndex((int) topRight.getY());
        int imageRowToCodewordIndex2 = imageRowToCodewordIndex((int) bottomRight.getY());
        float rowCount = ((float) (imageRowToCodewordIndex2 - imageRowToCodewordIndex)) / ((float) barcodeMetadata.getRowCount());
        int i = -1;
        int i2 = 1;
        int i3 = 0;
        int i4 = imageRowToCodewordIndex;
        while (i4 < imageRowToCodewordIndex2) {
            if (codewords[i4] != null) {
                Codeword codeword = codewords[i4];
                int rowNumber = codeword.getRowNumber() - i;
                if (rowNumber == 0) {
                    i3++;
                } else if (rowNumber == 1) {
                    i2 = Math.max(i2, i3);
                    i3 = 1;
                    i = codeword.getRowNumber();
                } else if (rowNumber >= 0 && codeword.getRowNumber() < barcodeMetadata.getRowCount() && rowNumber <= i4) {
                    int i5 = i2 <= 2 ? rowNumber : (i2 - 2) * rowNumber;
                    Object obj = i5 < i4 ? null : 1;
                    for (int i6 = 1; i6 <= i5 && obj == null; i6++) {
                        obj = codewords[i4 - i6] == null ? null : 1;
                    }
                    if (obj == null) {
                        i = codeword.getRowNumber();
                        i3 = 1;
                    } else {
                        codewords[i4] = null;
                    }
                } else {
                    codewords[i4] = null;
                }
            }
            i4++;
        }
        return (int) (((double) rowCount) + 0.5d);
    }

    int adjustIncompleteIndicatorColumnRowNumbers(BarcodeMetadata barcodeMetadata) {
        BoundingBox boundingBox = getBoundingBox();
        ResultPoint topRight = !this.isLeft ? boundingBox.getTopRight() : boundingBox.getTopLeft();
        ResultPoint bottomRight = !this.isLeft ? boundingBox.getBottomRight() : boundingBox.getBottomLeft();
        int imageRowToCodewordIndex = imageRowToCodewordIndex((int) topRight.getY());
        int imageRowToCodewordIndex2 = imageRowToCodewordIndex((int) bottomRight.getY());
        float rowCount = ((float) (imageRowToCodewordIndex2 - imageRowToCodewordIndex)) / ((float) barcodeMetadata.getRowCount());
        Codeword[] codewords = getCodewords();
        int i = -1;
        int i2 = 1;
        int i3 = 0;
        for (int i4 = imageRowToCodewordIndex; i4 < imageRowToCodewordIndex2; i4++) {
            if (codewords[i4] != null) {
                Codeword codeword = codewords[i4];
                codeword.setRowNumberAsRowIndicatorColumn();
                int rowNumber = codeword.getRowNumber() - i;
                if (rowNumber == 0) {
                    i3++;
                } else if (rowNumber == 1) {
                    i2 = Math.max(i2, i3);
                    i3 = 1;
                    i = codeword.getRowNumber();
                } else if (codeword.getRowNumber() < barcodeMetadata.getRowCount()) {
                    i = codeword.getRowNumber();
                    i3 = 1;
                } else {
                    codewords[i4] = null;
                }
            }
        }
        return (int) (((double) rowCount) + 0.5d);
    }

    BarcodeMetadata getBarcodeMetadata() {
        Codeword[] codewords = getCodewords();
        BarcodeValue barcodeValue = new BarcodeValue();
        BarcodeValue barcodeValue2 = new BarcodeValue();
        BarcodeValue barcodeValue3 = new BarcodeValue();
        BarcodeValue barcodeValue4 = new BarcodeValue();
        for (Codeword codeword : codewords) {
            if (codeword != null) {
                codeword.setRowNumberAsRowIndicatorColumn();
                int value = codeword.getValue() % 30;
                int rowNumber = codeword.getRowNumber();
                if (!this.isLeft) {
                    rowNumber += 2;
                }
                switch (rowNumber % 3) {
                    case 0:
                        barcodeValue2.setValue((value * 3) + 1);
                        break;
                    case 1:
                        barcodeValue4.setValue(value / 3);
                        barcodeValue3.setValue(value % 3);
                        break;
                    case 2:
                        barcodeValue.setValue(value + 1);
                        break;
                    default:
                        break;
                }
            }
        }
        if (barcodeValue.getValue().length == 0 || barcodeValue2.getValue().length == 0 || barcodeValue3.getValue().length == 0 || barcodeValue4.getValue().length == 0 || barcodeValue.getValue()[0] < 1 || barcodeValue2.getValue()[0] + barcodeValue3.getValue()[0] < 3 || barcodeValue2.getValue()[0] + barcodeValue3.getValue()[0] > 90) {
            return null;
        }
        BarcodeMetadata barcodeMetadata = new BarcodeMetadata(barcodeValue.getValue()[0], barcodeValue2.getValue()[0], barcodeValue3.getValue()[0], barcodeValue4.getValue()[0]);
        removeIncorrectCodewords(codewords, barcodeMetadata);
        return barcodeMetadata;
    }

    int[] getRowHeights() throws FormatException {
        BarcodeMetadata barcodeMetadata = getBarcodeMetadata();
        if (barcodeMetadata == null) {
            return null;
        }
        adjustIncompleteIndicatorColumnRowNumbers(barcodeMetadata);
        int[] iArr = new int[barcodeMetadata.getRowCount()];
        for (Codeword codeword : getCodewords()) {
            if (codeword != null) {
                int rowNumber = codeword.getRowNumber();
                if (rowNumber < iArr.length) {
                    iArr[rowNumber] = iArr[rowNumber] + 1;
                } else {
                    throw FormatException.getFormatInstance();
                }
            }
        }
        return iArr;
    }

    boolean isLeft() {
        return this.isLeft;
    }

    void setRowNumbers() {
        for (Codeword codeword : getCodewords()) {
            if (codeword != null) {
                codeword.setRowNumberAsRowIndicatorColumn();
            }
        }
    }

    public String toString() {
        return "IsLeft: " + this.isLeft + '\n' + super.toString();
    }
}
