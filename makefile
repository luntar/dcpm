BUND=dcpm.app

dcpm.jar:
	lein uberjar

dcpm: dcpm.jar 

clean:
	-rm dcpm.jar
	-rm -Rf $(BUND)

edit:
	vim -S ~/pfiles/dc.vim  

run: clean dcpm	
	java -jar dcpm.jar -r 1

bund: dcpm
	-mkdir dcpm.app
	-mkdir $(BUND)/Contents
	-mkdir $(BUND)/Contents/Resources
	-mkdir $(BUND)/Contents/Resources/Java
	-mkdir $(BUND)/Contents/MacOS
	cp /System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub $(BUND)/Contents/MacOS
	cp dcpm.jar $(BUND)/Contents/Resources/Java
	cp Info.plist $(BUND)/Contents
	cp dcpm.icns $(BUND)/Contents/Resources
	echo APPL???? > $(BUND)/Contents/PkgInfo
	SetFile -a B $(BUND)

