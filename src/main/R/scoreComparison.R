library(tidyr)
library(tidyverse)
library(lubridate)
library(plotly)
library(hms)
library(readr)
library(sf)
library(dplyr)
library(matsim)

"In this script, the score of base and policy case gets compared. Several tsv-files & graphs are written as output results."

########################################
# Preparation

#HPC Cluster
 args <- commandArgs(trailingOnly = TRUE)
 policyCaseDirectory <- args[1]
 baseCaseDirectory <- args[3]
 shp <- st_read(args[5])

#10pct
#baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued-10pct/"
#policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/noDRT/"

#1pct
# baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCaseContinued/"
# #policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/"
# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/1pct/optimum-flowCapacity/"

#shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

basePersons <- read.table(file = file.path(baseCaseDirectory, "output_plans_selectedPlanScores.tsv"), sep = '\t', header = TRUE)
policyPersons <- read.table(file = file.path(policyCaseDirectory, "output_plans_selectedPlanScores.tsv"), sep = '\t', header = TRUE)

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base"))
personsJoined <- personsJoined %>%
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)
personsJoined <- personsJoined %>% filter(score_diff > -400)

########################################
# Prepare folders

dir.create(paste0(policyCaseDirectory,"/analysis"))
dir.create(paste0(policyCaseDirectory,"/analysis/score"))

########################################
# Prepare basic trips

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

########################################
# Score of car / non-car users

autoBase2 <- baseTrips %>% filter(grepl("car",main_mode,fixed=TRUE))
autoPolicy <- policyTrips %>% filter(grepl("car",main_mode,fixed=TRUE))

# Car users before and after
autonutzerBase <- personsJoined %>% filter(person %in% autoBase2$person)
autonutzerPolicy <- personsJoined %>% filter(person %in% autoPolicy$person)

# by home location
autonutzerBaseZone <- autonutzerBase %>% filter(home.activity.zone_base == "innerCity")
autonutzerPolicyZone <- autonutzerPolicy %>% filter(home.activity.zone_policy == "innerCity")
autonutzerBaseOuterBerlin <- autonutzerBase %>% filter(home.activity.zone_base == "BerlinButNotInnerCity")
autonutzerPolicyOuterBerlin <- autonutzerPolicy %>% filter(home.activity.zone_policy == "BerlinButNotInnerCity")
autonutzerBaseBrandenburg <- autonutzerBase %>% filter(home.activity.zone_base == "Brandenburg")
autonutzerPolicyBrandenburg <- autonutzerPolicy %>% filter(home.activity.zone_policy == "Brandenburg")

results_carUsers <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Änderung Autonutzer (%)", value = (nrow(autonutzerBase) - nrow(autonutzerPolicy)) / nrow(autonutzerBase)) %>%
  add_row(key = "Änderung Autonutzer Verbotszone (%)", value = (nrow(autonutzerBaseZone) - nrow(autonutzerPolicyZone)) / nrow(autonutzerBaseZone)) %>%
  add_row(key = "Änderung Autonutzer restl. Berlin (%)", value = (nrow(autonutzerBaseOuterBerlin) - nrow(autonutzerPolicyOuterBerlin)) / nrow(autonutzerBaseOuterBerlin)) %>%
  add_row(key = "Änderung Autonutzer Brandenburg (%)", value = (nrow(autonutzerBaseBrandenburg) - nrow(autonutzerPolicyBrandenburg)) / nrow(autonutzerBaseBrandenburg))

########################################
# Prepare impacted trips (for the next cases)

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

"All impacted trips (Impacted Grenztrips + Impacted Binnentrips)"
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

cases <- list("allePersonen","betroffenePersonen","nichtBetroffenePersonen")
allePersonen <- personsJoined
betroffenePersonen <- personsJoined %>% filter(person %in% impacted_trips$person_policy)
nichtBetroffenePersonen <- personsJoined %>% filter(!person %in% betroffenePersonen$person)

########################################
# Boxplots & Results - by different criteria

