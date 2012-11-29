dcpm.jar:
	lein uberjar

dcpm: dcpm.jar 

clean:
	-rm dcpm.jar

edit:
	vim -S ~/pfiles/dc.vim  

run: clean dcpm	
	java -jar dcpm.jar 

