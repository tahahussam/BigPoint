package net.bigpoint.assessment.gasstation;

public enum GasType {

	REGULAR, SUPER, DIESEL;

	// to identify the price of each type
	private double price;

	/**
	 * @return the price of each type
	 */
	public double getPrice() {
		return price;
	}

	/**
	 * @param price set the price of each type
	 */
	public void setPrice(double price) {
		this.price = price;
	}

}
