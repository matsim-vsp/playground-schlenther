library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

"This script writes the most important metrics for air Pollution emissions and costs into a tsv-file."

#####################################
# Preparation

#HPC Cluster
args <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- args[1]
policy_runId <- args[2]
baseCaseDirectory <- args[3]
base_runId <- args[4]


base_runId <- "berlin-v5.5-10pct"

baseCaseDirectory <- "D:/Projekte/berlin-noprivate-cars/lorenz/baseCaseContinued-10pct/"

#policy_runId <- "roadtypesAllowed-all"
#policyCaseDirectory <- "D:/Projekte/berlin-noprivate-cars/lorenz/runs-2023-09-01/10pct/roadtypesAllowed-all"

policy_runId <- "noDRT"
policyCaseDirectory <- "D:/Projekte/berlin-noprivate-cars/lorenz/runs-2023-09-01/10pct/noDRT"



baseAirPollution <- read.table(file = file.path(baseCaseDirectory, paste0("analysis/airPollution/", base_runId,".emissionsPerLink.csv")),
                               sep = ";", header = TRUE)
policyAirPollution <- read.table(file = file.path(policyCaseDirectory, paste0("analysis/airPollution/", policy_runId,".emissionsPerLink.csv")),
                                 sep = ";", header = TRUE)
#policyAirPollution <- read.table(file = "D:/Projekte/berlin-noprivate-cars/lorenz/runs-2023-09-01/10pct/roadtypesAllowed-all/analysis/airPollution/roadtypesAllowed-all.emissionsPerLink.csv",
#                                 sep = ";", header = TRUE)


#### cost rates
### UBA Methodenkonvention 3.1
# https://www.umweltbundesamt.de/sites/default/files/medien/1410/publikationen/2020-12-21_methodenkonvention_3_1_kostensaetze.pdf

####### Kostenstand 2020. Projektionsjahre....
##   2030: 1% Zeitpräferenz = 215 €/t ; 0% Zeitpräferenz = 700 €
##   2050:   250 €/t                  ;    765 €/t
C02_cost_rate <- 700

## jeweils Gesundheitsschäeden + nicht-gesundheitliche Schäden
NOX_cost_rate <- 15800 + 3700
pm2_5_cost_rate <- 255300 + 0 
pm10_cost_rate <- 30000 + 0
SO2_cost_rate <- 14900 + 1500
NH3_cost_rate <- 24200 + 10900






#####################################
# CO2 - Emissions & Costs (wait for Tilmanns answer to do it for the rest)

## Veränderung Emissionen absolut [t] & relativ [%]
CO2_abs <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / (1000 * 1000)
CO2_rel <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / sum(baseAirPollution$CO2_TOTAL) * 100

## Veränderung Kosten absolut [€ / Tag] 

CO2_euro <- (sum(policyAirPollution$CO2_TOTAL) - sum(baseAirPollution$CO2_TOTAL)) / (1000 * 1000) * C02_cost_rate


#####################################
# NOx - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
NOx_abs <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / (1000 * 1000)
NOx_rel <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / sum(baseAirPollution$NOx) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 15.400€/t (Werte für 2010)
NOx_euro <- (sum(policyAirPollution$NOx) - sum(baseAirPollution$NOx)) / (1000 * 1000) * NOX_cost_rate

#####################################
# PM2_5 - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
PM2_5_abs <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / (1000 * 1000)
PM2_5_rel <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / sum(baseAirPollution$PM2_5) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 364.100€/t (Werte für 2010)
PM2_5_euro <- (sum(policyAirPollution$PM2_5) - sum(baseAirPollution$PM2_5)) / (1000 * 1000) * pm2_5_cost_rate

#####################################
# PM_non_exhaust - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
PM_non_exhaust_abs <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / (1000 * 1000)
PM_non_exhaust_rel <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / sum(baseAirPollution$PM_non_exhaust) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 33.700€/t (Werte für 2010)
PM_non_exhaust_euro <- (sum(policyAirPollution$PM_non_exhaust) - sum(baseAirPollution$PM_non_exhaust)) / (1000 * 1000) * pm10_cost_rate

