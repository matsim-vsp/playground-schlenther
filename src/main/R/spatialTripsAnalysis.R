require(matsim)
library(matsim)
library(dplyr)
library(sf)


trips1 <- readTripsTable("D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/output-berlin-v5.5-1pct/")

shp <- st_read("D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp")

quellverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)

zielverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)

binnenverkehr1 <-  filterByRegion(trips1, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE)

externerPlusDurchgang1 <- filterByRegion(trips1, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

plotMapWithTrips()