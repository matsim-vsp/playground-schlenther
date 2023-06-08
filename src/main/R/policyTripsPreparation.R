require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

########################################
# Preparation of policyTrips

input_path <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/"
policyTripsPrep <- readTripsTable(input_path)
output_filename <- "output_trips_prepared.tsv"
output_path <- file.path(input_path, output_filename)

policyTripsPrep$prStation <- ""

prStations <- read.table(file = "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv", sep = '\t', header = TRUE)


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
      if((policyTripsPrep[i,"end_x"] == prStations[k,"x"]) & (policyTripsPrep[i,"end_y"] == prStations[k,"y"])){
        policyTripsPrep[i,"prStation"] <- prStations[k,"name"]
        print(prStations[k,"name"])
      }
    }
    
    policyTripsPrep[i, "end_activity_type"] <- policyTripsPrep[i+1, "end_activity_type"]
    policyTripsPrep[i, "end_facility_id"] <- policyTripsPrep[i+1, "end_facility_id"]
    policyTripsPrep[i, "end_link"] <- policyTripsPrep[i+1, "end_link"]
    policyTripsPrep[i, "end_x"] <- policyTripsPrep[i+1, "end_x"]
    policyTripsPrep[i, "end_y"] <- policyTripsPrep[i+1, "end_y"]
    policyTripsPrep[i, "last_pt_egress_stop"] <- policyTripsPrep[i+1, "last_pt_egress_stop"]
    
    j <- i+2
    
    while(j < 1000000){
      if(policyTripsPrep[j, "trip_number"] != 1){
        policyTripsPrep[j, "trip_number"] <- policyTripsPrep[j,"trip_number"] - 1
        j <- j+1
      } else {
        j <- 1000000
      }
    }
  }
  policyTripsPrep[i,"trip_id"] <- paste(as.character(policyTripsPrep[i,"person"]), as.character(policyTripsPrep[i,"trip_number"]), sep = "_")
  print(i)
}

policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt+car"] <- "car+pt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt_w_drt_used+car"] <- "car+pt_w_drt_used"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "walk+car"] <- "car+walk"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "drt+car"] <- "car+drt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt+ride"] <- "ride+pt"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "pt_w_drt_used+ride"] <- "ride+pt_w_drt_used"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "walk+ride"] <- "ride+walk"
policyTripsPrep$main_mode[policyTripsPrep$main_mode == "drt+ride"] <- "ride+drt"

policyTripsPrep <- policyTripsPrep %>% filter(!trip_number == 0)

write.table(policyTripsPrep,output_path,row.names = FALSE, sep = '\t')
