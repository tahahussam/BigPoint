package net.bigpoint.assessment.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.logging.Logger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.bigpoint.assessment.gasstation.DrehbahnGasStation;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

public class DrehbahnGasStationTest {

	private static Logger logger = Logger.getLogger(DrehbahnGasStationTest.class.getName());
	// This object to manage the station through the manager of this station
	private static DrehbahnGasStation drehbahnGasStationManager;

	private static final double dieselPrice = 5.0, regularPrice = 6.0, superPrice = 7.0;

	private static final int nTooExpensiveCustomers = 10;
	private static DoubleAccumulator revenue = new DoubleAccumulator((x, y) -> x + y, 0.0);
	private static AtomicInteger nSales = new AtomicInteger(0), nNoGas = new AtomicInteger(0);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		drehbahnGasStationManager = new DrehbahnGasStation();

		logger.info("Adding Gas Prices");
		// set prices of Gas
		drehbahnGasStationManager.setPrice(GasType.DIESEL, dieselPrice);
		drehbahnGasStationManager.setPrice(GasType.REGULAR, regularPrice);
		drehbahnGasStationManager.setPrice(GasType.SUPER, superPrice);

		logger.info("Adding Pumps");
		// add Pumps
		drehbahnGasStationManager.addGasPump(new GasPump(GasType.DIESEL, 1000));
		drehbahnGasStationManager.addGasPump(new GasPump(GasType.DIESEL, 500));

		drehbahnGasStationManager.addGasPump(new GasPump(GasType.REGULAR, 2000));
		drehbahnGasStationManager.addGasPump(new GasPump(GasType.REGULAR, 2500));

		drehbahnGasStationManager.addGasPump(new GasPump(GasType.SUPER, 3000));
		drehbahnGasStationManager.addGasPump(new GasPump(GasType.SUPER, 1500));

		logger.info("Station setup is finished ");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		logger.info("DrehbahnGasStation today prices Was:-");

		logger.info("DIESEL: " + drehbahnGasStationManager.getPrice(GasType.DIESEL));
		logger.info("REGULAR: " + drehbahnGasStationManager.getPrice(GasType.REGULAR));
		logger.info("SUPER: " + drehbahnGasStationManager.getPrice(GasType.SUPER));

		logger.info(
				"Number Of Cancellations because of Too Expensive : " + drehbahnGasStationManager.getNumberOfCancellationsTooExpensive());

		logger.info("Number Of Cancellations because No Sufficient Gas : " + drehbahnGasStationManager.getNumberOfCancellationsNoGas());

		logger.info("Number Of Successfull Sales : " + drehbahnGasStationManager.getNumberOfSales());

		logger.info("Revenue : " + drehbahnGasStationManager.getRevenue());

		List<GasPump> pumps = (List<GasPump>) drehbahnGasStationManager.getGasPumps();

