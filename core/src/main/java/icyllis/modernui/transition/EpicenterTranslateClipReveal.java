/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2015 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.transition;

import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.AnimatorSet;
import icyllis.modernui.animation.MotionEasingUtils;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.TypeEvaluator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.Property;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

/**
 * EpicenterTranslateClipReveal captures the clip bounds and translation values
 * before and after the scene change and animates between those and the
 * epicenter bounds during a visibility transition.
 */
public class EpicenterTranslateClipReveal extends Visibility {
    private static final String PROPNAME_CLIP = "modernui:epicenterReveal:clip";
    private static final String PROPNAME_BOUNDS = "modernui:epicenterReveal:bounds";
    private static final String PROPNAME_TRANSLATE_X = "modernui:epicenterReveal:translateX";
    private static final String PROPNAME_TRANSLATE_Y = "modernui:epicenterReveal:translateY";
    private static final String PROPNAME_TRANSLATE_Z = "modernui:epicenterReveal:translateZ";
    private static final String PROPNAME_Z = "modernui:epicenterReveal:z";

    private final TimeInterpolator mInterpolatorX;
    private final TimeInterpolator mInterpolatorY;
    private final TimeInterpolator mInterpolatorZ;

    public EpicenterTranslateClipReveal() {
        mInterpolatorX = MotionEasingUtils.LINEAR_OUT_SLOW_IN;

        mInterpolatorY = MotionEasingUtils.FAST_OUT_SLOW_IN;

        mInterpolatorZ = MotionEasingUtils.FAST_OUT_SLOW_IN;
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    private void captureValues(@NonNull TransitionValues values) {
        final View view = values.view;
        if (view.getVisibility() == View.GONE) {
            return;
        }

        final Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        values.values.put(PROPNAME_BOUNDS, bounds);
        values.values.put(PROPNAME_TRANSLATE_X, view.getTranslationX());
        values.values.put(PROPNAME_TRANSLATE_Y, view.getTranslationY());
        values.values.put(PROPNAME_TRANSLATE_Z, view.getTranslationZ());
        values.values.put(PROPNAME_Z, view.getZ());

        final Rect clip = view.getClipBounds();
        values.values.put(PROPNAME_CLIP, clip);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view,
                             TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }

        final Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
        final Rect startBounds = getEpicenterOrCenter(endBounds);
        final float startX = startBounds.centerX() - endBounds.centerX();
        final float startY = startBounds.centerY() - endBounds.centerY();
        final float startZ = 0 - (float) endValues.values.get(PROPNAME_Z);

        // Translate the view to be centered on the epicenter.
        view.setTranslationX(startX);
        view.setTranslationY(startY);
        view.setTranslationZ(startZ);

        final float endX = (float) endValues.values.get(PROPNAME_TRANSLATE_X);
        final float endY = (float) endValues.values.get(PROPNAME_TRANSLATE_Y);
        final float endZ = (float) endValues.values.get(PROPNAME_TRANSLATE_Z);

        final Rect endClip = getBestRect(endValues);
        final Rect startClip = getEpicenterOrCenter(endClip);

        // Prepare the view.
        view.setClipBounds(startClip);

        final State startStateX = new State(startClip.left, startClip.right, startX);
        final State endStateX = new State(endClip.left, endClip.right, endX);
        final State startStateY = new State(startClip.top, startClip.bottom, startY);
        final State endStateY = new State(endClip.top, endClip.bottom, endY);

        return createRectAnimator(view, startStateX, startStateY, startZ, endStateX, endStateY,
                endZ, endValues, mInterpolatorX, mInterpolatorY, mInterpolatorZ);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view,
                                TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }

        final Rect startBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
        final Rect endBounds = getEpicenterOrCenter(startBounds);
        final float endX = endBounds.centerX() - startBounds.centerX();
        final float endY = endBounds.centerY() - startBounds.centerY();
        final float endZ = 0 - (float) startValues.values.get(PROPNAME_Z);

        final float startX = (float) endValues.values.get(PROPNAME_TRANSLATE_X);
        final float startY = (float) endValues.values.get(PROPNAME_TRANSLATE_Y);
        final float startZ = (float) endValues.values.get(PROPNAME_TRANSLATE_Z);

        final Rect startClip = getBestRect(startValues);
        final Rect endClip = getEpicenterOrCenter(startClip);

        // Prepare the view.
        view.setClipBounds(startClip);

        final State startStateX = new State(startClip.left, startClip.right, startX);
        final State endStateX = new State(endClip.left, endClip.right, endX);
        final State startStateY = new State(startClip.top, startClip.bottom, startY);
        final State endStateY = new State(endClip.top, endClip.bottom, endY);

