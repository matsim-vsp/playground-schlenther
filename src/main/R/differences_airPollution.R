library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

#####################################
# Preparation

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/analysis/airPollution/"
base_filename <- "berlin-v5.5-10pct.emissionsPerLink.csv"
#input_path_base <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-motorway/analysis/airPollution/"
policy_filename <- "roadtypesAllowed-motorway.emissionsPerLink.csv"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseAirPollution <- read.table(file = base_inputfile, sep = ";", header = TRUE)
policyAirPollution <- read.table(file = policy_inputfile, sep = ";", header = TRUE)

#####################################
# CO2 - Emissions & Costs (wait for Tilmanns answer to do it for the rest)

## Veränderung Emissionen absolut [t] & relativ [%]
CO2_abs <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / (1000 * 1000)
CO2_rel <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / sum(baseAirPollution$CO2_TOTAL) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 139€/t (Werte für 2030)
CO2_euro <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / (1000 * 1000) * 139

#####################################
# NOx - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
NOx_abs <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / (1000 * 1000)
NOx_rel <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / sum(baseAirPollution$NOx) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 15.400€/t (Werte für 2010)
NOx_euro <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / (1000 * 1000) * 15400

#####################################
# PM2_5 - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
PM2_5_abs <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / (1000 * 1000)
PM2_5_rel <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / sum(baseAirPollution$PM2_5) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 364.100€/t (Werte für 2010)
PM2_5_euro <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / (1000 * 1000) * 364100

#####################################
# PM_non_exhaust - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
PM_non_exhaust_abs <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / (1000 * 1000)
PM_non_exhaust_rel <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / sum(baseAirPollution$PM_non_exhaust) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 33.700€/t (Werte für 2010)
PM_non_exhaust_euro <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / (1000 * 1000) * 33700

#####################################
# SO2 - Emissions & Costs

SO2_abs <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / (1000 * 1000)
SO2_rel <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / sum(baseAirPollution$SO2) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 13.200€/t (Werte für 2010)
SO2_euro <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / (1000 * 1000) * 13200


#####################################
# NH3 - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
NH3_abs <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / (1000 * 1000)
NH3_rel <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / sum(baseAirPollution$NH3) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 26.800€/t (Werte für 2010)
NH3_euro <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / (1000 * 1000) * 26800

overallCost <- CO2_euro + NOx_euro + PM2_5_euro + PM_non_exhaust_euro + SO2_euro + NH3_euro

#####################################
# Preparation - Emissions & Costs

results_airPollution <- data.frame(key = character(), CO2_TOTAL = numeric(), NOx = numeric(), PM2_5 = numeric(),
                                    PM_non_exhaust = numeric(), SO2 = numeric(), NH3 = numeric()) %>%
  add_row(key = "Δ abs. pro Tag [t]",CO2_TOTAL = CO2_abs,NOx = NOx_abs,PM2_5 = PM2_5_abs,PM_non_exhaust = PM_non_exhaust_abs,SO2 = SO2_abs,NH3 = NH3_abs) %>%
  add_row(key = "Δ rel. pro Tag [%]",CO2_TOTAL = CO2_rel,NOx = NOx_rel,PM2_5 = PM2_5_rel,PM_non_exhaust = PM_non_exhaust_rel,SO2 = SO2_rel,NH3 = NH3_rel) %>%
  add_row(key = "Δ abs. pro Tag [€]",CO2_TOTAL = CO2_euro,NOx = NOx_euro,PM2_5 = PM2_5_euro,PM_non_exhaust = PM_non_exhaust_euro,SO2 = SO2_euro,NH3 = NH3_euro)

write.table(results_airPollution,file.path(policyCaseDirectory,"results_airPollution.tsv"),row.names = FALSE, sep = "\t")
