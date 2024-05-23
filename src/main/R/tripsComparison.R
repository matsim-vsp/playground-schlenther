library(tidyr)
library(lubridate)
library(hms)
library(readr)
library(sf)
library(dplyr)
library(matsim)
library(tidyverse)
library(ggalluvial)

"In this script, the trips of base and policy case gets compared. Several tsv-files & graphs are written as output results."


########################################
# Preparation

# #HPC Cluster
 args <- commandArgs(trailingOnly = TRUE)
 policyCaseDirectory <- args[1]
 baseCaseDirectory <- args[3]
 shp <- st_read(args[5])
 shp_berlin <- st_read(args[6])

 
 
# 10pct
baseCaseDirectory <- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/nitsch/berlin-no-inner-car-trips/scenarios/output/baseCaseContinued-10pct"
policyCaseDirectory <- "D:/replaceCarByDRT/nitsch-final/runs-2023-09-01/10pct/roadTypesAllowed-all"
shp <- st_read("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/nitsch/berlin-no-inner-car-trips/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")
shp_berlin <- st_read("//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/nitsch/berlin-no-inner-car-trips/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/berlin.shp")


# #1pct
# baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued/"
# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/1pct/optimum-flowCapacity/"
# #policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/"

# read the table which was created by policyTripsPreparation.R (which sticks together both parts of P+R trips)
policy_filename <- "output_trips_prepared.tsv"
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

baseTrips <- readTripsTable(baseCaseDirectory)

policyTrips <- read.table(file = policy_inputfile, sep ='\t', header = TRUE)
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

##read unprepared policy trips for de-bugging
policyTrips_unprepped <- readTripsTable(policyCaseDirectory)

policyTrips_debugged <- read.table(file = "D:/replaceCarByDRT/nitsch-final/runs-2023-09-01/10pct/roadTypesAllowed-all/output_trips_prepared_debugged.tsv",
                                   sep ='\t', header = TRUE)
policyTrips <- policyTrips_debugged %>% 
  mutate(trip_number = as.double(trip_number),
         dep_time = parse_hms(dep_time),
         trav_time = parse_hms(trav_time),
         wait_time = parse_hms(wait_time),
         traveled_distance = as.double(traveled_distance),
         euclidean_distance = as.double(euclidean_distance),
         start_x = as.double(start_x), 
         start_y = as.double(start_y), end_x = as.double(end_x), 
         end_y = as.double(end_y))


########################################
# Prepare folders

dir.create(paste0(policyCaseDirectory,"/analysis"))
dir.create(paste0(policyCaseDirectory,"/analysis/trips"))
policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips")

########################################
# Filter out all agents with scoreDiff > -400 (those are 3 persons in the roadTypes_all case)

basePersons <- read.table(file = file.path(baseCaseDirectory, "output_plans_selectedPlanScores.tsv"), sep = '\t', header = TRUE)
policyPersons <- read.table(file = file.path(policyCaseDirectory, "output_plans_selectedPlanScores.tsv"), sep = '\t', header = TRUE)

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base"))
personsJoined <- personsJoined %>%
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)
personsJoined <- personsJoined %>% filter(score_diff > -400)

baseTrips <- baseTrips %>% filter(person %in% personsJoined$person)
policyTrips <- policyTrips %>% filter(person %in% personsJoined$person)

########################################
# Prepare trips tables

"Impacted Grenztrips"
autoBase <- baseTrips %>% filter(main_mode == "car" | main_mode == "ride")
impQuell_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, FALSE)
impZiel_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, FALSE, TRUE)
impGrenz_trips_base <- rbind(impQuell_trips_base, impZiel_trips_base)
impGrenz_trips_policy <- policyTrips %>% filter(trip_id %in% impGrenz_trips_base$trip_id)