        return createRectAnimator(view, startStateX, startStateY, startZ, endStateX, endStateY,
                endZ, endValues, mInterpolatorX, mInterpolatorY, mInterpolatorZ);
    }

    private Rect getEpicenterOrCenter(Rect bestRect) {
        final Rect epicenter = getEpicenter();
        if (epicenter != null) {
            return epicenter;
        }

        final int centerX = bestRect.centerX();
        final int centerY = bestRect.centerY();
        return new Rect(centerX, centerY, centerX, centerY);
    }

    private Rect getBestRect(TransitionValues values) {
        final Rect clipRect = (Rect) values.values.get(PROPNAME_CLIP);
        if (clipRect == null) {
            return (Rect) values.values.get(PROPNAME_BOUNDS);
        }
        return clipRect;
    }

    private static Animator createRectAnimator(final View view, State startX, State startY,
                                               float startZ, State endX, State endY, float endZ, TransitionValues endValues,
                                               TimeInterpolator interpolatorX, TimeInterpolator interpolatorY,
                                               TimeInterpolator interpolatorZ) {
        final StateEvaluator evaluator = new StateEvaluator();

        final ObjectAnimator animZ = ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, startZ, endZ);
        if (interpolatorZ != null) {
            animZ.setInterpolator(interpolatorZ);
        }

        final StateProperty propX = new StateProperty(StateProperty.TARGET_X);
        final ObjectAnimator animX = ObjectAnimator.ofObject(view, propX, evaluator, startX, endX);
        if (interpolatorX != null) {
            animX.setInterpolator(interpolatorX);
        }

        final StateProperty propY = new StateProperty(StateProperty.TARGET_Y);
        final ObjectAnimator animY = ObjectAnimator.ofObject(view, propY, evaluator, startY, endY);
        if (interpolatorY != null) {
            animY.setInterpolator(interpolatorY);
        }

        final Rect terminalClip = (Rect) endValues.values.get(PROPNAME_CLIP);
        final AnimatorListener animatorListener = new AnimatorListener() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                view.setClipBounds(terminalClip);
            }
        };

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animX, animY, animZ);
        animSet.addListener(animatorListener);
        return animSet;
    }

    private static class State {
        int lower;
        int upper;
        float trans;

        public State() {}

        public State(int lower, int upper, float trans) {
            this.lower = lower;
            this.upper = upper;
            this.trans = trans;
        }
    }

    private static class StateEvaluator implements TypeEvaluator<State> {
        private final State mTemp = new State();

        @Override
        public State evaluate(float fraction, State startValue, State endValue) {
            mTemp.upper = startValue.upper + (int) ((endValue.upper - startValue.upper) * fraction);
            mTemp.lower = startValue.lower + (int) ((endValue.lower - startValue.lower) * fraction);
            mTemp.trans = startValue.trans + (int) ((endValue.trans - startValue.trans) * fraction);
            return mTemp;
        }
    }

    private static class StateProperty extends Property<View, State> {
        public static final char TARGET_X = 'x';
        public static final char TARGET_Y = 'y';

        private final Rect mTempRect = new Rect();
        private final State mTempState = new State();

        private final int mTargetDimension;

        public StateProperty(char targetDimension) {
            super(State.class, "state_" + targetDimension);

            mTargetDimension = targetDimension;
        }

        @Override
        public State get(View object) {
            final Rect tempRect = mTempRect;
            if (!object.getClipBounds(tempRect)) {
                tempRect.setEmpty();
            }
            final State tempState = mTempState;
            if (mTargetDimension == TARGET_X) {
                tempState.trans = object.getTranslationX();
                tempState.lower = tempRect.left + (int) tempState.trans;
                tempState.upper = tempRect.right + (int) tempState.trans;
            } else {
                tempState.trans = object.getTranslationY();
                tempState.lower = tempRect.top + (int) tempState.trans;
                tempState.upper = tempRect.bottom + (int) tempState.trans;
            }
            return tempState;
        }

        @Override
        public void set(View object, State value) {
            final Rect tempRect = mTempRect;
            if (object.getClipBounds(tempRect)) {
                if (mTargetDimension == TARGET_X) {
                    tempRect.left = value.lower - (int) value.trans;
                    tempRect.right = value.upper - (int) value.trans;
                } else {
                    tempRect.top = value.lower - (int) value.trans;
                    tempRect.bottom = value.upper - (int) value.trans;
                }
                object.setClipBounds(tempRect);
            }

            if (mTargetDimension == TARGET_X) {
                object.setTranslationX(value.trans);
            } else {
                object.setTranslationY(value.trans);
            }
        }
    }
}
