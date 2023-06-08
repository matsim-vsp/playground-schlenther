require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)
library(hms)

########################################
# Preparation

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-05-26/baseCaseContinued"
baseTrips <- readTripsTable(baseCaseDirectory)

policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true"
policy_filename <- "output_trips_prepared.tsv"
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

policyTrips <- read.table(file = policy_inputfile, sep ='\t', header = TRUE)
policyTrips <- policyTrips %>% 
  mutate(trip_number = as.double(trip_number),
         dep_time = parse_hms(dep_time),
         trav_time = parse_hms(trav_time),
         wait_time = parse_hms(wait_time),
         traveled_distance = as.double(traveled_distance),
         euclidean_distance = as.double(euclidean_distance),
         start_x = as.double(start_x), 
         start_y = as.double(start_y), end_x = as.double(end_x), 
         end_y = as.double(end_y))

########################################
# Prepare folders

dir.create(paste0(policyCaseDirectory,"/analysis"))
dir.create(paste0(policyCaseDirectory,"/analysis/score"))

policyCaseOutputDir_all <- paste0(policyCaseDirectory,"/analysis/score/all_agents")
policyCaseOutputDir_impacted <- paste0(policyCaseDirectory,"/analysis/score/impacted_agents")
policyCaseOutputDir_nonImpacted <- paste0(policyCaseDirectory,"/analysis/score/non_impacted_agents")

dir.create(policyCaseOutputDir_all)
dir.create(policyCaseOutputDir_impacted)
dir.create(policyCaseOutputDir_nonImpacted)

########################################
# Prepare tables

"Impacted Grenztrips"





"Impacted Binnentrips"
autoBase <- baseTrips %>% filter(main_mode == "car" | main_mode == "ride")
impBinnen_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_trips_policy <- policyTrips %>% filter(trip_id %in% impBinnen_trips_base$trip_id)

