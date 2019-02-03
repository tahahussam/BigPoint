package net.bigpoint.assessment.gasstation;


import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.logging.Logger;

import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

public class DrehbahnGasStation implements GasStation {

	private static Logger logger = Logger.getLogger(DrehbahnGasStation.class.getName());

	private static AtomicInteger numberOfSales = new AtomicInteger(0), noGasCancellations = new AtomicInteger(0),
			tooExpensiveCancellations = new AtomicInteger(0);

	private static DoubleAccumulator revenue = new DoubleAccumulator((x, y) -> x + y, 0.0);

	private static Map<GasType, List<GasPump>> gasPumpsMap = new Hashtable<>();

	@Override
	public void addGasPump(GasPump pump) {
		logger.info("addGasPump called with pump : " + pump);

		if (!gasPumpsMap.containsKey(pump.getGasType()))
			gasPumpsMap.put(pump.getGasType(), new Vector<>());

		gasPumpsMap.get(pump.getGasType()).add(pump);

		logger.info("After adding gasPumpsList size : " + gasPumpsMap.get(pump.getGasType()).size());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<GasPump> getGasPumps() {
		logger.info("getGasPumps called");

		List<GasPump> clonedGasPumps = new Vector<>();

		for (Entry<GasType, List<GasPump>> mapEntry : gasPumpsMap.entrySet())
			clonedGasPumps.addAll((Vector<GasPump>) ((Vector<GasPump>) mapEntry.getValue()).clone());

		return clonedGasPumps;
	}

	@Override
	public double buyGas(GasType gasType, double amountInLiters, double maxPricePerLiter)
			throws NotEnoughGasException, GasTooExpensiveException {

		logger.info("Car needs " + amountInLiters + " Liters from " + gasType + " with price: " + maxPricePerLiter);

		if (gasType.getPrice() > maxPricePerLiter) {
			logger.info(gasType + " price is greater than max Price Per Liter so will not buy Gas");
			tooExpensiveCancellations.incrementAndGet();
			throw new GasTooExpensiveException();
		}

		if (!gasPumpsMap.containsKey(gasType) || gasPumpsMap.get(gasType).isEmpty()) {
			logger.info("can't found any matched Gas pumps for " + gasType);
			noGasCancellations.incrementAndGet();
			throw new NotEnoughGasException();
		}
		List<GasPump> pumpsForGasTypeList = gasPumpsMap.get(gasType);

		GasPump firstAvailablePump = null;
		synchronized (pumpsForGasTypeList) {

			// To Get The Pump with the maximum amount to use it for serving.
//			pumpsForGasTypeList.sort(Comparator.comparing(GasPump::getRemainingAmount).reversed());

			// To Get The Pump with the nearest amount to the requested amountInLiters to
			// not waste Gas
//			pumpsForGasTypeList.sort((pump1, pump2) -> (Double.compare(pump1.getRemainingAmount() - amountInLiters,
//					pump2.getRemainingAmount() - amountInLiters)));

			// select first available pump to serve
			int i = 0;
			for (i = 0; i < pumpsForGasTypeList.size(); i++)
				if (pumpsForGasTypeList.get(i).getRemainingAmount() >= amountInLiters) {
					firstAvailablePump = pumpsForGasTypeList.get(i);
					break;
				}

			if (firstAvailablePump != null)
				pumpsForGasTypeList.remove(i);
		}

		if (firstAvailablePump != null) {
			logger.info("Found a pump and will buy Gas");

			firstAvailablePump.pumpGas(amountInLiters);
			pumpsForGasTypeList.add(firstAvailablePump);

			double priceToPay = amountInLiters * gasType.getPrice();

			revenue.accumulate(priceToPay);
			numberOfSales.incrementAndGet();

			return priceToPay;
		} else {
			logger.info("all pumps doesn't contain " + amountInLiters + " Liters or busy");
			noGasCancellations.incrementAndGet();
			throw new NotEnoughGasException();
		}
	}

	@Override
	public double getRevenue() {
		return revenue.doubleValue();
	}

	@Override
	public int getNumberOfSales() {
		return numberOfSales.get();
	}

	@Override
	public int getNumberOfCancellationsNoGas() {
		return noGasCancellations.get();
	}

	@Override
	public int getNumberOfCancellationsTooExpensive() {
		return tooExpensiveCancellations.get();

	}

	@Override
	public double getPrice(GasType gasType) {
		return gasType.getPrice();
	}

	@Override
	public void setPrice(GasType gasType, double price) {
		gasType.setPrice(price);
	}
}