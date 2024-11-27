library(tidyr)
library(lubridate)
library(readr)
library(dplyr)
library(matsim)
library(data.table)

# Define input and output paths
input_path <- "E:/schlenther/berlin/2024-berlin-v6.3-autofrei/output-10pct/speedUp/drtHndKpf7.5kV-prRing-ptDrt10pOnly"
output_filename <- "output_trips_prepared_offline.tsv"
output_path <- file.path(input_path, output_filename)
prStations_path <- "D:/git/playground-schlenther/scenarios/berlin-v6.3/berlin-v6.3-pr-stations-ring.tsv"

# Read policy trips
policyTrips <- matsim::read_output_trips(input_path)

# Read P+R stations
prStations <- read.table(file = prStations_path, sep = "\t", header = TRUE)

# Read policy persons and filter subpopulation
policyPersons <- matsim::read_output_persons(input_path) %>%
  filter(subpopulation == "person")

# Filter trips to include only those by conducted by subpopulation person
policyTripsPrep <- policyTrips %>%
  filter(person %in% policyPersons$person)

# Convert to data.table
policyTripsTable <- as.data.table(policyTripsPrep)
prStationsTable <- as.data.table(prStations)

# Add an index column for linking subsequent trips
policyTripsTable[, next_index := .I + 1]

# Merge with P+R stations data
policyTripsTable <- merge(policyTripsTable, prStationsTable, by.x = c("end_x", "end_y"), by.y = c("x", "y"), all.x = TRUE, suffixes = c("", ".pr"))

# Process combined trips at P+R stations
policyTripsTable[end_activity_type == "P+R", `:=` (
  trip_number = shift(trip_number, type = "lead", fill = NA, n = 1),
  trav_time = trav_time + shift(trav_time, type = "lead", fill = 0, n = 1),
  wait_time = wait_time + shift(wait_time, type = "lead", fill = 0, n = 1),
  traveled_distance = traveled_distance + shift(traveled_distance, type = "lead", fill = 0, n = 1),
  euclidean_distance = euclidean_distance + shift(euclidean_distance, type = "lead", fill = 0, n = 1),
  main_mode = paste(main_mode, shift(main_mode, type = "lead", fill = "", n = 1), sep = "+"),
  longest_distance_mode = paste(longest_distance_mode, shift(longest_distance_mode, type = "lead", fill = "", n = 1), sep = "+"),
  modes = paste(modes, shift(modes, type = "lead", fill = "", n = 1), sep = "+"),
  prStation = ifelse(!is.na(name), name, prStation),
  end_activity_type = shift(end_activity_type, type = "lead", fill = NA, n = 1),
  end_facility_id = shift(end_facility_id, type = "lead", fill = NA, n = 1),
  end_link = shift(end_link, type = "lead", fill = NA, n = 1),
  end_x = shift(end_x, type = "lead", fill = NA, n = 1),
  end_y = shift(end_y, type = "lead", fill = NA, n = 1),
  last_pt_egress_stop = shift(last_pt_egress_stop, type = "lead", fill = NA, n = 1)
)]

# Update trip_id
policyTripsTable[, trip_id := paste(as.character(person), as.character(trip_number), sep = "_")]

# Adjust trip_number for subsequent trips
policyTripsTable[, adjustment := 0]
policyTripsTable[end_activity_type == "P+R", adjustment := 1]

policyTripsTable[, trip_number := trip_number - cumsum(shift(adjustment, fill = 0)), by = person]

# Remove helper columns
policyTripsTable[, `:=`(next_index = NULL, adjustment = NULL)]

# Merge main modes for round trips
policyTripsTable$main_mode[policyTripsTable$main_mode == "pt+car"] <- "car+pt"
policyTripsTable$main_mode[policyTripsTable$main_mode == "pt_w_drt_used+car"] <- "car+pt_w_drt_used"
policyTripsTable$main_mode[policyTripsTable$main_mode == "walk+car"] <- "car+walk"
policyTripsTable$main_mode[policyTripsTable$main_mode == "drt+car"] <- "car+drt"
policyTripsTable$main_mode[policyTripsTable$main_mode == "bike+car"] <- "car+bike"
policyTripsTable$main_mode[policyTripsTable$main_mode == "pt+ride"] <- "ride+pt"
policyTripsTable$main_mode[policyTripsTable$main_mode == "pt_w_drt_used+ride"] <- "ride+pt_w_drt_used"
policyTripsTable$main_mode[policyTripsTable$main_mode == "walk+ride"] <- "ride+walk"
policyTripsTable$main_mode[policyTripsTable$main_mode == "drt+ride"] <- "ride+drt"
policyTripsTable$main_mode[policyTripsTable$main_mode == "bike+ride"] <- "ride+bike"

# Filter out trips with trip_number 0
policyTripsTable <- policyTripsTable %>% filter(trip_number != 0)

# Write the processed data to a file
write.table(policyTripsTable,
            output_path,
            row.names = FALSE,
            sep = '\t')

# Optional progress message
print(paste(format(Sys.time(), "%Y-%m-%d %H:%M:%S"), "Optimierung abgeschlossen"))
