package icyllis.arc3d.sketch.shaders;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.sketch.Image;
import icyllis.arc3d.sketch.Matrix;
import icyllis.arc3d.sketch.Matrixc;
import org.jspecify.annotations.Nullable;

public final class MipmapBlurShader implements Shader {
    @SharedPtr
    public final Image mImage;
    private final float mRadius;
    // If subset == (0,0,w,h) of the image, then no subset is applied. Subset will not be empty.
    public final Rect2f mSubset;

    MipmapBlurShader(Image image, Rect2fc subset, float radius) {
        this.mImage = image;
        this.mRadius = radius;
        mSubset = new Rect2f(subset);
    }

    @SharedPtr
    public static Shader make(Image image, float radius, @Nullable Matrixc localMatrix) {
        Rect2fc subset = image != null
                ? new Rect2f(0, 0, image.getWidth(), image.getHeight())
                : Rect2f.empty();

        return makeSubset(image, subset, radius, localMatrix);
    }

    @SharedPtr
    public static Shader makeSubset(Image image, Rect2fc subset, float radius, @Nullable Matrixc localMatrix) {
        if (!Float.isFinite(radius)) {
            // empty, infinite or NaN
            return EmptyShader.INSTANCE;
        }

        if (image == null || subset.isEmpty()) {
            RefCnt.move(image);
            return EmptyShader.INSTANCE;
        }

        if (!(0 <= subset.left() && 0 <= subset.top() && // also capture NaN
                image.getWidth() >= subset.right() &&
                image.getHeight() >= subset.bottom())) {
            image.unref();
            return null;
        }

        Shader s = new MipmapBlurShader(image, subset, radius);
        Matrix lm = localMatrix != null ? new Matrix(localMatrix) : new Matrix();
        return new LocalMatrixShader(s, // move
                lm);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public Image getImage() {
        return mImage;
    }

    public float getRadius() {
        return mRadius;
    }

    public Rect2fc getSubset() {
        return mSubset;
    }
}
