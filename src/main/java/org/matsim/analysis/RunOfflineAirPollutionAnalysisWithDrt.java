package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author ikaddoura
 */

public class RunOfflineAirPollutionAnalysisWithDrt {

    private final String runDirectory;
    private final String runId;
    private final String hbefaWarmFile;
    private final String hbefaColdFile;
    private final String analysisOutputDirectory;

    public static void main(String[] args) {

        final String runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
        final String runId = "massConservation-1506vehicles-8seats";
        final String outputDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/analysis/airPollution/";

        final String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/b63f949211b7c93776cdce8a7600eff4e36460c8.enc";
        final String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/f5b276f41a0531ed740a81f4615ec00f4ff7a28d.enc";

        RunOfflineAirPollutionAnalysisWithDrt analysis = new RunOfflineAirPollutionAnalysisWithDrt(
                runDirectory,
                runId,
                hbefaFileWarm,
                hbefaFileCold,
                runDirectory);

        analysis.run();
    }

    public RunOfflineAirPollutionAnalysisWithDrt(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold, String analysisOutputDirectory) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.hbefaWarmFile = hbefaFileWarm;
        this.hbefaColdFile = hbefaFileCold;

        if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
        this.analysisOutputDirectory = analysisOutputDirectory;
    }

    void run() {

        Config config = ConfigUtils.createConfig();
        config.vehicles().setVehiclesFile(runDirectory + runId + ".output_vehicles.xml.gz");
        config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
        config.transit().setTransitScheduleFile(runDirectory + runId + ".output_transitSchedule.xml.gz");
        config.transit().setVehiclesFile(runDirectory + runId + ".output_transitVehicles.xml.gz");
        config.global().setCoordinateSystem("EPSG:31468");
        config.plans().setInputFile(null);
        config.parallelEventHandling().setNumberOfThreads(null);
        config.parallelEventHandling().setEstimatedNumberOfEvents(null);
        config.global().setNumberOfThreads(1);

        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
        eConfig.setAverageColdEmissionFactorsFile(this.hbefaColdFile);
        eConfig.setAverageWarmEmissionFactorsFile(this.hbefaWarmFile);
        eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
        eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);

        final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
        final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // network
        for (Link link : scenario.getNetwork().getLinks().values()) {

            double freespeed = Double.NaN;

            if (link.getFreespeed() <= 13.888889) {
                freespeed = link.getFreespeed() * 2;
                // for non motorway roads, the free speed level was reduced
            } else {
                freespeed = link.getFreespeed();
                // for motorways, the original speed levels seems ok.
            }

            if(freespeed <= 8.333333333){ //30kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
            } else if(freespeed <= 11.111111111){ //40kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
            } else if(freespeed <= 13.888888889){ //50kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 16.666666667){ //60kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 19.444444444){ //70kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
            } else if(freespeed <= 22.222222222){ //80kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
            } else if(freespeed > 22.222222222){ //faster
                link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
            } else{
                throw new RuntimeException("Link not considered...");
            }
        }

        // vehicles

        Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
        Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);

        VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
        VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);

        EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );

        EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology( freightEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( freightEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( freightEngineInformation, "average" );


        // public transit vehicles should be considered as non-hbefa vehicles
        for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();
            // TODO: Check! Is this a zero emission vehicle?!
            VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
            VehicleUtils.setHbefaTechnology( engineInformation, "average" );
            VehicleUtils.setHbefaSizeClass( engineInformation, "average" );
            VehicleUtils.setHbefaEmissionsConcept( engineInformation, "average" );
        }

        // the following is copy paste from the example...

        EventsManager eventsManager = EventsUtils.createEventsManager();

        AbstractModule module = new AbstractModule(){
            @Override
            public void install(){
                bind( Scenario.class ).toInstance( scenario );
                bind( EventsManager.class ).toInstance( eventsManager );
                bind( EmissionModule.class ) ;
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();
    }

}
