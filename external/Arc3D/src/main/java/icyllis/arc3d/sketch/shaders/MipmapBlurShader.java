package icyllis.arc3d.sketch.shaders;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.sketch.Image;

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
    public static Shader make(Image image, float radius) {
        Rect2fc subset = image != null
                ? new Rect2f(0, 0, image.getWidth(), image.getHeight())
                : Rect2f.empty();

        return makeSubset(image,subset,radius);
    }

    @SharedPtr
    public static Shader makeSubset(Image image, Rect2fc subset,float radius) {
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

        return new MipmapBlurShader(image, subset, radius);
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
