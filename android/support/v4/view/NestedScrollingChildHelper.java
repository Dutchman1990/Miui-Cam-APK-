package android.support.v4.view;

import android.view.View;
import android.view.ViewParent;

public class NestedScrollingChildHelper {
    private boolean mIsNestedScrollingEnabled;
    private ViewParent mNestedScrollingParent;
    private int[] mTempNestedScrollConsumed;
    private final View mView;

    public NestedScrollingChildHelper(View view) {
        this.mView = view;
    }

    public boolean dispatchNestedFling(float f, float f2, boolean z) {
        return (!isNestedScrollingEnabled() || this.mNestedScrollingParent == null) ? false : ViewParentCompat.onNestedFling(this.mNestedScrollingParent, this.mView, f, f2, z);
    }

    public boolean dispatchNestedPreFling(float f, float f2) {
        return (!isNestedScrollingEnabled() || this.mNestedScrollingParent == null) ? false : ViewParentCompat.onNestedPreFling(this.mNestedScrollingParent, this.mView, f, f2);
    }

    public boolean dispatchNestedPreScroll(int i, int i2, int[] iArr, int[] iArr2) {
        boolean z = true;
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            if (i != 0 || i2 != 0) {
                int i3 = 0;
                int i4 = 0;
                if (iArr2 != null) {
                    this.mView.getLocationInWindow(iArr2);
                    i3 = iArr2[0];
                    i4 = iArr2[1];
                }
                if (iArr == null) {
                    if (this.mTempNestedScrollConsumed == null) {
                        this.mTempNestedScrollConsumed = new int[2];
                    }
                    iArr = this.mTempNestedScrollConsumed;
                }
                iArr[0] = 0;
                iArr[1] = 0;
                ViewParentCompat.onNestedPreScroll(this.mNestedScrollingParent, this.mView, i, i2, iArr);
                if (iArr2 != null) {
                    this.mView.getLocationInWindow(iArr2);
                    iArr2[0] = iArr2[0] - i3;
                    iArr2[1] = iArr2[1] - i4;
                }
                if (iArr[0] == 0 && iArr[1] == 0) {
                    z = false;
                }
                return z;
            } else if (iArr2 != null) {
                iArr2[0] = 0;
                iArr2[1] = 0;
            }
        }
        return false;
    }

    public boolean dispatchNestedScroll(int i, int i2, int i3, int i4, int[] iArr) {
        if (isNestedScrollingEnabled() && this.mNestedScrollingParent != null) {
            if (i != 0 || i2 != 0 || i3 != 0 || i4 != 0) {
                int i5 = 0;
                int i6 = 0;
                if (iArr != null) {
                    this.mView.getLocationInWindow(iArr);
                    i5 = iArr[0];
                    i6 = iArr[1];
                }
                ViewParentCompat.onNestedScroll(this.mNestedScrollingParent, this.mView, i, i2, i3, i4);
                if (iArr != null) {
                    this.mView.getLocationInWindow(iArr);
                    iArr[0] = iArr[0] - i5;
                    iArr[1] = iArr[1] - i6;
                }
                return true;
            } else if (iArr != null) {
                iArr[0] = 0;
                iArr[1] = 0;
            }
        }
        return false;
    }

    public boolean hasNestedScrollingParent() {
        return this.mNestedScrollingParent != null;
    }

    public boolean isNestedScrollingEnabled() {
        return this.mIsNestedScrollingEnabled;
    }

    public void setNestedScrollingEnabled(boolean z) {
        if (this.mIsNestedScrollingEnabled) {
            ViewCompat.stopNestedScroll(this.mView);
        }
        this.mIsNestedScrollingEnabled = z;
    }

    public boolean startNestedScroll(int i) {
        if (hasNestedScrollingParent()) {
            return true;
        }
        if (isNestedScrollingEnabled()) {
            View view = this.mView;
            for (ViewParent parent = this.mView.getParent(); parent != null; parent = parent.getParent()) {
                if (ViewParentCompat.onStartNestedScroll(parent, view, this.mView, i)) {
                    this.mNestedScrollingParent = parent;
                    ViewParentCompat.onNestedScrollAccepted(parent, view, this.mView, i);
                    return true;
                }
                if (parent instanceof View) {
                    view = (View) parent;
                }
            }
        }
        return false;
    }

    public void stopNestedScroll() {
        if (this.mNestedScrollingParent != null) {
            ViewParentCompat.onStopNestedScroll(this.mNestedScrollingParent, this.mView);
            this.mNestedScrollingParent = null;
        }
    }
}
