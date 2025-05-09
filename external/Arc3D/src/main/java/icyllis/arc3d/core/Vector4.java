/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import org.jspecify.annotations.NonNull;

/**
 * Represents a four-dimensional vector.
 */
@SuppressWarnings("unused")
public class Vector4 {

    // coordinate components
    public float x;
    public float y;
    public float z;
    public float w;

    /**
     * Transform this vector by a 4x4 transformation matrix.
     *
     * @param mat the matrix used as the transformation
     */
    public void transform(@NonNull Matrix4 mat) {
        mat.preTransform(this);
    }
}
