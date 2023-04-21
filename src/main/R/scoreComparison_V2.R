require(matsim)
library(matsim)
library(tidyverse)
library(dplyr)
library(ggalluvial)
library(lubridate)
library(plotly)
library(sf)

########################################
# Preparation
# Open questions: Sollen Ausreißer generell vor der Analyse herausgefiltert werden?

shp <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

baseCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/"
policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutsideActivity/inside-allow-0.5-1506vehicles-8seats/"

basePersons <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)
"basePersons <- readPersonsTable(baseCaseDirectory)"
baseTrips <- readTripsTable(baseCaseDirectory)

policyPersons <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutsideActivity/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)
"policyPersons <- readPersonsTable(policyCaseDirectory)"
policyTrips <- readTripsTable(policyCaseDirectory)

########################################
# Tests

" NonZero-Test "
baseNonZero <- basePersons %>%
  filter(executed_score != 0)
policyNonZero <- policyPersons %>%
  filter(executed_score != 0)
if(! count(baseNonZero) == count(policyNonZero) ) {
  warning("base case has a different number of non-active/non-mobile persons than policy case !!")
}

personsJoined <- merge(policyPersons, basePersons, by = "person", suffixes = c("_policy","_base")) %>% 
  add_column(score_diff = personsJoined$executed_score_policy - personsJoined$executed_score_base)

########################################
# General results

"General metrics"
mean(personsJoined$score_diff)
sd(personsJoined$score_diff)
quantile(personsJoined$score_diff, probs = 0.05)

"Results table"
results_general <- data.frame(avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
results_general[1,] <- list(mean(personsJoined$score_diff), quantile(personsJoined$score_diff, probs = 0.05),sd(personsJoined$score_diff))

"Histogram"
ggplot(personsJoined, aes(x = score_diff)) +
  geom_histogram(binwidth = 5) +
  labs(
    title = "Distribution of score differences",
    subtitle = "General results (policy vs base)",
    caption = "score_delta = score(policy) - score(base)",
    x = "score_delta"
  )+
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic")
  )
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/histogram_general.png")

