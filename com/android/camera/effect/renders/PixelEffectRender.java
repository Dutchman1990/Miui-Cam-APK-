package com.android.camera.effect.renders;

import android.opengl.GLES20;
import com.android.camera.effect.ShaderUtil;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawIntTexAttribute;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;

public abstract class PixelEffectRender extends ShaderRender {
    private static final float[] TEXTURES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] VERTICES = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

    public PixelEffectRender(GLCanvas gLCanvas) {
        super(gLCanvas);
    }

    public PixelEffectRender(GLCanvas gLCanvas, int i) {
        super(gLCanvas, i);
    }

    private void drawTexture(int i, float f, float f2, float f3, float f4, boolean z) {
        GLES20.glUseProgram(this.mProgram);
        bindTexture(i, 33984);
        bindExtraTexture();
        updateViewport();
        setBlendEnabled(false);
        this.mGLCanvas.getState().pushState();
        this.mGLCanvas.getState().translate(f, f2, 0.0f);
        this.mGLCanvas.getState().scale(f3, f4, 1.0f);
        initShaderValue(z);
        GLES20.glDrawArrays(5, 0, 4);
        this.mGLCanvas.getState().popState();
    }

    protected void bindExtraTexture() {
    }

    public boolean draw(DrawAttribute drawAttribute) {
        if (!isAttriSupported(drawAttribute.getTarget())) {
            return false;
        }
        switch (drawAttribute.getTarget()) {
            case 5:
                DrawBasicTexAttribute drawBasicTexAttribute = (DrawBasicTexAttribute) drawAttribute;
                drawTexture(drawBasicTexAttribute.mBasicTexture, (float) drawBasicTexAttribute.mX, (float) drawBasicTexAttribute.mY, (float) drawBasicTexAttribute.mWidth, (float) drawBasicTexAttribute.mHeight, drawBasicTexAttribute.mIsSnapshot);
                break;
            case 6:
                DrawIntTexAttribute drawIntTexAttribute = (DrawIntTexAttribute) drawAttribute;
                drawTexture(drawIntTexAttribute.mTexId, (float) drawIntTexAttribute.mX, (float) drawIntTexAttribute.mY, (float) drawIntTexAttribute.mWidth, (float) drawIntTexAttribute.mHeight, true);
                break;
        }
        return true;
    }

    protected void drawTexture(BasicTexture basicTexture, float f, float f2, float f3, float f4, boolean z) {
        GLES20.glUseProgram(this.mProgram);
        if (basicTexture.onBind(this.mGLCanvas) && bindTexture(basicTexture, 33984)) {
            bindExtraTexture();
            this.mGLCanvas.getState().pushState();
            updateViewport();
            setBlendEnabled(false);
            this.mGLCanvas.getState().translate(f, f2, 0.0f);
            this.mGLCanvas.getState().scale(f3, f4, 1.0f);
            initShaderValue(z);
            GLES20.glDrawArrays(5, 0, 4);
            this.mGLCanvas.getState().popState();
        }
    }

    protected void initShader() {
        this.mProgram = ShaderUtil.createProgram(getVertexShaderString(), getFragShaderString());
        if (this.mProgram != 0) {
            GLES20.glUseProgram(this.mProgram);
            this.mUniformMVPMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uMVPMatrix");
            this.mUniformSTMatrixH = GLES20.glGetUniformLocation(this.mProgram, "uSTMatrix");
            this.mUniformTextureH = GLES20.glGetUniformLocation(this.mProgram, "sTexture");
            this.mAttributePositionH = GLES20.glGetAttribLocation(this.mProgram, "aPosition");
            this.mAttributeTexCoorH = GLES20.glGetAttribLocation(this.mProgram, "aTexCoord");
            this.mUniformAlphaH = GLES20.glGetUniformLocation(this.mProgram, "uAlpha");
            return;
        }
        throw new IllegalArgumentException(getClass() + ": mProgram = 0");
    }

    protected void initShaderValue(boolean z) {
        GLES20.glVertexAttribPointer(this.mAttributePositionH, 2, 5126, false, 8, this.mVertexBuffer);
        GLES20.glVertexAttribPointer(this.mAttributeTexCoorH, 2, 5126, false, 8, this.mTexCoorBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttributePositionH);
        GLES20.glEnableVertexAttribArray(this.mAttributeTexCoorH);
        GLES20.glUniformMatrix4fv(this.mUniformMVPMatrixH, 1, false, this.mGLCanvas.getState().getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(this.mUniformSTMatrixH, 1, false, this.mGLCanvas.getState().getTexMaxtrix(), 0);
        GLES20.glUniform1i(this.mUniformTextureH, 0);
        GLES20.glUniform1f(this.mUniformAlphaH, z ? 1.0f : this.mGLCanvas.getState().getAlpha());
    }

    protected void initSupportAttriList() {
        this.mAttriSupportedList.add(Integer.valueOf(5));
        this.mAttriSupportedList.add(Integer.valueOf(6));
    }

    protected void initVertexData() {
        this.mVertexBuffer = ShaderRender.allocateByteBuffer((VERTICES.length * 32) / 8).asFloatBuffer();
        this.mVertexBuffer.put(VERTICES);
        this.mVertexBuffer.position(0);
        this.mTexCoorBuffer = ShaderRender.allocateByteBuffer((TEXTURES.length * 32) / 8).asFloatBuffer();
        this.mTexCoorBuffer.put(TEXTURES);
        this.mTexCoorBuffer.position(0);
    }
}
