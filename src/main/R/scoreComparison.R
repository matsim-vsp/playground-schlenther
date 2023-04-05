library(dplyr)
library(tidyverse)
library(lubridate)
library(ggplot2)

# Preparation

scoresNoCars <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoresBaseCase <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoreComparison <- merge(scoresNoCars, scoresBaseCase, by = "agentId", suffixes = c("",".baseCase"))
scoreComparison <- merge(scoresNoCars, scoresBaseCase, by = "agentId", suffixes = c("",".baseCase")) %>%
  add_column(comparedScore = scoreComparison$selectedPlanScore - scoreComparison$selectedPlanScore.baseCase)

# Writing results tables

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

## By main mode
mainModeCategories <- unique(scoreComparison$mainMode)
results_mainMode <- data.frame(mainMode = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in mainModeCategories){
  iterator <- iterator + 1
  results_mainMode[iterator, ] <- list(entry, 
                                     sum(scoreComparison[which(scoreComparison$mainMode == entry),16]), 
                                     mean(scoreComparison[which(scoreComparison$mainMode == entry),16]), 
                                     quantile((scoreComparison[which(scoreComparison$mainMode == entry),16]), probs = 0.05))
}

## By amount of activities 

activityAmountCategories <- unique(scoreComparison$activityAmount)
results_activityAmount <- data.frame(activityAmount = character(), total_cS = numeric(), avg_cS = numeric(), pt_95_cS = numeric())
iterator = 0

for (entry in activityAmountCategories){
  iterator <- iterator + 1
  results_activityAmount[iterator, ] <- list(entry, 
                                     sum(scoreComparison[which(scoreComparison$activityAmount == entry),16]), 
                                     mean(scoreComparison[which(scoreComparison$activityAmount == entry),16]), 
                                     quantile((scoreComparison[which(scoreComparison$activityAmount == entry),16]), probs = 0.05))
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

" only plot it as a graph? "


# Graphs

## Plot tryouts
ggplot(scoreComparison, aes(x = income, y = comparedScore)) +
  geom_line()



## Export CSV with results - TO DO
