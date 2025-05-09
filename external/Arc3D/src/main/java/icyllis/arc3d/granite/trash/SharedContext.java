/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.trash;

import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.Device;
import icyllis.arc3d.core.ImageInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static icyllis.arc3d.engine.Engine.*;

/**
 * Can be used to perform actions related to the generating {@link Context} in a thread safe manner. The
 * proxy does not access the 3D API (e.g. OpenGL) that backs the generating {@link Context}.
 * <p>
 * This class is a public API, except where noted.
 */
@Deprecated
public final class SharedContext {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    private static int createUniqueID() {
        for (;;) {
            final int value = sNextID.get();
            final int newValue = value == -1 ? 1 : value + 1; // 0 is reserved
            if (sNextID.weakCompareAndSetVolatile(value, newValue)) {
                return value;
            }
        }
    }

    private final int mBackend;
    private final ContextOptions mOptions;
    private final int mContextID;

    private volatile Caps mCaps;
    private volatile ThreadSafeCache mThreadSafeCache;
    private volatile GlobalResourceCache mGlobalResourceCache;

    private final AtomicBoolean mDiscarded = new AtomicBoolean(false);

    SharedContext(int backend, ContextOptions options) {
        mBackend = backend;
        mOptions = options;
        mContextID = createUniqueID();
    }

    /**
     * Retrieve the default {@link BackendFormat} for a given {@code ColorType} and renderability.
     * It is guaranteed that this backend format will be the one used by the following
     * {@code ColorType} and {@link SurfaceCharacterization#createBackendFormat(int, BackendFormat)}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param colorType  see {@link ImageDesc}
     * @param renderable true if the format will be used as color attachments
     */
    @Nullable
    public BackendFormat getDefaultBackendFormat(int colorType, boolean renderable) {
        assert (mCaps != null);

        colorType = colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, renderable);
        if (format == null) {
            return null;
        }
        assert (!renderable ||
                mCaps.isFormatRenderable(colorType, format, 1));
        return format;
    }

    /**
     * Retrieve the {@link BackendFormat} for a given {@code CompressionType}. This is
     * guaranteed to match the backend format used by the following
     * createCompressedBackendTexture methods that take a {@code CompressionType}.
     * <p>
     * The caller should check that the returned format is valid (nullability).
     *
     * @param compressionType see {@link ImageDesc}
     */
    @Nullable
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        assert (mCaps != null);

        BackendFormat format = mCaps.getCompressedBackendFormat(compressionType);
        assert (format == null) ||
                (!format.isExternal() && mCaps.isFormatTexturable(format));
        return format;
    }

    /**
     * Gets the maximum supported sample count for a color type. 1 is returned if only non-MSAA
     * rendering is supported for the color type. 0 is returned if rendering to this color type
     * is not supported at all.
     *
     * @param colorType see {@link ImageDesc}
     */
    public int getMaxSurfaceSampleCount(int colorType) {
        assert (mCaps != null);

        colorType = colorTypeToPublic(colorType);
        BackendFormat format = mCaps.getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return 0;
        }
        return mCaps.getMaxRenderTargetSampleCount(format);
    }

    /**
     * @return initialized or not, if {@link ImmediateContext} is created, it must be true
     */
    public boolean isValid() {
        return mCaps != null;
    }

    @ApiStatus.Internal
    public boolean matches(Context c) {
        //return c != null && this == c.mContextInfo;
        return false;
    }

    @ApiStatus.Internal
    public int getBackend() {
        return mBackend;
    }

    @ApiStatus.Internal
    public ContextOptions getOptions() {
        return mOptions;
    }

    @ApiStatus.Internal
    public int getContextID() {
        return mContextID;
    }

    @ApiStatus.Internal
    public Caps getCaps() {
        return mCaps;
    }

    @ApiStatus.Internal
    public ThreadSafeCache getThreadSafeCache() {
        return mThreadSafeCache;
    }

    @ApiStatus.Internal
    public GlobalResourceCache getPipelineCache() {
        return mGlobalResourceCache;
    }

    void init(Device device) {
        assert (device != null);
        mCaps = device.getCaps();
        mThreadSafeCache = new ThreadSafeCache();
        mGlobalResourceCache = device.getGlobalResourceCache();
    }

    boolean discard() {
        return !mDiscarded.compareAndExchange(false, true);
    }

    boolean isDiscarded() {
        return mDiscarded.get();
    }

    @Override
    public int hashCode() {
        return mContextID;
    }

    // use reference equality
}
