/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.fp2.core.client.render.state;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.lib.common.misc.Cloneable;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * Encapsulates the camera state during rendering.
 *
 * @author DaPorkchop_
 */
@EqualsAndHashCode
@ToString
public final class CameraState implements Cloneable<CameraState> {
    public final float[] modelViewProjectionMatrix;

    public int positionFloorX;
    public int positionFloorY;
    public int positionFloorZ;

    public float positionFracX;
    public float positionFracY;
    public float positionFracZ;

    public CameraState() {
        this.modelViewProjectionMatrix = PUnsafe.allocateUninitializedFloatArray(MatrixHelper.MAT4_ELEMENTS);
    }

    private CameraState(CameraState state) {
        this.modelViewProjectionMatrix = state.modelViewProjectionMatrix.clone();
        this.positionFloorX = state.positionFloorX;
        this.positionFloorY = state.positionFloorY;
        this.positionFloorZ = state.positionFloorZ;
        this.positionFracX = state.positionFracX;
        this.positionFracY = state.positionFracY;
        this.positionFracZ = state.positionFracZ;
    }

    @Override
    public CameraState clone() {
        return new CameraState(this);
    }

    /**
     * Sets the ModelViewProjection matrix by multiplying the given ModelView and Projection matrices.
     *
     * @param modelViewMatrix  the ModelView matrix
     * @param projectionMatrix the Projection matrix
     * @param client           the current {@link FP2Client} instance
     */
    public void setModelViewMatrixAndProjectionMatrix(float[] modelViewMatrix, float[] projectionMatrix, FP2Client client) {
        MatrixHelper.multiply4x4(projectionMatrix, modelViewMatrix, this.modelViewProjectionMatrix);
        this.applyMatrixTweaks(client);
    }

    /**
     * Sets the ModelViewProjection matrix.
     *
     * @param modelViewProjectionMatrix the ModelViewProjection matrix
     * @param client                    the current {@link FP2Client} instance
     */
    public void setModelViewProjectionMatrix(float[] modelViewProjectionMatrix, FP2Client client) {
        MatrixHelper.copy4x4(modelViewProjectionMatrix, this.modelViewProjectionMatrix);
        this.applyMatrixTweaks(client);
    }

    /**
     * Apply additional transformations to the ModelViewProjection matrix.
     *
     * @param client the current {@link FP2Client} instance
     */
    public void applyMatrixTweaks(FP2Client client) {
        //offset the projected points' depth values to avoid z-fighting with vanilla terrain
        MatrixHelper.offsetDepth(this.modelViewProjectionMatrix, client.isReverseZ() ? -0.00001f : 0.00001f);
    }

    /**
     * Sets the integer (floor) part of the camera position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public void positionFloor(int x, int y, int z) {
        this.positionFloorX = x;
        this.positionFloorY = y;
        this.positionFloorZ = z;
    }

    /**
     * Sets the fractional part of the camera position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public void positionFrac(float x, float y, float z) {
        this.positionFracX = x;
        this.positionFracY = y;
        this.positionFracZ = z;
    }

    /**
     * Sets both the integer and fractional parts of the camera position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public void positionDouble(double x, double y, double z) {
        double floorX = Math.floor(x);
        double floorY = Math.floor(y);
        double floorZ = Math.floor(z);
        this.positionFloor((int) floorX, (int) floorY, (int) floorZ);
        this.positionFrac((float) (x - floorX), (float) (y - floorY), (float) (z - floorZ));
    }

    /**
     * Stores this camera state in the given {@link CameraStateUniforms} instance.
     *
     * @param uniforms the {@link CameraStateUniforms} instance to store the camera state values in
     */
    public void configureUniforms(CameraStateUniforms uniforms) {
        uniforms.modelViewProjectionMatrix(this.modelViewProjectionMatrix);
        uniforms.positionFloor(this.positionFloorX, this.positionFloorY, this.positionFloorZ);
        uniforms.positionFrac(this.positionFracX, this.positionFracY, this.positionFracZ);
    }

    /**
     * Stores this camera state in the given {@link GlobalUniformAttributes} instance.
     *
     * @param uniforms the {@link GlobalUniformAttributes} instance to store the camera state values in
     */
    public void configureUniforms(GlobalUniformAttributes uniforms) {
        uniforms.modelViewProjectionMatrix(this.modelViewProjectionMatrix);
        uniforms.positionFloor(this.positionFloorX, this.positionFloorY, this.positionFloorZ);
        uniforms.positionFrac(this.positionFracX, this.positionFracY, this.positionFracZ);
    }
}
