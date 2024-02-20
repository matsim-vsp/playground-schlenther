library(tidyr)
library(lubridate)
library(hms)
library(readr)
library(sf)
library(dplyr)
library(matsim)
library(tidyverse)
library(ggalluvial)

########################################
# Preparation

# #HPC Cluster
# args <- commandArgs(trailingOnly = TRUE)
# policyCaseDirectory <- args[1]


# 10pct
baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/"

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

policy_filename <- "output_trips_prepared.tsv"
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseTrips <- readTripsTable(baseCaseDirectory)

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

comparingTrips <- read.table(file = "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-all/output_trips_prepared.tsv", sep ='\t', header = TRUE)
comparingTrips <- comparingTrips %>% 
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
# Filter out all agents with scoreDiff > -400

basePerson_filename <- "output_plans_selectedPlanScores.tsv"
policyPerson_filename <- "output_plans_selectedPlanScores.tsv"
basePerson_inputfile <- file.path(baseCaseDirectory, basePerson_filename)
policyPerson_inputfile <- file.path(policyCaseDirectory, policyPerson_filename)

basePersons <- read.table(file = basePerson_inputfile, sep = '\t', header = TRUE)
policyPersons <- read.table(file = policyPerson_inputfile, sep = '\t', header = TRUE)

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base"))
personsJoined <- personsJoined %>%
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)
personsJoined <- personsJoined %>% filter(score_diff > -400)

impacted_persons <- personsJoined %>% filter(person %in% impacted_trips$person_policy)

baseTrips <- baseTrips %>% filter(person %in% personsJoined$person)
policyTrips <- policyTrips %>% filter(person %in% personsJoined$person)

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
  add_column(euclideanDistance_diff = impGrenz_trips$euclidean_distance_policy - impGrenz_trips$euclidean_distance_base) %>%
  filter(travTime_diff < 20000)

########################################
"Modal Shift Sankeys"
# Filter bedingt durch teilweise falsch erkannte Trips durch filterByRegion, siehe trips_falselyClassified.tsv

"Grenztrips"
prep_grenz_policy <- impGrenz_trips_policy %>% 
  filter(!main_mode == "ride") %>%
  filter(!main_mode == "car") %>%
  filter(!main_mode == "bicycle")
prep_grenz_policy$main_mode[prep_grenz_policy$main_mode == "bicycle+ride"] <- "ride+bicycle"
prep_grenz_policy$main_mode[prep_grenz_policy$main_mode == "bicycle+car"] <- "car+bicycle"

prep_grenz_compare <- comparingTrips %>% filter(trip_id %in% prep_grenz_policy$trip_id) %>%
  filter(!main_mode == "ride") %>%
  filter(!main_mode == "car") %>%
  filter(!main_mode == "bicycle")

others <- prep_grenz_policy %>% filter(!trip_id %in% prep_grenz_compare$trip_id)

plotModalShiftSankey(prep_grenz_compare, prep_grenz_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_compared.png"))
