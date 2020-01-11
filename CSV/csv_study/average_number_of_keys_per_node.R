library(dplyr)
library(ggplot2)

objectFromCSVFile <- function(file){
  return(read.csv(file, header = T))
}

quantile_95 <- function(x){
  r<-quantile(x, probs=c(0.01, 0.05, 0.5, 0.95, 0.99))
  names(r)<- c("ymin", "lower", "middle", "upper", "ymax")
  return(r)
}

getquantile_99 <- function(x){
  return(quantile(x, probs = (0.95)))
}

getquantile_01 <- function(x){
  return(quantile(x, probs = (0.05)))
}

folder1 <- '../num_di_chiavi_per_nodo'
fileList1 <- list.files(folder1,full.names = TRUE)
print(fileList1)
csv_obj_10 = objectFromCSVFile(fileList1[2])
csv_obj_20 = objectFromCSVFile(fileList1[3])
csv_obj_30 = objectFromCSVFile(fileList1[4])
csv_obj_40 = objectFromCSVFile(fileList1[5])
csv_obj_50 = objectFromCSVFile(fileList1[6])
csv_obj_60 = objectFromCSVFile(fileList1[7])
csv_obj_70 = objectFromCSVFile(fileList1[8])
csv_obj_80 = objectFromCSVFile(fileList1[9])
csv_obj_90 = objectFromCSVFile(fileList1[10])
csv_obj_100 = objectFromCSVFile(fileList1[1])

mean_keys_number <- c("10K","20K","30K","40K","50K","60K","70K","80K","90K","100K")
mean_keys_value <- c()

mean_keys_value <- c(mean_keys_value, mean(csv_obj_10$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_20$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_30$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_40$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_50$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_60$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_70$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_80$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_90$keys_size))
mean_keys_value <- c(mean_keys_value, mean(csv_obj_100$keys_size))

mean_keys_quantile <- c()

mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_10$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_20$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_30$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_40$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_50$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_60$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_70$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_80$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_90$keys_size))
mean_keys_quantile <- c(mean_keys_quantile, getquantile_99(csv_obj_100$keys_size))

mean_keys_quantile_lower <- c()

mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_10$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_20$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_30$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_40$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_50$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_60$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_70$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_80$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_90$keys_size))
mean_keys_quantile_lower <- c(mean_keys_quantile_lower, getquantile_01(csv_obj_100$keys_size))

mean_dataframe <- data.frame(mean_keys_number, mean_keys_value, "upper" = mean_keys_quantile, "lower"=mean_keys_quantile_lower)

mean_dataframe$mean_keys_number <- factor(mean_dataframe$mean_keys_number, levels = mean_dataframe$mean_keys_number)

ggplot(mean_dataframe, aes(x = mean_keys_number, y = mean_keys_value, group = 1)) +
  geom_line() + geom_point() + geom_errorbar(aes(ymin=lower, ymax=upper,width=0.2)) + theme_classic() +
  labs(x = "# Keys inserted in the system", y = "# Keys per node", 
       title = "Keys assigned to nodes")+ 
  theme(plot.title = element_text(hjust = 0.5, size = 20), 
        axis.title.x = element_text(face="bold", size = 17),
        axis.title.y = element_text(face="bold", size = 17),
        axis.text = element_text(size = 14))

d <- density(csv_obj_100$keys_size) # returns the density data
plot(d, main="PDF for the number of keys in a node",
     xlab="Number of keys",
     ylab="PDF")
