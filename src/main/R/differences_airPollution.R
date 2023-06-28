library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/analysis/noise/noise-analysis/"
base_filename <- "damages_receiverPoint_merged_xyt.csv.gz"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-13/finalRun-10pct/massConservation-true/analysis/noise/noise-analysis/"
policy_filename <- "damages_receiverPoint_merged_xyt.csv.gz"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseAirPollution <- read.table(file = base_inputfile, sep = ';', header = TRUE)
policyAirPollution <- read.table(file = policy_inputfile, sep = ';', header = TRUE)

differenceAirPollution <- baseAirPollution
differenceAirPollution$damages_receiverPoint <- policyNoise$damages_receiverPoint - differenceNoise$damages_receiverPoint

differenceNoise_summed <- aggregate(damages_receiverPoint ~ Receiver.Point.Id, data = differenceNoise, FUN = sum)

sum(differenceNoise_summed$damages_receiverPoint)

write.table(differenceNoise_summed,file.path(policyCaseDirectory,"differences_airPollution.tsv"),row.names = FALSE, sep = "\t")

