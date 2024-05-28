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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.PrActivityEventHandler;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.legacy.run.BerlinExperimentalConfigGroup;
import org.matsim.legacy.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.legacy.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.run.OpenBerlinDrtScenario;
import picocli.CommandLine;

import java.util.*;

/**
 *
 * For all trips that originate <i>or</i> end within the drt service area, car and ride is not allowed.
 * See {@link ReplaceCarByDRT} for more documentation.
 */
@CommandLine.Command( header = ":: BerlinReplaceCarByDrtScenario ::", version = "0.2")
public final class BerlinReplaceCarByDrtScenario extends OpenBerlinDrtScenario {

	private static final Logger log = LogManager.getLogger(BerlinReplaceCarByDrtScenario.class);

	@CommandLine.Option(names = "--replacing-modes",
			defaultValue = "pt,drt",
			description = "Set of modes which might replace car and ride trips within the ban area. " +
					"Use comma as separator! " +
					"If the plan has border-crossing trips, a ptOnly plan that does not make use of P+R is created automatically.")
	private static String REPLACING_MODES;

	@CommandLine.Option(names = "--ban-area",
			defaultValue = "scenarios/berlin-v6.1/shp/hundekopf-carBanArea-25832.shp",
			description = "Path to (single geom) shape file depicting the area where private cars are banned from. If you adjust, think about adjusting the drt area+stops file, as well!")
	private static String URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE;

	@CommandLine.Option(names = "--pr-stations",
			defaultValue = "scenarios/berlin-v6.1/berlin-v6.1-pr-stations-ring.tsv",
			description = "Path to the .tsv file containing the PR station specifications.")
	protected static String URL_2_PR_STATIONS;

	@CommandLine.Option(names = "--car-road-types",
			defaultValue = "motorwayAndPrimaryAndTrunk",
			description = "Can be one of [nowhere, motorway, motorwayAndPrimaryAndTrunk]. Determines the type of roads inside in the ban area, where cars are allowed to drive, but not to park.")
	private static CarsAllowedOnRoadTypesInsideBanArea ROAD_TYPES_CAR_ALLOWED;

	public static void main(String[] args) {
		MATSimApplication.run(BerlinReplaceCarByDrtScenario.class, args);
	}

