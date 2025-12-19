package com.soothe.sapApplication.ui;

import android.view.KeyEvent;

public interface OnKeyEventInterceptor {
    boolean onKeyDown(int keyCode, KeyEvent event);
    boolean onKeyUp(int keyCode, KeyEvent event);
}
