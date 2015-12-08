chooseIdea = function(dat) {
	n = ncol(dat)
	names = colnames(dat)
	for(i in 1:n){
		tmser = ts(dat[i], frequency = 1, start = c(1993, 1))
		b = predict(arima(tmser, order = c(0,1,1)),1)
		print(names[i])
		print(b[1])
	}
}

