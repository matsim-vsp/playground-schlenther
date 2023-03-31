require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)

# Importing and filtering trips for closestToInsideActivity

tripsI <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/")
tripsI <- filter(tripsI, main_mode %in% c("ride", "car"))

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

view(head(tripsI))
quellverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehrI <-  filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgangI <- filterByRegion(tripsI, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

plotMapWithTrips(externerPlusDurchgang1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

# Importing and filtering trips for closestToOutsideActivity

tripsO <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct/pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/")
tripsO <- filter(tripsO, main_mode %in% c("ride", "car"))
quellverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgangO <- filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

# Tryout plots
ggplot(quellverkehrI, aes(x = longest_distance_mode, y = wait_time)) +
  geom_boxplot()

ggplot(quellverkehrO, aes(x = end_facility_id, y = trav_time)) +
  geom_boxplot()