impGrenz_trips <- merge(impGrenz_trips_policy, impGrenz_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impGrenz_trips <- impGrenz_trips %>% 
  add_column(travTime_diff = impGrenz_trips$trav_time_policy - impGrenz_trips$trav_time_base) %>%
  add_column(waitTime_diff = impGrenz_trips$wait_time_policy - impGrenz_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impGrenz_trips$traveled_distance_policy - impGrenz_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impGrenz_trips$euclidean_distance_policy - impGrenz_trips$euclidean_distance_base) %>%
  filter(travTime_diff < 20000)

"Impacted Binnentrips"
impBinnen_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_trips_policy <- policyTrips %>% filter(trip_id %in% impBinnen_trips_base$trip_id)

impBinnen_trips <- merge(impBinnen_trips_policy, impBinnen_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_trips <- impBinnen_trips %>% 
  add_column(travTime_diff = impBinnen_trips$trav_time_policy - impBinnen_trips$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_trips$wait_time_policy - impBinnen_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_trips$traveled_distance_policy - impBinnen_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_trips$euclidean_distance_policy - impBinnen_trips$euclidean_distance_base)%>%
  filter(travTime_diff < 20000)

"All impacted trips (Impacted Grenztrips + Impacted Binnentrips)"
impacted_trips_base <- rbind(impGrenz_trips_base,impBinnen_trips_base)
impacted_trips_policy <- rbind(impGrenz_trips_policy,impBinnen_trips_policy)

impacted_trips <- merge(impacted_trips_policy, impacted_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_trips <- impacted_trips %>% 
  add_column(travTime_diff = impacted_trips$trav_time_policy - impacted_trips$trav_time_base) %>%
  add_column(waitTime_diff = impacted_trips$wait_time_policy - impacted_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_trips$traveled_distance_policy - impacted_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_trips$euclidean_distance_policy - impacted_trips$euclidean_distance_base)%>%
  filter(travTime_diff < 20000)

########################################
"Modal Shift Sankeys - betroffene Trips"
# Filter bedingt durch teilweise falsch erkannte Trips durch filterByRegion, siehe trips_falselyClassified.tsv

"Grenztrips"
prep_grenz_policy <- impGrenz_trips_policy %>%
  filter(!main_mode == "ride") %>%
  filter(!main_mode == "car") %>%
  filter(!main_mode == "drt") %>%
  filter(!main_mode == "bicycle")
prep_grenz_policy$main_mode[prep_grenz_policy$main_mode == "bicycle+ride"] <- "ride+bicycle"
prep_grenz_policy$main_mode[prep_grenz_policy$main_mode == "bicycle+car"] <- "car+bicycle"
prep_grenz_base <- impGrenz_trips_base %>% filter(trip_id %in% prep_grenz_policy$trip_id)
plotModalShiftSankey(prep_grenz_base, prep_grenz_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_grenz.png"))

"Binnentrips"
prep_binnen_policy <- impBinnen_trips_policy %>%
  filter(!grepl("+", main_mode, fixed = TRUE)) %>%
  filter(!main_mode == "car") %>%
  filter(!main_mode == "ride") %>%
  filter(!main_mode == "bicycle")
prep_binnen_base <- impBinnen_trips_base %>% filter(trip_id %in% prep_binnen_policy$trip_id)
plotModalShiftSankey(prep_binnen_base,prep_binnen_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_binnen.png"))

"All impacted trips"
prep_policy <- rbind(prep_grenz_policy, prep_binnen_policy)
prep_base <- rbind(prep_grenz_base, prep_binnen_base)
plotModalShiftSankey(prep_base,prep_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_impacted.png"))

# Zahlen Modal Split für betroffene Trips
results_modalSplitAffected <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "drt (Binnen) [%]", value = nrow(prep_binnen_policy %>% filter(main_mode == "drt")) / nrow(prep_binnen_policy) * 100) %>%
  add_row(key = "pt_w_drt_used (Binnen) [%]", value = nrow(prep_binnen_policy %>% filter(main_mode == "pt_w_drt_used")) / nrow(prep_binnen_policy) * 100) %>%
  add_row(key = "pt (Binnen) [%]", value = nrow(prep_binnen_policy %>% filter(main_mode == "pt")) / nrow(prep_binnen_policy) * 100) %>%
  add_row(key = "walk (Binnen) [%]", value = nrow(prep_binnen_policy %>% filter(main_mode == "walk")) / nrow(prep_binnen_policy) * 100) %>%
  add_row(key = "pt_only,pt_w_drt_used_only (QZ) [%]", value = nrow(prep_grenz_policy %>% filter(main_mode == "pt" | main_mode == "pt_w_drt_used")) / nrow(prep_grenz_policy) * 100) %>%
  add_row(key = "pt_w_drt_used_only / all_pt_only (QZ) [%]", value = nrow(prep_grenz_policy %>% filter(main_mode == "pt_w_drt_used")) / nrow(prep_grenz_policy %>% filter(main_mode == "pt" | main_mode == "pt_w_drt_used")) * 100) %>%
  add_row(key = "PR_used (QZ) [%]", value = nrow(prep_grenz_policy %>% filter(grepl("+",main_mode,fixed=TRUE))) / nrow(prep_grenz_policy) * 100) %>%
  add_row(key = "drt_inside / PR_used (QZ) [%]", value = nrow(prep_grenz_policy  %>% filter(grepl("+drt",main_mode,fixed=TRUE))) / nrow(prep_grenz_policy %>% filter(grepl("+",main_mode,fixed=TRUE))) * 100)

########################################
"Modal Shift Sankeys - alle Trips"

quell_base <- baseTrips %>% filterByRegion(., shp_berlin, crs = 31468, TRUE, FALSE)
ziel_base <- baseTrips %>% filterByRegion(., shp_berlin, crs = 31468, FALSE, TRUE)
grenz_base <- rbind(quell_base, ziel_base)
binnen_base <- baseTrips %>% filterByRegion(., shp_berlin, crs = 31468, TRUE, TRUE)

quell_policy <- policyTrips %>% filterByRegion(., shp_berlin, crs = 31468, TRUE, FALSE)
ziel_policy <- policyTrips %>% filterByRegion(., shp_berlin, crs = 31468, FALSE, TRUE)
grenz_policy <- rbind(quell_policy, ziel_policy)
binnen_policy <- policyTrips %>% filterByRegion(., shp_berlin, crs = 31468, TRUE, TRUE)

plotModalShiftSankey(grenz_base,grenz_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_all_grenz.png"))
plotModalShiftSankey(binnen_base,binnen_policy)
ggsave(file.path(policyTripsOutputDir,"modalShiftSankey_all_binnen.png"))

pr_trips_policy <- policyTrips %>% filter(grepl("+", main_mode, fixed = TRUE))
pr_trips_base <- baseTrips %>% filter(trip_id %in% pr_trips_policy$trip_id)

# Zahlen Modal Split für ganz Berlin: Anteile der Verkehrsmittel am Modal Split vorher & nachher - normalisiert auf 100% für Policy-Case
results_modalSplitAll <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "car (base, Berlin) [%]", value = nrow(binnen_base %>% filter(main_mode == "car")) / nrow(binnen_base) * 100) %>%
  add_row(key = "car (policy, Berlin) [%]", value = nrow(binnen_policy %>% filter(grepl("car",main_mode, fixed = TRUE))) / (nrow(binnen_policy) + nrow(binnen_policy %>% filter(grepl("+",main_mode,fixed=TRUE)))) * 100) %>%
  add_row(key = "ride (base, Berlin) [%]", value = nrow(binnen_base %>% filter(main_mode == "ride")) / nrow(binnen_base) * 100) %>%
  add_row(key = "ride (policy, Berlin) [%]", value = nrow(binnen_policy %>% filter(grepl("ride",main_mode, fixed = TRUE))) / (nrow(binnen_policy) + nrow(binnen_policy %>% filter(grepl("+",main_mode,fixed=TRUE)))) * 100) %>%
  add_row(key = "pt (base, Berlin) [%]", value = nrow(binnen_base %>% filter(main_mode == "pt")) / nrow(binnen_base) * 100) %>%
  add_row(key = "pt,drt,pt_w_drt_used (policy, Berlin) [%]", value = nrow(binnen_policy %>% filter(grepl("pt",main_mode, fixed = TRUE) | grepl("drt",main_mode,fixed=TRUE))) / (nrow(binnen_policy) + nrow(binnen_policy %>% filter(grepl("+",main_mode,fixed=TRUE)))) * 100) %>%
  add_row(key = "pt_only (policy, Berlin) [%]", value = nrow(binnen_policy %>% filter(main_mode == "pt")) / nrow(binnen_policy) * 100) %>%
  add_row(key = "car (base, QZ Berlin) [%]", value = nrow(grenz_base %>% filter(main_mode == "car" | main_mode == "ride")) / nrow(grenz_base) * 100) %>%
  add_row(key = "car (policy, QZ Berlin) [%]", value = nrow(grenz_policy %>% filter(grepl("car",main_mode, fixed = TRUE) | grepl("ride",main_mode,fixed=TRUE))) / (nrow(grenz_policy) + nrow(grenz_policy %>% filter(grepl("+",main_mode,fixed=TRUE)))) * 100) %>%
  add_row(key = "pt (base, QZ Berlin) [%]", value = nrow(grenz_base %>% filter(main_mode == "pt")) / nrow(grenz_base) * 100) %>%
  add_row(key = "pt,drt,pt_w_drt_used (policy, QZ Berlin) [%]", value = nrow(grenz_policy %>% filter(grepl("pt",main_mode, fixed = TRUE) | grepl("drt",main_mode,fixed=TRUE))) / (nrow(grenz_policy) + nrow(grenz_policy %>% filter(grepl("+",main_mode,fixed=TRUE)))) * 100) %>%
  add_row(key = "pt_only (policy, QZ Berlin) [%]", value = nrow(grenz_policy %>% filter(main_mode == "pt")) / nrow(grenz_policy) * 100) %>%
  add_row(key = "car (base, all) [%]", value = nrow(baseTrips %>% filter(main_mode == "car" | main_mode == "ride")) / nrow(baseTrips) * 100) %>%
  add_row(key = "car (policy, all) [%]", value = nrow(policyTrips %>% filter(grepl("car",main_mode, fixed = TRUE) | grepl("ride",main_mode,fixed=TRUE))) / (nrow(policyTrips) + nrow(pr_trips_policy)) * 100) %>%
  add_row(key = "pt (base, all) [%]", value = nrow(baseTrips %>% filter(main_mode == "pt")) / nrow(baseTrips) * 100) %>%
  add_row(key = "pt,drt,pt_w_drt_used (policy,all) [%]", value = nrow(policyTrips %>% filter(grepl("pt",main_mode, fixed = TRUE) | grepl("drt",main_mode,fixed=TRUE))) / (nrow(policyTrips) + nrow(pr_trips_policy)) * 100) %>%
  add_row(key = "pt_only (policy,all) [%]", value = nrow(policyTrips %>% filter(main_mode == "pt")) / nrow(policyTrips) * 100) %>%
  add_row(key = "PR_used / all trips (policy,all) [%]", value = nrow(policyTrips %>% filter(grepl("+",main_mode,fixed=TRUE))) / nrow(policyTrips) * 100) %>%
  add_row(key = "% of trips impacted [%]", value = nrow(impacted_trips) / nrow(policyTrips) * 100)

########################################
# General results - travelTime of impacted_trips, impacted_binnen_trips, pr_trips

impGrenz_trips$tripType <- "Betr. Quell-/Zielverkehr"
impBinnen_trips$tripType <- "Betr. Binnenverkehr"
impacted_trips$tripType <- "Betr. Verkehr"

boxplot_helper <- rbind(impGrenz_trips,impBinnen_trips,impacted_trips)

"Results table"
tripTypes <- unique(boxplot_helper$tripType)
iterator = 0
results_travTime <- data.frame(tripType = character(), avg_travTime_diff = numeric(), pt95_travTime_diff = numeric(), sd_travTime_diff = numeric())

for (tripType in tripTypes){
  iterator <- iterator + 1
  results_travTime[iterator, ] <- list(tripType, 
                                            mean(boxplot_helper[which(boxplot_helper$tripType == tripType),47]), 
                                            quantile((boxplot_helper[which(boxplot_helper$tripType == tripType),47]), probs = 0.95), 
                                            sd(boxplot_helper[which(boxplot_helper$tripType == tripType),47])
  )
}

boxplot_helper$travTime_diff <- as.numeric(boxplot_helper$travTime_diff)

"Boxplot"
ggplot(boxplot_helper, aes(x = tripType, y = travTime_diff)) +
  geom_boxplot(fill = "#0099f8") +
  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
  labs(
    title = "Verteilung der Reisezeit-Differenzen",
    subtitle = "Betroffene Trips (Maßnahmenfall vs Basisfall)",
    caption = "Reisezeit Δ = Reisezeit (Maßnahmenfall) - Reisezeit (Basisfall)",
    y = "Reisezeit Δ [s]"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
    plot.caption = element_text(face = "italic", size = 20),
    axis.ticks.x = element_blank(),
    axis.text.x = element_text(size = 20),
    axis.title.x = element_blank(),
    axis.title.y = element_text(size = 20),
    axis.text.y = element_text(size = 20)
  )
ggsave(file.path(policyTripsOutputDir,"boxplot_travTime.png"))

##### DEBUGGING
#Tilmann: analysiere die negativen Reisezeit-Differenzen


negativeTravTimeDiff <- impacted_trips %>%  
  filter(travTime_diff < 0) %>% 
  mutate(main_mode = main_mode_policy,
         start_x = start_x_base,
         start_y = start_y_base,
         end_x = end_x_base,
         end_y = end_y_base)

matsim::plot_mainmode_barchart(negativeTravTimeDiff)

negative_car <- negativeTravTimeDiff %>% 
  filter(main_mode_policy == "car") %>% 
  mutate(main_mode = main_mode_base)

matsim::plot_mainmode_barchart(negative_car)
matsim::plot_map_trips(negative_car, crs = 31468, optimized = TRUE)

negative_drt <- negativeTravTimeDiff %>% 
  filter(main_mode_policy == "drt") %>% 
  mutate(main_mode = main_mode_base)

matsim::plot_mainmode_barchart(negative_drt)
matsim::plot_map_trips(negative_drt, crs = 31468, optimized = TRUE)
hist(as.numeric(negative_drt$travTime_diff))
hist(as.numeric(negative_drt$traveledDistance_diff))

negative_walk <- negativeTravTimeDiff %>% 
  filter(main_mode_policy == "walk") %>% 
  mutate(main_mode = main_mode_base)

matsim::plot_mainmode_barchart(negative_walk)
matsim::plot_map_trips(negative_walk, crs = 31468, optimized = TRUE)
hist(negative_walk$travTime_diff)
hist(negative_walk$traveledDistance_diff)

negative_pt_int <- negativeTravTimeDiff %>% 
  filter(main_mode_policy == "pt_w_drt_used") %>% 
  mutate(main_mode = main_mode_base)

matsim::plot_mainmode_barchart(negative_pt_int)
matsim::plot_map_trips(negative_pt_int, crs = 31468, optimized = TRUE)
hist(as.numeric(negative_pt_int$travTime_diff))
hist(negative_pt_int$traveledDistance_diff)


###

nonMatchingStartActs <- negativeTravTimeDiff %>% 
  filter(start_activity_type_policy != start_activity_type_base)

nonMatchingEndActs <- negativeTravTimeDiff %>% 
  filter(end_activity_type_policy != end_activity_type_base)


bb <- autoBase %>% 
  filter(person == "412573301")

pp <- impacted_trips_policy %>% 
  filter(person == "412573301")


########################################
# General results - traveledDistance of impacted_trips, impacted_binnen_trips, pr_trips

"Results table"
iterator = 0
results_travelledDistance <- data.frame(tripType = character(), avg_travelledDistance_diff = numeric(), pt95_travelledDistance_diff = numeric(), sd_travelledDistance_diff = numeric())

for (tripType in tripTypes){
  iterator <- iterator + 1
  results_travelledDistance[iterator, ] <- list(tripType, 
                                       mean(boxplot_helper[which(boxplot_helper$tripType == tripType),49]), 
                                       quantile((boxplot_helper[which(boxplot_helper$tripType == tripType),49]), probs = 0.95), 
                                       sd(boxplot_helper[which(boxplot_helper$tripType == tripType),49])
  )
}

"Boxplot"
ggplot(boxplot_helper, aes(x = tripType, y = traveledDistance_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Verteilung der Reiseweite-Differenzen",
    subtitle = "Betroffene Trips (Maßnahmenfall vs Basisfall)",
    caption = "Reiseweite Δ = Reiseweite (Maßnahmenfall) - Reiseweite (Basisfall)",
    y = "Reiseweite Δ [m]"
  ) +
  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
    plot.caption = element_text(face = "italic", size = 20),
    axis.ticks.x = element_blank(),
    axis.text.x = element_text(size = 20),
    axis.title.x = element_blank(),
    axis.title.y = element_text(size = 20),
    axis.text.y = element_text(size = 20)
  )
ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance.png"))


########################################
# Boxplots & Results - by different criteria

tripCases <- list("Betroffener Quell- und Zielverkehr","Betroffener Binnenverkehr","Betroffener Verkehr")

for (case in tripCases){
  if(case == "Betroffener Quell- und Zielverkehr"){
    caseTrips <- impGrenz_trips
  }
  if(case == "Betroffener Binnenverkehr"){
    caseTrips <- impBinnen_trips
  }
  if(case == "Betroffener Verkehr"){
    caseTrips <- impacted_trips
  }
  
  policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips/",case)
  dir.create(policyTripsOutputDir, showWarnings = FALSE)
  
  ########################################
  # Results by mainMode
  
  "Boxplot - Q/Z-Trips by transport mode (travTime)"
  ggplot(caseTrips, aes(x = reorder(main_mode_policy,travTime_diff,median), y = travTime_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Verteilung der Reisezeit-Differenzen",
      subtitle = paste0("nach Verkehrsmittel (Maßnahmenfall vs Basisfall - ", case ,")"),
      caption = "Reisezeit Δ = Reisezeit (Maßnahmenfall) - Reisezeit (Basisfall)",
      y = "Reisezeit Δ [s]",
      x = "Verkehrsmittel"
    ) +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
      plot.caption = element_text(face = "italic", size = 20),
      axis.ticks.x = element_blank(),
      axis.text.x = element_text(size = 20),
      axis.title.x = element_blank(),
      axis.title.y = element_text(size = 20),
      axis.text.y = element_text(size = 20)
    )
  ggsave(file.path(policyTripsOutputDir,"boxplot_travTime_mainMode.png"))
  
  "Boxplot - Q/Z-Trips by transport mode (travelledDistance)"
  ggplot(caseTrips, aes(x = reorder(main_mode_policy,traveledDistance_diff,median), y = traveledDistance_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Verteilung der Reiseweite-Differenzen",
      subtitle = paste0("nach Verkehrsmittel (Maßnahmenfall vs Basisfall - ", case ,")"),
      caption = "Reiseweite Δ = Reiseweite (Maßnahmenfall) - Reiseweite (Basisfall)",
      y = "Reiseweite Δ [m]",
      x = "Verkehrsmittel"
    ) +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
      plot.caption = element_text(face = "italic", size = 20),
      axis.ticks.x = element_blank(),
      axis.text.x = element_text(size = 20),
      axis.title.x = element_blank(),
      axis.title.y = element_text(size = 20),
      axis.text.y = element_text(size = 20)
    )
  ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance_mainMode.png"))
  
  ########################################
  # by PR Station (travTime, travelledDistance)
  
  "Boxplot"
  ggplot(caseTrips, aes(x = reorder(prStation, travTime_diff, median), y = travTime_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Verteilung der Reisezeit-Differenzen",
      subtitle = paste0("nach P+R-Station (Maßnahmenfall vs Basisfall - ", case ,")"),
      caption = "Reisezeit Δ = Reisezeit (Maßnahmenfall) - Reisezeit (Basisfall)",
      y = "Reisezeit Δ [s]"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5, size = 20),
      plot.caption = element_text(face = "italic", size = 20),
      axis.ticks.x = element_blank(),
      axis.title.x = element_blank(),
      axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1, size = 25),
      axis.text.y = element_text(size = 20),
      axis.title.y = element_text(size = 20)
    )
  ggsave(file.path(policyTripsOutputDir,"boxplot_travTime_PRStation.png"))
  
  
  "Boxplot"
  ggplot(caseTrips, aes(x = reorder(prStation, traveledDistance_diff, median), y = traveledDistance_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = "Verteilung der Reiseweite-Differenzen",
      subtitle = paste0("nach P+R-Station (Maßnahmenfall vs Basisfall - ", case ,")"),
      caption = "Reiseweite Δ = Reiseweite (Maßnahmenfall) - Reiseweite (Basisfall)",
      y = "Reiseweite Δ [m]"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5, size = 20),
      plot.caption = element_text(face = "italic", size = 20),
      axis.ticks.x = element_blank(),
      axis.title.x = element_blank(),
      axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1, size = 25),
      axis.text.y = element_text(size = 20),
      axis.title.y = element_text(size = 20)
    )
  ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance_PRStation.png"))
  
}

########################################
# Boxplots & Results - by hasPRStation, Note: only looking at border crossing trips
# Note: Somehow these boxplots do not work when run through a Java command. That´s why it´s not used here.


prPersons <- personsJoined %>% filter(personsJoined$hasPRActivity_policy == "true")
otherPersons <- personsJoined %>% filter(personsJoined$hasPRActivity_policy == "false")
impGrenzPR_trips <- impGrenz_trips %>% filter(impGrenz_trips$person_policy %in% prPersons$person) %>%
  add_column(hasPRStation = "true")
impGrenzOther_trips <- impGrenz_trips %>% filter(impGrenz_trips$person_policy %in% otherPersons$person) %>%
  add_column(hasPRStation = "false")
boxplot_helper2 <- rbind(impGrenzPR_trips, impGrenzOther_trips)

# "Boxplot"
# means1 <- aggregate(travTime_diff ~ hasPRStation, boxplot_helper2, mean)
# ggplot(boxplot_helper2, aes(x = hasPRStation, y = travTime_diff)) +
#  geom_boxplot(fill = "#0099f8") +
#  # stat_summary(fun = "mean", geom = "crossbar", aes(group = 1), colour = "red") +
#  # geom_text(data = means1, aes(label = travTime_diff, y = travTime_diff + 2)) +
#  labs(
#    title = "Verteilung der Reisezeit-Differenzen",
#    subtitle = "Betroffene Grenztrips (Maßnahmenfall vs Basisfall)",
#    caption = "Reisezeit Δ = Reisezeit (Maßnahmenfall) - Reisezeit (Basisfall)",
#    y = "Reisezeit Δ [s]"
#  ) +
#  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
#  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
#  theme_classic() +
#  theme(
#    plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
#    plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
#    plot.caption = element_text(face = "italic", size = 20),
#    axis.ticks.x = element_blank(),
#    axis.text.x = element_text(size = 20),
#    axis.title.x = element_blank(),
#    axis.title.y = element_text(size = 20),
#    axis.text.y = element_text(size = 20)
#  )
# ggsave(file.path(policyTripsOutputDir,"boxplot_travTime_hasPRStation.png"))


# "Boxplot"
# means2 <- aggregate(traveledDistance_diff ~ hasPRStation, boxplot_helper2, mean)
# ggplot(boxplot_helper2, aes(x = hasPRStation, y = traveledDistance_diff)) +
#   geom_boxplot(fill = "#0099f8") +
#   geom_text(data = means2, aes(label = traveledDistance_diff, y = traveledDistance_diff + 2)) +
#   labs(
#     title = "Verteilung der Reiseweite-Differenzen",
#     subtitle = "Betroffene Grenztrips (Maßnahmenfall vs Basisfall)",
#     caption = "Reiseweite Δ = Reiseweite (Maßnahmenfall) - Reiseweite (Basisfall)",
#     y = "Reiseweite Δ [m]"
#   ) +
#   stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
#   stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
#   theme_classic() +
#   theme(
#     plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
#     plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
#     plot.caption = element_text(face = "italic", size = 20),
#     axis.ticks.x = element_blank(),
#     axis.text.x = element_text(size = 20),
#     axis.title.x = element_blank(),
#     axis.title.y = element_text(size = 20),
#     axis.text.y = element_text(size = 20)
#   )
# ggsave(file.path(policyTripsOutputDir,"boxplot_travelledDistance_hasPRStation.png"))


########################################
# Quantifying problems made by filterByRegion, this only works without extraPtPlan = true though

# Merging all trips
allTrips <- merge(policyTrips, baseTrips, by = "trip_id", suffixes = c("_policy","_base"))
pr_trips <- merge(pr_trips_policy, pr_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
pr_trips <- pr_trips %>% 
  add_column(travTime_diff = pr_trips$trav_time_policy - pr_trips$trav_time_base) %>%
  add_column(waitTime_diff = pr_trips$wait_time_policy - pr_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = pr_trips$traveled_distance_policy - pr_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = pr_trips$euclidean_distance_policy - pr_trips$euclidean_distance_base)

# Case 1: Falsely classified as non-impacted Trip -> has PR Trip, but no Grenztrip? (only works without extraPtPlan)
non_impacted_trips <- allTrips %>% filter(!trip_id %in% impacted_trips$trip_id)
lupe <- pr_trips %>% filter(pr_trips$trip_id %in% non_impacted_trips$trip_id)

# Case 2: Falsely classified as impacted Trip -> no PR Trip, but is Grenztrip? (only works without extraPtPlan)
not_pr_trips <- allTrips %>% filter(!trip_id %in% pr_trips$trip_id)
lupe2 <- not_pr_trips %>% filter(trip_id %in% impGrenz_trips$trip_id) %>%
  filter(!main_mode_policy == "pt") %>%
  filter(!main_mode_policy == "pt_w_drt_used") %>%
  filter(!main_mode_policy == "walk") 

results_falselyClassified <- data.frame(falselyAsNotImpacted = numeric(), falselyAsImpacted = numeric())
results_falselyClassified[1,] <- list(nrow(lupe),nrow(lupe2))

########################################
# Dump result tables
policyTripsOutputDir <- paste0(policyCaseDirectory,"/analysis/trips")

write.table(results_travTime,file.path(policyTripsOutputDir,"trips_travTime.tsv"),row.names = FALSE, sep = "\t")
write.table(results_travelledDistance,file.path(policyTripsOutputDir,"trips_travelledDistance.tsv"),row.names = FALSE, sep = "\t")
write.table(results_falselyClassified,file.path(policyTripsOutputDir,"trips_falselyClassified.tsv"),row.names = FALSE, sep = "\t")

write.table(results_modalSplitAffected,file.path(policyTripsOutputDir,"modalSplit_affectedTrips.tsv"),row.names = FALSE, sep = "\t")
write.table(results_modalSplitAll,file.path(policyTripsOutputDir,"modalSplit_all.tsv"),row.names = FALSE, sep = "\t")


