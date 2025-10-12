package com.example.tonbo_app;

/**
 * 簡單的緯度經度類，替代Google Maps的LatLng
 */
public class LatLng {
    public final double latitude;
    public final double longitude;
    
    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    @Override
    public String toString() {
        return "LatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LatLng latLng = (LatLng) o;
        
        if (Double.compare(latLng.latitude, latitude) != 0) return false;
        return Double.compare(latLng.longitude, longitude) == 0;
    }
    
    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
