/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.SoundEffectConstants;
import org.jetbrains.annotations.ApiStatus;

/**
 * A button with two states, checked and unchecked. When the button is pressed
 * or clicked to perform an action, the state changes automatically.
 * <p>
 * Note that all events are handled on UI thread, this means your listener code
 * must execute quickly to avoid delaying your UI response to further actions.
 */
public abstract class CompoundButton extends Button implements Checkable2 {

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int[] CHECKABLE_STATE_SET = {
            R.attr.state_checkable
    };
    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int[] CHECKED_STATE_SET = {
            R.attr.state_checked
    };

    boolean mChecked;
    boolean mBroadcasting;

    private Drawable mButtonDrawable;
    private ColorStateList mButtonTintList;
    private BlendMode mButtonBlendMode;
    private boolean mHasButtonTint;
    private boolean mHasButtonBlendMode;

    OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeListenerInternal;

    @StyleableRes
    private static final String[] STYLEABLE = {
            R.ns, R.attr.button,
            R.ns, R.attr.checked,
    };

    public CompoundButton(Context context) {
        super(context);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    }

    public CompoundButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public CompoundButton(Context context, @Nullable AttributeSet attrs,
                          @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public CompoundButton(Context context, @Nullable AttributeSet attrs,
                          @Nullable @AttrRes ResourceId defStyleAttr,
                          @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, STYLEABLE);

        final Drawable d = a.getDrawable(0); // button
        if (d != null) {
            setButtonDrawable(d);
        }

        final boolean checked = a.getBoolean(1, false); // checked
        setChecked(checked);

        a.recycle();
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        toggle();

        final boolean handled = super.performClick();
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK);
        }

        return handled;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            // Modern UI changed: the internal listener is called first, since it may
            // alter the checked state.
            if (mOnCheckedChangeListenerInternal != null) {
                mOnCheckedChangeListenerInternal.onCheckedChanged(this, mChecked);
            }
            // Then reads mChecked field again to get the correct state.
            // Even though the final state may not change, we still call the listener.
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes. This callback is used for internal purpose only.
     *
     * @param listener the callback to call on checked state change
     * @hidden
     */
    @ApiStatus.Internal
    public void setInternalOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListenerInternal = listener;
    }

    /**
     * Sets a drawable as the compound button image.
     *
     * @param drawable the drawable to set
     */
    public void setButtonDrawable(@Nullable Drawable drawable) {
        if (mButtonDrawable != drawable) {
            if (mButtonDrawable != null) {
                mButtonDrawable.setCallback(null);
                unscheduleDrawable(mButtonDrawable);
            }

            mButtonDrawable = drawable;

            if (drawable != null) {
                drawable.setCallback(this);
                drawable.setLayoutDirection(getLayoutDirection());
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                drawable.setVisible(getVisibility() == VISIBLE, false);
                setMinHeight(drawable.getIntrinsicHeight());
                applyButtonTint();
            }
        }
    }

    /**
     * @return the drawable used as the compound button image
     * @see #setButtonDrawable(Drawable)
     */
    @Nullable
    public Drawable getButtonDrawable() {
        return mButtonDrawable;
    }

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is <code>SRC_IN</code> by default.
     * <p>
     * Subsequent calls to {@link #setButtonDrawable(Drawable)} will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #setButtonTintList(ColorStateList)
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setButtonTintList(@Nullable ColorStateList tint) {
        mButtonTintList = tint;
        mHasButtonTint = true;

        applyButtonTint();
    }

    /**
     * @return the tint applied to the button drawable
     * @see #setButtonTintList(ColorStateList)
     */
    @Nullable
    public final ColorStateList getButtonTintList() {
        return mButtonTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setButtonTintList(ColorStateList)}} to the button drawable. The
     * default mode is {@link BlendMode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see Drawable#setTintBlendMode(BlendMode)
     */
    public void setButtonTintBlendMode(@Nullable BlendMode tintMode) {
        mButtonBlendMode = tintMode;
        mHasButtonBlendMode = true;

        applyButtonTint();
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @see #setButtonTintBlendMode(BlendMode)
     */
    @Nullable
    public BlendMode getButtonTintBlendMode() {
        return mButtonBlendMode;
    }

    private void applyButtonTint() {
        if (mButtonDrawable != null && (mHasButtonTint || mHasButtonBlendMode)) {
            mButtonDrawable = mButtonDrawable.mutate();

            if (mHasButtonTint) {
                mButtonDrawable.setTintList(mButtonTintList);
            }

            if (mHasButtonBlendMode) {
                mButtonDrawable.setTintBlendMode(mButtonBlendMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mButtonDrawable.isStateful()) {
                mButtonDrawable.setState(getDrawableState());
            }
        }
    }

    @Override
    public int getCompoundPaddingLeft() {
        int padding = super.getCompoundPaddingLeft();
        if (!isLayoutRtl()) {
            final Drawable buttonDrawable = mButtonDrawable;
            if (buttonDrawable != null) {
                padding += buttonDrawable.getIntrinsicWidth();
            }
        }
        return padding;
    }

    @Override
    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight();
        if (isLayoutRtl()) {
            final Drawable buttonDrawable = mButtonDrawable;
            if (buttonDrawable != null) {
                padding += buttonDrawable.getIntrinsicWidth();
            }
        }
        return padding;
    }

    @Override
    public int getHorizontalOffsetForDrawables() {
        final Drawable buttonDrawable = mButtonDrawable;
        return (buttonDrawable != null) ? buttonDrawable.getIntrinsicWidth() : 0;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final Drawable buttonDrawable = mButtonDrawable;
        if (buttonDrawable != null) {
            final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            final int drawableHeight = buttonDrawable.getIntrinsicHeight();
            final int drawableWidth = buttonDrawable.getIntrinsicWidth();

            final int top = switch (verticalGravity) {
                case Gravity.BOTTOM -> getHeight() - drawableHeight;
                case Gravity.CENTER_VERTICAL -> (getHeight() - drawableHeight) / 2;
                default -> 0;
            };
            final int bottom = top + drawableHeight;
            final int left = isLayoutRtl() ? getWidth() - drawableWidth : 0;
            final int right = isLayoutRtl() ? getWidth() : drawableWidth;

            buttonDrawable.setBounds(left, top, right, bottom);

            final Drawable background = getBackground();
            if (background != null) {
                background.setHotspotBounds(left, top, right, bottom);
            }
        }

        super.onDraw(canvas);

        if (buttonDrawable != null) {
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            if (scrollX == 0 && scrollY == 0) {
                buttonDrawable.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                buttonDrawable.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }
    }

    @NonNull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        // Modern UI changed: always add checkable state
        mergeDrawableStates(drawableState, CHECKABLE_STATE_SET);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable buttonDrawable = mButtonDrawable;
        if (buttonDrawable != null && buttonDrawable.isStateful()
                && buttonDrawable.setState(getDrawableState())) {
            invalidateDrawable(buttonDrawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mButtonDrawable != null) {
            mButtonDrawable.setHotspot(x, y);
        }
    }

    @Override
    public void onResolveDrawables(@ResolvedLayoutDir int layoutDirection) {
        super.onResolveDrawables(layoutDirection);
        if (mButtonDrawable != null) {
            mButtonDrawable.setLayoutDirection(layoutDirection);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mButtonDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mButtonDrawable != null) {
            mButtonDrawable.jumpToCurrentState();
        }
    }
}