for (case in cases){
  if(case == "allePersonen"){
    casePersons <- allePersonen
  }
  if(case == "betroffenePersonen"){
    casePersons <- betroffenePersonen
  }
  if(case == "nichtBetroffenePersonen"){
    casePersons <- nichtBetroffenePersonen
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
  ggplot(casePersons, aes(x = 1, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "Allgemeine Ergebnisse (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 40, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", size = 20, hjust = 0.5),
      plot.caption = element_text(face = "italic", size = 20),
      axis.ticks.x = element_blank(),
      axis.text.x = element_blank(),
      axis.title.x = element_blank(),
      axis.title.y = element_text(size = 20),
      axis.text.y = element_text(size = 20)
    )
  ggsave(file.path(policyCaseOutputDir,"boxplot_general.png"))
  
  ########################################
  # Results by hasPRActivity
  "Test 3"
  
  mean(casePersons$score_diff[casePersons$hasPRActivity_policy == "true"])
  
  hasPRActivityCategories <- unique(casePersons$hasPRActivity_policy)
  results_hasPRActivity <- data.frame(hasPRActivity = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in hasPRActivityCategories){
    iterator <- iterator + 1
    results_hasPRActivity[iterator, ] <- list(entry, 
                                              mean(casePersons[which(casePersons$hasPRActivity_policy == entry),24]), 
                                              quantile((casePersons[which(casePersons$hasPRActivity_policy == entry),24]), probs = 0.05), 
                                              sd(casePersons[which(casePersons$hasPRActivity_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = hasPRActivity_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "Agent nutzt mind. 1 P+R-Station (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_hasPRActivity.png"))
  
  ########################################
  # Results by livesInsideBoundaryZone_policy
  
  mean(casePersons$score_diff[casePersons$livesInsideBoundaryZone_policy == "true"])
  
  livesInsideBoundaryZone_policyCategories <- unique(casePersons$livesInsideBoundaryZone_policy)
  results_livesInsideBoundaryZone_policy <- data.frame(livesInsideBoundaryZone_policy = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in livesInsideBoundaryZone_policyCategories){
    iterator <- iterator + 1
    results_livesInsideBoundaryZone_policy[iterator, ] <- list(entry, 
                                              mean(casePersons[which(casePersons$livesInsideBoundaryZone_policy == entry),24]), 
                                              quantile((casePersons[which(casePersons$livesInsideBoundaryZone_policy == entry),24]), probs = 0.05), 
                                              sd(casePersons[which(casePersons$livesInsideBoundaryZone_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = livesInsideBoundaryZone_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "lebt max. 500m außerhalb der Verbotszone (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_livesInsideBoundaryZone_policy.png"))
  
  ########################################
  # Results by isCarUser_policy
  
  mean(casePersons$score_diff[casePersons$isCarUser_policy == "true"])
  
  isCarUser_policyCategories <- unique(casePersons$isCarUser_policy)
  results_isCarUser_policy <- data.frame(isCarUser_policy = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in isCarUser_policyCategories){
    iterator <- iterator + 1
    results_isCarUser_policy[iterator, ] <- list(entry, 
                                                        mean(casePersons[which(casePersons$isCarUser_policy == entry),24]), 
                                                        quantile((casePersons[which(casePersons$isCarUser_policy == entry),24]), probs = 0.05), 
                                                        sd(casePersons[which(casePersons$isCarUser_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = isCarUser_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nutzt privaten Pkw (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_isCarUser_policy.png"))
  
  ########################################
  # Results by homeActivityZone
  
  homeActivityZoneCategories <- unique(casePersons$home.activity.zone_policy)
  results_homeActivityZone <- data.frame( homeActivityZone = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in homeActivityZoneCategories){
    iterator <- iterator + 1
    results_homeActivityZone[iterator, ] <- list(entry, 
                                                 mean(casePersons[which(casePersons$home.activity.zone_policy == entry),24]), 
                                                 quantile((casePersons[which(casePersons$home.activity.zone_policy == entry),24]), probs = 0.05), 
                                                 sd(casePersons[which(casePersons$home.activity.zone_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = home.activity.zone_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
    stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nach Wohnort (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
                                               mean(casePersons[which(casePersons$noOfActivities_policy == entry),24]), 
                                               quantile((casePersons[which(casePersons$noOfActivities_policy == entry),24]), probs = 0.05), 
                                               sd(casePersons[which(casePersons$noOfActivities_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = noOfActivities_policy, group = noOfActivities_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nach Anzahl an Aktivitäten (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nach Reiseweite (Maßnahmenfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      x = "travelledDistance_policy",
      y = "Score Δ"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_scoreByTravelledDistance.png"))
  
  
  ########################################
  # Results by mainMode
  
  mm_helper <- unique(casePersons$mainMode_policy)
  remove <- c("","freight")
  mainModeCategories <- mm_helper [! mm_helper %in% remove]
  results_mainMode <- data.frame(mainMode = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
  iterator = 0
  
  "Results table"
  for (entry in mainModeCategories){
    iterator <- iterator + 1
    results_mainMode[iterator, ] <- list(entry, 
                                         mean(casePersons[which(casePersons$mainMode_policy == entry),24]), 
                                         quantile((casePersons[which(casePersons$mainMode_policy == entry),24]), probs = 0.05), 
                                         sd(casePersons[which(casePersons$mainMode_policy == entry),24])
    )
  }
  
  
  "Boxplot"
  ggplot(casePersons, aes(x = mainMode_policy, y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nach Hauptverkehrsmittel (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
  ggsave(file.path(policyCaseOutputDir,"boxplot_mainMode.png"))
  
  ########################################
  # Results by last PR Station
  
  onlyPR <- casePersons %>% filter(!casePersons$LastPRStation_policy == "")
  
  "Boxplot"
  ggplot(onlyPR, aes(x = reorder(LastPRStation_policy, score_diff, median), y = score_diff)) +
    geom_boxplot(fill = "#0099f8") +
    labs(
      title = paste0("Verteilung der Score-Differenzen (",case,")"),
      subtitle = "nach zuletzt genutzter P+R-Station (Maßnahmenfall vs Basisfall)",
      caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
      y = "Score Δ"
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
  write.table(results_livesInsideBoundaryZone_policy,file.path(policyCaseOutputDir,"score_livesInsideBoundaryZone_policy.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_isCarUser_policy,file.path(policyCaseOutputDir,"score_isCarUser_policy.tsv"),row.names = FALSE, sep = "\t")
  write.table(results_general,file.path(policyCaseOutputDir,"score_general.tsv") ,row.names = FALSE, sep = "\t")
}

########################################
# Backup: Quantify problems made by filterByRegion

# # 14 von 494.104 Agenten, die fälschlicherweise in nichtBetroffenePersonen gelandet sind -> Problem: filterByRegion (again) 
# lupe3 <- nichtBetroffenePersonen %>%
#   filter(hasPRActivity_policy == "true")
# 
# # 16 von 494.104 Agenten, die fälschlicherweise in nichtBetroffenePersonen gelandet sind -> Problem: filterByRegion (again)
# lupe4 <- nichtBetroffenePersonen %>%
#   filter(home.activity.zone_policy == "innerCity") %>%
#   filter(mainMode_base == "ride" | mainMode_base == "car")
# 
# lupe5 <- nichtBetroffenePersonen %>%
#   filter(home.activity.zone_policy == "innerCity")

########################################
# Looking at especially bad losses 

betroffenePersonen_noCarUser <- betroffenePersonen %>%
  filter(isCarUser_policy == "false")
betroffenePersonen_carUser <- betroffenePersonen %>%
  filter(isCarUser_policy == "true")
betroffenePersonen_tryout <- betroffenePersonen %>%
  filter(isCarUser_policy == "true") %>%
  filter(noOfActivities_policy > 8)

worstPR <- betroffenePersonen %>% filter(LastPRStation_policy == "Halensee")
bestPR <- betroffenePersonen %>% filter(LastPRStation_policy == "Tempelhof")

mean(worstPR$score_diff)
mean(bestPR$score_diff)

ggplot(betroffenePersonen_noCarUser, aes(x = home.activity.zone_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
  labs(
    title = paste0("Verteilung der Score-Differenzen (Betroffene Nicht-Autonutzer)"),
    subtitle = "nach Wohnort (Maßnahmenfall vs Basisfall)",
    caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
    y = "Score Δ"
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
ggsave(file.path(paste0(policyCaseDirectory,"/analysis/score/"),"boxplot_betr_noCarUser_byHomeZone.png"))

ggplot(betroffenePersonen_carUser, aes(x = home.activity.zone_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
  labs(
    title = paste0("Verteilung der Score-Differenzen (Betroffene Autonutzer)"),
    subtitle = "nach Wohnort (Maßnahmenfall vs Basisfall)",
    caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
    y = "Score Δ"
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
ggsave(file.path(paste0(policyCaseDirectory,"/analysis/score/"),"boxplot_betr_carUser_byHomeZone.png"))

########################################
# Looking at other metrics (total economic loss, percentage of impacted agents)

(sum(policyPersons$executed_score) - sum(basePersons$executed_score)) / sum(basePersons$executed_score) * 100

results_otherScoreMetrics <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "Anteil betroffener Agenten (%)", value = nrow(betroffenePersonen) / nrow(allePersonen) * 100) %>%
  add_row(key = "Ökonomischer Nutzen [€]", value = mean(personsJoined$score_diff) * 1 / 0.6 * nrow(personsJoined) * 10) %>%
  add_row(key = "Veränderung ökonomischer Nutzen [%]", value = (sum(policyPersons$executed_score) - sum(basePersons$executed_score)) / sum(basePersons$executed_score) * 100)


policyCaseOutputDir <- paste0(policyCaseDirectory,"/analysis/score/")
ggplot(betroffenePersonen, aes(x = home.activity.zone_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  stat_summary(fun = mean, geom = "text", aes(label = round(after_stat(y),2)), size = 8, vjust = -1.0, hjust = 1.1) +
  stat_summary(fun = mean, geom = "point", color = "red", size = 3) +
  labs(
    title = paste0("Verteilung der Score-Differenzen (betroffene Personen)"),
    subtitle = "nach Wohnort (Maßnahmenfall vs Basisfall)",
    caption = "Score Δ = Score(Maßnahmenfall) - Score(Basisfall)",
    y = "Score Δ"
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
ggsave(file.path(policyCaseOutputDir,"boxplot_homeActivityZone.png"))

########################################
# Dump tables

write.table(results_carUsers,file.path(policyCaseDirectory,"/analysis/score/amount_carUsers.tsv") ,row.names = FALSE, sep = "\t")
write.table(results_otherScoreMetrics,file.path(policyCaseDirectory, "/analysis/score/score_otherMetrics.tsv") ,row.names = FALSE, sep = "\t")