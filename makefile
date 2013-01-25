BUND=dcpm.app
COPY=cp 
BLD=build

dcpm.jar:
	lein uberjar

dcpm: dcpm.jar 

clean:
	-rm dcpm.jar
	-rm -Rf $(BLD)/$(BUND)

edit:
	vim -S ~/pfiles/dc.vim  

run: clean dcpm	
	java -jar dcpm.jar -r 1

app: dcpm.app dcpm
	-mkdir $(BLD)/dcpm.app
	-mkdir $(BLD)/$(BUND)/Contents
	-mkdir $(BLD)/$(BUND)/Contents/Resources
	-mkdir $(BLD)/$(BUND)/Contents/Resources/Java
	-mkdir $(BLD)/$(BUND)/Contents/MacOS
	$(COPY)/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub $(BLD)/$(BUND)/Contents/MacOS
	$(COPY)dcpm.jar $(BLD)/$(BUND)/Contents/Resources/Java
	$(COPY)osx/Info.plist $(BLD)/$(BUND)/Contents
	$(COPY)osx/dcpm.icns $(BLD)/$(BUND)/Contents/Resources
	$(COPY)osx/libmmj.jnilib $(BLD)/$(BUND)/Contents/Resources/Java 
	$(COPY)osx/mmj.jar $(BLD)/$(BUND)/Contents/Resources/Java

	echo APPL???? > $(BLD)/$(BUND)/Contents/PkgInfo
	SetFile -a B $(BLD)/$(BUND)

