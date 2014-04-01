/*******************************************************************************
 * Copyright 2014 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.core.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

/**
 * Basic implementation of {@link IViewState}.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class ViewState implements IViewState
{
	//state
	protected Position center = Position.ZERO;
	protected Angle heading = Angle.ZERO;
	protected Angle pitch = Angle.ZERO;
	protected Angle roll = Angle.ZERO;
	protected double zoom = 1.0;

	//cached derived values
	protected Globe lastGlobe;
	protected Vec4 lastUp;
	protected Vec4 lastForward;
	protected Vec4 lastSide;
	protected Matrix lastTransform;
	protected Matrix lastRotation;
	protected Matrix lastRotationInverse;
	protected Vec4 lastCenterPoint;
	protected Vec4 lastEyePoint;
	protected Position lastEye;

	protected void clearCachedValues()
	{
		lastUp = null;
		lastForward = null;
		lastSide = null;
		lastTransform = null;
		lastRotation = null;
		lastRotationInverse = null;
		lastCenterPoint = null;
		lastEyePoint = null;
		lastEye = null;
	}

	protected void clearCachedValuesIfGlobeChanged(Globe globe)
	{
		if (lastGlobe != globe)
		{
			clearCachedValues();
			lastGlobe = globe;
		}
	}

	@Override
	public Position getCenter()
	{
		return center;
	}

	@Override
	public void setCenter(Position center)
	{
		this.center = center;
		clearCachedValues();
	}

	@Override
	public Angle getHeading()
	{
		return heading;
	}

	@Override
	public void setHeading(Angle heading)
	{
		this.heading = heading.normalizedLongitude();
		clearCachedValues();
	}

	@Override
	public Angle getPitch()
	{
		return pitch;
	}

	@Override
	public void setPitch(Angle pitch)
	{
		this.pitch = pitch.normalizedLongitude();
		clearCachedValues();
	}

	@Override
	public Angle getRoll()
	{
		return roll;
	}

	@Override
	public void setRoll(Angle roll)
	{
		this.roll = roll.normalizedLongitude();
		clearCachedValues();
	}

	@Override
	public double getZoom()
	{
		return zoom;
	}

	@Override
	public void setZoom(double zoom)
	{
		this.zoom = zoom;
		clearCachedValues();
	}

	@Override
	public Matrix getRotation(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastRotation == null)
		{
			lastRotation = getRotationInverse(globe).getInverse();
		}
		return lastRotation;
	}

	@Override
	public Matrix getRotationInverse(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastRotationInverse == null)
		{
			//compute heading/pitch/roll transform
			Matrix transform = Matrix.fromRotationZ(roll).multiply(
					Matrix.fromRotationXYZ(pitch, Angle.ZERO, heading.multiply(-1)));

			//compute rotation for current position on globe
			Vec4 up = globe.computeNorthPointingTangentAtLocation(center.getLatitude(), center.getLongitude());
			Vec4 f = globe.computeSurfaceNormalAtLocation(center.getLatitude(), center.getLongitude());
			Vec4 s = f.cross3(up);
			Vec4 u = s.cross3(f);
			Matrix rotation = new Matrix(
					s.x, s.y, s.z, 0.0,
					u.x, u.y, u.z, 0.0,
					-f.x, -f.y, -f.z, 0.0,
					0.0, 0.0, 0.0, 1.0);

			//multiply the two rotations to get the center->eye rotation matrix
			lastRotationInverse = transform.multiply(rotation);
		}
		return lastRotationInverse;
	}

	@Override
	public Vec4 getForward(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastForward == null)
		{
			Matrix rotation = getRotation(globe);
			lastForward = lastSide != null && lastUp != null ?
					lastSide.cross3(lastUp) :
					Vec4.UNIT_Z.transformBy3(rotation);
		}
		return lastForward;
	}

	@Override
	public Vec4 getUp(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastUp == null)
		{
			Matrix rotation = getRotation(globe);
			lastUp = lastForward != null && lastSide != null ?
					lastForward.cross3(lastSide) :
					Vec4.UNIT_Y.transformBy3(rotation);
		}
		return lastUp;
	}

	@Override
	public Vec4 getSide(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastSide == null)
		{
			Matrix rotation = getRotation(globe);
			lastSide = lastUp != null && lastForward != null ?
					lastUp.cross3(lastForward) :
					Vec4.UNIT_X.transformBy3(rotation);
		}
		return lastSide;
	}

	@Override
	public Matrix getTransform(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastTransform == null)
		{
			//gluLookAt defines s as (f x u), but getSide returns (u x f), so negate it during matrix creation
			Vec4 s = getSide(globe);
			Vec4 u = getUp(globe);
			Vec4 f = getForward(globe);
			Vec4 eye = getEyePoint(globe);
			Matrix mAxes = new Matrix(
					-s.x, -s.y, -s.z, 0.0,
					u.x, u.y, u.z, 0.0,
					-f.x, -f.y, -f.z, 0.0,
					0.0, 0.0, 0.0, 1.0);
			Matrix mEye = Matrix.fromTranslation(-eye.x, -eye.y, -eye.z);
			lastTransform = mAxes.multiply(mEye);
		}
		return lastTransform;
	}

	@Override
	public Vec4 getCenterPoint(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastCenterPoint == null)
		{
			lastCenterPoint = globe.computePointFromPosition(getCenter());
		}
		return lastCenterPoint;
	}

	@Override
	public Vec4 getEyePoint(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastEyePoint == null)
		{
			Vec4 center = getCenterPoint(globe);
			Vec4 forward = getForward(globe);
			lastEyePoint = center.add3(forward.multiply3(-zoom));
		}
		return lastEyePoint;
	}

	@Override
	public Position getEye(Globe globe)
	{
		clearCachedValuesIfGlobeChanged(globe);

		if (lastEye == null)
		{
			lastEye = globe.computePositionFromPoint(getEyePoint(globe));
		}
		return lastEye;
	}

	@Override
	public void setEye(Position eye, Globe globe)
	{
		//TODO implement this so that it doesn't modify the center point, if possible, or required?

		setRoll(Angle.ZERO);
		setPitch(Angle.ZERO);
		setHeading(Angle.ZERO);
		setZoom(eye.elevation);
		setCenter(new Position(eye, 0));
	}
}