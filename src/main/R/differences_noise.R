library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

#####################################
# Preparation

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/analysis/noise/noise-analysis/"
base_filename <- "damages_receiverPoint_merged_xyt.csv.gz"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-motorway/analysis/noise/noise-analysis/"
policy_filename <- "damages_receiverPoint_merged_xyt.csv.gz"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseNoise <- read.table(file = base_inputfile, sep = ';', header = TRUE)
policyNoise <- read.table(file = policy_inputfile, sep = ';', header = TRUE)

differenceNoise <- baseNoise
differenceNoise$damages_receiverPoint <- policyNoise$damages_receiverPoint - differenceNoise$damages_receiverPoint
differenceNoise_summed <- aggregate(damages_receiverPoint ~ Receiver.Point.Id, data = differenceNoise, FUN = sum)


#####################################
# Preparing "regions" (could only do it with shape-files and a regional filter, hmm not optimal -> prepare in Java?)


####################################
# Calculation of noise costs

# ggplot(differenceNoise_summed, aes(x = 1, y = damages_receiverPoint)) +
#   geom_boxplot()

# Summe Änderung Lärmkosten pro Tag
sum(differenceNoise_summed$damages_receiverPoint)

# Relative Änderung zum BaseCase
(sum(policyNoise$damages_receiverPoint) - sum(baseNoise$damages_receiverPoint)) / sum(baseNoise$damages_receiverPoint) * 100

results_noise <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung Lärmkosten pro Tag [€]", value = sum(differenceNoise_summed$damages_receiverPoint)) %>%
  add_row(key = "Relative Änderung zum BaseCase [%]", value = (sum(policyNoise$damages_receiverPoint) - sum(baseNoise$damages_receiverPoint)) / sum(baseNoise$damages_receiverPoint) * 100)

write.table(results_noise,file.path(policyCaseDirectory,"results_noiseCosts.tsv"),row.names = FALSE, sep = "\t")