	@Override
	public Config prepareConfig(Config config) {
		super.prepareConfig(config); // side effects: mutates config


		disableModeChoiceAndDistributeStrategyWeights(config);

		ScoringConfigGroup.ActivityParams actParams = new ScoringConfigGroup.ActivityParams(ReplaceCarByDRT.PR_ACTIVITY_TYPE);
		actParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(actParams);

		//for the time being, assume one drt mode only
		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
		DvrpConfigGroup dvrpConfigGroup = DvrpConfigGroup.get(config);
		IntermodalTripFareCompensatorsConfigGroup compensatorsConfig = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

		ScoringConfigGroup.ModeParams ptParams = config.scoring().getModes().get(TransportMode.pt);
		ScoringConfigGroup.ModeParams drtParams = config.scoring().getModes().get(drtCfg.getMode());
		Preconditions.checkArgument(ptParams.getDailyMonetaryConstant() == drtParams.getDailyMonetaryConstant(), "in this scenario, we assume fare integration of pt and drt.\n" +
				"in the open berlin scenario, pt fare is modeled via dailyMonetaryConstant. So should it be for drt");

		//sets the drt mode to be dvrp network mode. sets fare compensations for agents using both pt and drt
		configureDVRPAndDRT(dvrpConfigGroup, drtCfg, ptParams, compensatorsConfig);

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
		Collection<ReplanningConfigGroup.StrategySettings> strategySettings = config.replanning().getStrategySettings();
		List<ReplanningConfigGroup.StrategySettings> rerouteSettings = new ArrayList<>();
		ReplanningConfigGroup.StrategySettings selector = null;

		double totalOldWeightModeAndTimeChoiceStrategies = 0;
		for (ReplanningConfigGroup.StrategySettings strategySetting : strategySettings) {
			if(strategySetting.getSubpopulation().equals("person")){
				switch (strategySetting.getStrategyName()){
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode :
					case DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeLegMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleLegMode:
					case DefaultPlanStrategiesModule.DefaultStrategy.TripSubtourModeChoice:
						totalOldWeightModeAndTimeChoiceStrategies += strategySetting.getWeight();
						strategySetting.setWeight(0);
						break;
					case DefaultPlanStrategiesModule.DefaultStrategy.ReRoute:
					case DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute:
						rerouteSettings.add(strategySetting);
						break;
					case DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta:
						selector = strategySetting;
						break;
					/*
					 * in Lorenz' MA we saw that TimeAllocation leads ti mutated P+R activity times, which is something we need to avoid. Thus we disable time mutation
					 */
					case DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator:
						totalOldWeightModeAndTimeChoiceStrategies += strategySetting.getWeight();
						strategySetting.setWeight(0);
					default:
						break;
				}
			}
		}

		/* redistribute mode choice weight(s):
		 * 50% to the selector strategy
		 * 50% to the reroute-strategies (equally distributed between them)
		 */
		if(totalOldWeightModeAndTimeChoiceStrategies > 0){
			log.warn("you had mode choice enabled via config. this scenario is meant to work without mode choice. please make sure, you do not have custom mode choice strategies enabled.\n" +
					"Following, all mode choice strategies will be disabled. Their strategy weights will be distributed to one selecotr (50%) and to all reroute strategies (the other 50%)." +
					"Please consider to decrease the number of iterations! Probably, you will not require as many iterations now, as mode choice gets disabled...");
			if(rerouteSettings.isEmpty()) throw new IllegalArgumentException("you have not configured any reroute strategy");
			selector.setWeight(selector.getWeight() + 0.5 * totalOldWeightModeAndTimeChoiceStrategies );
			for (ReplanningConfigGroup.StrategySettings rerouteSetting : rerouteSettings) {
				rerouteSetting.setWeight(rerouteSetting.getWeight() + 0.5/rerouteSettings.size()*totalOldWeightModeAndTimeChoiceStrategies);
			}
		}




	}

