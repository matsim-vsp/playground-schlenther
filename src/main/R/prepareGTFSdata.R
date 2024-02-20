## Get coordinates of all S-Bahn stations
## -> outside the shape-file 
## -> and excluding the ones I already have
## -> Transform to needed EPSG
## -> Write .tsv-file out of it

"This script prepares GTFS data for the extra P+R stations outside the zone."

## TODO: fink closest link to each coordinate (and not by hand)

library(sf)

inputData <- read.table("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit-NichtJava/Daten/GTFS/stops.txt", header = TRUE, sep = ",")
shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")
s
stations_all <- subset(inputData, grepl("^S |^S\\+U ", stop_name))
stations_all <- stations_all[!duplicated(stations_all$stop_name), ]

data_frame_sf <- st_as_sf(stations_all, coords = c("stop_lon", "stop_lat"), crs = 4326)
data_frame_transformed <- st_transform(data_frame_sf, crs = 31468)
result_sf <- st_intersection(data_frame_transformed, shp)
result_df <- as.data.frame(result_sf)

data_frame_df <- as.data.frame(data_frame_transformed)
stations_outside <- data_frame_df %>%
  filter(!stop_name %in% result_df$stop_name) %>%
  filter(!grepl("5555", zone_id)) %>%
  filter(!grepl("/", stop_name)) %>%
  filter(!grepl("Bushalt", stop_desc)) %>%
  filter(!grepl("Tram", stop_desc))

stations_outside <- stations_outside %>%
  separate(col = geometry, into = c("x","y"), sep = ",")

stations_outside$x <- gsub("c\\(","",stations_outside$x)
stations_outside$y <- gsub("\\)", "", stations_outside$y)

final_data <- stations_outside %>%
  select(stop_name, x, y) %>%
  rename(name = stop_name) %>%
  mutate(linkId = 85582)

write.table(final_data,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/stops.tsv", row.names = FALSE, sep = "\t")