"Boxplot"
ggplot(personsJoined, aes(y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_general.png")

########################################
# Results by hasPRActivity

hasPRActivityCategories <- unique(personsJoined$hasPRActivity_policy)
results_hasPRActivity <- data.frame(hasPRActivity = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
iterator = 0

"Results table + Histograms"
for (entry in hasPRActivityCategories){
  iterator <- iterator + 1
  results_hasPRActivity[iterator, ] <- list(entry, 
                                            mean(personsJoined[which(personsJoined$hasPRActivity_policy == entry),16]), 
                                            quantile((personsJoined[which(personsJoined$hasPRActivity_policy == entry),16]), probs = 0.05), 
                                            sd(personsJoined[which(personsJoined$hasPRActivity_policy == entry),16])
                                            )
  relevant_persons <- personsJoined %>%
    filter(hasPRActivity_policy == entry)
  
  ggplot(relevant_persons, aes(x = score_diff)) +
    geom_histogram(binwidth = 5) +
    labs(
      title = "Distribution of score differences",
      subtitle = paste("hasPRActivity =",entry),
      caption = "score_delta = score(policy) - score(base)",
      x = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic")
    )
  ggsave(paste("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/histogram_hasPRActivity_",entry,".png",sep=""))
}


"Boxplot"
ggplot(personsJoined, aes(x = hasPRActivity_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "by hasPRActivity (policy)",
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_hasPRActivity.png")

########################################
# Results by homeActivityZone

homeActivityZoneCategories <- unique(personsJoined$home.activity.zone_policy)
results_homeActivityZone <- data.frame( homeActivityZone = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
iterator = 0

"Results table + Histograms"
for (entry in homeActivityZoneCategories){
  iterator <- iterator + 1
  results_homeActivityZone[iterator, ] <- list(entry, 
                                            mean(personsJoined[which(personsJoined$home.activity.zone_policy == entry),16]), 
                                            quantile((personsJoined[which(personsJoined$home.activity.zone_policy == entry),16]), probs = 0.05), 
                                            sd(personsJoined[which(personsJoined$home.activity.zone_policy == entry),16])
  )
  
  relevant_persons <- personsJoined %>%
    filter(home.activity.zone_policy == entry)
  
  ggplot(relevant_persons, aes(x = score_diff)) +
    geom_histogram(binwidth = 5) +
    labs(
      title = "Distribution of score differences",
      subtitle = paste("homeActivityZone =",entry),
      caption = "score_delta = score(policy) - score(base)",
      x = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic")
    )
  ggsave(paste("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/histogram_homeActivityZone_",entry,".png",sep=""))
}


"Boxplot"
ggplot(personsJoined, aes(x = home.activity.zone_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "by homeActivityZone (policy)",
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_homeActivityZone.png")

########################################
# Results by noOfActivities

noOfActivitiesCategories <- unique(personsJoined$noOfActivities_policy)
results_noOfActivities <- data.frame(noOfActivities = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
iterator = 0

"Results table"
for (entry in noOfActivitiesCategories){
  iterator <- iterator + 1
  results_noOfActivities[iterator, ] <- list(entry, 
                                            mean(personsJoined[which(personsJoined$noOfActivities_policy == entry),16]), 
                                            quantile((personsJoined[which(personsJoined$noOfActivities_policy == entry),16]), probs = 0.05), 
                                            sd(personsJoined[which(personsJoined$noOfActivities_policy == entry),16])
  )
}


"Boxplot"
ggplot(personsJoined, aes(x = noOfActivities_policy, group = noOfActivities_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "by noOfActivities (policy)",
    caption = "score_delta = score(policy) - score(base)",
    y = "score_delta"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic")
  )
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_noOfActivities.png")

########################################
# Results by travelledDistance

"Boxplot"
options(scipen = 999)
personsJoined_bins <- personsJoined %>%
  mutate(bin = cut_width(travelledDistance_policy, width = 25000, boundary = 0, dig.lab = 50))

ggplot(personsJoined_bins, aes(x = bin, group = bin, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_travelledDistance.png")


########################################
# Results by longestDistanceMode [without "", "freight"]

ldmc_helper <- unique(personsJoined$longestDistanceMode_policy)
remove <- c("","freight")
longestDistanceModeCategories <- ldmc_helper [! ldmc_helper %in% remove]
results_longestDistanceMode <- data.frame(longestDistanceMode = character(), avg_score_diff = numeric(), pt95_score_diff = numeric(), sd_score_diff = numeric())
iterator = 0

"Results table + Histograms"
for (entry in longestDistanceModeCategories){
  iterator <- iterator + 1
  results_longestDistanceMode[iterator, ] <- list(entry, 
                                            mean(personsJoined[which(personsJoined$longestDistanceMode_policy == entry),16]), 
                                            quantile((personsJoined[which(personsJoined$longestDistanceMode_policy == entry),16]), probs = 0.05), 
                                            sd(personsJoined[which(personsJoined$longestDistanceMode_policy == entry),16])
  )
  relevant_persons <- personsJoined %>%
    filter(longestDistanceMode_policy == entry)
  
  ggplot(relevant_persons, aes(x = score_diff)) +
    geom_histogram(binwidth = 5) +
    labs(
      title = "Distribution of score differences",
      subtitle = paste("longestDistanceMode =",entry),
      caption = "score_delta = score(policy) - score(base)",
      x = "score_delta"
    ) +
    theme_classic() +
    theme(
      plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
      plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
      plot.caption = element_text(face = "italic")
    )
  ggsave(paste("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/histogram_longestDistanceMode_",entry,".png",sep=""))
}


"Boxplot"
ggplot(personsJoined, aes(x = longestDistanceMode_policy, y = score_diff)) +
  geom_boxplot(fill = "#0099f8") +
  labs(
    title = "Distribution of score differences",
    subtitle = "by longestDistanceMode (policy)",
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
ggsave("C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/boxplot_longestDistanceMode.png")

########################################
# Dump results tables

write.table(results_homeActivityZone,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/score_by_homeActivityZone.tsv",row.names = FALSE, sep = "\t")
write.table(results_longestDistanceMode,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/score_by_longestDistanceMode.tsv",row.names = FALSE, sep = "\t")
write.table(results_noOfActivities,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/score_by_noOfActivities.tsv",row.names = FALSE, sep = "\t")
write.table(results_hasPRActivity,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/score_by_hasPRActivity.tsv",row.names = FALSE, sep = "\t")
write.table(results_general,"C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/analysis/scores/OutsideVsBase/score_general.tsv" ,row.names = FALSE, sep = "\t")


