package com.tomato.testsamples;

import android.os.Binder;

public class LocalBinder extends Binder {
	private double distance = 30.0;
	
	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getDistance() {
		return this.distance;
	}
}