#####################################
# SO2 - Emissions & Costs

SO2_abs <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / (1000 * 1000)
SO2_rel <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / sum(baseAirPollution$SO2) * 100

## Veränderung Kosten absolut [€ / Tag] 
# 13.200€/t (Werte für 2010)
SO2_euro <- (sum(policyAirPollution$SO2) - sum(baseAirPollution$SO2)) / (1000 * 1000) * SO2_cost_rate


#####################################
# NH3 - Emissions & Costs

## Veränderung Emissionen absolut [t] & relativ [%]
NH3_abs <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / (1000 * 1000)
NH3_rel <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / sum(baseAirPollution$NH3) * 100

## Veränderung Kosten absolut [€ / Tag] & relativ [%]
# 26.800€/t (Werte für 2010)
NH3_euro <- (sum(policyAirPollution$NH3) - sum(baseAirPollution$NH3)) / (1000 * 1000) * NH3_cost_rate



#####################################
# Overall - Emissions & Costs

overall_euro_policy <- (sum(policyAirPollution$CO2_TOTAL) * C02_cost_rate)  + (sum(policyAirPollution$NOx) * NOX_cost_rate) + (sum(policyAirPollution$PM2_5) * pm2_5_cost_rate) +
  (sum(policyAirPollution$PM_non_exhaust) * pm10_cost_rate) + (sum(policyAirPollution$SO2) * SO2_cost_rate) + (sum(policyAirPollution$NH3) * NH3_cost_rate) / (1000 * 1000)
overall_euro_base <- (sum(baseAirPollution$CO2_TOTAL) * C02_cost_rate)  + (sum(baseAirPollution$NOx) * NOX_cost_rate) + (sum(baseAirPollution$PM2_5) * pm2_5_cost_rate) +
  (sum(baseAirPollution$PM_non_exhaust) * pm10_cost_rate) + (sum(baseAirPollution$SO2) * SO2_cost_rate) + (sum(baseAirPollution$NH3) * NH3_cost_rate) / (1000 * 1000)

overall_euro_rel <- (overall_euro_policy - overall_euro_base) / overall_euro_base * 100
overall_euro_abs <- CO2_euro + NOx_euro + PM2_5_euro + PM_non_exhaust_euro + SO2_euro + NH3_euro


#####################################
# Table - Emissions & Costs

results_airPollution <- data.frame(key = character(), Overall = numeric(), CO2_TOTAL = numeric(), NOx = numeric(), PM2_5 = numeric(),
                                    PM_non_exhaust = numeric(), SO2 = numeric(), NH3 = numeric()) %>%
  add_row(key = "Δ abs. pro Tag [t]",Overall = NA, CO2_TOTAL = CO2_abs,NOx = NOx_abs,PM2_5 = PM2_5_abs,PM_non_exhaust = PM_non_exhaust_abs,SO2 = SO2_abs,NH3 = NH3_abs) %>%
  add_row(key = "Δ rel. t pro Tag [%]",Overall = NA, CO2_TOTAL = CO2_rel,NOx = NOx_rel,PM2_5 = PM2_5_rel,PM_non_exhaust = PM_non_exhaust_rel,SO2 = SO2_rel,NH3 = NH3_rel) %>%
  add_row(key = "Δ abs. pro Tag [€]",Overall = overall_euro_abs, CO2_TOTAL = CO2_euro,NOx = NOx_euro,PM2_5 = PM2_5_euro,PM_non_exhaust = PM_non_exhaust_euro,SO2 = SO2_euro,NH3 = NH3_euro) %>%
  add_row(key = "Δ rel. € pro Tag [%]",Overall = overall_euro_rel, CO2_TOTAL = NA, NOx = NA, PM2_5 = NA, PM_non_exhaust = NA, SO2 = NA, NH3 = NA)

write.table(results_airPollution,file.path(policyCaseDirectory,"analysis/airPollution/results_airPollutionUBA3.1.tsv"),row.names = FALSE, sep = "\t")