	private static void configureDVRPAndDRT(DvrpConfigGroup dvrpConfigGroup, DrtConfigGroup drtConfigGroup, ScoringConfigGroup.ModeParams ptParams, IntermodalTripFareCompensatorsConfigGroup compensatorsConfig) {
		if(! dvrpConfigGroup.networkModes.contains(drtConfigGroup.getMode()) ){
			log.warn("the drt mode " + drtConfigGroup.getMode() + " is not registered as network mode for dvrp - which is necessary in a bannedCarInDRTServiceArea scenario");
			log.warn("adding mode " + drtConfigGroup.getMode() + " as network mode for dvrp... ");
			dvrpConfigGroup.networkModes.add(drtConfigGroup.getMode());
		}

		Preconditions.checkArgument(!drtConfigGroup.getDrtSpeedUpParams().isPresent(),
				"you are using drt-speed-up. this scenario setup is meant for experiments without mode choice, so basically, drt-speed-up should not be necessary.");

//		// Setting operational scheme to stop based
//		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
//		log.warn("you are now using a stop based operational scheme for drt! This is still under development.");
//		Preconditions.checkNotNull(drtConfigGroup.transitStopFile,
//				"this scenario currently only works with a specified stopFile for drt!");

		if(! drtConfigGroup.useModeFilteredSubnetwork){
			log.warn("setting drtConfigGroup.isUseModeFilteredSubnetwork() to true! Was false before......");
			drtConfigGroup.useModeFilteredSubnetwork = true;
		}

		if( drtConfigGroup.getDrtFareParams().isPresent()){
			log.warn("you are using " + DrtFareParams.SET_NAME + "params. Will now override all fare values therein to 0, because we assume pt and drt fare integration. In Berlin, this is modeled via dailyMonetaryConstant.");
			DrtFareParams fares = drtConfigGroup.getDrtFareParams().get();
			fares.baseFare = 0;
			fares.timeFare_h = 0;
			fares.dailySubscriptionFee = 0;
			fares.distanceFare_m = 0;
			fares.minFarePerTrip = 0;
		}

		if(compensatorsConfig.getIntermodalTripFareCompensatorConfigGroups().size() > 0){
			if(compensatorsConfig.getIntermodalTripFareCompensatorConfigGroups().stream()
					.filter(cfg -> cfg.getNonPtModes().contains(drtConfigGroup.getMode()))
					.filter(cfg -> cfg.getCompensationCondition().equals(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedInSameTrip) ||
									cfg.getCompensationMoneyPerTrip() > 0 ||
									cfg.getCompensationScorePerTrip() > 0 ||
									cfg.getCompensationMoneyPerDay() != ptParams.getDailyMonetaryConstant() ||
									cfg.getCompensationScorePerDay() > 0)
					.findAny().isPresent())
			throw new RuntimeException("you are using " + IntermodalTripFareCompensatorConfigGroup.GROUP_NAME + " with configurations that contradict pt+drt fare integration!" +
					" We rather abort here... Please check your config. ");
		} else {

			IntermodalTripFareCompensatorConfigGroup intermodalTripFareCompensatorConfigGroup = new IntermodalTripFareCompensatorConfigGroup();
			intermodalTripFareCompensatorConfigGroup.setNonPtModesAsString(TransportMode.drt);
			intermodalTripFareCompensatorConfigGroup.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);
			intermodalTripFareCompensatorConfigGroup.setCompensationMoneyPerDay(ptParams.getDailyMonetaryConstant());
			compensatorsConfig.addParameterSet(intermodalTripFareCompensatorConfigGroup);
		}

	}

	@Override
	public void prepareScenario(Scenario scenario) {
		//runs AssignIncome
		super.prepareScenario(scenario);

//		//if the input plans contain DrtRoutes, this will cause problems later in the DrtRouteFactory
//		//to avoid this, the DrtRouteFactory would have to get set before loading the scenario, just like in Open Berlin v5.x
//		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
//		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

//		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(scenario.getConfig());
//		prepareNetworkAndTransitScheduleForDrt(scenario);

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

		//ban car and ride from the shp-provided area
		ReplaceCarByDRT.banCarAndRideFromNetworkArea(scenario,
				IOUtils.resolveFileOrResource(URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE),
				roadTypesWithCarAllowed);

		OpenBerlinIntermodalPtDrtRouterModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterModeIdentifier();

		// replace all car+ride trips - cut border-crossing trips in two parts assuming P+R stations
		ReplaceCarByDRT.prepareInputPlansForCarProhibitionWithPRLogic(scenario,
				Set.of(TransportMode.car, TransportMode.ride),
				Set.of(REPLACING_MODES.split(",")),
				IOUtils.resolveFileOrResource(URL_2_CAR_FREE_SINGLE_GEOM_SHAPE_FILE),
				IOUtils.resolveFileOrResource(URL_2_PR_STATIONS),
				mainModeIdentifier,
				//choose among the 3 closest P+R stations
				//in Lorenz' thesis we saw that this significantly reduces overloading single stations
				//more specifically, the maximum of all peak demands per stations reduced by 30%
				3
		);

	}

	/**
	 * most stuff is copied from {@link RunDrtOpenBerlinScenario}.prepareControler() and sub-methods.
	 * @param controler
	 * @return
	 */
	@Override
	public void prepareControler(Controler controler) {
		super.prepareControler(controler);

		//maybe not needed ?
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				PrActivityEventHandler handler = new PrActivityEventHandler(IOUtils.resolveFileOrResource(URL_2_PR_STATIONS));
				bind(PrActivityEventHandler.class).toInstance(handler);
				addEventHandlerBinding().toInstance(handler);
				addControlerListenerBinding().toInstance(handler);
			}
		});

	}

	private enum CarsAllowedOnRoadTypesInsideBanArea {
		nowhere, motorway, motorwayAndPrimaryAndTrunk
	}
}
