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

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued/"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/closestToOutside-0.5-1506vehicles-8seats/"

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
baseGrenzTripsCnt <- count(filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE)) + 
  count(filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = TRUE))

"This should be the same result -> NOT PASSED"
count(policyTrips)
count(baseTrips) + baseGrenzTripsCnt

"This should be the same result -> TEST PASSED"
policyTrips %>% filter(start_activity_type == "P+R") %>% count(.)
policyTrips %>% filter(end_activity_type == "P+R") %>% count(.)


########################################
# Prepare (general) policyTrips

pTest <- policyTrips

for(i in 1:nrow(pTest)) {
  if(pTest[i,"end_activity_type"] == "P+R"){
    pTest[i+1, "trip_number"] <- pTest[i,"trip_number"]
    j <- i+2
    while(j < 1000000){
      if(!pTest[j, "trip_number"] == 1){
        pTest[j, "trip_number"] <- pTest[j,"trip_number"] - 1
        j <- j+1
      } else {
        j <- 1000000
      }
    }
  }
  pTest[i,"trip_id"] <- paste(as.character(pTest[i,"person"]), as.character(pTest[i,"trip_number"]), sep = "_")
  print(i)
}

########################################
# Prepare relevant policyTrips (only PR-Trips)

rtpTest <- pTest %>% filter(end_activity_type == "P+R" | start_activity_type == "P+R") %>%
  add_column(combined_main_mode = "")

for(i in 1:nrow(rtpTest)) {
  if(rtpTest[i,"end_activity_type"] == "P+R"){
    rtpTest[i, "trav_time"] <- rtpTest[i, "trav_time"] + rtpTest[i+1, "trav_time"] + period_to_seconds(minutes(5))
    rtpTest[i, "wait_time"] <- rtpTest[i, "wait_time"] + rtpTest[i+1, "wait_time"]
    rtpTest[i, "traveled_distance"] <- rtpTest[i, "traveled_distance"] + rtpTest[i+1, "traveled_distance"]
    rtpTest[i, "combined_main_mode"] <- paste(rtpTest[i,"main_mode"], rtpTest[i+1,"main_mode"],sep = "+")
    # avg_speed ergänzen?
    print(i)
  }
}

relevant_trips_policy <- rtpTest %>% filter(end_activity_type == "P+R")

########################################
# Prepare relevant baseTrips
"Get those trips in base case that turn into PR trips in policy case - TODO"

relevant_trips_base <- baseTrips %>% filter(trip_id %in% relevant_trips_policy$trip_id)

########################################
# Travel times & distances
# Comparison of travelTime & travelledDistance (maybe speed?) between PR-Trips(policy) and Grenztrips(base)

relevant_trips <- merge(relevant_trips_policy, relevant_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
relevant_trips <- relevant_trips %>% 
  add_column(travTime_diff = relevant_trips$trav_time_policy - relevant_trips$trav_time_base) %>%
  add_column(waitTime_diff = relevant_trips$wait_time_policy - relevant_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = relevant_trips$traveled_distance_policy - relevant_trips$traveled_distance_base)

# Backup: for comparing speed difference
" add_column(speed_policy = relevant_trips$traveled_distance_policy * 3.6 / period_to_seconds(hms(relevant_trips$trav_time_policy))) %>%
  add_column(speed_base = relevant_trips$traveled_distance_base * 3.6 / period_to_seconds(hms(relevant_trips$trav_time_base)))
relevant_trips <- relevant_trips %>%
  add_column(speed_diff = relevant_trips$speed_policy - relevant_trips$speed_base)"

########################################
# General results - travelTime

"General metrics"
mean(relevant_trips$travTime_diff)
sd(relevant_trips$travTime_diff)
quantile(relevant_trips$travTime_diff, probs = 0.95)

"Histogram"
ggplot(relevant_trips, aes(x = travTime_diff)) +
  geom_histogram(binwidth = 5) +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    x = "score_delta [s]"
  )+
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic")
  )

