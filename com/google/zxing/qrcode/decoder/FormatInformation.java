package com.google.zxing.qrcode.decoder;

final class FormatInformation {
    private static final int[] BITS_SET_IN_HALF_BYTE;
    private static final int[][] FORMAT_INFO_DECODE_LOOKUP;
    private final byte dataMask;
    private final ErrorCorrectionLevel errorCorrectionLevel;

    static {
        int[][] iArr = new int[32][];
        int[] iArr2 = new int[]{21522, iArr2};
        iArr[1] = new int[]{20773, 1};
        iArr[2] = new int[]{24188, 2};
        iArr[3] = new int[]{23371, 3};
        iArr[4] = new int[]{17913, 4};
        iArr[5] = new int[]{16590, 5};
        iArr[6] = new int[]{20375, 6};
        iArr[7] = new int[]{19104, 7};
        iArr[8] = new int[]{30660, 8};
        iArr[9] = new int[]{29427, 9};
        iArr[10] = new int[]{32170, 10};
        iArr[11] = new int[]{30877, 11};
        iArr[12] = new int[]{26159, 12};
        iArr[13] = new int[]{25368, 13};
        iArr[14] = new int[]{27713, 14};
        iArr[15] = new int[]{26998, 15};
        iArr[16] = new int[]{5769, 16};
        iArr[17] = new int[]{5054, 17};
        iArr[18] = new int[]{7399, 18};
        iArr[19] = new int[]{6608, 19};
        iArr[20] = new int[]{1890, 20};
        iArr[21] = new int[]{597, 21};
        iArr[22] = new int[]{3340, 22};
        iArr[23] = new int[]{2107, 23};
        iArr[24] = new int[]{13663, 24};
        iArr[25] = new int[]{12392, 25};
        iArr[26] = new int[]{16177, 26};
        iArr[27] = new int[]{14854, 27};
        iArr[28] = new int[]{9396, 28};
        iArr[29] = new int[]{8579, 29};
        iArr[30] = new int[]{11994, 30};
        iArr[31] = new int[]{11245, 31};
        FORMAT_INFO_DECODE_LOOKUP = iArr;
        int[] iArr3 = new int[16];
        iArr3[1] = 1;
        iArr3[2] = 1;
        iArr3[3] = 2;
        iArr3[4] = 1;
        iArr3[5] = 2;
        iArr3[6] = 2;
        iArr3[7] = 3;
        iArr3[8] = 1;
        iArr3[9] = 2;
        iArr3[10] = 2;
        iArr3[11] = 3;
        iArr3[12] = 2;
        iArr3[13] = 3;
        iArr3[14] = 3;
        iArr3[15] = 4;
        BITS_SET_IN_HALF_BYTE = iArr3;
    }

    private FormatInformation(int i) {
        this.errorCorrectionLevel = ErrorCorrectionLevel.forBits((i >> 3) & 3);
        this.dataMask = (byte) ((byte) (i & 7));
    }

    static FormatInformation decodeFormatInformation(int i, int i2) {
        FormatInformation doDecodeFormatInformation = doDecodeFormatInformation(i, i2);
        return doDecodeFormatInformation == null ? doDecodeFormatInformation(i ^ 21522, i2 ^ 21522) : doDecodeFormatInformation;
    }

    private static FormatInformation doDecodeFormatInformation(int i, int i2) {
        int i3 = Integer.MAX_VALUE;
        int i4 = 0;
        for (int[] iArr : FORMAT_INFO_DECODE_LOOKUP) {
            int i5 = iArr[0];
            if (i5 == i || i5 == i2) {
                return new FormatInformation(iArr[1]);
            }
            int numBitsDiffering = numBitsDiffering(i, i5);
            if (numBitsDiffering < i3) {
                i4 = iArr[1];
                i3 = numBitsDiffering;
            }
            if (i != i2) {
                numBitsDiffering = numBitsDiffering(i2, i5);
                if (numBitsDiffering < i3) {
                    i4 = iArr[1];
                    i3 = numBitsDiffering;
                }
            }
        }
        return i3 > 3 ? null : new FormatInformation(i4);
    }

    static int numBitsDiffering(int i, int i2) {
        i ^= i2;
        return ((((((BITS_SET_IN_HALF_BYTE[i & 15] + BITS_SET_IN_HALF_BYTE[(i >>> 4) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 8) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 12) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 16) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 20) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 24) & 15]) + BITS_SET_IN_HALF_BYTE[(i >>> 28) & 15];
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof FormatInformation)) {
            return false;
        }
        FormatInformation formatInformation = (FormatInformation) obj;
        return this.errorCorrectionLevel == formatInformation.errorCorrectionLevel && this.dataMask == formatInformation.dataMask;
    }

    byte getDataMask() {
        return this.dataMask;
    }

    ErrorCorrectionLevel getErrorCorrectionLevel() {
        return this.errorCorrectionLevel;
    }

    public int hashCode() {
        return (this.errorCorrectionLevel.ordinal() << 3) | this.dataMask;
    }
}
