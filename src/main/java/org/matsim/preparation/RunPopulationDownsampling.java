package org.matsim.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;

public class RunPopulationDownsampling {

    private final String inputPopFilename;
    private final String outputPopFilename;

    private RunPopulationDownsampling(String inputPopFilename, String outputPopFilename) {
        this.inputPopFilename = inputPopFilename;
        this.outputPopFilename = outputPopFilename;
    }

    public static void main(final String[] args) {

        String outputPopFilename = "scenarios/berlin/replaceCarByDRT/noModeChoice/berlin-v5.5-sample.plans.xml.gz";
        String inputPopFilename = "scenarios/berlin/replaceCarByDRT/noModeChoice/testConfig/berlin-v5.5-1pct.plans.xml.gz";

       /* if ( args!=null ) {
            if (args.length != 2) {
                System.err.println("Usage: cmd inputPop.xml.gz outputPop.xml.gz");
                System.exit(401);
            } else {
                inputPopFilename = args[0] ;
                outputPopFilename = args[1] ;
            }
        }*/

        RunPopulationDownsampling app = new RunPopulationDownsampling(inputPopFilename, outputPopFilename);
        app.run();
    }

    private void run() {

        // create an empty scenario using an empty configuration
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        // the writer will be called by the reader and write the new population file. As parameter the fraction of the
        // input population is passed. In our case we will downsize the population to 1%.
        StreamingPopulationWriter writer = new StreamingPopulationWriter(0.0001);

        // the reader will read in an existing population file
        StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
        reader.addAlgorithm(writer);

        try {
            writer.startStreaming(outputPopFilename);
            reader.readFile(inputPopFilename);
        } finally {
            writer.closeStreaming();
        }
    }
}
