package org.matsim.run.replaceCarByDRT;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

class RunBerlinNoInnerCarTripsScenarioIT {

	@Test
	void main() {
		try {
			RunBerlinNoInnerCarTripsScenario.main(new String[0]);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}