package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

public final class UPCEReader extends UPCEANReader {
    private static final int[] MIDDLE_END_PATTERN = new int[]{1, 1, 1, 1, 1, 1};
    private static final int[][] NUMSYS_AND_CHECK_DIGIT_PATTERNS;
    private final int[] decodeMiddleCounters = new int[4];

    static {
        r0 = new int[2][];
        r0[0] = new int[]{56, 52, 50, 49, 44, 38, 35, 42, 41, 37};
        r0[1] = new int[]{7, 11, 13, 14, 19, 25, 28, 21, 22, 26};
        NUMSYS_AND_CHECK_DIGIT_PATTERNS = r0;
    }

    public static String convertUPCEtoUPCA(String str) {
        char[] cArr = new char[6];
        str.getChars(1, 7, cArr, 0);
        StringBuilder stringBuilder = new StringBuilder(12);
        stringBuilder.append(str.charAt(0));
        char c = cArr[5];
        switch (c) {
            case '0':
            case '1':
            case '2':
                stringBuilder.append(cArr, 0, 2);
                stringBuilder.append(c);
                stringBuilder.append("0000");
                stringBuilder.append(cArr, 2, 3);
                break;
            case '3':
                stringBuilder.append(cArr, 0, 3);
                stringBuilder.append("00000");
                stringBuilder.append(cArr, 3, 2);
                break;
            case '4':
                stringBuilder.append(cArr, 0, 4);
                stringBuilder.append("00000");
                stringBuilder.append(cArr[4]);
                break;
            default:
                stringBuilder.append(cArr, 0, 5);
                stringBuilder.append("0000");
                stringBuilder.append(c);
                break;
        }
        stringBuilder.append(str.charAt(7));
        return stringBuilder.toString();
    }

    private static void determineNumSysAndCheckDigit(StringBuilder stringBuilder, int i) throws NotFoundException {
        for (int i2 = 0; i2 <= 1; i2++) {
            int i3 = 0;
            while (i3 < 10) {
                if (i != NUMSYS_AND_CHECK_DIGIT_PATTERNS[i2][i3]) {
                    i3++;
                } else {
                    stringBuilder.insert(0, (char) (i2 + 48));
                    stringBuilder.append((char) (i3 + 48));
                    return;
                }
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }

    protected boolean checkChecksum(String str) throws FormatException {
        return super.checkChecksum(convertUPCEtoUPCA(str));
    }

    protected int[] decodeEnd(BitArray bitArray, int i) throws NotFoundException {
        return UPCEANReader.findGuardPattern(bitArray, i, true, MIDDLE_END_PATTERN);
    }

    protected int decodeMiddle(BitArray bitArray, int[] iArr, StringBuilder stringBuilder) throws NotFoundException {
        int[] iArr2 = this.decodeMiddleCounters;
        iArr2[0] = 0;
        iArr2[1] = 0;
        iArr2[2] = 0;
        iArr2[3] = 0;
        int size = bitArray.getSize();
        int i = iArr[1];
        int i2 = 0;
        for (int i3 = 0; i3 < 6 && i < size; i3++) {
            int decodeDigit = UPCEANReader.decodeDigit(bitArray, iArr2, i, L_AND_G_PATTERNS);
            stringBuilder.append((char) ((decodeDigit % 10) + 48));
            for (int i4 : iArr2) {
                i += i4;
            }
            if (decodeDigit >= 10) {
                i2 |= 1 << (5 - i3);
            }
        }
        determineNumSysAndCheckDigit(stringBuilder, i2);
        return i;
    }

    BarcodeFormat getBarcodeFormat() {
        return BarcodeFormat.UPC_E;
    }
}
