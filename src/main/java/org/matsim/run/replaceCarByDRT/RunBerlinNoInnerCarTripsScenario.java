/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run.replaceCarByDRT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import org.matsim.analysis.PrActivityEventHandler;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.run.BerlinExperimentalConfigGroup;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 *
 * For all trips that originate <i>and</i> end within the drt service area, car and ride is not allowed.
 * This modelled by setting all corresponding car and ride trips to drt.<br>
 * In this scenario, no mode choice is modeled!
 * This serves as similar study as the taxi study conducted by Joschka Bischoff, but with a pooling service.<br>
 * Trips originating <i>or</i> ending in Berlin are not touched, here.
 */
//can not extend MATSimApplication because matsim-berlin is not designed for that (yet).
//@CommandLine.Command( header = ":: MyScenario ::", version = "1.0")
public class RunBerlinNoInnerCarTripsScenario /*extends MATSimApplication*/ {

	private static final Logger log = Logger.getLogger(RunBerlinNoInnerCarTripsScenario.class);
	private static Set<String> REPLACING_MODES;

	private static URL URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE;
	private static URL URL_2_DRT_STOPS;
	protected static URL URL_2_PR_STATIONS;

	private static CarsAllowedOnRoadTypesInsideBanArea ROAD_TYPES_CAR_ALLOWED;

	public static void main(String[] args) throws MalformedURLException {

		String[] configArgs;
		if ( args.length==0 ) {
			//careful if you change this: you would probably want to adjust the drt service area as well!
			URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp");
//			URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE = IOUtils.resolveFileOrResource("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/S5/berlin-hundekopf-minus-250m.shp");
			ROAD_TYPES_CAR_ALLOWED = CarsAllowedOnRoadTypesInsideBanArea.motorwayAndPrimaryAndTrunk;
			URL_2_PR_STATIONS = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv");
			REPLACING_MODES = Set.of(TransportMode.drt, TransportMode.pt);
			URL_2_DRT_STOPS = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-03-29-prStations.xml");

			String OUTPUT_DIRECTORY = "./scenarios/output/sample-test/";
			int LAST_ITERATION = 0;
			String RUN_ID = "sample-run";

			configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-1pct.config.xml",
					"--config:controler.lastIteration", String.valueOf(LAST_ITERATION),
					"--config:controler.outputDirectory", OUTPUT_DIRECTORY,
					"--config:controler.runId", RUN_ID};


//			URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE = IOUtils.resolveFileOrResource("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/S5/berlin-minus-500m-buffer.shp");
//			ROAD_TYPES_CAR_ALLOWED = CarsAllowedOnRoadTypesInsideBanArea.motorwayAndPrimaryAndTrunk;
//			URL_2_PR_STATIONS = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2022-11-17-pr-stations-berlin.tsv");
//			PR_STATION_CHOICE = ReplaceCarByDRT.PRStationChoice.closestToOutSideActivity;
//			configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-1pct.config.test.xml",
//					"--config:controler.lastIteration", "0" ,
//					"--config:controler.outputDirectory", "./scenarios/output/berlin-v5.5-10pct/replaceCarByDRT-BERLIN-testLichtenrade",
//					"--config:multiModeDrt.drt[mode=drt].vehiclesFile",  "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/berlin-drt-v5.5-10pct/input/drtVehicles/berlin-drt-v5.5.drt-by-rndLocations-10000vehicles-4seats.xml.gz",
//					"--config:multiModeDrt.drt[mode=drt].drtServiceAreaShapeFile", "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/pave/shp-files/S5/berlin-plus-500m-buffer.shp"};


		} else {
			configArgs = prepareConfigArguments(args);
		}

		Config config = prepareConfig(configArgs);
		Scenario scenario = prepareScenario(config);

