library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/"
base_filename <- "berlin-v5.5-10pct.output_events_dailyTrafficVolume_vehicles.tsv"
base_mileage_filename <- "berlin-v5.5-10pct.output_events_dailyMileage_vehicles.tsv"

policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/"
policy_filename <- "noDRT.output_events_dailyTrafficVolume_vehicles.tsv"
policy_mileage_filename <- "noDRT.output_events_dailyMileage_vehicles.tsv"

base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseDTV <- read.table(file = base_inputfile, sep = '\t', header = TRUE)
policyDTV <- read.table(file = policy_inputfile, sep = '\t', header = TRUE)

baseMileage <- read.table(file = file.path(baseCaseDirectory, base_mileage_filename), sep = '\t', header = TRUE)
policyMileage <- read.table(file = file.path(policyCaseDirectory, policy_mileage_filename), sep = '\t', header = TRUE)

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

dir.create(paste0(policyCaseDirectory,"/analysis/dailyTrafficVolume"))


#######################################################################
# Verkehrsaufkommen

# Preparing tables
# By region
baseDTV_zone <- baseDTV %>% filter(zone == "innerCity")
policyDTV_zone <- policyDTV %>% filter(zone == "innerCity")
baseDTV_rberlin <- baseDTV %>% filter(zone == "BerlinButNotInnerCity")
policyDTV_rberlin <- policyDTV %>% filter(zone == "BerlinButNotInnerCity")
baseDTV_brandenburg <- baseDTV %>% filter(zone == "Brandenburg")
policyDTV_brandenburg <- policyDTV %>% filter(zone == "Brandenburg")
baseDTV_boundaryZone <- baseDTV %>% filter(isInBoundaryZone == "true")
policyDTV_boundaryZone <- policyDTV %>% filter(isInBoundaryZone == "true")

# By roadtype & region
baseDTV_zone_mprimary <- baseDTV_zone %>% filter(roadtype == "motorway" | roadtype == "motorway_link" | roadtype == "primary" | roadtype == "primary_link" |
                                                   roadtype == "trunk" | roadtype == "trunk_link")
policyDTV_zone_mprimary <- policyDTV_zone %>% filter(roadtype == "motorway" | roadtype == "motorway_link" | roadtype == "primary" | roadtype == "primary_link" |
                                                       roadtype == "trunk" | roadtype == "trunk_link")
baseDTV_zone_other <- baseDTV_zone %>% filter(roadtype == "secondary" | roadtype == "secondary_link" | roadtype == "tertiary" | roadtype == "residential" |
                                                roadtype == "living_street")
policyDTV_zone_other <- policyDTV_zone %>% filter(roadtype == "secondary" | roadtype == "secondary_link" | roadtype == "tertiary" | roadtype == "residential" |
                                                    roadtype == "living_street")
baseDTV_rberlin_motorway <- baseDTV_rberlin %>% filter(roadtype == "motorway" | roadtype == "motorway_link")
policyDTV_rberlin_motorway <- policyDTV_rberlin %>% filter(roadtype == "motorway" | roadtype == "motorway_link")
baseDTV_rberlin_primary <- baseDTV_rberlin %>% filter(roadtype == "primary" | roadtype == "primary_link" | roadtype == "trunk" | roadtype == "trunk_link")
policyDTV_rberlin_primary <- policyDTV_rberlin %>% filter(roadtype == "primary" | roadtype == "primary_link" | roadtype == "trunk" | roadtype == "trunk_link")
baseDTV_rberlin_other <- baseDTV_rberlin %>% filter(roadtype == "secondary" | roadtype == "secondary_link" | roadtype == "tertiary" | roadtype == "residential" |
                                                      roadtype == "living_street")
policyDTV_rberlin_other <- policyDTV_rberlin %>% filter(roadtype == "secondary" | roadtype == "secondary_link" | roadtype == "tertiary" | roadtype == "residential" |
                                                          roadtype == "living_street")

