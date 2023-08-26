library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

#baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/"
#base_filename <- "berlin-v5.5-10pct.output_events_dailyTrafficVolume_vehicles.tsv"
baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-08-11/stationChoice-closestToInside/"
base_filename <- "stationChoice-closestToInside.output_events_dailyTrafficVolume_vehicles.tsv"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-08-11/stationChoice-closestToOutside/"
policy_filename <- "stationChoice-closestToOutside.output_events_dailyTrafficVolume_vehicles.tsv"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseDTV <- read.table(file = base_inputfile, sep = '\t', header = TRUE)
policyDTV <- read.table(file = policy_inputfile, sep = '\t', header = TRUE)

sum(baseDTV$agents) / sum(policyDTV$agents)
sum(policyDTV$agents) / sum(baseDTV$agents)

differenceDTV <- baseDTV
differenceDTV$agents <- policyDTV$agents - baseDTV$agents

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

#TODO: How big is the DTV change inside the city center?
#names(differenceNoise_summed)[1] <- "link"
#helper <- merge(differenceDTV, differenceNoise_summed, by = "link", suffixes = c("_DTV","_noise"))

differenceDTV_reverse <- baseDTV
differenceDTV_reverse$agents <- differenceDTV_reverse$agents - policyDTV$agents

sum(differenceDTV$agents)

write.table(differenceDTV,file.path(policyCaseDirectory,"differences_dailyTrafficVolume_vehicles_toClosestToInside.tsv"),row.names = FALSE, sep = "\t")
#write.table(differenceDTV_reverse,file.path(policyCaseDirectory,"differencesReverse_dailyTrafficVolume_vehicles.tsv"),row.names = FALSE, sep = "\t")


