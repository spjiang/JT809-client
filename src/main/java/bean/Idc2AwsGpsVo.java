package bean;

public class Idc2AwsGpsVo {

    private double lon;

    private double lat;

    private  short speed;
    private  short speed2;

    private  int mileage; //历程

    private short direction;//方向

    private  String vehicleNo;

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public short getSpeed() {
        return speed;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public short getDirection() {
        return direction;
    }

    public void setDirection(short direction) {
        this.direction = direction;
    }

	/**
	 * @return the speed2
	 */
	public short getSpeed2() {
		return speed2;
	}

	/**
	 * @param speed2 the speed2 to set
	 */
	public void setSpeed2(short speed2) {
		this.speed2 = speed2;
	}
    
}
