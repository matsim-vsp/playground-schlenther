library(tidyr)
library(lubridate)
library(readr)
library(dplyr)

"This script prepares the output_trips table for the policy case. Mostly, it combines trips previously split into two trips at a P+R station."

#load relevant MATSim R functions (change when MATSim R works on cluster, devtools not working)
readTripsTable <- function (input_path = ".", n_max = Inf) 
{
  options(digits = 18)
  trips_file <- ""
  if (dir.exists(input_path)) {
    files <- list.files(input_path, full.names = TRUE)
    trip_file_indicies <- grep("output_trips.csv.gz$", files)
    if (length(trip_file_indicies) == 1) {
      trips_file <- files[trip_file_indicies]
    }
    else {
      stop("There is supposed to be a single \"output_trips.csv.gz\" found in directory")
    }
  }
  else {
    trips_file <- input_path
  }
  trips_output_table <- read_delim(trips_file, delim = ";", 
                                   locale = locale(decimal_mark = "."), n_max = n_max, 
                                   col_types = cols(start_x = col_character(), start_y = col_character(), 
                                                    end_x = col_character(), end_y = col_character(), 
                                                    end_link = col_character(), start_link = col_character()))
  trips_output_table <- trips_output_table %>% mutate(start_x = as.double(start_x), 
                                                      start_y = as.double(start_y), end_x = as.double(end_x), 
                                                      end_y = as.double(end_y))
  attr(trips_output_table, "table_name") <- trips_file
  return(trips_output_table)
}

########################################
# Preparation of policyTrips

args <- commandArgs(trailingOnly = TRUE)

input_path <- "D:/replaceCarByDRT/nitsch-final/runs-2023-09-01/10pct/roadTypesAllowed-all"
#input_path <- args[1]
policyTripsPrep <- readTripsTable(input_path)
output_filename <- "output_trips_prepared_debugged.tsv"
output_path <- file.path(input_path, output_filename)
prStations_path <- args[2]

print(paste("Amount of rows in tripsTable: ", nrow(policyTripsPrep)))
print(input_path)
print(prStations_path)

policyTripsPrep$prStation <- ""

prStations <- read.table(file = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/nitsch/berlin-no-inner-car-trips/scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv", sep = '\t', header = TRUE)
#prStations <- read.table(file = prStations_path, sep = "\t", header = TRUE)

for(i in 1:nrow(prStations)) {
  print(prStations[i,"name"])
}

for(i in 1:nrow(policyTripsPrep)) {
  if(policyTripsPrep[i,"end_activity_type"] == "P+R"){
    policyTripsPrep[i+1, "trip_number"] <- 0
    policyTripsPrep[i,"trav_time"] <- policyTripsPrep[i,"trav_time"] + policyTripsPrep[i+1,"trav_time"]  + period_to_seconds(minutes(5))
    policyTripsPrep[i, "wait_time"] <- policyTripsPrep[i, "wait_time"] + policyTripsPrep[i+1, "wait_time"]
    policyTripsPrep[i, "traveled_distance"] <- policyTripsPrep[i, "traveled_distance"] + policyTripsPrep[i+1, "traveled_distance"]
    policyTripsPrep[i, "euclidean_distance"] <- policyTripsPrep[i, "euclidean_distance"] + policyTripsPrep[i+1, "euclidean_distance"]
    policyTripsPrep[i, "main_mode"] <- paste(policyTripsPrep[i,"main_mode"], policyTripsPrep[i+1,"main_mode"],sep = "+")
    policyTripsPrep[i, "longest_distance_mode"] <- paste(policyTripsPrep[i,"longest_distance_mode"], policyTripsPrep[i+1,"longest_distance_mode"],sep = "+")
    policyTripsPrep[i, "modes"] <- paste(policyTripsPrep[i,"modes"], policyTripsPrep[i+1,"modes"],sep = "+")

    #TODO possibly: Get Information about the PR-Station in the trip
    for(k in 1:nrow(prStations)) {
      if(policyTripsPrep[i,"end_x"] == prStations[k,"x"]){
        if(policyTripsPrep[i,"end_y"] == prStations[k,"y"]){
          policyTripsPrep[i,"prStation"] <- prStations[k,"name"]
          print(prStations[k,"name"])
        }
      }
    }

    ## das übernimmt die attribute vom nachfolgenden trip - vermeintlich zweite Hälfte des P+R (impacted) trips
    policyTripsPrep[i, "end_activity_type"] <- policyTripsPrep[i+1, "end_activity_type"]
    policyTripsPrep[i, "end_facility_id"] <- policyTripsPrep[i+1, "end_facility_id"]
    policyTripsPrep[i, "end_link"] <- policyTripsPrep[i+1, "end_link"]
    policyTripsPrep[i, "end_x"] <- policyTripsPrep[i+1, "end_x"]
    policyTripsPrep[i, "end_y"] <- policyTripsPrep[i+1, "end_y"]
    policyTripsPrep[i, "last_pt_egress_stop"] <- policyTripsPrep[i+1, "last_pt_egress_stop"]

    ### reduziere trip number der nachfolgenden einträge
    j <- i+2
    #gehe alle Trips dieses Agenten durch
    #und reduziere die trip_number um 1, da wir oben ja einen trip in einen anderen gemerged haben. (i und i+1)
    while(policyTripsPrep[i,"person"] == policyTripsPrep[j,"person"]){
        policyTripsPrep[j, "trip_number"] <- policyTripsPrep[j,"trip_number"] - 1
        j <- j+1
    }
  }
  policyTripsPrep[i,"trip_id"] <- paste(as.character(policyTripsPrep[i,"person"]), as.character(policyTripsPrep[i,"trip_number"]), sep = "_")
  if(i %% 1000 == 0){
    print(paste(format(Sys.time(), "%Y-%m-%d %H:%M:%S"), " aktuelle Zeile = ",i, sep = "   "))
    
  }
}

#merge main_mode fuer hin und rueckrichtung
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt+car"] <- "car+pt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt_w_drt_used+car"] <- "car+pt_w_drt_used"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "walk+car"] <- "car+walk"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "drt+car"] <- "car+drt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "bicycle+car"] <- "car+bicycle"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt+ride"] <- "ride+pt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt_w_drt_used+ride"] <- "ride+pt_w_drt_used"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "walk+ride"] <- "ride+walk"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "drt+ride"] <- "ride+drt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "bicycle+ride"] <- "ride+bicycle"

policyTripsPrep <- policyTripsPrep %>% filter(!trip_number == 0)

write.table(policyTripsPrep,output_path,row.names = FALSE, sep = '\t')
