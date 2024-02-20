library(tidyr)
library(tidyverse)
library(lubridate)
library(plotly)
library(hms)
library(readr)
library(sf)
library(dplyr)
library(matsim)
library(ggplot2)
library(viridis)

########################################
# Preparation

#HPC Cluster
# args <- commandArgs(trailingOnly = TRUE)
# policyCaseDirectory <- args[1]
# baseCaseDirectory <- args[3]
# shp <- st_read(args[5])

#10pct
baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/"

#1pct
# baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued/"
# #policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/"
# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/1pct/optimum-flowCapacity/"

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")
shp_lor_import <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/lor_planungsraeume_2021.shp")
shp_lor <- st_transform(shp_lor_import, crs = 31468)

basePersons <- read.table(file = file.path(baseCaseDirectory, "output_plans_selectedPlanScores.tsv"), sep = '\t', header = TRUE)
policyPersons <- read.table(file = file.path(policyCaseDirectory, "output_plans_selectedPlanScores2.tsv"), sep = '\t', header = TRUE)

########################################
# Prepare basic trips

baseTrips <- readTripsTable(baseCaseDirectory)
policy_trips_filename <- "output_trips_prepared.tsv"
policy_inputfile <- file.path(policyCaseDirectory, policy_trips_filename)

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
# Prepare impacted trips (for the next cases)

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

"All impacted trips (Impacted Grenztrips + Impacted Binnentrips)"
impacted_trips_base <- rbind(impGrenz_trips_base,impBinnen_trips_base)
impacted_trips_policy <- rbind(impGrenz_trips_policy,impBinnen_trips_policy)

impacted_trips <- merge(impacted_trips_policy, impacted_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_trips <- impacted_trips %>% 
  add_column(travTime_diff = impacted_trips$trav_time_policy - impacted_trips$trav_time_base) %>%
  add_column(waitTime_diff = impacted_trips$wait_time_policy - impacted_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_trips$traveled_distance_policy - impacted_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_trips$euclidean_distance_policy - impacted_trips$euclidean_distance_base)


########################################
# Prepare spatial persons

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base"))
personsJoined <- personsJoined %>%
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)
personsJoined <- personsJoined %>% filter(score_diff > -400)

betroffenePersonen <- personsJoined %>% filter(person %in% impacted_trips$person_policy)
nichtBetroffenePersonen <- personsJoined %>% filter(!person %in% betroffenePersonen$person)

personsJoined_sf <- st_as_sf(personsJoined, coords = c("home_x", "home_y"), crs = 31468)
betroffenePersonen_sf <- st_as_sf(betroffenePersonen, coords = c("home_x", "home_y"), crs = 31468)
nichtBetroffenePersonen_sf <- st_as_sf(nichtBetroffenePersonen, coords = c("home_x", "home_y"), crs = 31468)

########################################
# By LOR (Berlin)

personsByLOR<- st_join(shp_lor, personsJoined_sf, join = st_intersects)
impactedByLOR <- st_join(shp_lor, betroffenePersonen_sf, join = st_intersects)
nonImpactedByLOR <- st_join(shp_lor, nichtBetroffenePersonen_sf, join = st_intersects)

scorePersonsByLOR <- personsByLOR %>% group_by(PLR_ID) %>% summarize(mean_score = mean(score_diff), count = n())
scoreImpactedByLOR <- impactedByLOR %>% group_by(PLR_ID) %>% summarize(mean_score = mean(score_diff), count = n())
scoreNonImpactedByLOR <- nonImpactedByLOR %>% group_by(PLR_ID) %>% summarize(mean_score = mean(score_diff), count = n())

ggplot(scorePersonsByLOR) +
  geom_sf(aes(fill = mean_score)) + 
  scale_fill_viridis() +
  labs(title = "Ø Score-Diff. nach LOR (Alle Personen)") +
  theme_minimal() +
  theme(
    panel.background = element_rect(fill = "white"),
    plot.background = element_rect(fill = "white")
  )
ggsave(file.path(policyCaseDirectory,"/analysis/score/scoreByLOR_all.png"))

ggplot(scoreImpactedByLOR) +
  geom_sf(aes(fill = mean_score)) + 
  scale_fill_viridis() +
  labs(title = "Ø Score-Diff. nach LOR (Betroffene Personen)") +
  theme_minimal() +
  theme(
    panel.background = element_rect(fill = "white"),
    plot.background = element_rect(fill = "white")
  )
ggsave(file.path(policyCaseDirectory,"/analysis/score/scoreByLOR_impacted.png"))

ggplot(scoreNonImpactedByLOR) +
  geom_sf(aes(fill = mean_score)) + 
  scale_fill_viridis() +
  labs(title = "Ø Score-Diff. nach LOR (Nicht betr. Personen)") +
  theme_minimal() +
  theme(
    panel.background = element_rect(fill = "white"),
    plot.background = element_rect(fill = "white")
  )
ggsave(file.path(policyCaseDirectory,"/analysis/score/scoreByLOR_nonImpacted.png"))

########################################
# Tryouts boundary zones

# persons_boundary <- betroffenePersonen %>%
#   filter(livesInsideBoundaryZone_policy == "true") %>%
#   filter(home.activity.zone_policy == "innerCity")
# 
# persons_boundary2 <- personsJoined %>%
#   filter(livesInsideBoundaryZone_policy == "true") %>%
#   filter(home.activity.zone_policy == "innerCity")
# 
# persons_non_boundary <- betroffenePersonen %>%
#   filter(livesInsideBoundaryZone_policy == "false") %>%
#   filter(home.activity.zone_policy == "innerCity")
# 
# persons_non_boundary2 <- personsJoined %>%
#   filter(livesInsideBoundaryZone_policy == "false") %>%
#   filter(home.activity.zone_policy == "innerCity")
# 
# results_scoreSpatial <- data.frame(key = character(), value = numeric()) %>%
#   add_row(key = "Score (Betr.) 250m in Zone", value = mean(persons_boundary$score_diff)) %>%
#   add_row(key = "Score (Betr.) restl. Zone", value = mean(persons_non_boundary$score_diff))
# 
# write.table(results_scoreSpatial,file.path(policyCaseDirectory,"/analysis/score/score_inBoundaries.tsv") ,row.names = FALSE, sep = "\t")
