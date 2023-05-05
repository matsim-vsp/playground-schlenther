require(matsim)
library(matsim)
library(dplyr)
library(sf)
library(tidyverse)
library(lubridate)
library(ggalluvial)
library(hms)

########################################
# Preparation

#TODO
colClasses = c("chr","num","chr")

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued/"
baseTrips <- readTripsTable(baseCaseDirectory)

policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/output_trips_prepared.tsv"


policyTrips <- read.table(file = policyCaseDirectory, sep ='\t', header = TRUE)
policyTrips <- policyTrips %>% 
  mutate(trip_number = as.double(trip_number),
         dep_time = parse_hms(dep_time),
         trav_time = parse_hms(trav_time),
         wait_time = parse_hms(wait_time),
         traveled_distance = as.double(traveled_distance),
         euclidean_distance = as.double(euclidean_distance),
         start_x = as.double(start_x), 
         start_y = as.double(start_y), end_x = as.double(end_x), 
         end_y = as.double(end_y))


policyTripsDRT
policyTripsPT 

plotModalShiftSankey(pr_trips_base,pr_trips_policy)


"PR trips = all those trips that got replaced by P+R"
pr_trips_policy <- policyTrips %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_trips_base <- baseTrips %>% filter(trip_id %in% pr_trips_policy$trip_id)

pr_trips <- merge(pr_trips_policy, pr_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_trips <- pr_trips %>% 
  add_column(travTime_diff = pr_trips$trav_time_policy - pr_trips$trav_time_base) %>%
  add_column(waitTime_diff = pr_trips$wait_time_policy - pr_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_trips$traveled_distance_policy - pr_trips$traveled_distance_base)

"Impacted trips = all those trips that got impacted by the policy"
#TODO



# Backup: for comparing speed difference
" add_column(speed_policy = relevant_trips$traveled_distance_policy * 3.6 / period_to_seconds(hms(relevant_trips$trav_time_policy))) %>%
  add_column(speed_base = relevant_trips$traveled_distance_base * 3.6 / period_to_seconds(hms(relevant_trips$trav_time_base)))
relevant_trips <- relevant_trips %>%
  add_column(speed_diff = relevant_trips$speed_policy - relevant_trips$speed_base)"


########################################
# Preparation of policyTrips

policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/"
policyTripsPrep <- readTripsTable(policyCaseDirectory)

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


########################################
# General results - travelTime of PR_Trips

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
    caption = "travTime_delta = travTime(policy) - travTime(base)",
    y = "travTime_delta [s]"
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/analysis/boxplot_travTime.png")


########################################
# General results - traveledDistance of PR_Trips

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
    caption = "traveledDistance_delta = traveledDistance(policy) - traveledDistance(base)",
    y = "traveledDistance_delta [m]"
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/analysis/boxplot_travelledDistance.png")


########################################
# TODO: 
# 1. Test impacted trips -> filter main_mode == car -> WARUM sind die noch drin?
# 2. Look at all impacted trips
# 3. Modal Shift Vergleich



########################################
# Losing vs winning agents (still interesting?)
relevant_trips <- relevant_trips %>%
  add_column(winnerLoser = "")

winners <- personsJoined %>% filter(score_diff > 0)
losers <- personsJoined %>% filter(score_diff < 0)
zeroChanges <- personsJoined %>% filter(score_diff == 0)

relevant_winnerTrips <- relevant_trips %>% filter(person_policy %in% winners$person)
relevant_winnerTrips$winnerLoser <- "winner"
relevant_loserTrips <- relevant_trips %>% filter(person_policy %in% losers$person)
relevant_loserTrips$winnerLoser <- "loser"

tryout <- rbind(relevant_winnerTrips, relevant_loserTrips)

"Boxplot - winners vs losers (travTime)"
ggplot(tryout, aes(x = winnerLoser, y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

"Boxplot - winners vs losers (traveledDistance)"
ggplot(tryout, aes(x = winnerLoser, y = traveledDistance_diff)) +
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
# Losing agents -> what transport modes?
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "pt+car"] <- "car+pt"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "pt_w_drt_used+car"] <- "car+pt_w_drt_used"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "walk+car"] <- "car+walk"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "drt+car"] <- "car+drt"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "pt+ride"] <- "ride+pt"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "pt_w_drt_used+ride"] <- "ride+pt_w_drt_used"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "walk+ride"] <- "ride+walk"
relevant_loserTrips$combined_main_mode[relevant_loserTrips$combined_main_mode == "drt+ride"] <- "ride+drt"


