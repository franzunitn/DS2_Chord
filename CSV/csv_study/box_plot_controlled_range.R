library(dplyr)
library(ggplot2)

objectFromCSVFile <- function(file){
  return(read.csv(file, header = T))
}

folder1 <- '..'
fileList1 <- list.files(folder1,full.names = TRUE)
print(fileList1)
csv_obj_30_nodes = objectFromCSVFile(fileList1[3])
csv_obj_100_nodes = objectFromCSVFile(fileList1[1])
csv_obj_1000_nodes = objectFromCSVFile(fileList1[2])

total_keys = 1461501637330902918203684832716283019655932542975

percentage_occupation_30 = c()
percentage_occupation_100 = c()
percentage_occupation_1000 = c()

graph <- c()
percentage <- c()

for (obj in csv_obj_100_nodes$range_id) {
  percentage_occupation_100 <- c(percentage_occupation_100, (obj*100)/total_keys)
  graph <- c(graph, "100_nodes")
  percentage <- c(percentage, (obj*100)/total_keys)
}

for (obj in csv_obj_1000_nodes$range_id) {
  percentage_occupation_1000 <- c(percentage_occupation_1000, (obj*100)/total_keys)
  graph <- c(graph, "1000_nodes")
  percentage <- c(percentage, (obj*100)/total_keys)
}

for (obj in csv_obj_30_nodes$range_id) {
  percentage_occupation_30 <- c(percentage_occupation_30, (obj*100)/total_keys)
  graph <- c(graph, "30_nodes")
  percentage <- c(percentage, (obj*100)/total_keys)
}

percentage_dataframe <- data.frame(graph, percentage)

ggplot(percentage_dataframe, aes(x = graph, y = percentage)) +
  geom_boxplot()+ theme_classic() +
  labs(x = "Topology dimension", y = "Competence percentage on the total ring [%]", 
       title = "Competence range of nodes")+ 
  theme(plot.title = element_text(hjust = 0.5, size = 20), 
        axis.title.x = element_text(face="bold", size = 17),
        axis.title.y = element_text(face="bold", size = 17),
        axis.text = element_text(size = 14))

boxplot(data.frame(Topology30 = percentage_occupation_30), data.frame(Topology100 = percentage_occupation_100), data.frame(Topology1000 = percentage_occupation_1000),
        main="Competence range of nodes",
        xlab="Topology dimension",
        ylab="Competence percentage on the total ring [%]",
        cex.lab = 1.35, cex.axis = 1.35, cex.main = 1.7)