library(tidyr)
library(tidyverse)
library(lubridate)
library(plotly)
library(hms)
library(readr)
library(sf)
library(dplyr)
library(matsim)
library(scales)

#HPC Cluster
args <- commandArgs(trailingOnly = TRUE)
policyCaseDirectory <- args[1]
policy_runId <- args[2]

# policyCaseDirectory <- "C:/Users/loren/Documents/TU_Berlin/Semester_6/Masterarbeit/scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-all/"
# policy_runId <- "roadtypesAllowed-all"

prStationUsage <- read.table(file = file.path(policyCaseDirectory, paste0(policy_runId,".output_events_carsInPrStationPerMinute.tsv")), sep = '\t', header = TRUE)
prStationActivities <- read.table(file = file.path(policyCaseDirectory, paste0(policy_runId,".output_events_activitiesPerPRStation.tsv")), sep = '\t', header = TRUE)
dir.create(paste0(policyCaseDirectory,"/analysis/prStations"))

# Reframe column names & convert to datetime objects
col_names <- colnames(prStationUsage)
new_col_names <- gsub("^X","",col_names)
new_col_names <- gsub("\\.",":",new_col_names)
colnames(prStationUsage) <- new_col_names

df_long <- pivot_longer(prStationUsage, cols = matches("^[0-9]"), names_to = "Variable", values_to = "Value")
df_long <- df_long %>%
  mutate(datetime_str = paste("2023-10-20 ", Variable))
df_long$datetime <- as.POSIXct(df_long$datetime_str, format = "%Y-%m-%d %H:%M", tz = "UTC")

# Maximale Auslastung insgesamt an allen P+R Stationen
onlyNumbers <- prStationUsage[,-1]
results_pr <- data.frame(key = character(), value = numeric()) %>%
  add_row(key = "maximale Auslastung", value = max(onlyNumbers))

# Facet-Plot: Tagesverläufe bei allen P+R-Stationen
ggplot(df_long, aes(x = datetime, y = Value, group = PRStation, color = PRStation)) +
  geom_line() +
  labs(x = "P+R Station", y = "Auslastung") +
  ggtitle("Auslastung Tagesganglinien P+R Stationen") +
  facet_wrap(~ PRStation, scales = "fixed") +
  scale_x_datetime(labels = date_format("%H:%M")) +
  theme(
    legend.position = "none",
    # text = element_text(size = 20) # Adjust the size to your preference
  )
ggsave(file.path(policyCaseDirectory,"/analysis/prStations/prStations_auslastung.png"))

write.table(results_pr,file.path(policyCaseDirectory,"/analysis/prStations/metrics_PR.tsv"),row.names = FALSE, sep = "\t")

