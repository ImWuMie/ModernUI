/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ISurface;
import icyllis.arc3d.sketch.Canvas;
import icyllis.arc3d.sketch.Image;
import icyllis.arc3d.sketch.Surface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The surface that is backed by GPU.
 */
public final class GraniteSurface extends Surface {

    @SharedPtr
    public GraniteDevice mDevice;

    public GraniteSurface(@SharedPtr GraniteDevice device) {
        super(device.getWidth(), device.getHeight());
        mDevice = device;
    }

    @Nullable
    @SharedPtr
    static GraniteSurface make(RecordingContext rc,
                               ImageInfo info,
                               boolean budgeted,
                               boolean mipmapped,
                               boolean approxFit,
                               int surfaceOrigin,
                               byte initialLoadOp,
                               String label,
                               boolean trackDevice) {
        int flags = 0;
        if (budgeted) {
            flags |= ISurface.FLAG_BUDGETED;
        }
        if (mipmapped) {
            flags |= ISurface.FLAG_MIPMAPPED;
        }
        if (approxFit) {
            flags |= ISurface.FLAG_APPROX_FIT;
        }
        @SharedPtr
        GraniteDevice device = GraniteDevice.make(
                rc, info, flags, surfaceOrigin, initialLoadOp, label, trackDevice
        );
        if (device == null) {
            return null;
        }
        // A non-budgeted surface should be fully instantiated before we return it
        // to the client.
        assert (budgeted || device.getReadView().isInstantiated());
        return new GraniteSurface(device); // move
    }

    @Nullable
    @SharedPtr
    public static GraniteSurface make(RecordingContext rc,
                                      ImageInfo info,
                                      boolean budgeted,
                                      boolean mipmapped,
                                      boolean approxFit,
                                      int surfaceOrigin,
                                      String label) {
        return make(rc, info, budgeted, mipmapped, approxFit,
                surfaceOrigin, Engine.LoadOp.kClear, label, true);
    }

    /**
     * While clients hold a ref on a Surface, the backing gpu object does <em>not</em>
     * count against the budget. Once a Surface is freed, the backing gpu object may or may
     * not become a scratch (i.e., reusable) resource but, if it does, it will be counted against
     * the budget.
     */
    @Nullable
    @SharedPtr
    public static GraniteSurface makeRenderTarget(RecordingContext rc,
                                                  @NonNull ImageInfo info,
                                                  boolean mipmapped,
                                                  int surfaceOrigin,
                                                  @Nullable String label) {
        if (label == null) {
            label = "SurfaceRenderTarget";
        }
        // create non-budgeted, exact-fit device
        return make(rc, info, false, mipmapped, false,
                surfaceOrigin, label);
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        // Mark the device immutable when the Surface is destroyed to flush any pending work to the
        // recorder and to flag the device so that any linked image views can detach from the Device
        // when they are next drawn.
        mDevice.setImmutable();
        mDevice = RefCnt.move(mDevice);
    }

    public void flush() {
        mDevice.flushPendingWork();
    }

    @NonNull
    @Override
    public ImageInfo getImageInfo() {
        return mDevice.getImageInfo();
    }

    @Override
    protected Canvas onNewCanvas() {
        return new Canvas(RefCnt.create(mDevice));
    }

    @Nullable
    @Override
    protected Image onNewImageSnapshot(@Nullable Rect2ic subset) {
        return makeImageCopy(subset, mDevice.getReadView().isMipmapped());
    }

    @Override
    protected boolean onCopyOnWrite(int changeMode) {
        // onNewImageSnapshot() always copy, no-op here
        return true;
    }

    @Nullable
    @SharedPtr
    public GraniteImage makeImageCopy(@Nullable Rect2ic subset, boolean mipmapped) {
        assert !hasCachedImage();
        if (subset == null) {
            subset = new Rect2i(0, 0, getWidth(), getHeight());
        }
        return mDevice.makeImageCopy(subset, false, mipmapped, false);
    }

    @Override
    protected RecordingContext onGetCommandContext() {
        return mDevice.getCommandContext();
    }

    @RawPtr
    public GraniteDevice getDevice() {
        return mDevice;
    }
}
