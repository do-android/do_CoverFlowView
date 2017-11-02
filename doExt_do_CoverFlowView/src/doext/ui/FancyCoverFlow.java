/*
 * Copyright 2013 David Schreiber
 *           2013 John Paul Nalog
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package doext.ui;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class FancyCoverFlow extends Gallery {

	// =============================================================================
	// Constants
	// =============================================================================

	public static final int ACTION_DISTANCE_AUTO = Integer.MAX_VALUE;

	public static final float SCALEDOWN_GRAVITY_TOP = 0.0f;

	public static final float SCALEDOWN_GRAVITY_CENTER = 0.5f;

	public static final float SCALEDOWN_GRAVITY_BOTTOM = 1.0f;

	// =============================================================================
	// Private members
	// =============================================================================
	/**
	 * TODO: Doc
	 */
	private float unselectedAlpha;

	/**
	 * Camera used for view transformation.
	 */
	private Camera transformationCamera;

	/**
	 * TODO: Doc
	 */
	private int maxRotation = 0;

	/**
	 * Factor (0-1) that defines how much the unselected children should be
	 * scaled down. 1 means no scaledown.
	 */
	private float unselectedScale;

	/**
	 * TODO: Doc
	 */
	private float scaleDownGravity = SCALEDOWN_GRAVITY_CENTER;

	/**
	 * Distance in pixels between the transformation effects (alpha, rotation,
	 * zoom) are applied.
	 */
	private int actionDistance = ACTION_DISTANCE_AUTO;
	// =============================================================================
	// Constructors
	// =============================================================================

	public FancyCoverFlow(Context context) {
		super(context);
		this.initialize();
	}

	private void initialize() {
		this.transformationCamera = new Camera();
	}

	// =============================================================================
	// Getter / Setter
	// =============================================================================
	/**
	 * Sets the maximum rotation that is applied to items left and right of the
	 * center of the coverflow.
	 *
	 * @param maxRotation
	 */
	public void setMaxRotation(int maxRotation) {
		this.maxRotation = maxRotation;
	}

	public void setUnselectedScale(float unselectedScale) {
		this.unselectedScale = unselectedScale;
	}

	/**
	 * TODO: Doc
	 *
	 * @param scaleDownGravity
	 */
	public void setScaleDownGravity(float scaleDownGravity) {
		this.scaleDownGravity = scaleDownGravity;
	}

	/**
	 * TODO: Write doc
	 *
	 * @param actionDistance
	 */
	public void setActionDistance(int actionDistance) {
		this.actionDistance = actionDistance;
	}

	/**
	 * TODO: Write doc
	 *
	 * @param unselectedAlpha
	 */
	@Override
	public void setUnselectedAlpha(float unselectedAlpha) {
		super.setUnselectedAlpha(unselectedAlpha);
		this.unselectedAlpha = unselectedAlpha;
	}
	// =============================================================================
	// Supertype overrides
	// =============================================================================

	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t) {
		// We can cast here because FancyCoverFlowAdapter only creates wrappers.
//		FancyCoverFlowItemWrapper item = (FancyCoverFlowItemWrapper) child;
		View item = child;

		// Since Jelly Bean childs won't get invalidated automatically, needs to be added for the smooth coverflow animation
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			item.invalidate();
		}

		final int coverFlowWidth = this.getWidth();
		final int coverFlowCenter = coverFlowWidth / 2;
		final int childWidth = item.getWidth();
		final int childHeight = item.getHeight();
		final int childCenter = item.getLeft() + childWidth / 2;

		// Use coverflow width when its defined as automatic.
		final int actionDistance = (this.actionDistance == ACTION_DISTANCE_AUTO) ? (int) ((coverFlowWidth + childWidth) / 2.0f) : this.actionDistance;

		// Calculate the abstract amount for all effects.
		final float effectsAmount = Math.min(1.0f, Math.max(-1.0f, (1.0f / actionDistance) * (childCenter - coverFlowCenter)));

		// Clear previous transformations and set transformation type (matrix + alpha).
		t.clear();
		t.setTransformationType(Transformation.TYPE_BOTH);

		// Alpha
		if (this.unselectedAlpha != 1) {
			final float alphaAmount = (this.unselectedAlpha - 1) * Math.abs(effectsAmount) + 1;
			t.setAlpha(alphaAmount);
		}

		final Matrix imageMatrix = t.getMatrix();
		// Apply rotation.
		if (this.maxRotation != 0) {
			final int rotationAngle = (int) (-effectsAmount * this.maxRotation);
			this.transformationCamera.save();
			this.transformationCamera.rotateY(rotationAngle);
			this.transformationCamera.getMatrix(imageMatrix);
			this.transformationCamera.restore();
		}

		// Zoom.
		if (this.unselectedScale != 1) {
			final float zoomAmount = (this.unselectedScale - 1) * Math.abs(effectsAmount) + 1;
			// Calculate the scale anchor (y anchor can be altered)
			final float translateX = childWidth / 2.0f;
			final float translateY = childHeight * this.scaleDownGravity;
			imageMatrix.preTranslate(-translateX, -translateY);
			imageMatrix.postScale(zoomAmount, zoomAmount);
			imageMatrix.postTranslate(translateX, translateY);
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

}
