library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

"This script writes the most important metrics for accident costs into a tsv-file."

####################################
# Preparation

#HPC Cluster
args <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- args[1]
policy_runId <- args[2]
baseCaseDirectory <- args[3]
base_runId <- args[4]

# baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/analysis/accidents/"
# base_runId <- "berlin-v5.5-10pct"
# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/analysis/accidents/"
# policy_runId <- "noDRT"

baseAccidents <- read.table(file = file.path(baseCaseDirectory, paste0("analysis/accidents/ITERS/it.0/",base_runId,".0.accidentCosts_BVWP.csv")), sep = ';', header = TRUE)
policyAccidents <- read.table(file = file.path(policyCaseDirectory, paste0("analysis/accidents/ITERS/it.0/",policy_runId,".0.accidentCosts_BVWP.csv")), sep = ';', header = TRUE)

differenceAccidents <- baseAccidents[ ,c(1,122)]
differenceAccidents$Costs.per.Day..EUR. <- policyAccidents$Costs.per.Day..EUR. - differenceAccidents$Costs.per.Day..EUR.

#####################################
# Preparing "regions" for linkIDs (using the DTV file)

#10pct
regionsFromDTV <- read.table(file = paste0(policyCaseDirectory,policy_runId,".output_events_dailyTrafficVolume_vehicles.tsv"), sep = '\t', header = TRUE)
regionsFromDTV <- subset(regionsFromDTV, select = c(link,zone))

differenceAccidents_regions <- merge(differenceAccidents, regionsFromDTV, by.x = "Link.ID", by.y = "link")
differenceAccidents_zone <- differenceAccidents_regions %>% filter(zone == "innerCity")
differenceAccidents_rberlin <- differenceAccidents_regions %>% filter(zone == "BerlinButNotInnerCity")
differenceAccidents_brandenburg <- differenceAccidents_regions %>% filter(zone == "Brandenburg")

policyAccidents_regions <- merge(policyAccidents, regionsFromDTV, by.x = "Link.ID", by.y = "link")
policyAccidents_zone <- policyAccidents_regions %>% filter(zone == "innerCity")
policyAccidents_rberlin <- policyAccidents_regions %>% filter(zone == "BerlinButNotInnerCity")
policyAccidents_brandenburg <- policyAccidents_regions %>% filter(zone == "Brandenburg")

baseAccidents_regions <- merge(baseAccidents, regionsFromDTV, by.x = "Link.ID", by.y = "link")
baseAccidents_zone <- baseAccidents_regions %>% filter(zone == "innerCity")
baseAccidents_rberlin <- baseAccidents_regions %>% filter(zone == "BerlinButNotInnerCity")
baseAccidents_brandenburg <- baseAccidents_regions %>% filter(zone == "Brandenburg")


####################################
# Calculation of accident costs

results_accidentCosts <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung Unfallkosten pro Tag [€]", value = sum(differenceAccidents$Costs.per.Day..EUR.)) %>%
  add_row(key = "Relative Änderung zum BaseCase [%]", value = (sum(policyAccidents$Costs.per.Day..EUR.) - sum(baseAccidents$Costs.per.Day..EUR.)) / sum(baseAccidents$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (Zone) [%]", value = (sum(policyAccidents_zone$Costs.per.Day..EUR.) - sum(baseAccidents_zone$Costs.per.Day..EUR.)) / sum(baseAccidents_zone$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (restl. Berlin) [%]", value = (sum(policyAccidents_rberlin$Costs.per.Day..EUR.) - sum(baseAccidents_rberlin$Costs.per.Day..EUR.)) / sum(baseAccidents_rberlin$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (Brandenburg) [%]", value = (sum(policyAccidents_brandenburg$Costs.per.Day..EUR.) - sum(baseAccidents_brandenburg$Costs.per.Day..EUR.)) / sum(baseAccidents_brandenburg$Costs.per.Day..EUR.) * 100)

write.table(results_accidentCosts,file.path(policyCaseDirectory,"analysis/accidents/results_accidentCosts.tsv"),row.names = FALSE, sep = "\t")