		Controler controler = prepareControler(scenario);
		controler.run();
		RunBerlinScenario.runAnalysis(controler);
	}

	public static String[] prepareConfigArguments(String[] args){
		String[] configArgs;

		URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE = IOUtils.resolveFileOrResource(args[0]);
		ROAD_TYPES_CAR_ALLOWED = CarsAllowedOnRoadTypesInsideBanArea.valueOf(args[1]);
		URL_2_PR_STATIONS = IOUtils.resolveFileOrResource(args[2]);
		REPLACING_MODES = Set.of(args[3].split(","));
		URL_2_DRT_STOPS = IOUtils.resolveFileOrResource(args[4]);
		configArgs = new String[args.length-5];
		System.arraycopy(args, 5, configArgs, 0, args.length - 5);

		return configArgs;
	}

	public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
		Config config = RunDrtOpenBerlinScenario.prepareConfig(args, customModules);
		disableModeChoiceAndDistributeStrategyWeights(config);

		PlanCalcScoreConfigGroup.ActivityParams actParams = new PlanCalcScoreConfigGroup.ActivityParams(ReplaceCarByDRT.PR_ACTIVITY_TYPE);
		actParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(actParams);

		//for the time being, assume one drt mode only
		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
		DvrpConfigGroup dvrpConfigGroup = DvrpConfigGroup.get(config);
		IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

		PlanCalcScoreConfigGroup.ModeParams ptParams = config.planCalcScore().getModes().get(TransportMode.pt);
		PlanCalcScoreConfigGroup.ModeParams drtParams = config.planCalcScore().getModes().get(drtCfg.getMode());
		Preconditions.checkArgument(ptParams.getDailyMonetaryConstant() == drtParams.getDailyMonetaryConstant(), "in this scenario, we assume fare integration of pt and drt.\n" +
				"in the open berlin scenario, pt fare is modeled via dailyMonetaryConstant. So should it be for drt");

		//sets the drt mode to be dvrp network mode. sets fare compensitions for agents using both pt and drt
		configureDVRPAndDRT(dvrpConfigGroup, drtCfg, compensatorsConfig);

		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
		if (berlinCfg.getTagDrtLinksBufferAroundServiceAreaShp() <= 0.){
			double buffer = 2000.;
			log.warn("we need to add " + drtCfg.getMode() + " as allowed mode to the links in the service area.\n" +
					"Will set " + BerlinExperimentalConfigGroup.GROUP_NAME + ".tagDrtLinksBufferAroundServiceAreaShp to " + buffer);
			berlinCfg.setTagDrtLinksBufferAroundServiceAreaShp(buffer);
		}

		return config;
	}

	private static void disableModeChoiceAndDistributeStrategyWeights(Config config) {
		//disable mode choice strategies
		Collection<StrategyConfigGroup.StrategySettings> strategySettings = config.strategy().getStrategySettings();
		List<StrategyConfigGroup.StrategySettings> rerouteSettings = new ArrayList<>();
		StrategyConfigGroup.StrategySettings selector = null;

		double totalOldWeightModeChoiceStrategies = 0;
		for (StrategyConfigGroup.StrategySettings strategySetting : strategySettings) {
			if(strategySetting.getSubpopulation().equals("person")){
				switch (strategySetting.getStrategyName()){
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode :
					case DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeLegMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleLegMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.TripSubtourModeChoice:
						totalOldWeightModeChoiceStrategies += strategySetting.getWeight();
						strategySetting.setWeight(0);
						break;
					case DefaultPlanStrategiesModule.DefaultStrategy.ReRoute:
					case DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute:
						rerouteSettings.add(strategySetting);
					case DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta:
						selector = strategySetting;
						break;
					default:
						break;
				}
			}
		}

		/* redistribute mode choice weight(s):
		 * 50% to the selector strategy
		 * 50% to the reroute-strategies (equally distributed between them)
		 */
		if(totalOldWeightModeChoiceStrategies > 0){
			log.warn("you had mode choice enabled via config. this scenario is meant to work without mode choice. please make sure, you do not have custom mode choice strategies enabled.\n" +
					"Following, all mode choice strategies will be disabled. Their strategy weights will be distributed to one selecotr (50%) and to all reroute strategies (the other 50%)." +
					"Please consider to decrease the number of iterations! Probably, you will not require as many iterations now, as mode choice gets disabled...");
			if(rerouteSettings.isEmpty()) throw new IllegalArgumentException("you have not configured any reroute strategy");
			selector.setWeight(selector.getWeight() + 0.5 * totalOldWeightModeChoiceStrategies );
			for (StrategyConfigGroup.StrategySettings rerouteSetting : rerouteSettings) {
				rerouteSetting.setWeight(rerouteSetting.getWeight() + 0.5/rerouteSettings.size()*totalOldWeightModeChoiceStrategies);
			}
		}
	}

	private static final void configureDVRPAndDRT(DvrpConfigGroup dvrpConfigGroup, DrtConfigGroup drtConfigGroup, IntermodalTripFareCompensatorsConfigGroup compensatorsConfig) {
		if(! dvrpConfigGroup.getNetworkModes().contains(drtConfigGroup.getMode()) ){
			log.warn("the drt mode " + drtConfigGroup.getMode() + " is not registered as network mode for dvrp - which is necessary in a bannedCarInDRTServiceArea scenario");
			log.warn("adding mode " + drtConfigGroup.getMode() + " as network mode for dvrp... ");
			dvrpConfigGroup.setNetworkModes(ImmutableSet.<String>builder()
					.addAll(dvrpConfigGroup.getNetworkModes())
					.add(drtConfigGroup.getMode())
					.build());
		}

		Preconditions.checkArgument(!drtConfigGroup.getDrtSpeedUpParams().isPresent(),
				"you are using drt-speed-up. this scenario setup is meant for experiments without mode choice, so basically, drt-speed-up should not be necessary.");

		// Setting operational scheme to stop based
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drtConfigGroup.setTransitStopFile(String.valueOf(URL_2_DRT_STOPS));
		log.warn("you are now using a stop based operational scheme for drt! This is still under development.");
		Preconditions.checkNotNull(drtConfigGroup.getTransitStopFile(),
				"this scenario currently only works with a specified stopFile for drt!");

		if(! drtConfigGroup.isUseModeFilteredSubnetwork()){
			log.warn("setting drtConfigGroup.isUseModeFilteredSubnetwork() to true! Was false before......");
			drtConfigGroup.setUseModeFilteredSubnetwork(true);
		}

		if( drtConfigGroup.getDrtFareParams().isPresent()){
			log.warn("you are using " + DrtFareParams.SET_NAME + "params. Will now override all fare values therein to 0, because we assume pt and drt fare integration. In Berlin, this is modeled via dailyMonetaryConstant.");
			DrtFareParams fares = drtConfigGroup.getDrtFareParams().get();
			fares.setBaseFare(0);
			fares.setTimeFare_h(0);
			fares.setDailySubscriptionFee(0);
			fares.setDistanceFare_m(0);
			fares.setMinFarePerTrip(0);
		}

		if(compensatorsConfig.getIntermodalTripFareCompensatorConfigGroups().size() > 0){
			if(compensatorsConfig.getIntermodalTripFareCompensatorConfigGroups().stream()
					.filter(cfg -> cfg.getNonPtModes().contains(drtConfigGroup.getMode()))
					.filter(cfg -> cfg.getCompensationCondition().equals(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedInSameTrip) ||
									cfg.getCompensationMoneyPerTrip() > 0 ||
									cfg.getCompensationScorePerTrip() > 0 ||
									cfg.getCompensationMoneyPerDay() != 2.1 ||
									cfg.getCompensationScorePerDay() > 0)
					.findAny().isPresent())
			throw new RuntimeException("you are using " + IntermodalTripFareCompensatorConfigGroup.GROUP_NAME + " with configurations that contradict pt+drt fare integration!" +
					" We rather abort here... Please check your config. ");
		} else {

			IntermodalTripFareCompensatorConfigGroup intermodalTripFareCompensatorConfigGroup = new IntermodalTripFareCompensatorConfigGroup();
			intermodalTripFareCompensatorConfigGroup.setNonPtModesAsString(TransportMode.drt);
			intermodalTripFareCompensatorConfigGroup.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);
			intermodalTripFareCompensatorConfigGroup.setCompensationMoneyPerDay(2.1);
			compensatorsConfig.addParameterSet(intermodalTripFareCompensatorConfigGroup);
		}

	}

	public static Scenario prepareScenario(Config config) {
		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);

		Set<String> roadTypesWithCarAllowed = new HashSet<>();
		switch (ROAD_TYPES_CAR_ALLOWED) {
			case nowhere:
				break;
			case motorway:
				roadTypesWithCarAllowed.add("motorway");
				break;
			case motorwayAndPrimaryAndTrunk:
				roadTypesWithCarAllowed.add("motorway");
				roadTypesWithCarAllowed.add("primary");
				roadTypesWithCarAllowed.add("trunk");
				break;
		}

		ReplaceCarByDRT.banCarAndRideFromNetworkArea(scenario, URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE,roadTypesWithCarAllowed);

		OpenBerlinIntermodalPtDrtRouterModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterModeIdentifier();

		// replace all car+ride trips - cut border-crossing trips in two parts assuming P+R stations

		ReplaceCarByDRT.prepareInputPlansForCarProhibitionWithPRLogic(scenario,
				Set.of(TransportMode.car, TransportMode.ride),
				REPLACING_MODES,
				URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE,
				URL_2_PR_STATIONS,
				mainModeIdentifier,
				//choose among the 3 closest P+R stations
				//in Lorenz' thesis we saw that this significantly reduces overloading single stations
				//more specifically, the maximum of all peak demands per stations reduced by 30%
				3
		);

		return scenario;
	}

	public static Controler prepareControler(Scenario scenario) {
		Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				PrActivityEventHandler handler = new PrActivityEventHandler(URL_2_PR_STATIONS);
				bind(PrActivityEventHandler.class).toInstance(handler);
				addEventHandlerBinding().toInstance(handler);
				addControlerListenerBinding().toInstance(handler);
			}
		});

		return controler;
	}

	private enum CarsAllowedOnRoadTypesInsideBanArea {
		nowhere, motorway, motorwayAndPrimaryAndTrunk
	}
}
