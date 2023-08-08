package org.matsim.run.replaceCarByDRT;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class RunBerlinNoInnerCarTripsScenarioIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void main() {
		String[] configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-0.1pct.config.test.xml",
				"--config:controler.lastIteration", "1",
				"--config:controler.outputDirectory", utils.getOutputDirectory()};
		try {
			Config config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
			Scenario scenario = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
			Controler controler = RunBerlinNoInnerCarTripsScenario.prepareControler(scenario);
			controler.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}