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

policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-true/massConservation-false"
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
dir.create(paste0(policyCaseDirectory,"/analysis/trips"))

policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips")

########################################
# Prepare tables

"Impacted Grenztrips"
autoBase <- baseTrips %>% filter(main_mode == "car" | main_mode == "ride")
impQuell_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, FALSE)
impZiel_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, FALSE, TRUE)
impGrenz_trips_base <- rbind(impQuell_trips_base, impZiel_trips_base)
impGrenz_trips_policy <- policyTrips %>% filter(trip_id %in% impGrenz_trips_base$trip_id)

impGrenz_trips <- merge(impGrenz_trips_policy, impGrenz_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impGrenz_trips <- impGrenz_trips %>% 
  add_column(travTime_diff = impGrenz_trips$trav_time_policy - impGrenz_trips$trav_time_base) %>%
  add_column(waitTime_diff = impGrenz_trips$wait_time_policy - impGrenz_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impGrenz_trips$traveled_distance_policy - impGrenz_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impGrenz_trips$euclidean_distance_policy - impGrenz_trips$euclidean_distance_base)

"Impacted Binnentrips"
impBinnen_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_trips_policy <- policyTrips %>% filter(trip_id %in% impBinnen_trips_base$trip_id)

impBinnen_trips <- merge(impBinnen_trips_policy, impBinnen_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_trips <- impBinnen_trips %>% 
  add_column(travTime_diff = impBinnen_trips$trav_time_policy - impBinnen_trips$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_trips$wait_time_policy - impBinnen_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_trips$traveled_distance_policy - impBinnen_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_trips$euclidean_distance_policy - impBinnen_trips$euclidean_distance_base)

"Impacted trips (Impacted Grenztrips + Impacted Binnentrips)"
impacted_trips_base <- rbind(impGrenz_trips_base,impBinnen_trips_base)
impacted_trips_policy <- rbind(impGrenz_trips_policy,impBinnen_trips_policy)

impacted_trips <- merge(impacted_trips_policy, impacted_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_trips <- impacted_trips %>% 
  add_column(travTime_diff = impacted_trips$trav_time_policy - impacted_trips$trav_time_base) %>%
  add_column(waitTime_diff = impacted_trips$wait_time_policy - impacted_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_trips$traveled_distance_policy - impacted_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_trips$euclidean_distance_policy - impacted_trips$euclidean_distance_base)


########################################
# Modal Shift Sankeys

#Grenztrips
plotModalShiftSankey(impGrenz_trips_base,impGrenz_trips_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_grenz.png"))

#Binnentrips
prep_binnen_policy <- impBinnen_trips_policy %>% 
  filter(!grepl("+", main_mode, fixed = TRUE))
prep_binnen_base <- impBinnen_trips_base %>% filter(trip_id %in% prep_binnen_policy$trip_id)
plotModalShiftSankey(prep_binnen_base,prep_binnen_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_binnen.png"))

#All impacted trips
plotModalShiftSankey(impacted_trips_base,impacted_trips_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_impacted.png"))

########################################
# General results - travelTime of impacted_trips, impacted_binnen_trips, pr_trips

impGrenz_trips$tripType <- "Impacted_Grenz_Trips"
impBinnen_trips$tripType <- "Impacted_Binnen_Trips"
impacted_trips$tripType <- "All_Impacted_Trips"

boxplot_helper <- rbind(impGrenz_trips,impBinnen_trips,impacted_trips)

"Results table"
tripTypes <- unique(boxplot_helper$tripType)
iterator = 0

results_travTime <- data.frame(tripType = character(), avg_travTime_diff = numeric(), pt95_travTime_diff = numeric(), sd_travTime_diff = numeric())

for (tripType in tripTypes){
  iterator <- iterator + 1
  results_travTime[iterator, ] <- list(tripType, 
                                            mean(boxplot_helper[which(boxplot_helper$tripType == tripType),48]), 
                                            quantile((boxplot_helper[which(boxplot_helper$tripType == tripType),48]), probs = 0.95), 
                                            sd(boxplot_helper[which(boxplot_helper$tripType == tripType),48])
  )
}

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
ggsave(file.path(policyTripsOutputDir,"boxplot_travTime.png"))

########################################
# General results - traveledDistance of impacted_trips, impacted_binnen_trips, pr_trips

"Results table"
iterator = 0
results_travelledDistance <- data.frame(tripType = character(), avg_travelledDistance_diff = numeric(), pt95_travelledDistance_diff = numeric(), sd_travelledDistance_diff = numeric())

for (tripType in tripTypes){
  iterator <- iterator + 1
  results_travelledDistance[iterator, ] <- list(tripType, 
                                       mean(boxplot_helper[which(boxplot_helper$tripType == tripType),50]), 
                                       quantile((boxplot_helper[which(boxplot_helper$tripType == tripType),50]), probs = 0.95), 
                                       sd(boxplot_helper[which(boxplot_helper$tripType == tripType),50])
  )
}

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
ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance.png"))

########################################
# Boxplots & Results 

tripCases <- list("impacted_grenztrips","impacted_binnentrips","impacted_trips")

for (case in tripCases){
  if(case == "impacted_grenztrips"){
    caseTrips <- impGrenz_trips
  }
  if(case == "impacted_binnentrips"){
    caseTrips <- impBinnen_trips
  }
  if(case == "impacted_trips"){
    caseTrips <- impacted_trips
  }
  
  policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips/",case)
  dir.create(policyCaseOutputDir, showWarnings = FALSE)
  
  ########################################
  # Results by mainMode
  
  "Boxplot - Grenztrips by transport mode (travTime)"
  ggplot(caseTrips, aes(x = reorder(main_mode_policy,travTime_diff,median), y = travTime_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Distribution of travTime differences",
      subtitle = paste0("by mainMode (policy vs base - ", case ,")"),
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
  ggsave(file.path(policyTripsOutputDir,"boxplot_travTime_mainMode.png"))
  
  "Boxplot - Grenztrips by transport mode (travelledDistance)"
  ggplot(caseTrips, aes(x = reorder(main_mode_policy,traveledDistance_diff,median), y = traveledDistance_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Distribution of travelledDistance differences",
      subtitle = paste0("by mainMode (policy vs base - ", case ,")"),
      caption = "travelledDistance_delta = travelledDistance(policy) - travelledDistance(base)",
      y = "traveledDistance_delta [m]",
      x = "main_mode"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic"),
      axis.title.x = element_blank()
    )
  ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance_mainMode.png"))
  
  ########################################
  # by PR Station (travTime, travelledDistance)
  
  "Boxplot"
  ggplot(caseTrips, aes(x = reorder(prStation, travTime_diff, median), y = travTime_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Distribution of travTime differences",
      subtitle = paste0("by PRStation (policy vs base - ", case ,")"),
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
  ggsave(file.path(policyTripsOutputDir,"boxplot_travTime_PRStation.png"))
  
  
  "Boxplot"
  ggplot(caseTrips, aes(x = reorder(prStation, traveledDistance_diff, median), y = traveledDistance_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Distribution of traveledDistance differences",
      subtitle = paste0("by PRStation (policy vs base - ", case ,")"),
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
  ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance_PRStation.png"))
  
}

########################################
# Test: filterByRegion-Probleme, Case 2 only works without extraPtPlan=true

allTrips <- merge(policyTrips, baseTrips, by = "trip_id", suffixes = c("_policy","_base"))

"PR trips = all those trips that got replaced by P+R"
pr_trips_policy <- policyTrips %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_trips_base <- baseTrips %>% filter(trip_id %in% pr_trips_policy$trip_id)

pr_trips <- merge(pr_trips_policy, pr_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_trips <- pr_trips %>% 
  add_column(travTime_diff = pr_trips$trav_time_policy - pr_trips$trav_time_base) %>%
  add_column(waitTime_diff = pr_trips$wait_time_policy - pr_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_trips$traveled_distance_policy - pr_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = pr_trips$euclidean_distance_policy - pr_trips$euclidean_distance_base)

# Case 1: Falsely classified as non-impacted Trip -> has PR Trip, but no Grenztrip?
non_impacted_trips <- allTrips %>% filter(!trip_id %in% impacted_trips$trip_id)
lupe <- pr_trips %>% filter(trip_id %in% non_impacted_trips$trip_id)

# Case 2: Falsely classified as impacted Trip -> no PR Trip, but is Grenztrip?
not_pr_trips <- allTrips %>% filter(!trip_id %in% pr_trips$trip_id)
lupe2 <- not_pr_trips %>% filter(trip_id %in% impGrenz_trips$trip_id) %>%
  filter(!main_mode_policy == "pt") %>%
  filter(!main_mode_policy == "pt_w_drt_used") %>%
  filter(!main_mode_policy == "walk") 

results_falselyClassified <- data.frame(falselyAsNotImpacted = numeric(), falselyAsImpacted = numeric())
results_falselyClassified[1,] <- list(nrow(lupe),nrow(lupe2))

########################################
# Dump result tables
policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips")

write.table(results_travTime,file.path(policyTripsOutputDir,"trips_travTime.tsv"),row.names = FALSE, sep = "\t")
write.table(results_travelledDistance,file.path(policyTripsOutputDir,"trips_travelledDistance.tsv"),row.names = FALSE, sep = "\t")
write.table(results_falselyClassified,file.path(policyTripsOutputDir,"trips_falselyClassified.tsv"),row.names = FALSE, sep = "\t")
