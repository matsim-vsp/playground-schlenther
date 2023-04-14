library(dplyr)
library(tidyverse)
library(lubridate)
library(ggplot2)
library(tibble)

# Preparation

scoresNoCars <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutsideActivity/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoresBaseCase <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoreComparison <- merge(scoresNoCars, scoresBaseCase, by = "agentId", suffixes = c("",".baseCase"))
scoreComparison <- merge(scoresNoCars, scoresBaseCase, by = "agentId", suffixes = c("",".baseCase")) %>% 
  add_column(comparedScore = scoreComparison$selectedPlanScore - scoreComparison$selectedPlanScore.baseCase)

scoreComparison <- filter(scoreComparison, comparedScore > -350)

# Writing results tables

## General comparison

results_general <- data.frame(total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric(), sd_cS = numeric())
results_general[1,] <- list(sum(scoreComparison[,16]),mean(scoreComparison[,16]),quantile(scoreComparison[,16], probs = 0.05),sd(scoreComparison[,16]))

## By living location

livingLocations <- unique(scoreComparison$livingLocation)
results_livingLocation <- data.frame(livingLocation = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in livingLocations){
  iterator <- iterator + 1
  results_livingLocation[iterator, ] <- list(entry, 
                                      sum(scoreComparison[which(scoreComparison$livingLocation == entry),16]), 
                                      mean(scoreComparison[which(scoreComparison$livingLocation == entry),16]), 
                                      quantile((scoreComparison[which(scoreComparison$livingLocation == entry),16]), probs = 0.05))
}

str(scoreComparison)

## By income

incomeCategories <- unique(scoreComparison$income)
results_income <- data.frame(income = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in incomeCategories){
  iterator <- iterator + 1
  results_income[iterator, ] <- list(entry, 
                                             sum(scoreComparison[which(scoreComparison$income == entry),16]), 
                                             mean(scoreComparison[which(scoreComparison$income == entry),16]), 
                                             quantile((scoreComparison[which(scoreComparison$income == entry),16]), probs = 0.05))
}

## By longest Distance Mode
longestDistanceModeCategories <- unique(scoreComparison$longestDistanceMode)
results_longestDistanceMode <- data.frame(longestDistanceMode = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in longestDistanceModeCategories){
  iterator <- iterator + 1
  results_longestDistanceMode[iterator, ] <- list(entry, 
                                     sum(scoreComparison[which(scoreComparison$longestDistanceMode == entry),16]), 
                                     mean(scoreComparison[which(scoreComparison$longestDistanceMode == entry),16]), 
                                     quantile((scoreComparison[which(scoreComparison$longestDistanceMode == entry),16]), probs = 0.05))
}

## By amount of activities 

noOfActivitiesCategories <- unique(scoreComparison$noOfActivities)
results_noOfActivities <- data.frame(noOfActivities = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in noOfActivitiesCategories){
  iterator <- iterator + 1
  results_noOfActivities[iterator, ] <- list(entry, 
                                     sum(scoreComparison[which(scoreComparison$noOfActivities == entry),16]), 
                                     mean(scoreComparison[which(scoreComparison$noOfActivities == entry),16]), 
                                     quantile((scoreComparison[which(scoreComparison$noOfActivities == entry),16]), probs = 0.05))
}

## By hasPRActivity

hasPRActivityCategories <- unique(scoreComparison$hasPRActivity)
results_hasPRActivity <- data.frame(hasPRActivity = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in hasPRActivityCategories){
  iterator <- iterator + 1
  results_hasPRActivity[iterator, ] <- list(entry, 
                                     sum(scoreComparison[which(scoreComparison$hasPRActivity == entry),16]), 
                                     mean(scoreComparison[which(scoreComparison$hasPRActivity == entry),16]), 
                                     quantile((scoreComparison[which(scoreComparison$hasPRActivity == entry),16]), probs = 0.05))
}

## By travelledDistance - TO DO

travelledDistanceBins <- c("0 - 50.000", "50.000 - 100.000")
results_travelledDistance <- data.frame(travelledDistance = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())

# Export CSV - TO DO

write.table(results_livingLocation,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_by_living_location.tsv",row.names = FALSE, sep = "\t")
write.table(results_income,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_by_income.tsv",row.names = FALSE, sep = "\t")
write.table(results_longestDistanceMode,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_by_longestDistance_mode.tsv",row.names = FALSE, sep = "\t")
write.table(results_noOfActivities,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_by_no_of_activities.tsv",row.names = FALSE, sep = "\t")
write.table(results_hasPRActivity,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_by_PR_activity.tsv",row.names = FALSE, sep = "\t")
write.table(results_general,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/ohne_ausreißer/score_general.tsv" ,row.names = FALSE, sep = "\t")

# Graphs

## Plot tryouts
ggplot(scoreComparison, aes(x = income, y = comparedScore)) +
  geom_line()


