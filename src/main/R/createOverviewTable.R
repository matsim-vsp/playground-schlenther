library(tidyverse)
library(dplyr)

drtTableDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-07-13/extraPtPlan/"
runId <- "extraPtPlan-true"

outputDirectory <- paste0(drtTableDirectory,"/analysis")

scoreAllTableDirectory <- paste0(drtTableDirectory,"/analysis/score/allePersonen")
scoreImpactedTableDirectory <- paste0(drtTableDirectory,"/analysis/score/betroffenePersonen")
tripsTableDirectory <- paste0(drtTableDirectory,"/analysis/trips")

scoreFilename <- "score_general.tsv"
tripsTimeFilename <- "trips_travTime.tsv"
tripsDistFilename <- "trips_travelledDistance.tsv"
drtCustomerFilename <- paste0(runId, ".drt_customer_stats_drt.csv")
drtVehicleFilename <- paste0(runId, ".drt_vehicle_stats_drt.csv")

scoreAllFilepath <- file.path(scoreAllTableDirectory,scoreFilename)
scoreImpFilepath <- file.path(scoreImpactedTableDirectory, scoreFilename)
tripsTimeFilepath <- file.path(tripsTableDirectory, tripsTimeFilename)
tripsDistFilepath <- file.path(tripsTableDirectory, tripsDistFilename)
drtCustomerFilepath <- file.path(drtTableDirectory, drtCustomerFilename)
drtVehicleFilepath <- file.path(drtTableDirectory, drtVehicleFilename)

scoreAllTable <- read.table(file = scoreAllFilepath, sep ='\t', header = TRUE)
scoreImpTable <- read.table(file = scoreImpFilepath, sep ='\t', header = TRUE)
tripsTimeTable <- read.table(file = tripsTimeFilepath, sep = "\t", header = TRUE)
tripsDistTable <- read.table(file = tripsDistFilepath, sep = "\t", header = TRUE)
drtCustomerTable <- read.table(file = drtCustomerFilepath, sep = ";", header = TRUE)
drtVehicleTable <- read.table(file = drtVehicleFilepath, sep = ";", header = TRUE)

avgScoreDiffAll <- scoreAllTable$avg_score_diff
avgScoreDiffImp <- scoreImpTable$avg_score_diff
avgTripsTimeAll <- tripsTimeTable$avg_travTime_diff[tripsTimeTable$tripType == "All_Impacted_Trips"]
avgTripsTimeBinnen <- tripsTimeTable$avg_travTime_diff[tripsTimeTable$tripType == "Impacted_Binnen_Trips"]
avgTripsTimeGrenz <- tripsTimeTable$avg_travTime_diff[tripsTimeTable$tripType == "Impacted_Grenz_Trips"]
avgTripsDistAll <- tripsDistTable$avg_travelledDistance_diff[tripsDistTable$tripType == "All_Impacted_Trips"]
avgTripsDistBinnen <- tripsDistTable$avg_travelledDistance_diff[tripsDistTable$tripType == "Impacted_Binnen_Trips"]
avgTripsDistGrenz <- tripsDistTable$avg_travelledDistance_diff[tripsDistTable$tripType == "Impacted_Grenz_Trips"]
avgWaitDrt <- drtCustomerTable$wait_average[drtCustomerTable$iteration == "500"]
drtEmptyRatio <- drtVehicleTable$emptyRatio[drtVehicleTable$iteration == "500"]
drtDPDTRatio <- drtVehicleTable$d_p.d_t[drtVehicleTable$iteration == "500"]

overviewTable <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Durchschnittliche Score-Differenz (aller Agenten)", value = avgScoreDiffAll) %>%
  add_row(key = "Durchschnittliche Score-Differenz (aller betroffenen Agenten)", value = avgScoreDiffImp) %>%
  add_row(key = "Durchschnittliche Reisezeit-Differenz (aller betroffenen Trips) [s]", value = avgTripsTimeAll) %>%
  add_row(key = "Durchschnittliche Reisezeit-Differenz (aller betroffenen Quell- und Zieltrips) [s]", value = avgTripsTimeGrenz) %>%
  add_row(key = "Durchschnittliche Reisezeit-Differenz (aller betroffenen Binnentrips) [s]", value = avgTripsTimeBinnen) %>%
  add_row(key = "Durchschnittliche Reiseweiten-Differenz (aller betroffenen Trips) [m]", value = avgTripsDistAll) %>%
  add_row(key = "Durchschnittliche Reiseweiten-Differenz (aller betroffenen Quell- und Zieltrips) [m]", value = avgTripsDistGrenz) %>%
  add_row(key = "Durchschnittliche Reiseweiten-Differenz (aller betroffenen Binnentrips) [m]", value = avgTripsDistBinnen) %>%
  add_row(key = "Durchschnittliche Wartezeit (DRT-System)", value = avgWaitDrt) %>%
  add_row(key = "Anteil leere Fahrten (DRT-System)", value = drtEmptyRatio) %>%
  add_row(key = "Verhältnis Pkm/Fz-km (DRT-System)", value = drtDPDTRatio)
  
write.table(overviewTable,file.path(outputDirectory,"runOverview.tsv"),row.names = FALSE, sep = "\t")
