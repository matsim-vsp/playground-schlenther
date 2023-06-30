package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.replaceCarByDRT.RunBerlinNoInnerCarTripsScenario;

/**
 * @author Hugo
 */


public class BerlinRunOnlineAccidents {

    private static final Logger log = Logger.getLogger(RunBerlinAccidents.class);
    private final String runDirectory;
    private final String runId;
    private final String analysisOutputDirectory;
    private final String runType;

    private final String shapeFile;
    private final String roadTypesCarAllowed;
    private final String stationsFile;
    private final String prStationChoice;
    private final String replacingModes;
    private final String enforceMassConservation;
    private final String extraPtPlan;
    private final String drtStopBased;
    private final String drtStops;

    public BerlinRunOnlineAccidents(String runDirectory, String runId, String analysisOutputDirectory, String runType, String shapeFile, String roadTypesCarAllowed, String stationsFile, String prStationChoice, String replacingModes, String enforceMassConservation, String extraPtPlan, String drtStopBased, String drtStops){
        this.runDirectory = runDirectory;
        this.runId = runId;
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
        String prStationChoice = "";
        String replacingModes = "";
        String enforceMassConservation = "";
        String extraPtPlan = "";
        String drtStopBased = "";
        String drtStops = "";

        if (args.length == 0) {
            runDirectory = "scenarios/output/runs-2023-05-26/baseCaseContinued/";
            runId = "berlin-v5.5-1pct";
            runType = "base";

            shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            prStationChoice = "closestToOutSideActivity";
            replacingModes = "pt,drt";
            enforceMassConservation = "true";
            extraPtPlan = "false";
            drtStopBased = "false";
            drtStops = "scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-03-29-prStations.xml";
        } if (args.length == 13) {
            runDirectory = args[0];
            runId = args[1];
            analysisOutputDirectory = args[2];
            runType = args[3];

            shapeFile = args[4];
            roadTypesCarAllowed = args[5];
            stationsFile = args[6];
            prStationChoice = args[7];
            replacingModes = args[8];
            enforceMassConservation = args[9];
            extraPtPlan = args[10];
            drtStopBased = args[11];
            drtStops = args[12];


        } else {
            System.out.println("Insufficient arguments provided.");
        }

        BerlinRunOnlineAccidents accAnalysis = new BerlinRunOnlineAccidents(runDirectory,
                runId,
                analysisOutputDirectory,
                runType,
                shapeFile,
                roadTypesCarAllowed,
                stationsFile,
                prStationChoice,
                replacingModes,
                enforceMassConservation,
                extraPtPlan,
                drtStopBased,
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
                    prStationChoice,
                    replacingModes,
                    enforceMassConservation,
                    extraPtPlan,
                    drtStopBased,
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
