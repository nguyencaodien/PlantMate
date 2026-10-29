package com.example.ungdungchiasecaycanh.util;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

import androidx.core.graphics.ColorUtils;

/** Draws its text at 65% of the current colour, so a count beside a label follows selected / unselected colours. */
public final class FadedSpan extends CharacterStyle {
    @Override public void updateDrawState(TextPaint tp) {
        tp.setColor(ColorUtils.setAlphaComponent(tp.getColor(), 166));
        tp.setFakeBoldText(false);
    }
}