"Boxplot - Losers by transport mode (travTime)"
ggplot(relevant_loserTrips, aes(x = reorder(combined_main_mode,travTime_diff), y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

"Boxplot - Losers by transport mode (traveledDistance)"
ggplot(relevant_loserTrips, aes(x = reorder(combined_main_mode,traveledDistance_diff), y = traveledDistance_diff)) +
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
# Winning agents -> what transport modes?


########################################
# Test: relevant vs irrelevant trips (vllt auch Q/Z/B-Verkehr nochmal generelle / betroffene Trips-Vergleich?)


########################################
# Looking at: Binnenverkehr (nur betroffene Trips) 
baseBinnen <- filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = TRUE) #filter rather from PolicyBinnen
policyBinnen <- pTest %>% filter(trip_id %in% baseBinnen$trip_id)
#TODO: need to filter out all remaining P+R trips (right at the borders)

binnenTrips <- merge(policyBinnen, baseBinnen, by = "trip_id", suffixes = c("_policy","_base"))
binnenTrips <- binnenTrips %>% 
  add_column(travTime_diff = binnenTrips$trav_time_policy - binnenTrips$trav_time_base) %>%
  add_column(waitTime_diff = binnenTrips$wait_time_policy - binnenTrips$wait_time_base) %>%
  add_column(traveledDistance_diff = binnenTrips$traveled_distance_policy - binnenTrips$traveled_distance_base)

"Boxplot - Binnenverkehr (travTime)"
ggplot(binnenTrips, aes(y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of trav_time differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta [s]",
    x = "main_mode"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.title.x = element_blank()
  )

mean(binnenTrips$travTime_diff)

plotModalShiftSankey(baseBinnen,policyBinnen)

########################################
# Test: Externer/Durchgangsverkehr - vllt
baseExtern <- filterByRegion(baseTrips, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE) #filter rather from PolicyBinnen
policyExtern <- pTest %>% filter(trip_id %in% baseExtern$trip_id)
#TODO: need to filter out all remaining P+R trips (right at the borders)

externTrips <- merge(policyExtern, baseExtern, by = "trip_id", suffixes = c("_policy","_base"))
externTrips <- externTrips %>% 
  add_column(travTime_diff = externTrips$trav_time_policy - externTrips$trav_time_base) %>%
  add_column(waitTime_diff = externTrips$wait_time_policy - externTrips$wait_time_base) %>%
  add_column(traveledDistance_diff = externTrips$traveled_distance_policy - externTrips$traveled_distance_base)

########################################
# Test: Quell/Zielverkehr car/ride, der nicht P+R ist?
baseQuell <- filterByRegion(autoBase, shp, "EPSG:31468", start.inshape = TRUE, end.inshape = FALSE) #filter rather from PolicyBinnen
policyQuell <- pTest %>% filter(trip_id %in% baseQuell$trip_id)

quellTrips <- merge(policyQuell, baseQuell, by = "trip_id", suffixes = c("_policy","_base"))


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


# Where do agents go to whose trip got replaced? -> TODO: not using combined_main_mode? -> main_mode in tripsTable überschreiben
plotModalShiftSankey(relevant_trips_base, relevant_trips_policy)


##### TRYOUT: Comparison pt, drt, pt&drt
plotModalSplitBarChart(policyTrips)

policyTrips_E <- filterByRegion(policyTrips, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)
policyTripsPT_E <- filterByRegion(policyTrips, shp, "EPSG:31468", start.inshape = FALSE, end.inshape = FALSE)

policyTrips_relevant <- policyTrips %>% filter(!trip_id %in% policyTrips_E$trip_id)
policyTripsPT_relevant <- policyTripsPT %>% filter(!trip_id %in% policyTripsPT_E$trip_id)



sfTest <- policyTrips_relevant %>% 
  filter(main_mode == "car") %>%
  matsim::transformToSf(.,"EPSG:31468",geometry.type = st_point())

filtered_sf <- transformToSf(filtered, crs = crs, geometry.type = st_point())
st_geometry(filtered_sf) <- "start_wkt"

ggplot() +
  geom_sf(data = sfTest, aes(color = "Blue")) +
  geom_sf(data = shp, aes(color = "Red"))



plotModalShiftSankey(policyTripsPT_relevant, policyTrips_relevant)

policyTripsPT <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/closestToOutside-0.5-1506vehicles-8seats/")
policyTripsDRT <- readTripsTable("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/drt/closestToOutside-0.5-1506vehicles-8seats/")

plotModalSplitBarChart(policyTripsPT)
plotModalSplitBarChart(policyTripsDRT)
