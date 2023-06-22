package org.matsim.analysis;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.run.replaceCarByDRT.RunBerlinNoInnerCarTripsScenario;

/**
 * @author Hugo
 */


public class RunAccidentsWithDrt {

    private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
    private final String runDirectory;
    private final String runId;
    private final String analysisOutputDirectory;
    private final String runType;

    private static String shapeFile;
    private static String roadTypesCarAllowed;
    private static String stationsFile;
    private static String prStationChoice;
    private static String replacingModes;
    private static String enforceMassConservation;
    private static String extraPtPlan;
    private static String drtStopBased;

    public RunAccidentsWithDrt(String runDirectory, String runId, String analysisOutputDirectory, String runType, String shapeFile, String roadTypesCarAllowed, String stationsFile, String prStationChoice, String replacingModes, String enforceMassConservation, String extraPtPlan, String drtStopBased){
        this.runDirectory = runDirectory;
        this.runId = runId;
        if (!analysisOutputDirectory.endsWith("/")) {
            analysisOutputDirectory = analysisOutputDirectory + "/";
        }

        this.analysisOutputDirectory = analysisOutputDirectory;
        this.runType = runType;

        this.shapeFile = shapeFile;
        this.roadTypesCarAllowed = roadTypesCarAllowed;
        this.stationsFile = stationsFile;
        this.prStationChoice = prStationChoice;
        this.replacingModes = replacingModes;
        this.enforceMassConservation = enforceMassConservation;
        this.extraPtPlan = extraPtPlan;
        this.drtStopBased = drtStopBased;

    }

    public static void main(String[] args) {

        String runDirectory = "scenarios/output/sample/";
        String runId = "sample-run";
        String outputDirectory = "scenarios/output/accidents-test/";
        String runType = "policy";

        shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
        roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
        stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
        prStationChoice = "closestToOutSideActivity";
        replacingModes = "pt,drt";
        enforceMassConservation = "false";
        extraPtPlan = "true";
        drtStopBased = "false";

        RunAccidentsWithDrt accAnalysis = new RunAccidentsWithDrt(runDirectory, runId, outputDirectory, runType, shapeFile, roadTypesCarAllowed, stationsFile, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased);
        accAnalysis.run();
    }

    void run() {
        String outputConfigName = this.runDirectory + this.runId + ".output_config.xml";

        String BVWPNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network-with-bvwp-accidents-attributes.xml.gz";
        String plans = this.runId + ".output_plans.xml.gz";

        Config config;
        Scenario scenario;
        Controler controler;

        if (this.runType.equals("policy")){
            //TODO: Change for 10pct-analysis
            shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            prStationChoice = "closestToOutSideActivity";
            replacingModes = "pt,drt";
            enforceMassConservation = "false";
            extraPtPlan = "true";
            drtStopBased = "false";

            String[] preparationArgs = new String[] {
                    shapeFile,
                    roadTypesCarAllowed,
                    stationsFile,
                    prStationChoice,
                    replacingModes,
                    enforceMassConservation,
                    extraPtPlan,
                    drtStopBased,
                    outputConfigName
            };

            String[] configArgs;

            configArgs = RunBerlinNoInnerCarTripsScenario.prepareConfigArguments(preparationArgs);
            config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
        } else if (this.runType.equals("base")) {
            config = RunBerlinScenario.prepareConfig(new String[]{outputConfigName});
        } else {
            config = RunBerlinScenario.prepareConfig(new String[]{outputConfigName});
        }

        config.controler().setOutputDirectory(this.analysisOutputDirectory);
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(0);
        config.plans().setInputFile(plans);

        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        accidentsSettings.setEnableAccidentsModule(true);
        accidentsSettings.setScaleFactor(10);
        config.network().setInputFile(BVWPNetwork);
        //config.planCalcScore().getModes().get("car").setMonetaryDistanceRate(-0.0004);

        if (this.runType.equals("policy")){
            scenario = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
            controler = RunBerlinNoInnerCarTripsScenario.prepareControler(scenario);
        } else if (this.runType.equals("base")){
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
