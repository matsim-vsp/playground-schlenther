library(matsim)
library(tidyverse)
library(dplyr)
library(ggalluvial)
library(lubridate)
library(plotly)
library(sf)
library(hms)

########################################
# Preparation
# Open questions: Sollen Ausreißer generell vor der Analyse herausgefiltert werden?

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-05-26/baseCaseContinued"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/"
#policyCaseDirectory <- commandArgs(trailingOnly = TRUE)
base_filename <- "berlin-v5.5-1pct.output_plans_selectedPlanScores.tsv"
policy_filename <- "output_plans_selectedPlanScores.tsv"
base_inputfile <- file.path(baseCaseDirectory, base_filename)
policy_inputfile <- file.path(policyCaseDirectory, policy_filename)

basePersons <- read.table(file = base_inputfile, sep = '\t', header = TRUE)
policyPersons <- read.table(file = policy_inputfile, sep = '\t', header = TRUE)

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base"))
personsJoined <- personsJoined %>%
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)

personsJoined <- personsJoined %>% filter(score_diff > -400)

########################################
# Prepare folders

dir.create(paste0(policyCaseDirectory,"/analysis"))
dir.create(paste0(policyCaseDirectory,"/analysis/score"))

########################################
# Prepare impacted trips (for the next cases)
baseTrips <- readTripsTable(baseCaseDirectory)
policy_trips_filename <- "output_trips_prepared.tsv"
policy_inputfile <- file.path(policyCaseDirectory, policy_trips_filename)

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
  add_column(euclideanDistance_diff = impGrenz_trips$euclidean_distance_policy - impGrenz_trips$euclidean_distance_base)

"Impacted Binnentrips"
impBinnen_trips_base <- autoBase %>% filterByRegion(., shp, crs = 31468, TRUE, TRUE)
impBinnen_trips_policy <- policyTrips %>% filter(trip_id %in% impBinnen_trips_base$trip_id)