# Results
# roadtype = "unclassified" wird nicht berücksichtigt & herausgefiltert
results_DTV <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung DTV (Gesamt) [%]", value = (sum(policyDTV$agents) - sum(baseDTV$agents)) / sum(baseDTV$agents) * 100) %>%
  add_row(key = "Veränderung DTV (Verbotszone) [%]", value = (sum(policyDTV_zone$agents) - sum(baseDTV_zone$agents)) / sum(baseDTV_zone$agents) * 100) %>%
  add_row(key = "Veränderung DTV (restl. Berlin) [%]", value = (sum(policyDTV_rberlin$agents) - sum(baseDTV_rberlin$agents)) / sum(baseDTV_rberlin$agents) * 100) %>%
  add_row(key = "Veränderung DTV (Brandenburg) [%]", value = (sum(policyDTV_brandenburg$agents) - sum(baseDTV_brandenburg$agents)) / sum(baseDTV_brandenburg$agents) * 100) %>%
  add_row(key = "Veränderung DTV (500m um Verbotszone) [%]", value = (sum(policyDTV_boundaryZone$agents) - sum(baseDTV_boundaryZone$agents)) / sum(baseDTV_boundaryZone$agents) * 100) %>%
  add_row(key = "Anteil DRT an DTV (Verbotszone, policy) [%]", value = sum(policyDTV_zone$DRT) / sum(policyDTV_zone$agents) * 100) %>%
  add_row(key = "Veränderung DTV Autos (Verbotszone) [%]", value = (sum(policyDTV_zone$nonDRT) - sum(baseDTV_zone$nonDRT)) / sum(baseDTV_zone$nonDRT) * 100) %>%
  add_row(key = "Veränderung DTV (Verbotszone, Autobahnen & Bundesstraßen) [%]", value = (sum(policyDTV_zone_mprimary$agents) - sum(baseDTV_zone_mprimary$agents)) / sum(baseDTV_zone_mprimary$agents) * 100) %>%
  add_row(key = "Veränderung DTV (Verbotszone, restl. Straßen) [%]", value = (sum(policyDTV_zone_other$agents) - sum(baseDTV_zone_other$agents)) / sum(baseDTV_zone_other$agents) * 100) %>%
  add_row(key = "Veränderung DTV (restl. Berlin, Autobahnen [%]", value = (sum(policyDTV_rberlin_motorway$agents) - sum(baseDTV_rberlin_motorway$agents)) / sum(baseDTV_rberlin_motorway$agents) * 100) %>%
  add_row(key = "Veränderung DTV (restl. Berlin, Bundesstraßen [%]", value = (sum(policyDTV_rberlin_primary$agents) - sum(baseDTV_rberlin_primary$agents)) / sum(baseDTV_rberlin_primary$agents) * 100) %>%
  add_row(key = "Veränderung DTV (restl. Berlin, restl. Straßen [%]", value = (sum(policyDTV_rberlin_other$agents) - sum(baseDTV_rberlin_other$agents)) / sum(baseDTV_rberlin_other$agents) * 100)

  
#######################################################################
# Verkehrsleistung

# Preparing tables
baseMileage_zone <- baseMileage %>% filter(zone == "innerCity")
policyMileage_zone <- policyMileage %>% filter(zone == "innerCity")
baseMileage_rberlin <- baseMileage %>% filter(zone == "BerlinButNotInnerCity")
policyMileage_rberlin <- policyMileage %>% filter(zone == "BerlinButNotInnerCity")
baseMileage_brandenburg <- baseMileage %>% filter(zone == "Brandenburg")
policyMileage_brandenburg <- policyMileage %>% filter(zone == "Brandenburg")

# Results
results_mileage <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Veränderung VL (Gesamt) [%]", value = (sum(policyMileage$mileage) - sum(baseMileage$mileage)) / sum(baseMileage$mileage) * 100) %>%
  add_row(key = "Veränderung VL (Verbotszone) [%]", value = (sum(policyMileage_zone$mileage) - sum(baseMileage_zone$mileage)) / sum(baseMileage_zone$mileage) * 100) %>%
  add_row(key = "Veränderung VL (restl. Berlin) [%]", value = (sum(policyMileage_rberlin$mileage) - sum(baseMileage_rberlin$mileage)) / sum(baseMileage_rberlin$mileage) * 100) %>%
  add_row(key = "Veränderung VL (Brandenburg) [%]", value = (sum(policyMileage_brandenburg$mileage) - sum(baseMileage_brandenburg$mileage)) / sum(baseMileage_brandenburg$mileage) * 100)

write.table(results_DTV,file.path(policyCaseDirectory,"analysis/dailyTrafficVolume/metrics_DTV.tsv"),row.names = FALSE, sep = "\t")
write.table(results_mileage,file.path(policyCaseDirectory,"analysis/dailyTrafficVolume/metrics_mileage.tsv"),row.names = FALSE, sep = "\t")

#######################################################################
# Backup

# #Other stuff
# differenceDTV <- baseDTV
# differenceDTV$agents <- policyDTV$agents - baseDTV$agents
# sum(differenceDTV$agents) / sum(baseDTV$agents)
# write.table(differenceDTV,file.path(policyCaseDirectory,"differences_dailyTrafficVolume_vehicles_toClosestToInside.tsv"),row.names = FALSE, sep = "\t")
