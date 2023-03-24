require(matsim)
library(matsim)
library(dplyr)
library(sf)


trips1 <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/")

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

view(head(quellverkehr1))
quellverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgang1 <- filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

plotMapWithTrips(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

# Tryout plots
ggplot(quellverkehr1, aes(x = start_facility_id, y = trav_time)) +
  geom_boxplot()

ggplot(zielverkehr1, aes(x = start_facility_id, y = trav_time)) +
  geom_boxplot()
