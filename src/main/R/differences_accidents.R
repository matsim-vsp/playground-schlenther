library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

####################################
# Preparation

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/analysis/accidents/"
base_filename <- "ITERS/it.0/berlin-v5.5-10pct.0.accidentCosts_BVWP.csv"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/analysis/accidents/"
policy_filename <- "ITERS/it.0/noDRT.0.accidentCosts_BVWP.csv"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseAccidents <- read.table(file = base_inputfile, sep = ';', header = TRUE)
policyAccidents <- read.table(file = policy_inputfile, sep = ';', header = TRUE)

sum(baseAccidents$Costs.per.Day..EUR.)

differenceAccidents <- baseAccidents[ ,c(1,122)]
differenceAccidents$Costs.per.Day..EUR. <- policyAccidents$Costs.per.Day..EUR. - differenceAccidents$Costs.per.Day..EUR.

#####################################
# Preparing "regions" for linkIDs (using the DTV file)

#1pct

#10pct
regionsFromDTV <- read.table(file = "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-all/roadtypesAllowed-all.output_events_dailyTrafficVolume_vehicles.tsv", sep = '\t', header = TRUE)
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

# ggplot(differenceAccidents, aes(x = 1, y = Costs.per.Day..EUR.)) +
#   geom_boxplot()

# Summe Änderung Unfallkosten pro Tag
sum(differenceAccidents$Costs.per.Day..EUR.)
sum(differenceAccidents_zone$Costs.per.Day..EUR.)
sum(differenceAccidents_rberlin$Costs.per.Day..EUR.)
sum(differenceAccidents_brandenburg$Costs.per.Day..EUR.)

# Relative Änderung zum BaseCase
(sum(policyAccidents$Costs.per.Day..EUR.) - sum(baseAccidents$Costs.per.Day..EUR.)) / sum(baseAccidents$Costs.per.Day..EUR.) * 100
(sum(policyAccidents_zone$Costs.per.Day..EUR.) - sum(baseAccidents_zone$Costs.per.Day..EUR.)) / sum(baseAccidents_zone$Costs.per.Day..EUR.) * 100
(sum(policyAccidents_rberlin$Costs.per.Day..EUR.) - sum(baseAccidents_rberlin$Costs.per.Day..EUR.)) / sum(baseAccidents_rberlin$Costs.per.Day..EUR.) * 100
(sum(policyAccidents_brandenburg$Costs.per.Day..EUR.) - sum(baseAccidents_brandenburg$Costs.per.Day..EUR.)) / sum(baseAccidents_brandenburg$Costs.per.Day..EUR.) * 100

results_accidentCosts <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung Unfallkosten pro Tag [€]", value = sum(differenceAccidents$Costs.per.Day..EUR.)) %>%
  add_row(key = "Relative Änderung zum BaseCase [%]", value = (sum(policyAccidents$Costs.per.Day..EUR.) - sum(baseAccidents$Costs.per.Day..EUR.)) / sum(baseAccidents$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (Zone) [%]", value = (sum(policyAccidents_zone$Costs.per.Day..EUR.) - sum(baseAccidents_zone$Costs.per.Day..EUR.)) / sum(baseAccidents_zone$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (restl. Berlin) [%]", value = (sum(policyAccidents_rberlin$Costs.per.Day..EUR.) - sum(baseAccidents_rberlin$Costs.per.Day..EUR.)) / sum(baseAccidents_rberlin$Costs.per.Day..EUR.) * 100) %>%
  add_row(key = "Relative Änderung zum BaseCase (Brandenburg) [%]", value = (sum(policyAccidents_brandenburg$Costs.per.Day..EUR.) - sum(baseAccidents_brandenburg$Costs.per.Day..EUR.)) / sum(baseAccidents_brandenburg$Costs.per.Day..EUR.) * 100)


write.table(results_accidentCosts,file.path(policyCaseDirectory,"results_accidentCosts.tsv"),row.names = FALSE, sep = "\t")

# write.table(differenceAccidents,file.path(policyCaseDirectory,"differences_accidentCosts_BVWP.tsv"),row.names = FALSE, sep = "\t")
