/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28.02.2015
 */

package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.interestrate.products.components.ProductCollection;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.FixedCoupon;
import net.finmath.montecarlo.interestrate.products.indices.LinearCombinationIndex;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.ScheduleInterface;

/**
 * @author Christian Fries
 *
 */
public class SwapLeg extends AbstractLIBORMonteCarloProduct {

	private final ScheduleInterface				legSchedule;
	private final AbstractNotional				notional;
	private final AbstractIndex					index;
	private final double						spread;
	private boolean								isNotionalExchanged;

	private final ProductCollection				components;
	
	/**
	 * Creates a swap leg. The swap leg is build from elementary components
	 * 
	 * @param legSchedule Schedule of the leg.
     * @param notional The notional.
     * @param index The index.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
     */
    public SwapLeg(ScheduleInterface legSchedule, AbstractNotional notional, AbstractIndex index, double spread, boolean isNotionalExchanged) {
	    super();
	    this.legSchedule = legSchedule;
	    this.notional = notional;
	    this.index = index;
	    this.spread = spread;
	    this.isNotionalExchanged = isNotionalExchanged;

		/*
		 * Create components.
		 * 
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */
		Collection<AbstractProductComponent> periods = new ArrayList<AbstractProductComponent>();
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate	= legSchedule.getFixing(periodIndex);
			double paymentDate	= legSchedule.getPayment(periodIndex);
			double periodLength	= legSchedule.getPeriodLength(periodIndex);
			
			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified
			 * product, it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) continue;

			AbstractIndex coupon;
			if(index != null) {
				if(spread != 0)	coupon = new LinearCombinationIndex(1, index, 1, new FixedCoupon(spread));
				else			coupon = index;
			}
			else {
				coupon = new FixedCoupon(spread);
			}

			Period period = new Period(fixingDate, paymentDate, fixingDate, paymentDate, notional, coupon, periodLength, true, isNotionalExchanged, false);
			periods.add(period);
		}

		components = new ProductCollection(periods);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return components.getValue(evaluationTime, model);
	}

}
