library(matsim)
library(tidyverse)
library(dplyr)

agentsPerPRStation <- read.table(file = 'C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/closestToOutside-0.5-1506vehicles-8seats.output_events_agentsPerPRStation.tsv', sep = '\t', header = TRUE)

ggplot(agentsPerPRStation, aes(x = reorder(PRStation,Agents), y = Agents)) +
  geom_bar(stat = "identity") +
  labs(
    title = "Agents per PR Station"
  ) +
  theme_classic() +
  theme(
    plot.title = element_text(color = "#0099f8", size = 16, face = "bold", hjust = 0.5),
    plot.subtitle = element_text(face = "bold.italic", hjust = 0.5),
    plot.caption = element_text(face = "italic"),
    axis.text.x = element_text(angle = 90, vjust = 0.5, hjust = 1)
  )
