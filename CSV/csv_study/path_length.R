library(dplyr)
library(ggplot2)

objectFromCSVFile <- function(file){
  return(read.csv(file, header = T))
}

getquantile_99 <- function(x){
  return(quantile(x, probs = (0.95)))
}

getquantile_01 <- function(x){
  return(quantile(x, probs = (0.05)))
}

folder1 <- '../data'
fileList1 <- list.files(folder1,full.names = TRUE)
print(fileList1)
csv_obj_8 = objectFromCSVFile(fileList1[8])
csv_obj_16 = objectFromCSVFile(fileList1[3])
csv_obj_32 = objectFromCSVFile(fileList1[5])
csv_obj_64 = objectFromCSVFile(fileList1[7])
csv_obj_128 = objectFromCSVFile(fileList1[2])
csv_obj_256 = objectFromCSVFile(fileList1[4])
csv_obj_512 = objectFromCSVFile(fileList1[6])
csv_obj_1024 = objectFromCSVFile(fileList1[1])

mean_keys_number <- c("8","16","32","64","128","256","512","1024")
mean_keys_value <- c()

mean_keys_value <- c(mean_keys_value, mean(csv_obj_8$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_16$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_32$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_64$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_128$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_256$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_512$path_lengh))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_1024$path_lengh))

mean_keys_quantile <- c()

mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_8$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_16$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_32$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_64$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_128$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_256$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_512$path_lengh))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_1024$path_lengh))

mean_keys_quantile_lower <- c()

mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_8$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_16$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_32$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_64$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_128$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_256$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_512$path_lengh))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_1024$path_lengh))

mean_dataframe <- data.frame(mean_keys_number, mean_keys_value, "upper" = mean_keys_quantile, "lower"=mean_keys_quantile_lower)

mean_dataframe$mean_keys_number <- factor(mean_dataframe$mean_keys_number, levels = mean_dataframe$mean_keys_number)

ggplot(mean_dataframe, aes(x = mean_keys_number, y = mean_keys_value, group = 1)) +
  geom_line() + geom_point() + geom_errorbar(aes(ymin=lower, ymax=upper,width=0.2)) + theme_classic() +
  labs(x = "# Nodes in the ring", y = "Path length", 
       title = "Path length to reach a key")+ 
  theme(plot.title = element_text(hjust = 0.5, size = 20), 
        axis.title.x = element_text(face="bold", size = 17),
        axis.title.y = element_text(face="bold", size = 17),
        axis.text = element_text(size = 14))


d <- density(csv_obj_1024$path_lengh) # returns the density data
plot(d, main="PDF for the path length",
     xlab="Path length",
     ylab="PDF")