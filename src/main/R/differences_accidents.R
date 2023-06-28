library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/analysis/accidents/ITERS/it.0/"
base_filename <- "berlin-v5.5-10pct.0.accidentCosts_BVWP.csv"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/finalRun-10pct/massConservation-true/analysis/accidents/ITERS/it.0/"
policy_filename <- "final-10pct-7503vehicles-8seats.0.accidentCosts_BVWP.csv"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseAccidents <- read.table(file = base_inputfile, sep = ';', header = TRUE)
policyAccidents <- read.table(file = policy_inputfile, sep = ';', header = TRUE)

differenceAccidents <- baseAccidents[ ,c(1,122)]
differenceAccidents$Costs.per.Day..EUR. <- policyAccidents$Costs.per.Day..EUR. - differenceAccidents$Costs.per.Day..EUR.

sum(differenceAccidents$Costs.per.Day..EUR.)

write.table(differenceAccidents,file.path(policyCaseDirectory,"differences_accidentCosts_BVWP.tsv"),row.names = FALSE, sep = "\t")
