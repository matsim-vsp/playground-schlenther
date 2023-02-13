# MATSim R Analysis of Car-reduced Berlin Scenario

setwd("~/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct")
library(tidyverse)
library(lubridate)
library(sf)

# Preparation

install.packages("devtools")

devtools::install_github("matsim-vsp/matsim-r", force = TRUE)

hundekopf_shape <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")


# Read trips file

pr_00 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.0/inside-allow-0.0-1500vehicles-8seats/inside-allow-0.0-1500vehicles-8seats.output_trips.csv.gz")
pr_01 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.1/inside-allow-0.1-1512vehicles-8seats/inside-allow-0.1-1512vehicles-8seats.output_trips.csv.gz")
pr_02 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.2/inside-allow-0.2-1497vehicles-8seats/inside-allow-0.2-1497vehicles-8seats.output_trips.csv.gz")
pr_03 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.3/inside-allow-0.3-1509vehicles-8seats/inside-allow-0.3-1509vehicles-8seats.output_trips.csv.gz")
pr_04 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.4/inside-allow-0.4-1494vehicles-8seats/inside-allow-0.4-1494vehicles-8seats.output_trips.csv.gz")
pr_05 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")
pr_06 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.6/inside-allow-0.6-1491vehicles-8seats/inside-allow-0.6-1491vehicles-8seats.output_trips.csv.gz")
pr_07 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.7/inside-allow-0.7-1503vehicles-8seats/inside-allow-0.7-1503vehicles-8seats.output_trips.csv.gz")
pr_08 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.8/inside-allow-0.8-1488vehicles-8seats/inside-allow-0.8-1488vehicles-8seats.output_trips.csv.gz")
pr_09 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.9/inside-allow-0.9-1500vehicles-8seats/inside-allow-0.9-1500vehicles-8seats.output_trips.csv.gz")
pr_10 <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-1.0/inside-allow-1.0-1512vehicles-8seats/inside-allow-1.0-1512vehicles-8seats.output_trips.csv.gz")

# Total travel time


Scenario <- c("0.0","0.1","0.2","0.3","0.4","0.5","0.6","0.7","0.8","0.9","1.0")
total_trav_time <- c(sum(pr_00$trav_time) , sum(pr_01$trav_time) , sum(pr_02$trav_time), sum(pr_03$trav_time), sum(pr_04$trav_time), sum(pr_05$trav_time), sum(pr_06$trav_time), sum(pr_07$trav_time), sum(pr_08$trav_time), sum(pr_09$trav_time), sum(pr_10$trav_time))
total_wait_time <- c(sum(pr_00$wait_time), sum(pr_01$wait_time), sum(pr_02$wait_time), sum(pr_03$wait_time), sum(pr_04$wait_time), sum(pr_05$wait_time), sum(pr_06$wait_time), sum(pr_07$wait_time), sum(pr_08$wait_time), sum(pr_09$wait_time), sum(pr_10$wait_time))

drt_dist <- data.frame (Scenario, total_trav_time, total_wait_time)

view(drt_dist)

write.csv(drt_dist, ".DRTDistribution.csv")

# Total wait time



### All functions back-end

matsimDumpOutputDirectory <- "./matsim_r_output"
dashboard_file <- "/dashboard-1-trips.yaml"

#' Load MATSIM output_trips table into Memory
#'
#' Loads a MATSim CSV output_trips from file or archive,
#' creating a tibble with columns as in csv file
#'
#'
#'
#'
#'
#' @param pathToMATSimOutputDirectory character string, path to matsim output directory or http link to the file.
#'
#' @return tibble of trips_output
#'
#' @export
readTripsTable <- function(pathToMATSimOutputDirectory = ".") {
  # Get the file names, output_trips should be there
  options(digits = 18)
  # Read from URL
  if (grepl("http", pathToMATSimOutputDirectory) == TRUE) {
    trips_output_table <- read_delim(pathToMATSimOutputDirectory,
                                     delim = ";",
                                     col_types = cols(
                                       start_x = col_character(),
                                       start_y = col_character(), end_x = col_character(),
                                       end_y = col_character(),
                                       end_link = col_character(),
                                       start_link = col_character()
                                     )
    )
    
    trips_output_table <- trips_output_table %>% mutate(
      start_x = as.double(start_x),
      start_y = as.double(start_y),
      end_x = as.double(end_x),
      end_y = as.double(end_y)
    )
    attr(trips_output_table,"table_name") <- pathToMATSimOutputDirectory
    return(trips_output_table)
  }
  if (grepl("output_trips.csv.gz$", pathToMATSimOutputDirectory) == TRUE) {
    trips_output_table <- read_csv2(pathToMATSimOutputDirectory,
                                    col_types = cols(
                                      start_x = col_character(),
                                      start_y = col_character(),
                                      end_x = col_character(),
                                      end_y = col_character(),
                                      end_link = col_character(),
                                      start_link = col_character()
                                    )
    )
    # person is mostly integer, but contains also chars(see Hamburg 110813 observation)
    # doesn't reads coordinates correctly
    trips_output_table <- trips_output_table %>% mutate(
      start_x = as.double(start_x),
      start_y = as.double(start_y),
      end_x = as.double(end_x),
      end_y = as.double(end_y)
    )
    attr(trips_output_table,"table_name") <- pathToMATSimOutputDirectory
    return(trips_output_table)
  }
  
  files <- list.files(pathToMATSimOutputDirectory, full.names = TRUE)
  # Read from global/local directory
  # output_trips is contained as output_trips.csv.gz
  if (length(grep("output_trips.csv.gz$", files)) != 0) {
    trips_output_table <- read_csv2(files[grep("output_trips.csv.gz$", files)],
                                    col_types = cols(
                                      start_x = col_character(),
                                      start_y = col_character(),
                                      end_x = col_character(),
                                      end_y = col_character(),
                                      end_link = col_character(),
                                      start_link = col_character()
                                    )
    )
    # person is mostly integer, but contains also chars(see Hamburg 110813 observation)
    # doesn't reads coordinates correctly
    trips_output_table <- trips_output_table %>% mutate(
      start_x = as.double(start_x),
      start_y = as.double(start_y),
      end_x = as.double(end_x),
      end_y = as.double(end_y)
    )
    attr(trips_output_table,"table_name") <- files[grep("output_trips.csv.gz$", files)]
    return(trips_output_table)
  } else { # if Directory doesn't contain trips_output, then nothing to read
    return(NULL)
  }
}
