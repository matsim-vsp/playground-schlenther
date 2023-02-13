# MATSim R Analysis of Car-reduced Berlin Scenario

setwd("~/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/output/output-1pct")
library(tidyverse)
library(lubridate)
library(sf)

# Preparation

install.packages("devtools")

devtools::install_github("matsim-vsp/matsim-r", force = TRUE)

hundekopf_shape <- st_read("C:/Users/loren/Documents/TU_Berlin/Semester_5/Masterarbeit/Berlin_autoreduziert/scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp")

### Part 1: Amount of PR-Stations

## DRT Customer Stats

pr_old_drt_stats <- read.csv2("./pr-old/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1497vehicles-8seats/drt_customer_stats_drt.csv")
pr_new_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")

pr_wait_average_compare <- rbind(
  filter(pr_old_drt_stats, iteration == 500), 
  filter(pr_new_drt_stats, iteration == 500)
  )

view(head(pr_wait_average_compare))

## Total Travel Time

pr_old_trips <- readTripsTable("./pr-old/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1497vehicles-8seats/output_trips.csv.gz")

pr_new_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")

pr_totaltraveltime_compare <- rbind(
  sum(pr_old_trips$trav_time),
  sum(pr_new_trips$trav_time),
  sum(pr_old_trips$trav_time) - sum(pr_new_trips$trav_time)
)

view(pr_totaltraveltime_compare)

# Quellverkehr

pr_old_quelle_coord <- st_as_sf(pr_old_trips, coords = c("start_x", "start_y"))
st_crs(pr_old_quelle_coord) = 25832
pr_old_quelle_coord <- st_transform(pr_old_quelle_coord, crs = 25832)

view(head(pr_old_quelle_coord))

pr_old_test <- pr_old_quelle_coord[st_transform(pr_old_quelle_coord$geometry,hundekopf_shape, sparse = F)]

# Zielverkehr

# Binnenverkehr


## Total Wait Time

pr_totalwaittime_compare <- rbind(
  sum(pr_old_trips$wait_time),
  sum(pr_new_trips$wait_time),
  sum(pr_old_trips$wait_time) - sum(pr_new_trips$wait_time)
)

view(pr_totalwaittime_compare)

view(head(pr_old_trips))

### Part 2: PR_Station_Choice

## DRT Customer Stats

pr_outside_drt_stats <- read.csv2("./pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")
pr_inside_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")

stationchoice_wait_average_compare <- rbind(
  filter(pr_outside_drt_stats, iteration == 500), 
  filter(pr_inside_drt_stats, iteration == 500)
)

view(head(stationchoice_wait_average_compare))

## Total Travel Time

pr_outside_trips <- readTripsTable("./pr-new/closestToOutSideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")

pr_inside_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")

stationchoice_totaltraveltime_compare <- rbind(
  sum(pr_outside_trips$trav_time),
  sum(pr_inside_trips$trav_time),
  sum(pr_outside_trips$trav_time) - sum(pr_inside_trips$trav_time)
)

view(stationchoice_totaltraveltime_compare)

# Quellverkehr

pr_old_quelle_coord <- st_as_sf(pr_old_trips, coords = c("start_x", "start_y"))
st_crs(pr_old_quelle_coord) <- "DHDN / 3-degree Gauss-Kruger zone 4"

"for (row in 1:nrow(pr_old_quelle_coord)) {
  # if coordinate inside hundekopf_shape, keep row, else delete
}"

# Zielverkehr

# Binnenverkehr


## Total Wait Time

stationchoice_totalwaittime_compare <- rbind(
  sum(pr_outside_trips$wait_time),
  sum(pr_inside_trips$wait_time),
  sum(pr_outside_trips$wait_time) - sum(pr_inside_trips$wait_time)
)

view(stationchoice_totalwaittime_compare)



### Part 3: Initiale Verteilung DRT

## DRT Customer Stats

pr_33_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.33/inside-allow-0.33-1491vehicles-8seats/drt_customer_stats_drt.csv")
pr_50_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")

drtloc_wait_average_compare <- rbind(
  filter(pr_33_drt_stats, iteration == 500), 
  filter(pr_50_drt_stats, iteration == 500)
)

view(head(drtloc_wait_average_compare))

## Total Travel Time

pr_33_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.33/inside-allow-0.33-1491vehicles-8seats/output_trips.csv.gz")

pr_50_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")

drtloc_totaltraveltime_compare <- rbind(
  sum(pr_33_trips$trav_time),
  sum(pr_50_trips$trav_time),
  sum(pr_33_trips$trav_time) - sum(pr_50_trips$trav_time)
)

view(drtloc_totaltraveltime_compare)

# Quellverkehr

pr_old_quelle_coord <- st_as_sf(pr_old_trips, coords = c("start_x", "start_y"))
st_crs(pr_old_quelle_coord) <- "DHDN / 3-degree Gauss-Kruger zone 4"

"for (row in 1:nrow(pr_old_quelle_coord)) {
  # if coordinate inside hundekopf_shape, keep row, else delete
}"

# Zielverkehr

# Binnenverkehr


## Total Wait Time

drtloc_totalwaittime_compare <- rbind(
  sum(pr_33_trips$wait_time),
  sum(pr_50_trips$wait_time),
  sum(pr_33_trips$wait_time) - sum(pr_50_trips$wait_time)
)

view(drtloc_totalwaittime_compare)

view(head(pr_old_trips))



### Part 4: Cars allowed on road types inside ban area

## DRT Customer Stats

pr_motorway_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/motorway/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")
pr_nowhere_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/nowhere/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")
pr_normal_drt_stats <- read.csv2("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/drt_customer_stats_drt.csv")

roadtypes_wait_average_compare <- rbind(
  filter(pr_motorway_drt_stats, iteration == 500), 
  filter(pr_nowhere_drt_stats, iteration == 500),
  filter(pr_normal_drt_stats, iteration == 500)
)

view(head(roadtypes_wait_average_compare))

## Total Travel Time

pr_motorway_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/motorway/inside-allow-0.5-1506vehicles-8seats/output_trips.csv.gz")
pr_nowhere_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/nowhere/inside-allow-0.5-1506vehicles-8seats/output_trips.csv.gz")
pr_normal_trips <- readTripsTable("./pr-new/closestToInsideActivity/shareVehAtStations-0.5/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_trips.csv.gz")

roadtypes_totaltraveltime_compare <- rbind(
  sum(pr_motorway_trips$trav_time),
  sum(pr_nowhere_trips$trav_time),
  sum(pr_normal_trips$trav_time)
)

view(roadtypes_totaltraveltime_compare)

# Quellverkehr

pr_old_quelle_coord <- st_as_sf(pr_old_trips, coords = c("start_x", "start_y"))
st_crs(pr_old_quelle_coord) <- "DHDN / 3-degree Gauss-Kruger zone 4"

"for (row in 1:nrow(pr_old_quelle_coord)) {
  # if coordinate inside hundekopf_shape, keep row, else delete
}"

# Zielverkehr

# Binnenverkehr


## Total Wait Time

roadtypes_totalwaittime_compare <- rbind(
  sum(pr_motorway_trips$wait_time),
  sum(pr_nowhere_trips$wait_time),
  sum(pr_normal_trips$wait_time)
)

view(roadtypes_totalwaittime_compare)










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