impBinnen_trips <- merge(impBinnen_trips_policy, impBinnen_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_trips <- impBinnen_trips %>% 
  add_column(travTime_diff = impBinnen_trips$trav_time_policy - impBinnen_trips$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_trips$wait_time_policy - impBinnen_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_trips$traveled_distance_policy - impBinnen_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_trips$euclidean_distance_policy - impBinnen_trips$euclidean_distance_base)

prep_binnen_policy <- impBinnen_trips_policy %>% 
  filter(!grepl("+", main_mode, fixed = TRUE)) %>%
  filter(!main_mode == "bicycle")
         
prep_binnen_base <- impBinnen_trips_base %>% filter(trip_id %in% prep_binnen_policy$trip_id)

plotModalShiftSankey(prep_binnen_base,prep_binnen_policy)

"PR trips = all those trips that got replaced by P+R"
pr_trips_policy <- policyTrips %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_trips_base <- baseTrips %>% filter(trip_id %in% pr_trips_policy$trip_id)

pr_trips <- merge(pr_trips_policy, pr_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_trips <- pr_trips %>% 
  add_column(travTime_diff = pr_trips$trav_time_policy - pr_trips$trav_time_base) %>%
  add_column(waitTime_diff = pr_trips$wait_time_policy - pr_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_trips$traveled_distance_policy - pr_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = pr_trips$euclidean_distance_policy - pr_trips$euclidean_distance_base)

prep_base <- pr_trips_base %>% filter(pr_trips_base$main_mode == "car" | pr_trips_base$main_mode == "ride")
prep_policy <- pr_trips_policy %>% filter(trip_id %in% prep_base$trip_id)


"Impacted trips = all those trips that got impacted by the policy (Impacted Grenztrips + Impacted Binnentrips)"
impacted_trips_base <- rbind(pr_trips_base,impBinnen_trips_base)
impacted_trips_policy <- rbind(pr_trips_policy,impBinnen_trips_policy)

impacted_trips <- merge(impacted_trips_policy, impacted_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_trips <- impacted_trips %>% 
  add_column(travTime_diff = impacted_trips$trav_time_policy - impacted_trips$trav_time_base) %>%
  add_column(waitTime_diff = impacted_trips$wait_time_policy - impacted_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_trips$traveled_distance_policy - impacted_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_trips$euclidean_distance_policy - impacted_trips$euclidean_distance_base)

prep_base <- impacted_trips_base %>% filter(impacted_trips_base$main_mode == "car" | impacted_trips_base$main_mode == "ride")
prep_policy <- impacted_trips_policy %>% filter(trip_id %in% prep_base$trip_id)



########################################
# Modal Shift Sankeys

plotModalShiftSankey(prep_base,prep_policy)

plotModalShiftSankey(impacted_trips_base,impacted_trips_policy)


########################################
# General results - travelTime of impacted_trips, impacted_binnen_trips, pr_trips

pr_trips$tripType <- "Impacted_Grenz_Trips"
impBinnen_trips$tripType <- "Impacted_Binnen_Trips"
impacted_trips$tripType <- "All_Impacted_Trips"

boxplot_helper <- rbind(pr_trips,impBinnen_trips,impacted_trips)

"General metrics"
mean(impacted_trips$travTime_diff)
sd(impacted_trips$travTime_diff)
quantile(impacted_trips$travTime_diff, probs = 0.95)

mean(pr_trips$travTime_diff)

"Boxplot"
ggplot(boxplot_helper, aes(x = tripType, y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of travTime differences",
    subtitle = "Impacted trips (policy vs base)",
    caption = "travTime_delta = travTime(policy) - travTime(base)",
    y = "travTime_delta [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank()
  )
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/analysis/boxplot_travTime.png")

"Tryout - all non-impacted trips - TODO: Some questions concerning euclideanDistance came up"
other_trips_base <- baseTrips %>% filter(!trip_id %in% impacted_trips$trip_id)
other_trips_policy <- policyTrips %>% filter(!trip_id %in% impacted_trips$trip_id)

other_trips <- merge(other_trips_policy, other_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
other_trips <- other_trips %>% 
  add_column(travTime_diff = other_trips$trav_time_policy - other_trips$trav_time_base) %>%
  add_column(waitTime_diff = other_trips$wait_time_policy - other_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = other_trips$traveled_distance_policy - other_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = other_trips$euclidean_distance_policy - other_trips$euclidean_distance_base)

mean(other_trips$travTime_diff)

ggplot(other_trips, aes(y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of travTime differences",
    subtitle = "Impacted trips (policy vs base)",
    caption = "travTime_delta = travTime(policy) - travTime(base)",
    y = "travTime_delta [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank()
  )

########################################
# General results - traveledDistance of impacted_trips, impacted_binnen_trips, pr_trips

"General metrics"
mean(impacted_trips$traveledDistance_diff)
sd(impacted_trips$traveledDistance_diff)
quantile(impacted_trips$traveledDistance_diff, probs = 0.95)

mean(pr_trips$traveledDistance_diff)

"Boxplot"
ggplot(boxplot_helper, aes(x = tripType, y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveledDistance differences",
    subtitle = "Impacted trips (policy vs base)",
    caption = "traveledDistance_delta = traveledDistance(policy) - traveledDistance(base)",
    y = "traveledDistance_delta [m]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank()
  )
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/analysis/boxplot_travelledDistance.png")

########################################
# Results by mainMode - travTime of pr_trips

"Boxplot - PR Trips by transport mode (travTime)"
ggplot(pr_trips, aes(x = reorder(main_mode_policy,travTime_diff,median), y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of travTime differences",
    subtitle = "by mainMode (policy vs base)",
    caption = "travTime_delta = travTime(policy) - travTime(base)",
    y = "travTime_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

########################################
# by euclideanDistance (nur für PR Trips) -> erster Versuch, die krassen Umwege irgendwie beziffern zu können

"Boxplot"
ggplot(pr_trips, aes(x = tripType, y = euclideanDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of euclideanDistance differences",
    subtitle = "Impacted Grenztrips (policy vs base)",
    caption = "euclideanDistance_delta = euclideanDistance(policy) - euclideanDistance(base)",
    y = "euclideanDistance_delta [m]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank()
  )

########################################
# by PR Station (travTime, travelledDistance)
# TODO

"Boxplot"
ggplot(pr_trips, aes(x = reorder(prStation, travTime_diff, median), y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of travTime differences",
    subtitle = "Impacted Grenztrips (policy vs base)",
    caption = "travTime_delta = travTime(policy) - travTime(base)",
    y = "travTime_delta [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1)
  )


"Boxplot"
ggplot(pr_trips, aes(x = reorder(prStation, traveledDistance_diff, median), y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveledDistance differences",
    subtitle = "Impacted Grenztrips (policy vs base)",
    caption = "traveledDistance_delta = traveledDistance(policy) - traveledDistance(base)",
    y = "traveledDistance_delta [m]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1)
  )

########################################
# Losing vs winning agents (not that interesting -> there are mostly losers concerning scores & trips with this policy)
impacted_trips$winnerLoser <- ""

winners <- personsJoined %>% filter(score_diff > 0)
losers <- personsJoined %>% filter(score_diff < 0)
zeroChanges <- personsJoined %>% filter(score_diff == 0)

impacted_winnerTrips <- impacted_trips %>% filter(person_policy %in% winners$person)
impacted_winnerTrips$winnerLoser <- "winner"
impacted_loserTrips <- impacted_trips %>% filter(person_policy %in% losers$person)
impacted_loserTrips$winnerLoser <- "loser"

tryout <- rbind(impacted_winnerTrips, impacted_loserTrips)

"Boxplot - winners vs losers (travTime)"
ggplot(tryout, aes(x = winnerLoser, y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

"Boxplot - winners vs losers (traveledDistance)"
ggplot(tryout, aes(x = winnerLoser, y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveled_distance differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [m]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )


########################################
# Losing agents -> what transport modes? (not that interesting, see above)
"Boxplot - Losers by transport mode (travTime)"
ggplot(impacted_loserTrips, aes(x = reorder(main_mode_policy,travTime_diff), y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

"Boxplot - Losers by transport mode (traveledDistance)"
ggplot(relevant_loserTrips, aes(x = reorder(combined_main_mode,traveledDistance_diff), y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveled_distance differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [m]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

########################################
# Test: Externer/Durchgangsverkehr - vllt
baseExtern <- filterByRegion(baseTrips, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE) #filter rather from PolicyBinnen
policyExtern <- pTest %>% filter(trip_id %in% baseExtern$trip_id)
#TODO: need to filter out all remaining P+R trips (right at the borders)

externTrips <- merge(policyExtern, baseExtern, by = "trip_id", suffixes = c("_policy","_base"))
externTrips <- externTrips %>% 
  add_column(travTime_diff = externTrips$trav_time_policy - externTrips$trav_time_base) %>%
  add_column(waitTime_diff = externTrips$wait_time_policy - externTrips$wait_time_base) %>%
  add_column(traveledDistance_diff = externTrips$traveled_distance_policy - externTrips$traveled_distance_base)

########################################
# Test: filterByRegion-Probleme, z.B. Quell/Zielverkehr car/ride, der nicht P+R ist?
# TODO: Problem found but not solved. Irrelevant for now bcz filterByRegion almost not used

baseQuell <- filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE) #filter rather from PolicyBinnen
policyQuell <- policyTrips %>% filter(trip_id %in% baseQuell$trip_id)

quellTrips <- merge(policyQuell, baseQuell, by = "trip_id", suffixes = c("_policy","_base"))

lupe <- quellTrips %>% filter(main_mode_policy == "car")

plotModalShiftSankey(baseQuell,policyQuell)


##### TRYOUT: Comparison pt, drt, pt&drt

##### PT Preparation

"PR trips = all those trips that got replaced by P+R"
pr_tripsPT_policy <- policyTripsPT %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_tripsPT_base <- baseTrips %>% filter(trip_id %in% pr_tripsPT_policy$trip_id)

pr_tripsPT <- merge(pr_tripsPT_policy, pr_tripsPT_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_tripsPT <- pr_tripsPT %>% 
  add_column(travTime_diff = pr_tripsPT$trav_time_policy - pr_tripsPT$trav_time_base) %>%
  add_column(waitTime_diff = pr_tripsPT$wait_time_policy - pr_tripsPT$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_tripsPT$traveled_distance_policy - pr_tripsPT$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = pr_tripsPT$euclidean_distance_policy - pr_tripsPT$euclidean_distance_base)


"Impacted Binnentrips"
impBinnen_tripsPT_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_tripsPT_policy <- policyTripsPT %>% filter(trip_id %in% impBinnen_tripsPT_base$trip_id)

impBinnen_tripsPT <- merge(impBinnen_tripsPT_policy, impBinnen_tripsPT_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_tripsPT <- impBinnen_tripsPT %>% 
  add_column(travTime_diff = impBinnen_tripsPT$trav_time_policy - impBinnen_tripsPT$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_tripsPT$wait_time_policy - impBinnen_tripsPT$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_tripsPT$traveled_distance_policy - impBinnen_tripsPT$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_tripsPT$euclidean_distance_policy - impBinnen_tripsPT$euclidean_distance_base)


"Impacted trips = all those trips that got impacted by the policy (PR Trips + Impacted Binnentrips)"
impacted_tripsPT_base <- rbind(pr_tripsPT_base,impBinnen_tripsPT_base)
impacted_tripsPT_policy <- rbind(pr_tripsPT_policy,impBinnen_tripsPT_policy)

impacted_tripsPT <- merge(impacted_tripsPT_policy, impacted_tripsPT_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_tripsPT <- impacted_tripsPT %>% 
  add_column(travTime_diff = impacted_tripsPT$trav_time_policy - impacted_tripsPT$trav_time_base) %>%
  add_column(waitTime_diff = impacted_tripsPT$wait_time_policy - impacted_tripsPT$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_tripsPT$traveled_distance_policy - impacted_tripsPT$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_tripsPT$euclidean_distance_policy - impacted_tripsPT$euclidean_distance_base)

##### DRT Preparation

"PR trips = all those trips that got replaced by P+R"
pr_tripsDRT_policy <- policyTripsDRT %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_tripsDRT_base <- baseTrips %>% filter(trip_id %in% pr_tripsDRT_policy$trip_id)

pr_tripsDRT <- merge(pr_tripsDRT_policy, pr_tripsDRT_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_tripsDRT <- pr_tripsDRT %>% 
  add_column(travTime_diff = pr_tripsDRT$trav_time_policy - pr_tripsDRT$trav_time_base) %>%
  add_column(waitTime_diff = pr_tripsDRT$wait_time_policy - pr_tripsDRT$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_tripsDRT$traveled_distance_policy - pr_tripsDRT$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = pr_tripsDRT$euclidean_distance_policy - pr_tripsDRT$euclidean_distance_base)

"Impacted Binnentrips"
impBinnen_tripsDRT_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_tripsDRT_policy <- policyTripsDRT %>% filter(trip_id %in% impBinnen_tripsDRT_base$trip_id)

impBinnen_tripsDRT <- merge(impBinnen_tripsDRT_policy, impBinnen_tripsDRT_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_tripsDRT <- impBinnen_tripsDRT %>% 
  add_column(travTime_diff = impBinnen_tripsDRT$trav_time_policy - impBinnen_tripsDRT$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_tripsDRT$wait_time_policy - impBinnen_tripsDRT$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_tripsDRT$traveled_distance_policy - impBinnen_tripsDRT$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_tripsDRT$euclidean_distance_policy - impBinnen_tripsDRT$euclidean_distance_base)


"Impacted trips = all those trips that got impacted by the policy (PR Trips + Impacted Binnentrips)"
impacted_tripsDRT_base <- rbind(pr_tripsDRT_base,impBinnen_tripsDRT_base)
impacted_tripsDRT_policy <- rbind(pr_tripsDRT_policy,impBinnen_tripsDRT_policy)

impacted_tripsDRT <- merge(impacted_tripsDRT_policy, impacted_tripsDRT_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_tripsDRT <- impacted_tripsDRT %>% 
  add_column(travTime_diff = impacted_tripsDRT$trav_time_policy - impacted_tripsDRT$trav_time_base) %>%
  add_column(waitTime_diff = impacted_tripsDRT$wait_time_policy - impacted_tripsDRT$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_tripsDRT$traveled_distance_policy - impacted_tripsDRT$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_tripsDRT$euclidean_distance_policy - impacted_tripsDRT$euclidean_distance_base)

##### Analysis - v.a. DRT Metrics Vergleich!

plotModalShiftSankey(pr_trips_policy, pr_tripsDRT_policy)

mean(impacted_tripsPT$travTime_diff)
mean(impacted_tripsDRT$travTime_diff)

mean(pr_tripsPT$travTime_diff)
