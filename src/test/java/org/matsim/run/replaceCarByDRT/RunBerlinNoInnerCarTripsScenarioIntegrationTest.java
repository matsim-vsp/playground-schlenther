package org.matsim.run.replaceCarByDRT;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

public class RunBerlinNoInnerCarTripsScenarioIntegrationTest {

	@Test
	public void main() {
		try {
			RunBerlinNoInnerCarTripsScenario.main(new String[0]);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}