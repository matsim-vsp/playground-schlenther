library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

#####################################
# Preparation

#HPC Cluster
args <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- args[1]
baseCaseDirectory <- args[3]

# baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/"
# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-motorway/"

baseNoise <- read.table(file = file.path(baseCaseDirectory, "analysis/noise/noise-analysis/damages_receiverPoint_merged_xyt.csv.gz"), sep = ';', header = TRUE)
policyNoise <- read.table(file = file.path(policyCaseDirectory, "analysis/noise/noise-analysis/damages_receiverPoint_merged_xyt.csv.gz"), sep = ';', header = TRUE)

differenceNoise <- baseNoise
differenceNoise$damages_receiverPoint <- policyNoise$damages_receiverPoint - differenceNoise$damages_receiverPoint
differenceNoise_summed <- aggregate(damages_receiverPoint ~ Receiver.Point.Id, data = differenceNoise, FUN = sum)

####################################
# Calculation of noise costs

results_noise <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung Lärmkosten pro Tag [€]", value = sum(differenceNoise_summed$damages_receiverPoint)) %>%
  add_row(key = "Relative Änderung zum BaseCase [%]", value = (sum(policyNoise$damages_receiverPoint) - sum(baseNoise$damages_receiverPoint)) / sum(baseNoise$damages_receiverPoint) * 100)

write.table(results_noise,file.path(policyCaseDirectory,"analysis/noise/results_noiseCosts.tsv"),row.names = FALSE, sep = "\t")
