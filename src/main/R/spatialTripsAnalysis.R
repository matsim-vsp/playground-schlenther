require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)

# Vergleich PR Station Choice (closestToInside vs closestToOutSide)

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

# Importing and filtering for closestToInsideActivity

## Trips

tripsI <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/")
tripsI <- dplyr::filter(tripsI, main_mode %in% c("ride", "car"))
tripsI$trav_time <- period_to_seconds(hms(tripsI$trav_time))

quellverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)
zielverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)
binnenverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)
externerPlusDurchgangI <- filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

## DRT data

drtI_customer <- read.csv2("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")
drtI_vehicle <- read.csv2("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/drt_vehicle_stats_drt.csv")

drtI_customer <- dplyr::filter(drtI_customer, iteration == 500)
drtI_vehicle <- dplyr::filter(drtI_vehicle, iteration == 500)

# Importing and filtering for closestToOutsideActivity

## Trips

tripsO <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct/pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/")
tripsO <- dplyr::filter(tripsO, main_mode %in% c("ride", "car"))
tripsO$trav_time <- period_to_seconds(hms(tripsO$trav_time))

quellverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)
zielverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)
binnenverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)
externerPlusDurchgangO <- filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

## DRT data

drtO_customer <- read.csv2("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct/pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")
drtO_vehicle <- read.csv2("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct/pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_vehicle_stats_drt.csv")

drtO_customer <- dplyr::filter(drtO_customer, iteration == 500)
drtO_vehicle <- dplyr::filter(drtO_vehicle, iteration == 500)

# Create results dataframe

## DRT results dataframe

results_drt <- data.frame(prStationChoice = character(), drt_wait_avg = character(), drt_wait_median = character(), drt_wait_p95 = character(), drt_emptyRatio = character(), drt_totalEmptyDistance = character(), drt_meanOccupancy = character())

results_drt[1, ] <- list("closestToInsideActivity", drtI_customer$wait_average, drtI_customer$wait_median, drtI_customer$wait_p95, drtI_vehicle$emptyRatio, drtI_vehicle$totalEmptyDistance, drtI_vehicle$d_p.d_t)
results_drt[2, ] <- list("closestToOutsideActivity", drtO_customer$wait_average, drtO_customer$wait_median, drtO_customer$wait_p95, drtO_vehicle$emptyRatio, drtO_vehicle$totalEmptyDistance, drtO_vehicle$d_p.d_t)

## Trips results dataframe - innerCity vs Rest of Berlin/Brandenburg

trafficFlowTypes <- unique(tripsITest$trafficFlow)
results_trips <- data.frame(prStationChoice = character(), trafficFlow = character(), travtime_avg = numeric(), travtime_median = numeric(), travtime_p_95 = numeric(), dist_avg = numeric(), speed_avg = numeric())

results_trips[1, ] <- list("closestToInsideActivity", "Quellverkehr", mean(quellverkehrI$trav_time), median(quellverkehrI$trav_time), quantile(quellverkehrI$trav_time, probs = 0.95), mean(quellverkehrI$traveled_distance), (mean(quellverkehrI$traveled_distance) / mean(quellverkehrI$trav_time)) * 3.6)
results_trips[2, ] <- list("closestToInsideActivity", "Zielverkehr", mean(zielverkehrI$trav_time), median(zielverkehrI$trav_time), quantile(zielverkehrI$trav_time, probs = 0.95), mean(zielverkehrI$traveled_distance), (mean(zielverkehrI$traveled_distance) / mean(zielverkehrI$trav_time)) * 3.6)
results_trips[3, ] <- list("closestToInsideActivity", "Binnenverkehr", mean(binnenverkehrI$trav_time), median(binnenverkehrI$trav_time), quantile(binnenverkehrI$trav_time, probs = 0.95), mean(binnenverkehrI$traveled_distance), (mean(binnenverkehrI$traveled_distance) / mean(binnenverkehrI$trav_time)) * 3.6)
results_trips[4, ] <- list("closestToInsideActivity", "Externer_Durchgangsverkehr", mean(externerPlusDurchgangI$trav_time), median(externerPlusDurchgangI$trav_time), quantile(externerPlusDurchgangI$trav_time, probs = 0.95), mean(externerPlusDurchgangI$traveled_distance), (mean(externerPlusDurchgangI$traveled_distance) / mean(externerPlusDurchgangI$trav_time)) * 3.6)
results_trips[5, ] <- list("closestToInsideActivity", "Gesamtverkehr", mean(tripsI$trav_time), median(tripsI$trav_time), quantile(tripsI$trav_time, probs = 0.95), mean(tripsI$traveled_distance), (mean(tripsI$traveled_distance) / mean(tripsI$trav_time)) * 3.6)

results_trips[6, ] <- list("closestToOutsideActivity", "Quellverkehr", mean(quellverkehrO$trav_time), median(quellverkehrO$trav_time), quantile(quellverkehrO$trav_time, probs = 0.95), mean(quellverkehrO$traveled_distance), (mean(quellverkehrO$traveled_distance) / mean(quellverkehrO$trav_time)) * 3.6)
results_trips[7, ] <- list("closestToOutsideActivity", "Zielverkehr", mean(zielverkehrO$trav_time), median(zielverkehrO$trav_time), quantile(zielverkehrO$trav_time, probs = 0.95), mean(zielverkehrO$traveled_distance), (mean(zielverkehrO$traveled_distance) / mean(zielverkehrO$trav_time)) * 3.6)
results_trips[8, ] <- list("closestToOutsideActivity", "Binnenverkehr", mean(binnenverkehrO$trav_time), median(binnenverkehrO$trav_time), quantile(binnenverkehrO$trav_time, probs = 0.95), mean(binnenverkehrO$traveled_distance), (mean(binnenverkehrO$traveled_distance) / mean(binnenverkehrO$trav_time)) * 3.6)
results_trips[9, ] <- list("closestToOutsideActivity", "Externer_Durchgangsverkehr", mean(externerPlusDurchgangO$trav_time), median(externerPlusDurchgangO$trav_time), quantile(externerPlusDurchgangO$trav_time, probs = 0.95), mean(externerPlusDurchgangO$traveled_distance), (mean(externerPlusDurchgangO$traveled_distance) / mean(externerPlusDurchgangO$trav_time)) * 3.6)
results_trips[10, ] <- list("closestToOutsideActivity", "Gesamtverkehr", mean(tripsO$trav_time), median(tripsO$trav_time), quantile(tripsO$trav_time, probs = 0.95), mean(tripsO$traveled_distance), (mean(tripsO$traveled_distance) / mean(tripsO$trav_time)) * 3.6)

# Tryout plots
ggplot(quellverkehrI, aes(x = longest_distance_mode, y = wait_time)) +
  geom_boxplot()

ggplot(quellverkehrO, aes(x = end_facility_id, y = trav_time)) +
  geom_boxplot()