"Boxplot"
ggplot(relevant_trips, aes(y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )

########################################
# General results - traveledDistance

"General metrics"
mean(relevant_trips$traveledDistance_diff)
sd(relevant_trips$traveledDistance_diff)
quantile(relevant_trips$traveledDistance_diff, probs = 0.95)

"Histogram"
ggplot(relevant_trips, aes(x = traveledDistance_diff)) +
  geom_histogram(binwidth = 5) +
  labs(
    title = "Distribution of traveled_distance differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    x = "score_delta [m]"
  )+
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic")
  )

"Boxplot"
ggplot(relevant_trips, aes(y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveled_distance differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [m]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.ticks.x = element_blank(),
    axis.title.x = element_blank(),
    axis.text.x = element_blank()
  )


########################################
# Losing vs winning agents
winners <- personsJoined %>% filter(score_diff > 0)
losers <- personsJoined %>% filter(score_diff < 0)
zeroChanges <- personsJoined %>% filter(score_diff == 0)

relevant_winnerTrips <- relevant_trips %>% filter(person_policy %in% winners$person)
relevant_loserTrips <- relevant_trips %>% filter(person_policy %in% losers$person)
relevant_zeroChangeTrips <- relevant_trips %>% filter(person_policy %in% zeroChanges$person)

mean(relevant_winnerTrips$travTime_diff)
mean(relevant_loserTrips$travTime_diff)

mean(relevant_winnerTrips$traveledDistance_diff)
mean(relevant_loserTrips$traveledDistance_diff)

"Boxplot"
ggplot(relevant_loserTrips, aes(x = combined_main_mode, y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of traveled_distance differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [m]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )


########################################
# By Q/Z-Verkehr?, DRT users (policy), Losing users?, compared to all other trips?




########################################
# Backup

"Basic comparison"
mean(relevant_trips_base$trav_time)
mean(relevant_trips_policy$trav_time) * 2 + period_to_seconds(minutes(5))

mean(relevant_trips_base$traveled_distance)
mean(relevant_trips_policy$traveled_distance) * 2

(mean(relevant_trips_base$traveled_distance) / mean(period_to_seconds(hms(relevant_trips_base$trav_time)))) * 3.6 #Speed
(mean(relevant_trips_policy$traveled_distance) / mean(period_to_seconds(hms(relevant_trips_policy$trav_time)))) * 3.6
"Hier wär´s spannend, auch ein Histogram / Boxplot machen zu können, um bessere Schlüsse ziehen zu können"

"Test - other traffic"
irrelevant_trips_base <- baseTrips %>% filter(!trip_id %in% relevant_trips_base$trip_id)
irrelevant_trips_policy <- policyTrips %>% filter(!trip_id %in% relevant_trips_policy$trip_id)

mean(irrelevant_trips_base$trav_time)
mean(irrelevant_trips_policy$trav_time)

mean(irrelevant_trips_base$traveled_distance)
mean(irrelevant_trips_policy$traveled_distance)

(mean(irrelevant_trips_base$traveled_distance) / mean(period_to_seconds(hms(irrelevant_trips_base$trav_time)))) * 3.6 #Speed
(mean(irrelevant_trips_policy$traveled_distance) / mean(period_to_seconds(hms(irrelevant_trips_policy$trav_time)))) * 3.6



########################################
# Modal Shift
# Where do DRT users come from? Especially the losing ones?

drt_trips <- policyTrips %>% filter(grepl("drt", modes, fixed = TRUE)) #check if drt is in the mode chain (we can have intermodal trips where drt is not the main mode)

plotModalShiftSankey(baseTrips, drt_trips)

drtUsers <- personsJoined %>% filter(person %in% drt_trips$person)
drtUsers_neg <- drtUsers %>% filter(score_diff < 0)
drt_trips_neg <- drt_trips %>% filter(person %in% drtUsers_neg$person)

plotModalShiftSankey(baseTrips, drt_trips_neg)


# Where do agents go to whose trip got replaced? -> TODO: not using combined_main_mode?
plotModalShiftSankey(relevant_trips_base, relevant_trips_policy)

