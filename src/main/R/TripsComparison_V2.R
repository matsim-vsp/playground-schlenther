require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)

########################################
# Preparation

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutsideActivity/inside-allow-0.5-1506vehicles-8seats/"

basePersons <- readPersonsTable(baseCaseDirectory)
baseTrips <- readTripsTable(baseCaseDirectory)

policyPersons <- readPersonsTable(policyCaseDirectory)
policyTrips <- readTripsTable(policyCaseDirectory)

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base")) %>% 
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)


########################################
# Tests
# Is the amount of Grenztrips in baseCase the same like the amount of P+R trips in policyCase?

autoBase <- baseTrips %>% filter(main_mode %in% c("car","ride"))

"This should be the same result"
count(policyTrips)
count(baseTrips) + baseGrenzTripsCnt

"This should be the same result"
baseGrenzTripsCnt <- count(filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)) + 
  count(filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE))
policyTrips %>% filter(start_activity_type == "P+R") %>% count(.)
policyTrips %>% filter(end_activity_type == "P+R") %>% count(.)

# Does every Grenztrip have P+R as act_type? - TODO (with new simulation results)





########################################
# Travel times & distances
# Comparison of (travelTime, travelledDistance, speed) between PR-Trips(policy) and Grenztrips(base)
# Maybe as test: Comparison of the above for other traffic -> any significant changes there? [There shouldn´t be]

"Get those trips in base case that turn into PR trips in policy case - TODO"

rtb1 <- filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)
rtb2 <- filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE)
relevant_trips_base <- rbind(rtb1, rtb2)

rtp <- policyTrips %>% filter(end_activity_type == "P+R" | start_activity_type == "P+R")

personIds <- unique(rtp$person)
TripNumbers <- unique(rtp$trip_number)

for(person in personIds) {
  for(tripNumber in TripNumbers){
    
  }
}

for(i in 1:nrow(rtp)) {
  if(rtp[i,"end_activity_type"] == "P+R"){
    if(rtp[i, "person"] == rtp[i+1, "person"]){
      if(rtp[i, "trip_id"] == rtp[i+1, "trip_id"]){
        rtp[i, "trav_time"] <- rtp[i, "trav_time"] + rtp[i+1, "trav_time"] + minutes(5)
        rtp[i, "wait_time"] <- rtp[i, "wait_time"] + rtp[i+1, "wait_time"] + minutes(5)
        rtp[i, "traveled_distance"] <- rtp[i, "traveled_distance"] + rtp[i+1, "traveled_distance"]
        rtp[rtp$serial.id == i, "trav_time"] <- minutes(5)
      }
    }
  }
}

relevant_trips_policy <- rtp %>% filter(end_activity_type == "P+R" | start_activity_type == "P+R")

"Basic comparison"
mean(relevant_trips_base$trav_time)
mean(relevant_trips_policy$trav_time) * 2 + period_to_seconds(minutes(5))

mean(relevant_trips_base$traveled_distance)
mean(relevant_trips_policy$traveled_distance) * 2

(mean(relevant_trips_base$traveled_distance) / mean(period_to_seconds(hms(relevant_trips_base$trav_time)))) * 3.6
(mean(relevant_trips_policy$traveled_distance) / mean(period_to_seconds(hms(relevant_trips_policy$trav_time)))) * 3.6
"Hier wär´s spannend, auch ein Histogram / Boxplot machen zu können, um bessere Schlüsse ziehen zu können"

"Test - other traffic"
irrelevant_trips_base <- baseTrips %>% filter(!trip_id %in% relevant_trips_base$trip_id)
irrelevant_trips_policy <- policyTrips %>% filter(!trip_id %in% relevant_trips_policy$trip_id)

mean(irrelevant_trips_base$trav_time)
mean(irrelevant_trips_policy$trav_time)

mean(irrelevant_trips_base$traveled_distance)
mean(irrelevant_trips_policy$traveled_distance)

(mean(irrelevant_trips_base$traveled_distance) / mean(period_to_seconds(hms(irrelevant_trips_base$trav_time)))) * 3.6
(mean(irrelevant_trips_policy$traveled_distance) / mean(period_to_seconds(hms(irrelevant_trips_policy$trav_time)))) * 3.6

compareBasePolicyOutput(baseCaseDirectory, policyCaseDirectory, dump.output.to = "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/trips")
compareAverageTravelWait(relevant_trips_base, relevant_trips_policy)
compareModalDistanceDistribution(relevant_trips_base, relevant_trips_policy)

########################################
# Modal Shift
# Where do DRT users come from? Especially the losing ones?

drt_trips <- policyTrips %>% filter(grepl("drt", modes, fixed = TRUE)) #check if drt is in the mode chain (we can have intermodal trips where drt is not the main mode)

plotModalShiftSankey(baseTrips, drt_trips) +
  labs(
    title = "Modal Shift for all conv. KEXI Users"
  )

drtUsers <- personsJoined %>% filter(person %in% drt_trips$person)
drtUsers_neg <- drtUsers %>% filter(score_diff < 0)
drt_trips_neg <- drt_trips %>% filter(person %in% drtUsers_neg$person)

plotModalShiftSankey(baseTrips, drt_trips_neg) +
  labs(
    title = "Modal Shift for conv. KEXI Users who have a negative score_diff"
  )


# Where do Grenztrip agents go to?

plotModalShiftSankey(relevant_trips_base, relevant_trips_policy) #this is interesting, but doesn´t say anything yet bcz both tables don´t fit to each other

plotModalShiftSankey(baseTrips, policyTrips)

