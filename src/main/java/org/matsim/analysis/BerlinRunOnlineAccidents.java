package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.replaceCarByDRT.RunBerlinNoInnerCarTripsScenario;

import javax.management.RuntimeErrorException;

/**
 *
 * This method is mostly a copy from the Open Berlin Scenario.
 *
 * @author Hugo
 */


public class BerlinRunOnlineAccidents {

    private final String runDirectory;
    private final String runId;
    private final String analysisOutputDirectory;
    private final String runType;

    // all these are only necessary for the policy run.
    private final String shapeFile;
    private final String roadTypesCarAllowed;
    private final String stationsFile;
    private final String replacingModes;
    private final String drtStops;

    public BerlinRunOnlineAccidents(String runDirectory, String runId, String analysisOutputDirectory, String runType, String shapeFile, String roadTypesCarAllowed, String stationsFile,
                                    String replacingModes, String drtStops){
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.analysisOutputDirectory = analysisOutputDirectory;
        this.runType = runType;

        this.shapeFile = shapeFile;
        this.roadTypesCarAllowed = roadTypesCarAllowed;
        this.stationsFile = stationsFile;
        this.replacingModes = replacingModes;
        this.drtStops = drtStops;

    }

    public static void main(String[] args) {

        String runDirectory = "";
        String runId = "";
        String analysisOutputDirectory = "";
        String runType = "";

        String shapeFile = "";
        String roadTypesCarAllowed = "";
        String stationsFile = "";
        String replacingModes = "";
        String drtStops = "";

        if (args.length == 0) {
            runDirectory = "scenarios/output/runs-2023-05-26/baseCaseContinued/";
            runId = "berlin-v5.5-1pct";
            runType = "base";

            shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            replacingModes = "pt,drt";
            drtStops = "scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-03-29-prStations.xml";

        } if (args.length == 9) {
            runDirectory = args[0];
            runId = args[1];
            analysisOutputDirectory = args[2];
            runType = args[3];

            shapeFile = args[4];
            roadTypesCarAllowed = args[5];
            stationsFile = args[6];
            replacingModes = args[7];
            drtStops = args[8];

        } else {
            System.err.println("Insufficient arguments provided.");
        }

        BerlinRunOnlineAccidents accAnalysis = new BerlinRunOnlineAccidents(runDirectory,
                runId,
                analysisOutputDirectory,
                runType,
                shapeFile,
                roadTypesCarAllowed,
                stationsFile,
                replacingModes,
                drtStops);

        accAnalysis.run();
    }

    void run() {
        String outputConfigName = runDirectory + runId + ".output_config.xml";

        String BVWPNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network-with-bvwp-accidents-attributes.xml.gz";
        String plans = runId + ".output_plans.xml.gz";

        Config config;
        Scenario scenario;
        Controler controler;

        if (runType.equals("policy")){

            String[] preparationArgs = new String[] {
                    shapeFile,
                    roadTypesCarAllowed,
                    stationsFile,
                    replacingModes,
                    drtStops,
                    outputConfigName
            };

            String[] configArgs;

            configArgs = RunBerlinNoInnerCarTripsScenario.prepareConfigArguments(preparationArgs);
            config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
        } else if (runType.equals("base")) {
            config = RunBerlinScenario.prepareConfig(new String[]{outputConfigName});
        } else {
            config = RunBerlinScenario.prepareConfig(new String[]{outputConfigName});
        }

        config.controler().setOutputDirectory(analysisOutputDirectory);
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(0);
        config.plans().setInputFile(plans);

        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        accidentsSettings.setEnableAccidentsModule(true);
        accidentsSettings.setScaleFactor(10);
        config.network().setInputFile(BVWPNetwork);
        //config.planCalcScore().getModes().get("car").setMonetaryDistanceRate(-0.0004);

        if (runType.equals("policy")){
            scenario = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
            controler = RunBerlinNoInnerCarTripsScenario.prepareControler(scenario);
        } else if (runType.equals("base")){
            scenario = RunBerlinScenario.prepareScenario(config);
            controler = RunBerlinScenario.prepareControler(scenario);
        } else {
            scenario = RunBerlinScenario.prepareScenario(config);
            controler = RunBerlinScenario.prepareControler(scenario);
        }

        controler.addOverridingModule(new AccidentsModule());
        controler.run();
    }
}
