library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/"
base_filename <- "berlin-v5.5-10pct.output_events_dailyTrafficVolume_vehicles.tsv"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/finalRun-10pct/massConservation-true/"
policy_filename <- "final-10pct-7503vehicles-8seats.output_events_dailyTrafficVolume_vehicles.tsv"
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
names(differenceNoise_summed)[1] <- "link"
helper <- merge(differenceDTV, differenceNoise_summed, by = "link", suffixes = c("_DTV","_noise"))

differenceDTV_reverse <- baseDTV
differenceDTV_reverse$agents <- differenceDTV_reverse$agents - policyDTV$agents

sum(differenceDTV$agents)

write.table(differenceDTV,file.path(policyCaseDirectory,"differences_dailyTrafficVolume_vehicles.tsv"),row.names = FALSE, sep = "\t")
write.table(differenceDTV_reverse,file.path(policyCaseDirectory,"differencesReverse_dailyTrafficVolume_vehicles.tsv"),row.names = FALSE, sep = "\t")


