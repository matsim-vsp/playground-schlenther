library(dplyr)
library(tidyverse)
library(lubridate)
library(ggplot2)

scoresNoCars <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoresBaseCase <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans_selectedPlanScores.tsv', sep = '\t', header = TRUE)

scoreComparison <- merge(scoresNoCars, scoresBaseCase, by = "agentId") %>%
  add_column(comparedScore = scoreComparison$selectedPlanScore.x - scoreComparison$selectedPlanScore.y)

scoreComparisonBerlinOnly <- scoreComparison %>%
  filter(scoreComparison$livesInBerlin.x == "true")

onlyNegativeScore <- scoreComparison %>%
  filter(scoreComparison$comparedScore < 0)


## Boxplots for every region
# Berlin vs Brandenburg
ggplot(scoreComparison, aes(x = livesInBerlin.x, y = comparedScore)) +
  geom_boxplot(outlier.shape = NA) +
  coord_cartesian(ylim =  c(-0.01, 0.01))

# Berlin Inner City vs Rest of Berlin/Brandenburg
ggplot(scoreComparison, aes(x = livesInInnerCity.x, y = comparedScore)) +
  geom_boxplot(outlier.shape = NA) +
  coord_cartesian(ylim =  c(-5, 5))

# Berlin: Inner City vs Rest of Berlin
ggplot(scoreComparisonBerlinOnly, aes(x = livesInInnerCity.x, y = comparedScore)) +
  geom_boxplot(outlier.shape = NA) +
  coord_cartesian(ylim =  c(-0.01, 0.01))


## Export CSV with results - TO DO
