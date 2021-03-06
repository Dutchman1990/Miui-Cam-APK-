package com.google.zxing.oned.rss.expanded.decoders;

import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

final class AI01AndOtherAIs extends AI01decoder {
    AI01AndOtherAIs(BitArray bitArray) {
        super(bitArray);
    }

    public String parseInformation() throws NotFoundException, FormatException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(01)");
        int length = stringBuilder.length();
        stringBuilder.append(getGeneralDecoder().extractNumericValueFromBitArray(4, 4));
        encodeCompressedGtinWithoutAI(stringBuilder, 8, length);
        return getGeneralDecoder().decodeAllCodes(stringBuilder, 48);
    }
}