		logger.info("By the end of the day");
		for (GasPump gasPump : pumps)
			logger.info("Gas Pump with type: " + gasPump.getGasType() + " has : " + gasPump.getRemainingAmount() + " Liters");
	}

	@BeforeTest
	public void setUp() throws Exception {
	}

	@AfterTest
	public void tearDown() throws Exception {
	}

	@Test
	public void testModifyingGetGasPumpsShouldNotAffectGasStation() {

		DrehbahnGasStation drehbahnGasStation = new DrehbahnGasStation();

		Collection<GasPump> gasPumpsList = drehbahnGasStation.getGasPumps();

		// assert that list contains items
		assertTrue(gasPumpsList.size() == 6);

		// clear the list
		gasPumpsList.clear();

		// assert that the original list still contains items
		assertTrue(drehbahnGasStation.getGasPumps().size() > 0);
	}

	@Test(threadPoolSize = nTooExpensiveCustomers, invocationCount = nTooExpensiveCustomers)
	public void testGasPriceGraterThanRequestedByCustomer() {
		DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
		try {
			gasStationWorker.buyGas(GasType.DIESEL, 10, 0 /* Gas For Free */);
		} catch (NotEnoughGasException | GasTooExpensiveException e) {
			logger.info("Exception catched");
			assertTrue(e instanceof GasTooExpensiveException);
		}
		assertEquals(0, drehbahnGasStationManager.getNumberOfSales());
		assertEquals(0.0, drehbahnGasStationManager.getRevenue(), 0.0);
		assertEquals(0, drehbahnGasStationManager.getNumberOfCancellationsNoGas());
	}

	@Test(dependsOnMethods = { "testGasPriceGraterThanRequestedByCustomer" })
	public void testServingAllCars() {
		final int nCars = 6;
		ExecutorService executor = Executors.newFixedThreadPool(nCars);
		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.DIESEL, 10, dieselPrice));
					nSales.incrementAndGet();
				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
				}
			});

		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.REGULAR, 10, regularPrice));
					nSales.incrementAndGet();
				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
				}
			});

		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.SUPER, 10, superPrice));
					nSales.incrementAndGet();
				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
				}
			});

		executor.shutdown();

		while (!executor.isTerminated()) {
		}

		assertEquals(revenue.get(), drehbahnGasStationManager.getRevenue(), 0.0);
		assertEquals(nTooExpensiveCustomers, drehbahnGasStationManager.getNumberOfCancellationsTooExpensive());
		assertEquals(nNoGas.get(), drehbahnGasStationManager.getNumberOfCancellationsNoGas());
		assertEquals(nSales.get(), drehbahnGasStationManager.getNumberOfSales());
	}

	@Test(dependsOnMethods = { "testServingAllCars" })
	public void testNotEnoughGasForAllCars() {
		final int nCars = 6;
		ExecutorService executor = Executors.newFixedThreadPool(nCars);
		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					gasStationWorker.buyGas(GasType.DIESEL, Integer.MAX_VALUE, dieselPrice);
					nSales.incrementAndGet();

				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					gasStationWorker.buyGas(GasType.REGULAR, Integer.MAX_VALUE, regularPrice);
					nSales.incrementAndGet();

				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		for (int i = 0; i < 2; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					gasStationWorker.buyGas(GasType.SUPER, Integer.MAX_VALUE, superPrice);
					nSales.incrementAndGet();

				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		executor.shutdown();

		while (!executor.isTerminated()) {
		}

		assertEquals(revenue.get(), drehbahnGasStationManager.getRevenue(), 0.0);
		assertEquals(nTooExpensiveCustomers, drehbahnGasStationManager.getNumberOfCancellationsTooExpensive());
		assertEquals(nNoGas.get(), drehbahnGasStationManager.getNumberOfCancellationsNoGas());
		assertEquals(nSales.get(), drehbahnGasStationManager.getNumberOfSales());
	}

	@Test(dependsOnMethods = { "testNotEnoughGasForAllCars" })
	public void testAlotOfCarsSpecialCases() {
		final int nCars = 18;
		ExecutorService executor = Executors.newFixedThreadPool(nCars);
		for (int i = 0; i < 6; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.DIESEL, 100, dieselPrice));
					nSales.incrementAndGet();

				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();

					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		for (int i = 0; i < 6; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.REGULAR, 100, regularPrice));
					nSales.incrementAndGet();
				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		for (int i = 0; i < 6; i++)
			executor.submit(() -> {
				DrehbahnGasStation gasStationWorker = new DrehbahnGasStation();
				try {
					revenue.accumulate(gasStationWorker.buyGas(GasType.SUPER, 100, superPrice));
					nSales.incrementAndGet();
				} catch (NotEnoughGasException | GasTooExpensiveException e) {
					logger.info("Exception catched");
					nNoGas.incrementAndGet();
					assertTrue(e instanceof NotEnoughGasException);
				}
			});

		executor.shutdown();

		while (!executor.isTerminated()) {
		}

		assertEquals(revenue.get(), drehbahnGasStationManager.getRevenue(), 0.0);
		assertEquals(nTooExpensiveCustomers, drehbahnGasStationManager.getNumberOfCancellationsTooExpensive());
		assertEquals(nNoGas.get(), drehbahnGasStationManager.getNumberOfCancellationsNoGas());
		assertEquals(nSales.get(), drehbahnGasStationManager.getNumberOfSales());
	}
}