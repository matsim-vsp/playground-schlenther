require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)

# Importing and filtering trips for closestToInsideActivity

trips1 <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/")
trips1 <- filter(trips1, main_mode %in% c("ride", "car","drt"))

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

view(head(trips1))
quellverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgang1 <- filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

plotMapWithTrips(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

# Importing and filtering trips for closestToOutsideActivity

tripsO <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct/pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/")
tripsO <- filter(tripsO, main_mode %in% c("ride", "car","drt"))
quellverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehrO <-  filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgangO <- filterByRegion(tripsO, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

# Tryout plots
ggplot(quellverkehr1, aes(x = end_facility_id, y = trav_time)) +
  geom_boxplot()

ggplot(quellverkehrO, aes(x = end_facility_id, y = trav_time)) +
  geom_boxplot()


