package com.google.zxing.pdf417.decoder.ec;

final class ModulusPoly {
    private final int[] coefficients;
    private final ModulusGF field;

    ModulusPoly(ModulusGF modulusGF, int[] iArr) {
        if (iArr.length != 0) {
            this.field = modulusGF;
            int length = iArr.length;
            if (length > 1 && iArr[0] == 0) {
                int i = 1;
                while (i < length && iArr[i] == 0) {
                    i++;
                }
                if (i != length) {
                    this.coefficients = new int[(length - i)];
                    System.arraycopy(iArr, i, this.coefficients, 0, this.coefficients.length);
                    return;
                }
                this.coefficients = new int[1];
                return;
            }
            this.coefficients = iArr;
            return;
        }
        throw new IllegalArgumentException();
    }

    ModulusPoly add(ModulusPoly modulusPoly) {
        if (!this.field.equals(modulusPoly.field)) {
            throw new IllegalArgumentException("ModulusPolys do not have same ModulusGF field");
        } else if (isZero()) {
            return modulusPoly;
        } else {
            if (modulusPoly.isZero()) {
                return this;
            }
            Object obj = this.coefficients;
            Object obj2 = modulusPoly.coefficients;
            if (obj.length > obj2.length) {
                Object obj3 = obj;
                obj = obj2;
                obj2 = obj3;
            }
            Object obj4 = new int[obj2.length];
            int length = obj2.length - r3.length;
            System.arraycopy(obj2, 0, obj4, 0, length);
            for (int i = length; i < obj2.length; i++) {
                obj4[i] = this.field.add(r3[i - length], obj2[i]);
            }
            return new ModulusPoly(this.field, obj4);
        }
    }

    int evaluateAt(int i) {
        int i2 = 0;
        if (i == 0) {
            return getCoefficient(0);
        }
        int length = this.coefficients.length;
        int i3;
        if (i != 1) {
            i3 = this.coefficients[0];
            for (int i4 = 1; i4 < length; i4++) {
                i3 = this.field.add(this.field.multiply(i, i3), this.coefficients[i4]);
            }
            return i3;
        }
        i3 = 0;
        int[] iArr = this.coefficients;
        int length2 = iArr.length;
        while (i2 < length2) {
            i3 = this.field.add(i3, iArr[i2]);
            i2++;
        }
        return i3;
    }

    int getCoefficient(int i) {
        return this.coefficients[(this.coefficients.length - 1) - i];
    }

    int getDegree() {
        return this.coefficients.length - 1;
    }

    boolean isZero() {
        return this.coefficients[0] == 0;
    }

    ModulusPoly multiply(int i) {
        if (i == 0) {
            return this.field.getZero();
        }
        if (i == 1) {
            return this;
        }
        int length = this.coefficients.length;
        int[] iArr = new int[length];
        for (int i2 = 0; i2 < length; i2++) {
            iArr[i2] = this.field.multiply(this.coefficients[i2], i);
        }
        return new ModulusPoly(this.field, iArr);
    }

    ModulusPoly multiply(ModulusPoly modulusPoly) {
        if (!this.field.equals(modulusPoly.field)) {
            throw new IllegalArgumentException("ModulusPolys do not have same ModulusGF field");
        } else if (isZero() || modulusPoly.isZero()) {
            return this.field.getZero();
        } else {
            int[] iArr = this.coefficients;
            int length = iArr.length;
            int[] iArr2 = modulusPoly.coefficients;
            int length2 = iArr2.length;
            int[] iArr3 = new int[((length + length2) - 1)];
            for (int i = 0; i < length; i++) {
                int i2 = iArr[i];
                for (int i3 = 0; i3 < length2; i3++) {
                    iArr3[i + i3] = this.field.add(iArr3[i + i3], this.field.multiply(i2, iArr2[i3]));
                }
            }
            return new ModulusPoly(this.field, iArr3);
        }
    }

    ModulusPoly multiplyByMonomial(int i, int i2) {
        if (i < 0) {
            throw new IllegalArgumentException();
        } else if (i2 == 0) {
            return this.field.getZero();
        } else {
            int length = this.coefficients.length;
            int[] iArr = new int[(length + i)];
            for (int i3 = 0; i3 < length; i3++) {
                iArr[i3] = this.field.multiply(this.coefficients[i3], i2);
            }
            return new ModulusPoly(this.field, iArr);
        }
    }

    ModulusPoly negative() {
        int length = this.coefficients.length;
        int[] iArr = new int[length];
        for (int i = 0; i < length; i++) {
            iArr[i] = this.field.subtract(0, this.coefficients[i]);
        }
        return new ModulusPoly(this.field, iArr);
    }

    ModulusPoly subtract(ModulusPoly modulusPoly) {
        if (this.field.equals(modulusPoly.field)) {
            return !modulusPoly.isZero() ? add(modulusPoly.negative()) : this;
        } else {
            throw new IllegalArgumentException("ModulusPolys do not have same ModulusGF field");
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(getDegree() * 8);
        for (int degree = getDegree(); degree >= 0; degree--) {
            int coefficient = getCoefficient(degree);
            if (coefficient != 0) {
                if (coefficient < 0) {
                    stringBuilder.append(" - ");
                    coefficient = -coefficient;
                } else if (stringBuilder.length() > 0) {
                    stringBuilder.append(" + ");
                }
                if (degree == 0 || coefficient != 1) {
                    stringBuilder.append(coefficient);
                }
                if (degree != 0) {
                    if (degree != 1) {
                        stringBuilder.append("x^");
                        stringBuilder.append(degree);
                    } else {
                        stringBuilder.append('x');
                    }
                }
            }
        }
        return stringBuilder.toString();
    }
}