impBinnen_trips <- merge(impBinnen_trips_policy, impBinnen_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impBinnen_trips <- impBinnen_trips %>% 
  add_column(travTime_diff = impBinnen_trips$trav_time_policy - impBinnen_trips$trav_time_base) %>%
  add_column(waitTime_diff = impBinnen_trips$wait_time_policy - impBinnen_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impBinnen_trips$traveled_distance_policy - impBinnen_trips$traveled_distance_base) %>%
  add_column(euclideanDistance_diff = impBinnen_trips$euclidean_distance_policy - impBinnen_trips$euclidean_distance_base)

"Impacted trips (Impacted Grenztrips + Impacted Binnentrips)"
impacted_trips_base <- rbind(impGrenz_trips_base,impBinnen_trips_base)
impacted_trips_policy <- rbind(impGrenz_trips_policy,impBinnen_trips_policy)

impacted_trips <- merge(impacted_trips_policy, impacted_trips_base, by = "trip_id", suffixes = c("_policy","_base"))
impacted_trips <- impacted_trips %>% 
  add_column(travTime_diff = impacted_trips$trav_time_policy - impacted_trips$trav_time_base) %>%
  add_column(waitTime_diff = impacted_trips$wait_time_policy - impacted_trips$wait_time_base) %>%
  add_column(traveledDistance_diff = impacted_trips$traveled_distance_policy - impacted_trips$traveled_distance_base)  %>%
  add_column(euclideanDistance_diff = impacted_trips$euclidean_distance_policy - impacted_trips$euclidean_distance_base)


########################################
# Prepare impacted vs non-impacted agents

cases <- list("allPersons","impactedPersons","nonImpactedPersons")

allPersons <- personsJoined
impactedPersons <- personsJoined %>% filter(person %in% impacted_trips$person_policy)
nonImpactedPersons <- personsJoined %>% filter(!person %in% impactedPersons$person)

########################################
# TODO: didAgentUseDRT

########################################
# Boxplots & Results

for (case in cases){
  if(case == "allPersons"){
    casePersons <- allPersons
  }
  if(case == "impactedPersons"){
    casePersons <- impactedPersons
  }
  if(case == "nonImpactedPersons"){
    casePersons <- nonImpactedPersons
  }
  
  policyCaseOutputDir <- paste0(policyCaseDirectory,"/analysis/score/",case)
  dir.create(policyCaseOutputDir, showWarnings = FALSE)
  
  ########################################
  # General results
  
  "General metrics"
  mean(casePersons$score_diff)
  
  "Results table"
  results_general <- data.frame(avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  results_general[1,] <- list(mean(casePersons$score_diff), quantile(casePersons$score_diff, probs = 0.05),sd(casePersons$score_diff))
  
  "Boxplot"
  ggplot(casePersons, aes(y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "General results (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_general.png"))
  
  ########################################
  # Results by hasPRActivity
  
  mean(casePersons$score_diff[casePersons$hasPRActivity_policy == "true"])
  
  hasPRActivityCategories <- unique(casePersons$hasPRActivity_policy)
  results_hasPRActivity <- data.frame(hasPRActivity = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table + Histograms"
  for (entry in hasPRActivityCategories){
    iterator <- iterator + 1
    results_hasPRActivity[iterator, ] <- list(entry, 
                                              mean(casePersons[which(casePersons$hasPRActivity_policy == entry),20]), 
                                              quantile((casePersons[which(casePersons$hasPRActivity_policy == entry),20]), probs = 0.05), 
                                              sd(casePersons[which(casePersons$hasPRActivity_policy == entry),20])
    )
    relevant_persons <- casePersons %>%
      filter(hasPRActivity_policy == entry)
    
    ggplot(relevant_persons, aes(x = score_diff)) +
      geom_histogram(binwidth = 5) +
      labs(
        title = paste0("Distribution of score differences (",case,")"),
        subtitle = paste("hasPRActivity =",entry, "(policy vs base)"),
        caption = "score_delta = score(policy) - score(base)",
        x = "score_delta"
      ) +
      theme_classic() +
      theme(
        plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
        plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
        plot.caption = element_text(face = "italic")
      )
    ggsave(file.path(policyCaseOutputDir,paste0("histogram_hasPRActivity_",entry,".png")))
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = hasPRActivity_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by hasPRActivity (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic"),
      axis.title.x = element_blank()
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_hasPRActivity.png"))
  
  
  ########################################
  # Results by homeActivityZone
  
  homeActivityZoneCategories <- unique(casePersons$home.activity.zone_policy)
  results_homeActivityZone <- data.frame( homeActivityZone = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table + Histograms"
  for (entry in homeActivityZoneCategories){
    iterator <- iterator + 1
    results_homeActivityZone[iterator, ] <- list(entry, 
                                                 mean(casePersons[which(casePersons$home.activity.zone_policy == entry),20]), 
                                                 quantile((casePersons[which(casePersons$home.activity.zone_policy == entry),20]), probs = 0.05), 
                                                 sd(casePersons[which(casePersons$home.activity.zone_policy == entry),20])
    )
    
    relevant_persons <- casePersons %>%
      filter(home.activity.zone_policy == entry)
    
    ggplot(relevant_persons, aes(x = score_diff)) +
      geom_histogram(binwidth = 5) +
      labs(
        title = paste0("Distribution of score differences (",case,")"),
        subtitle = paste("homeActivityZone =",entry, "(policy vs base)"),
        caption = "score_delta = score(policy) - score(base)",
        x = "score_delta"
      ) +
      theme_classic() +
      theme(
        plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
        plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
        plot.caption = element_text(face = "italic")
      )
    ggsave(file.path(policyCaseOutputDir,paste0("histogram_homeActivityZone_",entry,".png")))
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = home.activity.zone_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by homeActivityZone (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic"),
      axis.title.x = element_blank()
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_homeActivityZone.png"))
  
  ########################################
  # Results by noOfActivities
  
  noOfActivitiesCategories <- unique(casePersons$noOfActivities_policy)
  results_noOfActivities <- data.frame(noOfActivities = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in noOfActivitiesCategories){
    iterator <- iterator + 1
    results_noOfActivities[iterator, ] <- list(entry, 
                                               mean(casePersons[which(casePersons$noOfActivities_policy == entry),20]), 
                                               quantile((casePersons[which(casePersons$noOfActivities_policy == entry),20]), probs = 0.05), 
                                               sd(casePersons[which(casePersons$noOfActivities_policy == entry),20])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = noOfActivities_policy, group = noOfActivities_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by noOfActivities (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic")
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_noOfActivities.png"))
  
  ########################################
  # Results by travelledDistance
  
  "Boxplot"
  options(scipen = 999)
  casePersons_bins <- casePersons %>%
    mutate(bin = cut_width(travelledDistance_policy, width = 25000, boundary = 0, dig.lab = 50))
  
  ggplot(casePersons_bins, aes(x = bin, group = bin, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by travelledDistance (policy)",
      caption = "score_delta = score(policy) - score(base)",
      x = "travelledDistance_policy",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic")
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_scoreByTravelledDistance.png"))
  
  
  ########################################
  # Results by mainMode [without "", "freight"]
  
  mm_helper <- unique(casePersons$mainMode_policy)
  remove <- c("","freight")
  mainModeCategories <- mm_helper [! mm_helper %in% remove]
  results_mainMode <- data.frame(mainMode = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table + Histograms"
  for (entry in mainModeCategories){
    iterator <- iterator + 1
    results_mainMode[iterator, ] <- list(entry, 
                                         mean(casePersons[which(casePersons$mainMode_policy == entry),20]), 
                                         quantile((casePersons[which(casePersons$mainMode_policy == entry),20]), probs = 0.05), 
                                         sd(casePersons[which(casePersons$mainMode_policy == entry),20])
    )
    relevant_persons <- casePersons %>%
      filter(mainMode_policy == entry)
    
    ggplot(relevant_persons, aes(x = score_diff)) +
      geom_histogram(binwidth = 5) +
      labs(
        title = paste0("Distribution of score differences (",case,")"),
        subtitle = paste("mainMode =",entry, "(policy vs base)"),
        caption = "score_delta = score(policy) - score(base)",
        x = "score_delta"
      ) +
      theme_classic() +
      theme(
        plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
        plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
        plot.caption = element_text(face = "italic")
      )
    ggsave(file.path(policyCaseOutputDir,paste0("histogram_mainMode_",entry,".png")))
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = mainMode_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by mainMode (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic"),
      axis.title.x = element_blank()
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_mainMode.png"))
  
  ########################################
  # Results by Last PR Station
  
  onlyPR <- casePersons %>% filter(!casePersons$LastPRStation_policy == "")
  
  "Boxplot"
  ggplot(onlyPR, aes(x = reorder(LastPRStation_policy, score_diff, median), y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Distribution of score differences (",case,")"),
      subtitle = "by LastPRStation (policy vs base)",
      caption = "score_delta = score(policy) - score(base)",
      y = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic"),
      axis.title.x = element_blank(),
      axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1)
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_lastPRStation.png"))
  
  ########################################
  # Dump results tables
  
  write.table(results_homeActivityZone,file.path(policyCaseOutputDir,"score_homeActivityZone.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_mainMode,file.path(policyCaseOutputDir,"score_mainMode.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_noOfActivities,file.path(policyCaseOutputDir,"score_noOfActivities.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_hasPRActivity,file.path(policyCaseOutputDir,"score_hasPRActivity.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_general,file.path(policyCaseOutputDir,"score_general.tsv") ,row.names = FALSE, sep = "\t")
}


