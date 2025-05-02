package icyllis.modernui.graphics;

import icyllis.arc3d.core.RefCnt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Core;

public class MipmapBlurShader extends Shader {
    public MipmapBlurShader(@NonNull Image image, float radius,Matrix localMatrix) {
        var shader = icyllis.arc3d.sketch.shaders.MipmapBlurShader.make(
                RefCnt.create(image.getNativeImage()),
                radius,
                localMatrix
        );
        if (shader == null) {
            throw new IllegalArgumentException();
        }
        mCleanup = Core.registerNativeResource(this, shader);
        mShader = shader;
    }
}
